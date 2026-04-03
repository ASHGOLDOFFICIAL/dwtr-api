package org.aulune.aggregator
package adapters.jdbc.postgres

import domain.errors.WorkConstraint
import domain.model.series.Series
import domain.model.shared.ExternalResourceType.Purchase
import domain.model.shared.{
  ExternalResource,
  ReleaseDate,
  SelfHostedLocation,
  Synopsis,
}
import domain.model.work.{
  CastMember,
  SeasonNumber,
  SeriesNumber,
  Title,
  Work,
  WorkField,
}
import domain.repositories.WorkRepository

import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import cats.syntax.all.given
import org.aulune.aggregator.adapters.service.testdata.Works
import org.aulune.commons.filter.Filter.Operator.GreaterThan
import org.aulune.commons.filter.Filter.{Condition, Literal}
import org.aulune.commons.repositories.RepositoryError
import org.aulune.commons.repositories.RepositoryError.{
  ConstraintViolation,
  FailedPrecondition,
  InvalidArgument,
}
import org.aulune.commons.testing.PostgresTestContainer
import org.aulune.commons.testing.syntax.{getLeft, getValid}
import org.aulune.commons.types.{NonEmptyString, Uuid}
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers

import java.net.URI


/** Tests for [[WorkRepositoryImpl]]. */
final class WorkRepositoryImplSpec
    extends AsyncFlatSpec
    with AsyncIOSpec
    with Matchers
    with PostgresTestContainer:

  import Samples.*

  private def stand = makeStand(WorkRepositoryImpl.build[IO])

  behavior of "contains"

  it should "return false for non-existent elements" in stand { repo =>
    for exists <- repo.contains(work.id)
    yield exists shouldBe false
  }

  it should "return true for existent element" in stand { repo =>
    for
      _ <- repo.persist(work)
      exists <- repo.contains(work.id)
    yield exists shouldBe true
  }

  behavior of "get"

  it should "retrieve elements with writers and resources" in stand { repo =>
    for
      _ <- repo.persist(work)
      audio <- repo.get(work.id)
    yield audio shouldBe Some(work)
  }

  it should "retrieve elements without resources" in stand { repo =>
    val without = work
      .update(externalResources = Nil)
      .getValid

    for
      _ <- repo.persist(without)
      audio <- repo.get(without.id)
    yield audio shouldBe Some(without)
  }

  it should "retrieve elements without writers" in stand { repo =>
    val without = work.update(writers = Nil).getValid
    for
      _ <- repo.persist(without)
      audio <- repo.get(without.id)
    yield audio shouldBe Some(without)
  }

  it should "return `None` for non-existent elements" in stand { repo =>
    for audio <- repo.get(work.id)
    yield audio shouldBe None
  }

  behavior of "persist"

  it should "throw error if element already exists" in stand { repo =>
    for
      _ <- repo.persist(work)
      result <- repo.persist(updatedWork).attempt
    yield result shouldBe Left(ConstraintViolation(WorkConstraint.UniqueId))
  }

  it should "throw error if element series info is not unique" in stand {
    repo =>
      val differentId = work
        .update(id = Uuid.unsafe("6e6abdfb-3010-4748-a721-eef1d87ca392"))
        .getValid

      for
        _ <- repo.persist(work)
        result <- repo.persist(differentId).attempt
      yield result.getLeft shouldBe ConstraintViolation(
        WorkConstraint.UniqueSeriesInfo)
  }

  behavior of "update"

  it should "update elements" in stand { repo =>
    for
      _ <- repo.persist(work)
      updated <- repo.update(updatedWork)
    yield updated shouldBe updatedWork
  }

  it should "throw error for non-existent elements" in stand { repo =>
    for updated <- repo.update(work).attempt
    yield updated shouldBe Left(FailedPrecondition)
  }

  it should "throw error if an element series info is not unique" in stand {
    repo =>
      val updated = Works.work2
        .update(
          seriesId = work.seriesId,
          seriesSeason = work.seriesSeason,
          seriesNumber = work.seriesNumber,
          episodeType = work.episodeType,
        )
        .getValid

      for
        _ <- repo.persist(work)
        _ <- repo.persist(Works.work2)
        result <- repo.update(updated).attempt
      yield result.getLeft shouldBe ConstraintViolation(
        WorkConstraint.UniqueSeriesInfo)
  }

  it should "be idempotent" in stand { repo =>
    for
      _ <- repo.persist(work)
      updated <- repo.update(updatedWork)
      updated <- repo.update(updatedWork)
    yield updated shouldBe updatedWork
  }

  behavior of "delete"

  it should "delete elements" in stand { repo =>
    for
      _ <- repo.persist(work)
      result <- repo.delete(work.id)
    yield result shouldBe ()
  }

  it should "be idempotent" in stand { repo =>
    for
      _ <- repo.persist(work)
      _ <- repo.delete(work.id)
      result <- repo.delete(work.id)
    yield result shouldBe ()
  }

  behavior of "list"

  it should "return empty list if no work's available" in stand { repo =>
    for audios <- repo.list(10, None)
    yield audios shouldBe Nil
  }

  it should "return no more than asked" in stand { repo =>
    for
      _ <- persistTestData(repo)
      audios <- repo.list(2, None)
    yield audios shouldBe workList.take(2)
  }

  it should "use filter" in stand { repo =>
    for
      _ <- persistTestData(repo)
      first <- repo.list(1, None).map(_.head)
      filter = Condition(WorkField.Id, GreaterThan, Literal(first.id.toString))
      second <- repo.list(1, Some(filter)).map(_.head)
    yield List(first, second) shouldBe workList.take(2)
  }

  behavior of "search"

  it should "return matching elements" in stand { repo =>
    val element = Works.work2
    val query = NonEmptyString.unsafe(element.title)
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
      result <- repo.search(NonEmptyString.unsafe("test thing"), 3)
    yield result shouldBe List(Works.work3, Works.work2)
  }

  it should "throw InvalidArgument when given non-positive limit" in stand {
    repo =>
      for result <- repo.search(NonEmptyString.unsafe("something"), 0).attempt
      yield result shouldBe InvalidArgument.asLeft
  }

  object Samples:
    val work: Work = Works.work1
    val updatedWork: Work = work
      .update(
        title = Title.unsafe("Updated"),
        writers = List(Uuid.unsafe("c5c1f3b9-175c-4fa2-800d-c9c20cb44539")),
        externalResources =
          List(ExternalResource(Purchase, URI.create("https://test.org/1"))),
      )
      .getValid

    val workList: List[Work] = List(Works.work1, Works.work2, Works.work3)
      .sortBy(_.id.toString)

  private def persistTestData(repo: WorkRepository[IO]) =
    workList.foldLeft(IO.unit)((io, audio) => io >> repo.persist(audio).void)

end WorkRepositoryImplSpec
