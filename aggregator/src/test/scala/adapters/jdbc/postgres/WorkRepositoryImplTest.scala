package org.aulune.aggregator
package adapters.jdbc.postgres


import adapters.service.Works
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
import org.aulune.commons.filter.Filter.Operator.GreaterThan
import org.aulune.commons.filter.Filter.{Condition, Literal}
import org.aulune.commons.repositories.RepositoryError
import org.aulune.commons.repositories.RepositoryError.{
  ConstraintViolation,
  FailedPrecondition,
  InvalidArgument,
}
import org.aulune.commons.testing.PostgresTestContainer
import org.aulune.commons.types.{NonEmptyString, Uuid}
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers

import java.net.URI


/** Tests for [[WorkRepositoryImpl]]. */
final class WorkRepositoryImplTest
    extends AsyncFreeSpec
    with AsyncIOSpec
    with Matchers
    with PostgresTestContainer:

  private def stand = makeStand(WorkRepositoryImpl.build[IO])

  private val work = Works.work1
  private val workList = List(Works.work1, Works.work2, Works.work3)
    .sortBy(_.id.toString)
  private val updatedWorkTest = work
    .update(
      title = Title.unsafe("Updated"),
      writers = List(Uuid.unsafe("c5c1f3b9-175c-4fa2-800d-c9c20cb44539")),
      externalResources =
        List(ExternalResource(Purchase, URI.create("https://test.org/1"))),
    )
    .getOrElse(throw new IllegalStateException())

  "contains method " - {
    "should " - {
      "return false for non-existent elements" in stand { repo =>
        for exists <- repo.contains(work.id)
        yield exists shouldBe false
      }

      "return true for existent element" in stand { repo =>
        for
          _ <- repo.persist(work)
          exists <- repo.contains(work.id)
        yield exists shouldBe true
      }
    }
  }

  "get method " - {
    "should " - {
      "retrieve elements with writers and resources" in stand { repo =>
        for
          _ <- repo.persist(work)
          audio <- repo.get(work.id)
        yield audio shouldBe Some(work)
      }

      "retrieve elements without resources" in stand { repo =>
        val without = work
          .update(externalResources = Nil)
          .toOption
          .get
        for
          _ <- repo.persist(without)
          audio <- repo.get(without.id)
        yield audio shouldBe Some(without)
      }

      "retrieve elements without writers" in stand { repo =>
        val without = work.update(writers = Nil).toOption.get
        for
          _ <- repo.persist(without)
          audio <- repo.get(without.id)
        yield audio shouldBe Some(without)
      }

      "return `None` for non-existent elements" in stand { repo =>
        for audio <- repo.get(work.id)
        yield audio shouldBe None
      }
    }
  }

  "persist method " - {
    "should " - {
      "throw error if element already exists" in stand { repo =>
        for
          _ <- repo.persist(work)
          result <- repo.persist(updatedWorkTest).attempt
        yield result shouldBe Left(ConstraintViolation(WorkConstraint.UniqueId))
      }

      "throw error if element series info is not unique" in stand { repo =>
        val differentId = work
          .update(id = Uuid.unsafe("6e6abdfb-3010-4748-a721-eef1d87ca392"))
          .getOrElse(throw new IllegalStateException())

        for
          _ <- repo.persist(work)
          result <- repo.persist(differentId).attempt
        yield result shouldBe Left(
          ConstraintViolation(WorkConstraint.UniqueSeriesInfo))
      }
    }
  }

  "update method" - {
    "should " - {
      "update elements" in stand { repo =>
        for
          _ <- repo.persist(work)
          updated <- repo.update(updatedWorkTest)
        yield updated shouldBe updatedWorkTest
      }

      "throw error for non-existent elements" in stand { repo =>
        for updated <- repo.update(work).attempt
        yield updated shouldBe Left(FailedPrecondition)
      }

      "throw error if an element series info is not unique" in stand { repo =>
        val updated = Works.work2
          .update(
            seriesId = work.seriesId,
            seriesSeason = work.seriesSeason,
            seriesNumber = work.seriesNumber,
            episodeType = work.episodeType,
          )
          .getOrElse(throw new IllegalStateException())

        for
          _ <- repo.persist(work)
          _ <- repo.persist(Works.work2)
          result <- repo.update(updated).attempt
        yield result shouldBe Left(
          ConstraintViolation(WorkConstraint.UniqueSeriesInfo))
      }

      "be idempotent" in stand { repo =>
        for
          _ <- repo.persist(work)
          updated <- repo.update(updatedWorkTest)
          updated <- repo.update(updatedWorkTest)
        yield updated shouldBe updatedWorkTest
      }
    }
  }

  "delete method " - {
    "should " - {
      "delete elements" in stand { repo =>
        for
          _ <- repo.persist(work)
          result <- repo.delete(work.id)
        yield result shouldBe ()
      }

      "be idempotent" in stand { repo =>
        for
          _ <- repo.persist(work)
          _ <- repo.delete(work.id)
          result <- repo.delete(work.id)
        yield result shouldBe ()
      }
    }
  }

  "list method " - {
    "should " - {
      "return empty list if no work's available" in stand { repo =>
        for audios <- repo.list(10, None)
        yield audios shouldBe Nil
      }

      "return no more than asked" in stand { repo =>
        for
          _ <- persistWorks(repo)
          audios <- repo.list(2, None)
        yield audios shouldBe workList.take(2)
      }

      "use filter" in stand { repo =>
        for
          _ <- persistWorks(repo)
          first <- repo.list(1, None).map(_.head)
          filter =
            Condition(WorkField.Id, GreaterThan, Literal(first.id.toString))
          second <- repo.list(1, Some(filter)).map(_.head)
        yield List(first, second) shouldBe workList.take(2)
      }
    }
  }

  "search method " - {
    "should " - {
      "return matching elements" in stand { repo =>
        val element = Works.work2
        val query = NonEmptyString.unsafe(element.title)
        for
          _ <- persistWorks(repo)
          result <- repo.search(query, 3)
        yield result should contain(element)
      }

      "not return elements when none of them match" in stand { repo =>
        for
          _ <- persistWorks(repo)
          result <- repo.search(NonEmptyString.unsafe("nothing"), 1)
        yield result shouldBe Nil
      }

      "return elements in order of likeness" in stand { repo =>
        for
          _ <- persistWorks(repo)
          result <- repo.search(NonEmptyString.unsafe("test thing"), 3)
        yield result shouldBe List(Works.work3, Works.work2)
      }

      "throw InvalidArgument when given non-positive limit" in stand { repo =>
        for result <- repo.search(NonEmptyString.unsafe("something"), 0).attempt
        yield result shouldBe InvalidArgument.asLeft
      }
    }
  }

  private def persistWorks(repo: WorkRepository[IO]) =
    workList.foldLeft(IO.unit)((io, audio) => io >> repo.persist(audio).void)

end WorkRepositoryImplTest
