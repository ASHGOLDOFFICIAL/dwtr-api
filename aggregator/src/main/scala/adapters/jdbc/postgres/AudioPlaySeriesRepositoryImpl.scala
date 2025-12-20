package org.aulune.aggregator
package adapters.jdbc.postgres


import adapters.jdbc.postgres.AudioPlaySeriesRepositoryImpl.handleConstraintViolation
import adapters.jdbc.postgres.metas.AudioPlayMetas.given
import domain.errors.AudioPlaySeriesConstraint
import domain.model.audioplay.series.AudioPlaySeriesFilterField as FilterField
import domain.model.audioplay.series.{
  AudioPlaySeries,
  AudioPlaySeriesFilterField,
  AudioPlaySeriesName,
}
import domain.repositories.AudioPlaySeriesRepository

import cats.MonadThrow
import cats.data.NonEmptyList
import cats.effect.MonadCancelThrow
import cats.syntax.all.given
import doobie.Transactor
import doobie.syntax.all.given
import org.aulune.commons.adapters.doobie.postgres.ErrorUtils.{
  checkIfPositive,
  checkIfUpdated,
  makeConstraintViolationConverter,
  toInternalError,
}
import org.aulune.commons.adapters.doobie.postgres.Metas.{
  nonEmptyStringMeta,
  uuidMeta,
  uuidsMeta,
}
import org.aulune.commons.adapters.doobie.queries.ListQueryMaker
import org.aulune.commons.filter.Filter
import org.aulune.commons.types.{NonEmptyString, Uuid}


/** [[AudioPlaySeriesRepository]] implementation for PostgreSQL. */
object AudioPlaySeriesRepositoryImpl:
  /** Builds an instance.
   *  @param transactor [[Transactor]] instance.
   *  @tparam F effect type.
   */
  def build[F[_]: MonadCancelThrow](
      transactor: Transactor[F],
  ): F[AudioPlaySeriesRepository[F]] = createSeriesTable
    .as(new AudioPlaySeriesRepositoryImpl[F](transactor))
    .transact(transactor)

  private val createSeriesTable = sql"""
    |CREATE TABLE IF NOT EXISTS audio_play_series (
    |  id   UUID         PRIMARY KEY,
    |  name VARCHAR(255) NOT NULL,
    |  CONSTRAINT audio_play_series_unique_id UNIQUE (id)
    |)""".stripMargin.update.run

  private val constraintMap = Map(
    "audio_play_series_unique_id" -> AudioPlaySeriesConstraint.UniqueId,
  )

  /** Converts constraint violations. */
  private def handleConstraintViolation[F[_]: MonadThrow, A] =
    makeConstraintViolationConverter[F, A, AudioPlaySeriesConstraint](
      constraintMap,
    )

end AudioPlaySeriesRepositoryImpl


private final class AudioPlaySeriesRepositoryImpl[F[_]: MonadCancelThrow](
    transactor: Transactor[F],
) extends AudioPlaySeriesRepository[F]:

  override def contains(id: Uuid[AudioPlaySeries]): F[Boolean] =
    sql"SELECT EXISTS (SELECT 1 FROM audio_play_series WHERE id = $id)"
      .query[Boolean]
      .unique
      .transact(transactor)
      .handleErrorWith(toInternalError)

  override def persist(elem: AudioPlaySeries): F[AudioPlaySeries] = sql"""
      |INSERT INTO audio_play_series (id, name)
      |VALUES (${elem.id}, ${elem.name})""".stripMargin.update.run
    .as(elem)
    .transact(transactor)
    .recoverWith(handleConstraintViolation)
    .handleErrorWith(toInternalError)

  override def get(id: Uuid[AudioPlaySeries]): F[Option[AudioPlaySeries]] =
    (selectBase ++ sql"WHERE aps.id = $id").stripMargin
      .query[SelectResult]
      .map(toAudioPlaySeries)
      .option
      .transact(transactor)
      .handleErrorWith(toInternalError)

  override def update(elem: AudioPlaySeries): F[AudioPlaySeries] = sql"""
      |UPDATE audio_play_series
      |SET name = ${elem.name}
      |WHERE id = ${elem.id}
      |""".stripMargin.update.run
    .flatMap(checkIfUpdated)
    .as(elem)
    .transact(transactor)
    .recoverWith(handleConstraintViolation)
    .handleErrorWith(toInternalError)

  override def delete(id: Uuid[AudioPlaySeries]): F[Unit] =
    sql"DELETE FROM audio_play_series WHERE id = $id".update.run
      .transact(transactor)
      .void
      .handleErrorWith(toInternalError)

  override def batchGet(
      ids: NonEmptyList[Uuid[AudioPlaySeries]],
  ): F[List[AudioPlaySeries]] = sql"""
    |SELECT aps.id, aps.name
    |FROM UNNEST(${ids.toList.toArray}) WITH ORDINALITY AS t(id, ord)
    |JOIN audio_play_series aps ON aps.id = t.id
    |ORDER BY t.ord
    """.stripMargin
    .query[SelectResult]
    .map(toAudioPlaySeries)
    .to[List]
    .transact(transactor)
    .handleErrorWith(toInternalError)

  override def list(
      count: Int,
      filter: Option[Filter[AudioPlaySeriesFilterField]],
  ): F[List[AudioPlaySeries]] =
    for
      _ <- checkIfPositive(count)
      query <- MonadThrow[F].fromEither(listQueryMaker.make(count, filter))
      result <- query.stripMargin
        .query[SelectResult]
        .map(toAudioPlaySeries)
        .to[List]
        .transact(transactor)
        .handleErrorWith(toInternalError)
    yield result

  override def search(
      query: NonEmptyString,
      limit: Int,
  ): F[List[AudioPlaySeries]] = checkIfPositive(limit) >> (selectBase ++ fr0"""
      |WHERE TO_TSVECTOR(aps.name) @@ PLAINTO_TSQUERY($query)
      |ORDER BY TS_RANK(TO_TSVECTOR(aps.name), PLAINTO_TSQUERY($query)) DESC
      |LIMIT $limit
      |""".stripMargin)
    .query[SelectResult]
    .map(toAudioPlaySeries)
    .to[List]
    .transact(transactor)
    .handleErrorWith(toInternalError)

  private type SelectResult = (
      Uuid[AudioPlaySeries],
      AudioPlaySeriesName,
  )

  private val selectBase = fr"""
    |SELECT aps.id, aps.name
    |FROM audio_play_series aps
    |""".stripMargin

  /** Makes audio play series from given data. */
  private def toAudioPlaySeries(
      uuid: Uuid[AudioPlaySeries],
      name: AudioPlaySeriesName,
  ): AudioPlaySeries = AudioPlaySeries.unsafe(
    id = uuid,
    name = name,
  )

  private val listQueryMaker = ListQueryMaker[FilterField](selectBase) {
    case FilterField.Id => NonEmptyString("id::text")
  }
