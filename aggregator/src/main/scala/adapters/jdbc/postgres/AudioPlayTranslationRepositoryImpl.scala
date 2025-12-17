package org.aulune.aggregator
package adapters.jdbc.postgres


import adapters.jdbc.postgres.AudioPlayTranslationRepositoryImpl.handleConstraintViolation
import adapters.jdbc.postgres.metas.AudioPlayTranslationMetas.given
import adapters.jdbc.postgres.metas.SharedMetas.given
import domain.errors.TranslationConstraint
import domain.model.audioplay.translation.{
  AudioPlayTranslation,
  AudioPlayTranslationFilterField,
  AudioPlayTranslationType,
}
import domain.model.audioplay.{AudioPlay, translation}
import domain.model.shared.{
  ExternalResource,
  Language,
  SelfHostedLocation,
  TranslatedTitle,
}
import domain.repositories.AudioPlayTranslationRepository
import domain.repositories.AudioPlayTranslationRepository.AudioPlayTranslationCursor

import cats.MonadThrow
import cats.effect.MonadCancelThrow
import cats.syntax.all.given
import doobie.implicits.toSqlInterpolator
import doobie.syntax.all.given
import doobie.{Fragment, Transactor}
import org.aulune.commons.adapters.doobie.postgres.ErrorUtils.{
  checkIfPositive,
  checkIfUpdated,
  makeConstraintViolationConverter,
  toInternalError,
}
import org.aulune.commons.adapters.doobie.postgres.Metas.uuidMeta
import org.aulune.commons.filter.Filter
import org.aulune.commons.filter.instances.DoobieFragmentFilterEvaluator
import org.aulune.commons.repositories.RepositoryError
import org.aulune.commons.types.{NonEmptyString, Uuid}
import org.typelevel.log4cats.{Logger, LoggerFactory}


/** [[AudioPlayTranslationRepository]] implementation for PostgreSQL. */
object AudioPlayTranslationRepositoryImpl:
  /** Builds an instance.
   *  @param transactor [[Transactor]] instance.
   *  @tparam F effect type.
   */
  def build[F[_]: MonadCancelThrow: LoggerFactory](
      transactor: Transactor[F],
  ): F[AudioPlayTranslationRepository[F]] =
    given Logger[F] = LoggerFactory[F].getLogger
    for _ <- createTranslationsTable.transact(transactor)
    yield AudioPlayTranslationRepositoryImpl[F](transactor)

  private val createTranslationsTable = sql"""
    |CREATE TABLE IF NOT EXISTS audio_play_translations (
    |  original_id   UUID    NOT NULL,
    |  id            UUID    NOT NULL PRIMARY KEY,
    |  title         TEXT    NOT NULL,
    |  type          INTEGER NOT NULL,
    |  language      TEXT    NOT NULL,
    |  self_host_uri TEXT,
    |  resources     JSONB   NOT NULL,
    |  CONSTRAINT audio_play_translations_unique_id UNIQUE (id)
    |)""".stripMargin.update.run

  private val constraintMap = Map(
    "audio_play_translations_unique_id" -> TranslationConstraint.UniqueId,
  )

  /** Converts constraint violations. */
  private def handleConstraintViolation[F[_]: MonadThrow, A] =
    makeConstraintViolationConverter[F, A, TranslationConstraint](
      constraintMap,
    )

end AudioPlayTranslationRepositoryImpl


private final class AudioPlayTranslationRepositoryImpl[F[_]: MonadCancelThrow](
    transactor: Transactor[F],
)(using
    logger: Logger[F],
) extends AudioPlayTranslationRepository[F]:

  override def contains(id: Uuid[AudioPlayTranslation]): F[Boolean] = sql"""
    |SELECT EXISTS (
    |  SELECT 1 FROM audio_play_translations
    |  WHERE id = $id
    |)""".stripMargin
    .query[Boolean]
    .unique
    .transact(transactor)
    .handleErrorWith(toInternalError)

  override def persist(
      elem: AudioPlayTranslation,
  ): F[AudioPlayTranslation] = sql"""
    |INSERT INTO audio_play_translations (
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
      id: Uuid[AudioPlayTranslation],
  ): F[Option[AudioPlayTranslation]] =
    val query = selectBase ++ fr0"WHERE id = $id"
    query
      .query[SelectResult]
      .map(toTranslation)
      .option
      .transact(transactor)
      .handleErrorWith(toInternalError)

  override def update(
      elem: AudioPlayTranslation,
  ): F[AudioPlayTranslation] = sql"""
      |UPDATE audio_play_translations
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
      id: Uuid[AudioPlayTranslation],
  ): F[Unit] =
    sql"DELETE FROM audio_play_translations WHERE id = $id".update.run.void
      .transact(transactor)
      .handleErrorWith(toInternalError)

  override def list(
      count: Int,
      cursor: Option[AudioPlayTranslationCursor],
      filter: Option[Filter[AudioPlayTranslationFilterField]],
  ): F[List[AudioPlayTranslation]] = (for
    _ <- checkIfPositive(count)
    query <- MonadThrow[F]
      .fromEither(makeListQuery(count, cursor, filter))
      .flatTap(f => println(f.toString).pure)
    result <- query.stripMargin
      .query[SelectResult]
      .map(toTranslation)
      .to[List]
      .transact(transactor)
  yield result)
    .handleErrorWith(toInternalError)

  private def makeListQuery(
      count: Int,
      cursor: Option[AudioPlayTranslationCursor],
      filter: Option[Filter[AudioPlayTranslationFilterField]],
  ): Either[RepositoryError, Fragment] = filter
    .traverse(filterEvaluator.eval)
    .map { optFilterFragment =>
      val where = (cursor, optFilterFragment) match
        case (Some(c), Some(f)) => fr"WHERE (id > ${c.id}) AND" ++ f
        case (Some(c), None)    => fr0"WHERE (id > ${c.id})"
        case (None, Some(f))    => fr"WHERE" ++ f
        case (None, None)       => Fragment.empty
      selectBase ++ where ++ fr0" LIMIT $count"
    }
    .leftMap(_ => RepositoryError.InvalidArgument)

  private type SelectResult = (
      Uuid[AudioPlay],
      Uuid[AudioPlayTranslation],
      TranslatedTitle,
      AudioPlayTranslationType,
      Language,
      Option[SelfHostedLocation],
      List[ExternalResource],
  )

  private val selectBase = fr"""
    |SELECT 
    |  original_id, id,
    |  title, type, language,
    |  self_host_uri, resources
    |FROM audio_play_translations""".stripMargin

  /** Makes translation from given data. */
  private def toTranslation(
      originalId: Uuid[AudioPlay],
      id: Uuid[AudioPlayTranslation],
      title: TranslatedTitle,
      translationType: AudioPlayTranslationType,
      language: Language,
      selfHostLocation: Option[SelfHostedLocation],
      resources: List[ExternalResource],
  ): AudioPlayTranslation = AudioPlayTranslation.unsafe(
    originalId = originalId,
    id = id,
    title = title,
    translationType = translationType,
    language = language,
    selfHostedLocation = selfHostLocation,
    externalResources = resources,
  )

  private val filterEvaluator
      : DoobieFragmentFilterEvaluator[AudioPlayTranslationFilterField] =
    DoobieFragmentFilterEvaluator[AudioPlayTranslationFilterField] {
      case AudioPlayTranslationFilterField.OriginalId =>
        NonEmptyString("original_id::text")
    }
