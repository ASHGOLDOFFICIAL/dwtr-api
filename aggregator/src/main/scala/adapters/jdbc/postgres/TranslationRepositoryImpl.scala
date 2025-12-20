package org.aulune.aggregator
package adapters.jdbc.postgres


import adapters.jdbc.postgres.TranslationRepositoryImpl.handleConstraintViolation
import adapters.jdbc.postgres.metas.SharedMetas.given
import adapters.jdbc.postgres.metas.TranslationMetas.given
import domain.errors.TranslationConstraint
import domain.model.audioplay.AudioPlay
import domain.model.shared.{ExternalResource, Language, SelfHostedLocation}
import domain.model.translation.{
  TranslatedTitle,
  Translation,
  TranslationField,
  TranslationType,
}
import domain.repositories.TranslationRepository

import cats.MonadThrow
import cats.effect.MonadCancelThrow
import cats.syntax.all.given
import doobie.Transactor
import doobie.implicits.toSqlInterpolator
import doobie.syntax.all.given
import org.aulune.commons.adapters.doobie.postgres.ErrorUtils.{
  checkIfPositive,
  checkIfUpdated,
  makeConstraintViolationConverter,
  toInternalError,
}
import org.aulune.commons.adapters.doobie.postgres.Metas.uuidMeta
import org.aulune.commons.adapters.doobie.queries.ListQueryMaker
import org.aulune.commons.filter.Filter
import org.aulune.commons.types.{NonEmptyString, Uuid}
import org.typelevel.log4cats.{Logger, LoggerFactory}


/** [[TranslationRepository]] implementation for PostgreSQL. */
object TranslationRepositoryImpl:
  /** Builds an instance.
   *  @param transactor [[Transactor]] instance.
   *  @tparam F effect type.
   */
  def build[F[_]: MonadCancelThrow: LoggerFactory](
      transactor: Transactor[F],
  ): F[TranslationRepository[F]] =
    given Logger[F] = LoggerFactory[F].getLogger
    for _ <- createTranslationsTable.transact(transactor)
    yield TranslationRepositoryImpl[F](transactor)

  private val createTranslationsTable = sql"""
    |CREATE TABLE IF NOT EXISTS translations (
    |  original_id   UUID    NOT NULL,
    |  id            UUID    NOT NULL PRIMARY KEY,
    |  title         TEXT    NOT NULL,
    |  type          INTEGER NOT NULL,
    |  language      TEXT    NOT NULL,
    |  self_host_uri TEXT,
    |  resources     JSONB   NOT NULL,
    |  CONSTRAINT translations_unique_id UNIQUE (id)
    |)""".stripMargin.update.run

  private val constraintMap = Map(
    "translations_unique_id" -> TranslationConstraint.UniqueId,
  )

  /** Converts constraint violations. */
  private def handleConstraintViolation[F[_]: MonadThrow, A] =
    makeConstraintViolationConverter[F, A, TranslationConstraint](
      constraintMap,
    )

end TranslationRepositoryImpl


private final class TranslationRepositoryImpl[F[_]: MonadCancelThrow](
    transactor: Transactor[F],
)(using
    logger: Logger[F],
) extends TranslationRepository[F]:

  override def contains(id: Uuid[Translation]): F[Boolean] = sql"""
    |SELECT EXISTS (
    |  SELECT 1 FROM translations
    |  WHERE id = $id
    |)""".stripMargin
    .query[Boolean]
    .unique
    .transact(transactor)
    .handleErrorWith(toInternalError)

  override def persist(
      elem: Translation,
  ): F[Translation] = sql"""
    |INSERT INTO translations (
    |  original_id, id,
    |  title, type, language,
    |  self_host_uri, resources
    |)
    |VALUES (
    |  ${elem.originalId}, ${elem.id},
    |  ${elem.title}, ${elem.translationType}, ${elem.language},
    |  ${elem.selfHostedLocation}, ${elem.externalResources}
    |)""".stripMargin.update.run
    .as(elem)
    .transact(transactor)
    .recoverWith(handleConstraintViolation)
    .handleErrorWith(toInternalError)

  override def get(
      id: Uuid[Translation],
  ): F[Option[Translation]] =
    val query = selectBase ++ fr0"WHERE id = $id"
    query
      .query[SelectResult]
      .map(toTranslation)
      .option
      .transact(transactor)
      .handleErrorWith(toInternalError)

  override def update(
      elem: Translation,
  ): F[Translation] = sql"""
      |UPDATE translations
      |SET original_id   = ${elem.originalId},
      |    title         = ${elem.title},
      |    type          = ${elem.translationType},
      |    language      = ${elem.language},
      |    self_host_uri = ${elem.selfHostedLocation},
      |    resources     = ${elem.externalResources}
      |WHERE id = ${elem.id}
      |""".stripMargin.update.run
    .flatMap(checkIfUpdated)
    .as(elem)
    .transact(transactor)
    .recoverWith(handleConstraintViolation)
    .handleErrorWith(toInternalError)

  override def delete(
      id: Uuid[Translation],
  ): F[Unit] = sql"DELETE FROM translations WHERE id = $id".update.run.void
    .transact(transactor)
    .handleErrorWith(toInternalError)

  override def list(
      count: Int,
      filter: Option[Filter[TranslationField]],
  ): F[List[Translation]] = (for
    _ <- checkIfPositive(count)
    query <- MonadThrow[F].fromEither(listQueryMaker.make(count, filter))
    result <- query.stripMargin
      .query[SelectResult]
      .map(toTranslation)
      .to[List]
      .transact(transactor)
  yield result)
    .handleErrorWith(toInternalError)

  private type SelectResult = (
      Uuid[AudioPlay],
      Uuid[Translation],
      TranslatedTitle,
      TranslationType,
      Language,
      Option[SelfHostedLocation],
      List[ExternalResource],
  )

  private val selectBase = fr"""
    |SELECT 
    |  original_id, id,
    |  title, type, language,
    |  self_host_uri, resources
    |FROM translations""".stripMargin

  /** Makes translation from given data. */
  private def toTranslation(
      originalId: Uuid[AudioPlay],
      id: Uuid[Translation],
      title: TranslatedTitle,
      translationType: TranslationType,
      language: Language,
      selfHostLocation: Option[SelfHostedLocation],
      resources: List[ExternalResource],
  ): Translation = Translation.unsafe(
    originalId = originalId,
    id = id,
    title = title,
    translationType = translationType,
    language = language,
    selfHostedLocation = selfHostLocation,
    externalResources = resources,
  )

  private val listQueryMaker = ListQueryMaker[TranslationField](selectBase) {
    case TranslationField.Id         => NonEmptyString("id::text")
    case TranslationField.OriginalId => NonEmptyString("original_id::text")
  }
