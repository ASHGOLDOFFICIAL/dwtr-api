package org.aulune.aggregator
package adapters.jdbc.postgres


import adapters.service.AudioPlaySeriesStubs
import domain.errors.AudioPlaySeriesConstraint
import domain.model.audioplay.AudioPlayFilterField
import domain.model.audioplay.series.{
  AudioPlaySeries,
  AudioPlaySeriesFilterField,
  AudioPlaySeriesName,
}
import domain.repositories.AudioPlaySeriesRepository

import cats.data.NonEmptyList
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


/** Tests for [[AudioPlaySeriesRepositoryImpl]]. */
final class AudioPlaySeriesRepositoryImplTest
    extends AsyncFreeSpec
    with AsyncIOSpec
    with Matchers
    with PostgresTestContainer:

  private def stand = makeStand(AudioPlaySeriesRepositoryImpl.build[IO])

  private val series = AudioPlaySeriesStubs.series1
  private val seriesList = List(
    AudioPlaySeriesStubs.series1,
    AudioPlaySeriesStubs.series2,
    AudioPlaySeriesStubs.series3).sortBy(_.id.toString)
  private val updatedSeries = series
    .update(name = AudioPlaySeriesName.unsafe("Updated"))
    .getOrElse(throw new IllegalStateException())

  "contains method " - {
    "should " - {
      "return false for non-existent series" in stand { repo =>
        for exists <- repo.contains(series.id)
        yield exists shouldBe false
      }

      "return true for existent series" in stand { repo =>
        for
          _ <- repo.persist(series)
          exists <- repo.contains(series.id)
        yield exists shouldBe true
      }
    }
  }

  "get method " - {
    "should " - {
      "retrieve existing series" in stand { repo =>
        for
          _ <- repo.persist(series)
          audio <- repo.get(series.id)
        yield audio shouldBe Some(series)
      }

      "return `None` for non-existent series" in stand { repo =>
        for audio <- repo.get(series.id)
        yield audio shouldBe None
      }
    }
  }

  "persist method " - {
    "should " - {
      "throw error if an series exists" in stand { repo =>
        for
          _ <- repo.persist(series)
          result <- repo.persist(updatedSeries).attempt
        yield result shouldBe Left(
          ConstraintViolation(AudioPlaySeriesConstraint.UniqueId))
      }
    }
  }

  "update method" - {
    "should " - {
      "update series" in stand { repo =>
        for
          _ <- repo.persist(series)
          updated <- repo.update(updatedSeries)
        yield updated shouldBe updatedSeries
      }

      "throw error for non-existent series" in stand { repo =>
        for updated <- repo.update(series).attempt
        yield updated shouldBe Left(FailedPrecondition)
      }

      "be idempotent" in stand { repo =>
        for
          _ <- repo.persist(series)
          updated <- repo.update(updatedSeries)
          updated <- repo.update(updatedSeries)
        yield updated shouldBe updatedSeries
      }
    }
  }

  "delete method " - {
    "should " - {
      "delete series" in stand { repo =>
        for
          _ <- repo.persist(series)
          result <- repo.delete(series.id)
        yield result shouldBe ()
      }

      "be idempotent" in stand { repo =>
        for
          _ <- repo.persist(series)
          _ <- repo.delete(series.id)
          result <- repo.delete(series.id)
        yield result shouldBe ()
      }
    }
  }

  "batchGet method " - {
    "should " - {
      "get elements in batches" in stand { repo =>
        val ids = NonEmptyList.of(
          AudioPlaySeriesStubs.series1.id,
          AudioPlaySeriesStubs.series2.id)
        for
          _ <- persistMany(repo)
          result <- repo.batchGet(ids)
        yield result shouldBe List(
          AudioPlaySeriesStubs.series1,
          AudioPlaySeriesStubs.series2)
      }

      "skip missing elements" in stand { repo =>
        val missingId =
          Uuid.unsafe[AudioPlaySeries]("1dbcb7ed-8c13-40c6-b4be-d4b323535d2b")
        val ids = NonEmptyList.of(AudioPlaySeriesStubs.series1.id, missingId)
        for
          _ <- persistMany(repo)
          result <- repo.batchGet(ids)
        yield result shouldBe List(AudioPlaySeriesStubs.series1)
      }

      "return empty list when none is found" in stand { repo =>
        val ids = NonEmptyList.of(AudioPlaySeriesStubs.series1.id)
        for result <- repo.batchGet(ids)
        yield result shouldBe Nil
      }
    }
  }

  "list method " - {
    "should " - {
      "return empty list if no series's available" in stand { repo =>
        for audios <- repo.list(10, None)
        yield audios shouldBe Nil
      }

      "return no more than asked" in stand { repo =>
        for
          _ <- persistMany(repo)
          audios <- repo.list(2, None)
        yield audios shouldBe seriesList.take(2)
      }

      "continue listing if token is given" in stand { repo =>
        for
          _ <- persistMany(repo)
          first <- repo.list(1, None).map(_.head)
          filter = Condition(
            AudioPlaySeriesFilterField.Id,
            GreaterThan,
            Literal(first.id.toString))
          second <- repo.list(1, Some(filter)).map(_.head)
        yield List(first, second) shouldBe seriesList.take(2)
      }
    }
  }

  "search method " - {
    "should " - {
      "return matching elements" in stand { repo =>
        val element = AudioPlaySeriesStubs.series1
        val query = NonEmptyString.unsafe(element.name)
        for
          _ <- persistMany(repo)
          result <- repo.search(query, 3)
        yield result should contain(element)
      }

      "not return elements when none of them match" in stand { repo =>
        for
          _ <- persistMany(repo)
          result <- repo.search(NonEmptyString.unsafe("nothing"), 1)
        yield result shouldBe Nil
      }

      "return elements in order of likeness" in stand { repo =>
        for
          _ <- persistMany(repo)
          result <- repo.search(NonEmptyString.unsafe("super series"), 3)
        yield result shouldBe List(
          AudioPlaySeriesStubs.series3,
          AudioPlaySeriesStubs.series2)
      }

      "throw InvalidArgument when given non-positive limit" in stand { repo =>
        for result <- repo.search(NonEmptyString.unsafe("something"), 0).attempt
        yield result shouldBe InvalidArgument.asLeft
      }
    }
  }

  private def persistMany(repo: AudioPlaySeriesRepository[IO]) =
    seriesList.foldLeft(IO.unit)((io, audio) => io >> repo.persist(audio).void)

end AudioPlaySeriesRepositoryImplTest
