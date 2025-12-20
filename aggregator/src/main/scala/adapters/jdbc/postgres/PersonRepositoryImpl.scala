package org.aulune.aggregator
package adapters.jdbc.postgres


import adapters.jdbc.postgres.PersonRepositoryImpl.handleConstraintViolation
import adapters.jdbc.postgres.metas.PersonMetas.given
import domain.errors.PersonConstraint
import domain.model.person.PersonField as FilterField
import domain.model.person.{FullName, Person, PersonField}
import domain.repositories.PersonRepository

import cats.MonadThrow
import cats.data.NonEmptyList
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
import org.aulune.commons.adapters.doobie.postgres.Metas.{
  nonEmptyStringMeta,
  uuidMeta,
  uuidsMeta,
}
import org.aulune.commons.adapters.doobie.queries.ListQueryMaker
import org.aulune.commons.filter.Filter
import org.aulune.commons.types.{NonEmptyString, Uuid}


/** [[PersonRepository]] implementation for PostgreSQL. */
object PersonRepositoryImpl:
  /** Builds an instance.
   *  @param transactor [[Transactor]] instance.
   *  @tparam F effect type.
   */
  def build[F[_]: MonadCancelThrow](
      transactor: Transactor[F],
  ): F[PersonRepository[F]] = createPersonTable
    .transact(transactor)
    .as(new PersonRepositoryImpl[F](transactor))

  private val createPersonTable = sql"""
    |CREATE TABLE IF NOT EXISTS people (
    |  id   UUID         PRIMARY KEY,
    |  name VARCHAR(255) NOT NULL,
    |  CONSTRAINT people_unique_id UNIQUE (id)
    |)""".stripMargin.update.run

  private val constraintMap = Map(
    "people_unique_id" -> PersonConstraint.UniqueId,
  )

  /** Converts constraint violations. */
  private def handleConstraintViolation[F[_]: MonadThrow, A] =
    makeConstraintViolationConverter[F, A, PersonConstraint](
      constraintMap,
    )

end PersonRepositoryImpl


private final class PersonRepositoryImpl[F[_]: MonadCancelThrow](
    transactor: Transactor[F],
) extends PersonRepository[F]:

  override def contains(id: Uuid[Person]): F[Boolean] =
    sql"SELECT EXISTS (SELECT 1 FROM people WHERE id = $id)"
      .query[Boolean]
      .unique
      .transact(transactor)
      .handleErrorWith(toInternalError)

  override def persist(elem: Person): F[Person] = sql"""
      |INSERT INTO people (id, name)
      |VALUES (${elem.id}, ${elem.name})""".stripMargin.update.run
    .as(elem)
    .transact(transactor)
    .recoverWith(handleConstraintViolation)
    .handleErrorWith(toInternalError)

  override def get(id: Uuid[Person]): F[Option[Person]] =
    val query = selectBase ++ sql"WHERE id = $id"
    query.stripMargin
      .query[SelectType]
      .map(toPerson)
      .option
      .transact(transactor)
      .handleErrorWith(toInternalError)

  override def update(elem: Person): F[Person] = sql"""
      |UPDATE people
      |SET name = ${elem.name}
      |WHERE id = ${elem.id}
      |""".stripMargin.update.run
    .flatMap(checkIfUpdated)
    .as(elem)
    .transact(transactor)
    .recoverWith(handleConstraintViolation)
    .handleErrorWith(toInternalError)

  override def delete(id: Uuid[Person]): F[Unit] =
    sql"DELETE FROM people WHERE id = $id".update.run
      .transact(transactor)
      .void
      .handleErrorWith(toInternalError)

  override def batchGet(ids: NonEmptyList[Uuid[Person]]): F[List[Person]] =
    sql"""
    |SELECT p.id, p.name
    |FROM UNNEST(${ids.toList.toArray}) WITH ORDINALITY AS t(id, ord)
    |JOIN people p ON p.id = t.id
    |ORDER BY t.ord
    """.stripMargin
      .query[SelectType]
      .map(toPerson)
      .to[List]
      .transact(transactor)
      .handleErrorWith(toInternalError)

  override def list(
      count: Int,
      filter: Option[Filter[PersonField]],
  ): F[List[Person]] =
    for
      _ <- checkIfPositive(count)
      query <- MonadThrow[F].fromEither(listQueryMaker.make(count, filter))
      result <- query.stripMargin
        .query[SelectType]
        .map(toPerson)
        .to[List]
        .transact(transactor)
        .handleErrorWith(toInternalError)
    yield result

  override def search(query: NonEmptyString, limit: Int): F[List[Person]] =
    checkIfPositive(limit) >> (selectBase ++ fr0"""
      |WHERE TO_TSVECTOR(name) @@ PLAINTO_TSQUERY($query)
      |ORDER BY TS_RANK(TO_TSVECTOR(name), PLAINTO_TSQUERY($query)) DESC
      |LIMIT $limit
      |""".stripMargin)
      .query[SelectType]
      .map(toPerson)
      .to[List]
      .transact(transactor)
      .handleErrorWith(toInternalError)

  private type SelectType = (Uuid[Person], FullName)

  private val selectBase = fr"SELECT id, name FROM people"

  /** Makes person from given data. */
  private def toPerson(uuid: Uuid[Person], name: FullName): Person =
    Person.unsafe(id = uuid, name = name)

  private val listQueryMaker = ListQueryMaker[FilterField](selectBase) {
    case FilterField.Id => NonEmptyString("id::text")
  }
