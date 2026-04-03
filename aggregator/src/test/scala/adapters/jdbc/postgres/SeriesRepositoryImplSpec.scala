package org.aulune.aggregator
package adapters.jdbc.postgres

import domain.errors.SeriesConstraint
import domain.model.series.{Series, SeriesField, SeriesName}
import domain.model.work.WorkField
import domain.repositories.SeriesRepository

import cats.data.NonEmptyList
import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import cats.syntax.all.given
import org.aulune.aggregator.adapters.service.testdata.SeriesStubs
import org.aulune.commons.filter.Filter.Operator.GreaterThan
import org.aulune.commons.filter.Filter.{Condition, Literal}
import org.aulune.commons.repositories.RepositoryError
import org.aulune.commons.repositories.RepositoryError.{
  ConstraintViolation,
  FailedPrecondition,
  InvalidArgument,
}
import org.aulune.commons.testing.PostgresTestContainer
import org.aulune.commons.testing.syntax.getValid
import org.aulune.commons.types.{NonEmptyString, Uuid}
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers


/** Tests for [[SeriesRepositoryImpl]]. */
final class SeriesRepositoryImplSpec
    extends AsyncFlatSpec
    with AsyncIOSpec
    with Matchers
    with PostgresTestContainer:

  import Samples.*

  private def stand = makeStand(SeriesRepositoryImpl.build[IO])

  behavior of "contains"

  it should "return false for non-existent series" in stand { repo =>
    for exists <- repo.contains(series.id)
    yield exists shouldBe false
  }

  it should "return true for existent series" in stand { repo =>
    for
      _ <- repo.persist(series)
      exists <- repo.contains(series.id)
    yield exists shouldBe true
  }

  behavior of "get"

  it should "retrieve existing series" in stand { repo =>
    for
      _ <- repo.persist(series)
      audio <- repo.get(series.id)
    yield audio shouldBe Some(series)
  }

  it should "return `None` for non-existent series" in stand { repo =>
    for audio <- repo.get(series.id)
    yield audio shouldBe None
  }

  behavior of "persist"

  it should "throw error if an series exists" in stand { repo =>
    for
      _ <- repo.persist(series)
      result <- repo.persist(updatedSeries).attempt
    yield result shouldBe Left(ConstraintViolation(SeriesConstraint.UniqueId))
  }

  it should "update series" in stand { repo =>
    for
      _ <- repo.persist(series)
      updated <- repo.update(updatedSeries)
    yield updated shouldBe updatedSeries
  }

  it should "throw error for non-existent series" in stand { repo =>
    for updated <- repo.update(series).attempt
    yield updated shouldBe Left(FailedPrecondition)
  }

  it should "be idempotent" in stand { repo =>
    for
      _ <- repo.persist(series)
      updated <- repo.update(updatedSeries)
      updated <- repo.update(updatedSeries)
    yield updated shouldBe updatedSeries
  }

  behavior of "delete"

  it should "delete series" in stand { repo =>
    for
      _ <- repo.persist(series)
      result <- repo.delete(series.id)
    yield result shouldBe ()
  }

  it should "be idempotent" in stand { repo =>
    for
      _ <- repo.persist(series)
      _ <- repo.delete(series.id)
      result <- repo.delete(series.id)
    yield result shouldBe ()
  }

  behavior of "batchGet"

  it should "get elements in batches" in stand { repo =>
    val ids =
      NonEmptyList.of(SeriesStubs.series1.id, SeriesStubs.series2.id)
    for
      _ <- persistTestData(repo)
      result <- repo.batchGet(ids)
    yield result shouldBe List(SeriesStubs.series1, SeriesStubs.series2)
  }

  it should "skip missing elements" in stand { repo =>
    val missingId = Uuid.unsafe[Series]("1dbcb7ed-8c13-40c6-b4be-d4b323535d2b")
    val ids = NonEmptyList.of(SeriesStubs.series1.id, missingId)
    for
      _ <- persistTestData(repo)
      result <- repo.batchGet(ids)
    yield result shouldBe List(SeriesStubs.series1)
  }

  it should "return empty list when none is found" in stand { repo =>
    val ids = NonEmptyList.of(SeriesStubs.series1.id)
    for result <- repo.batchGet(ids)
    yield result shouldBe Nil
  }

  behavior of "list"

  it should "return empty list if no series's available" in stand { repo =>
    for audios <- repo.list(10, None)
    yield audios shouldBe Nil
  }

  it should "return no more than asked" in stand { repo =>
    for
      _ <- persistTestData(repo)
      audios <- repo.list(2, None)
    yield audios shouldBe seriesList.take(2)
  }

  it should "continue listing if token is given" in stand { repo =>
    for
      _ <- persistTestData(repo)
      first <- repo.list(1, None).map(_.head)
      filter =
        Condition(SeriesField.Id, GreaterThan, Literal(first.id.toString))
      second <- repo.list(1, Some(filter)).map(_.head)
    yield List(first, second) shouldBe seriesList.take(2)
  }

  behavior of "search"

  it should "return matching elements" in stand { repo =>
    val element = SeriesStubs.series1
    val query = NonEmptyString.unsafe(element.name)
    for
      _ <- persistTestData(repo)
      result <- repo.search(query, 3)
    yield result should contain(element)
  }

  it should "not return elements when none of them match" in stand { repo =>
    for
      _ <- persistTestData(repo)
      result <- repo.search(NonEmptyString.unsafe("nothing"), 1)
    yield result shouldBe Nil
  }

  it should "return elements in order of likeness" in stand { repo =>
    for
      _ <- persistTestData(repo)
      result <- repo.search(NonEmptyString.unsafe("super series"), 3)
    yield result shouldBe List(SeriesStubs.series3, SeriesStubs.series2)
  }

  it should "throw InvalidArgument when given non-positive limit" in stand {
    repo =>
      for result <- repo.search(NonEmptyString.unsafe("something"), 0).attempt
      yield result shouldBe InvalidArgument.asLeft
  }

  object Samples:
    val series: Series = SeriesStubs.series1
    val updatedSeries: Series = series
      .update(name = SeriesName.unsafe("Updated"))
      .getValid

    val seriesList: List[Series] = List(
      SeriesStubs.series1,
      SeriesStubs.series2,
      SeriesStubs.series3,
    ).sortBy(_.id.toString)

  private def persistTestData(repo: SeriesRepository[IO]) =
    seriesList.foldLeft(IO.unit)((io, audio) => io >> repo.persist(audio).void)

end SeriesRepositoryImplSpec
