package org.aulune.aggregator
package adapters.jdbc.postgres


import adapters.jdbc.postgres.WorkRepositoryImpl.handleConstraintViolation
import adapters.jdbc.postgres.metas.SharedMetas.given
import adapters.jdbc.postgres.metas.WorkMetas.given
import domain.errors.WorkConstraint
import domain.model.person.Person
import domain.model.series.Series
import domain.model.shared.ReleaseDate.DateAccuracy
import domain.model.shared.{
  ExternalResource,
  ImageUri,
  ReleaseDate,
  SelfHostedLocation,
  Synopsis,
}
import domain.model.work.{
  CastMember,
  EpisodeType,
  SeasonNumber,
  SeriesNumber,
  Title,
  Work,
  WorkField as FilterField,
}
import domain.repositories.WorkRepository

import cats.MonadThrow
import cats.effect.MonadCancelThrow
import cats.syntax.all.given
import doobie.Transactor
import doobie.postgres.implicits.JavaLocalDateMeta
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
}
import org.aulune.commons.adapters.doobie.queries.ListQueryMaker
import org.aulune.commons.filter.Filter
import org.aulune.commons.types.{NonEmptyString, Uuid}

import java.time.LocalDate


/** [[WorkRepository]] implementation for PostgreSQL. */
object WorkRepositoryImpl:
  /** Builds an instance.
   *  @param transactor [[Transactor]] instance.
   *  @tparam F effect type.
   */
  def build[F[_]: MonadCancelThrow](
      transactor: Transactor[F],
  ): F[WorkRepository[F]] = createTable
    .as(new WorkRepositoryImpl[F](transactor))
    .transact(transactor)

  private val createTable = sql"""
    |CREATE TABLE IF NOT EXISTS works (
    |  id            UUID         PRIMARY KEY,
    |  title         VARCHAR(255) NOT NULL,
    |  synopsis      TEXT         NOT NULL,
    |  release_date  DATE         NOT NULL,
    |  date_accuracy INTEGER      NOT NULL,
    |  writers       JSONB        NOT NULL,
    |  cast_members  JSONB        NOT NULL,
    |  series_id     UUID,
    |  series_season INTEGER,
    |  series_number INTEGER,
    |  episode_type  INTEGER,
    |  cover_url     TEXT,
    |  self_host_uri TEXT,
    |  resources     JSONB        NOT NULL,
    |  CONSTRAINT works_unique_id UNIQUE (id),
    |  CONSTRAINT works_unique_series_info
    |    UNIQUE (series_id, series_season, series_number, episode_type)
    |)""".stripMargin.update.run

  private val constraintMap = Map(
    "works_unique_id" -> WorkConstraint.UniqueId,
    "works_unique_series_info" -> WorkConstraint.UniqueSeriesInfo,
  )

  /** Converts constraint violations. */
  private def handleConstraintViolation[F[_]: MonadThrow, A] =
    makeConstraintViolationConverter[F, A, WorkConstraint](
      constraintMap,
    )

end WorkRepositoryImpl


private final class WorkRepositoryImpl[F[_]: MonadCancelThrow](
    transactor: Transactor[F],
) extends WorkRepository[F]:

  override def contains(id: Uuid[Work]): F[Boolean] =
    sql"SELECT EXISTS (SELECT 1 FROM works WHERE id = $id)"
      .query[Boolean]
      .unique
      .transact(transactor)
      .handleErrorWith(toInternalError)

  override def persist(elem: Work): F[Work] = sql"""
      |INSERT INTO works (
      |  id, title, synopsis,
      |  release_date, date_accuracy,
      |  writers, cast_members,
      |  series_id, series_season,
      |  series_number, episode_type,
      |  cover_url, self_host_uri, resources
      |)
      |VALUES (
      |  ${elem.id}, ${elem.title}, ${elem.synopsis},
      |  ${elem.releaseDate.date}, ${elem.releaseDate.accuracy},
      |  ${elem.writers}, ${elem.cast},
      |  ${elem.seriesId}, ${elem.seriesSeason},
      |  ${elem.seriesNumber}, ${elem.episodeType},
      |  ${elem.coverUri}, ${elem.selfHostedLocation}, ${elem.externalResources}
      |)""".stripMargin.update.run
    .as(elem)
    .transact(transactor)
    .recoverWith(handleConstraintViolation)
    .handleErrorWith(toInternalError)

  override def get(id: Uuid[Work]): F[Option[Work]] =
    val getAudioPlays = selectBase ++ sql"WHERE ap.id = $id"
    getAudioPlays.stripMargin
      .query[SelectResult]
      .map(toWork)
      .option
      .transact(transactor)
      .handleErrorWith(toInternalError)

  override def update(elem: Work): F[Work] = sql"""
      |UPDATE works
      |SET title         = ${elem.title},
      |    synopsis      = ${elem.synopsis},
      |    release_date  = ${elem.releaseDate.date},
      |    date_accuracy = ${elem.releaseDate.accuracy},
      |    writers       = ${elem.writers},
      |    cast_members  = ${elem.cast},
      |    series_id     = ${elem.seriesId},
      |    series_season = ${elem.seriesSeason},
      |    series_number = ${elem.seriesNumber},
      |    episode_type  = ${elem.episodeType},
      |    cover_url     = ${elem.coverUri},
      |    self_host_uri = ${elem.selfHostedLocation},
      |    resources     = ${elem.externalResources}
      |WHERE id = ${elem.id}
      |""".stripMargin.update.run
    .flatMap(checkIfUpdated)
    .as(elem)
    .transact(transactor)
    .recoverWith(handleConstraintViolation)
    .handleErrorWith(toInternalError)

  override def delete(id: Uuid[Work]): F[Unit] =
    sql"DELETE FROM works WHERE id = $id".update.run
      .transact(transactor)
      .void
      .handleErrorWith(toInternalError)

  override def list(
      count: Int,
      filter: Option[Filter[FilterField]],
  ): F[List[Work]] = (for
    _ <- checkIfPositive(count)
    query <- MonadThrow[F].fromEither(listQueryMaker.make(count, filter))
    result <- query.stripMargin
      .query[SelectResult]
      .map(toWork)
      .to[List]
      .transact(transactor)
  yield result)
    .handleErrorWith(toInternalError)

  override def search(query: NonEmptyString, limit: Int): F[List[Work]] =
    checkIfPositive(limit) >> (selectBase ++ fr0"""
      |WHERE TO_TSVECTOR(ap.title) @@ PLAINTO_TSQUERY($query)
      |ORDER BY TS_RANK(TO_TSVECTOR(ap.title), PLAINTO_TSQUERY($query)) DESC
      |LIMIT $limit
      |""".stripMargin)
      .query[SelectResult]
      .map(toWork)
      .to[List]
      .transact(transactor)
      .handleErrorWith(toInternalError)

  private type SelectResult = (
      Uuid[Work],
      Title,
      Synopsis,
      LocalDate,
      DateAccuracy,
      List[Uuid[Person]],
      List[CastMember],
      Option[Uuid[Series]],
      Option[SeasonNumber],
      Option[SeriesNumber],
      Option[EpisodeType],
      Option[ImageUri],
      Option[SelfHostedLocation],
      List[ExternalResource],
  )

  private val selectBase = fr"""
    |SELECT ap.id,
    |    ap.title,
    |    ap.synopsis,
    |    ap.release_date,
    |    ap.date_accuracy,
    |    ap.writers,
    |    ap.cast_members,
    |    ap.series_id,
    |    ap.series_season,
    |    ap.series_number,
    |    ap.episode_type,
    |    ap.cover_url,
    |    ap.self_host_uri,
    |    ap.resources
    |FROM works ap
    |""".stripMargin

  /** Makes work from given data. */
  private def toWork(
      uuid: Uuid[Work],
      title: Title,
      synopsis: Synopsis,
      releaseDate: LocalDate,
      dateAccuracy: DateAccuracy,
      writerIds: List[Uuid[Person]],
      cast: List[CastMember],
      seriesId: Option[Uuid[Series]],
      season: Option[SeasonNumber],
      number: Option[SeriesNumber],
      episodeType: Option[EpisodeType],
      coverUrl: Option[ImageUri],
      selfHostLocation: Option[SelfHostedLocation],
      resources: List[ExternalResource],
  ): Work = Work.unsafe(
    id = uuid,
    title = title,
    synopsis = synopsis,
    releaseDate = ReleaseDate.unsafe(releaseDate, dateAccuracy),
    writers = writerIds,
    cast = cast,
    seriesId = seriesId,
    seriesSeason = season,
    seriesNumber = number,
    episodeType = episodeType,
    coverUrl = coverUrl,
    selfHostedLocation = selfHostLocation,
    externalResources = resources,
  )

  private val listQueryMaker = ListQueryMaker[FilterField](selectBase) {
    case FilterField.Id => NonEmptyString("id::text")
  }
