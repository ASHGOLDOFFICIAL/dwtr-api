package org.aulune.aggregator
package adapters.jdbc.postgres


import adapters.jdbc.postgres.SeriesRepositoryImpl.handleConstraintViolation
import adapters.jdbc.postgres.metas.WorkMetas.given
import domain.errors.SeriesConstraint
import domain.model.series.SeriesField
import domain.model.series.{Series, SeriesName, SeriesField as FilterField}
import domain.repositories.SeriesRepository

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


/** [[SeriesRepository]] implementation for PostgreSQL. */
object SeriesRepositoryImpl:
  /** Builds an instance.
   *  @param transactor [[Transactor]] instance.
   *  @tparam F effect type.
   */
  def build[F[_]: MonadCancelThrow](
      transactor: Transactor[F],
  ): F[SeriesRepository[F]] = createSeriesTable
    .as(new SeriesRepositoryImpl[F](transactor))
    .transact(transactor)

  private val createSeriesTable = sql"""
    |CREATE TABLE IF NOT EXISTS series (
    |  id   UUID         PRIMARY KEY,
    |  name VARCHAR(255) NOT NULL,
    |  CONSTRAINT series_unique_id UNIQUE (id)
    |)""".stripMargin.update.run

  private val constraintMap = Map(
    "series_unique_id" -> SeriesConstraint.UniqueId,
  )

  /** Converts constraint violations. */
  private def handleConstraintViolation[F[_]: MonadThrow, A] =
    makeConstraintViolationConverter[F, A, SeriesConstraint](
      constraintMap,
    )

end SeriesRepositoryImpl


private final class SeriesRepositoryImpl[F[_]: MonadCancelThrow](
    transactor: Transactor[F],
) extends SeriesRepository[F]:

  override def contains(id: Uuid[Series]): F[Boolean] =
    sql"SELECT EXISTS (SELECT 1 FROM series WHERE id = $id)"
      .query[Boolean]
      .unique
      .transact(transactor)
      .handleErrorWith(toInternalError)

  override def persist(elem: Series): F[Series] = sql"""
      |INSERT INTO series (id, name)
      |VALUES (${elem.id}, ${elem.name})""".stripMargin.update.run
    .as(elem)
    .transact(transactor)
    .recoverWith(handleConstraintViolation)
    .handleErrorWith(toInternalError)

  override def get(id: Uuid[Series]): F[Option[Series]] =
    (selectBase ++ sql"WHERE s.id = $id").stripMargin
      .query[SelectResult]
      .map(toSeries)
      .option
      .transact(transactor)
      .handleErrorWith(toInternalError)

  override def update(elem: Series): F[Series] = sql"""
      |UPDATE series
      |SET name = ${elem.name}
      |WHERE id = ${elem.id}
      |""".stripMargin.update.run
    .flatMap(checkIfUpdated)
    .as(elem)
    .transact(transactor)
    .recoverWith(handleConstraintViolation)
    .handleErrorWith(toInternalError)

  override def delete(id: Uuid[Series]): F[Unit] =
    sql"DELETE FROM series WHERE id = $id".update.run
      .transact(transactor)
      .void
      .handleErrorWith(toInternalError)

  override def batchGet(
      ids: NonEmptyList[Uuid[Series]],
  ): F[List[Series]] = sql"""
    |SELECT s.id, s.name
    |FROM UNNEST(${ids.toList.toArray}) WITH ORDINALITY AS t(id, ord)
    |JOIN series s ON s.id = t.id
    |ORDER BY t.ord
    """.stripMargin
    .query[SelectResult]
    .map(toSeries)
    .to[List]
    .transact(transactor)
    .handleErrorWith(toInternalError)

  override def list(
      count: Int,
      filter: Option[Filter[SeriesField]],
  ): F[List[Series]] =
    for
      _ <- checkIfPositive(count)
      query <- MonadThrow[F].fromEither(listQueryMaker.make(count, filter))
      result <- query.stripMargin
        .query[SelectResult]
        .map(toSeries)
        .to[List]
        .transact(transactor)
        .handleErrorWith(toInternalError)
    yield result

  override def search(
      query: NonEmptyString,
      limit: Int,
  ): F[List[Series]] = checkIfPositive(limit) >> (selectBase ++ fr0"""
      |WHERE TO_TSVECTOR(s.name) @@ PLAINTO_TSQUERY($query)
      |ORDER BY TS_RANK(TO_TSVECTOR(s.name), PLAINTO_TSQUERY($query)) DESC
      |LIMIT $limit
      |""".stripMargin)
    .query[SelectResult]
    .map(toSeries)
    .to[List]
    .transact(transactor)
    .handleErrorWith(toInternalError)

  private type SelectResult = (Uuid[Series], SeriesName)

  private val selectBase = fr"""
    |SELECT s.id, s.name
    |FROM series s
    |""".stripMargin

  /** Makes series from given data. */
  private def toSeries(
      uuid: Uuid[Series],
      name: SeriesName,
  ): Series = Series.unsafe(
    id = uuid,
    name = name,
  )

  private val listQueryMaker = ListQueryMaker[FilterField](selectBase) {
    case FilterField.Id => NonEmptyString("id::text")
  }
