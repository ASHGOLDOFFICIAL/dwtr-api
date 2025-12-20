package org.aulune.aggregator
package adapters.jdbc.postgres


import adapters.service.AudioPlays
import domain.errors.AudioPlayConstraint
import domain.model.audioplay.series.AudioPlaySeries
import domain.model.audioplay.{
  AudioPlay,
  AudioPlayFilterField,
  AudioPlaySeason,
  AudioPlaySeriesNumber,
  AudioPlayTitle,
  CastMember,
}
import domain.model.shared.ExternalResourceType.Purchase
import domain.model.shared.{
  ExternalResource,
  ReleaseDate,
  SelfHostedLocation,
  Synopsis,
}
import domain.repositories.AudioPlayRepository

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


/** Tests for [[AudioPlayRepositoryImpl]]. */
final class AudioPlayRepositoryImplTest
    extends AsyncFreeSpec
    with AsyncIOSpec
    with Matchers
    with PostgresTestContainer:

  private def stand = makeStand(AudioPlayRepositoryImpl.build[IO])

  private val audioPlayTest = AudioPlays.audioPlay1
  private val audioPlayTests =
    List(AudioPlays.audioPlay1, AudioPlays.audioPlay2, AudioPlays.audioPlay3)
      .sortBy(_.id.toString)
  private val updatedAudioPlayTest = audioPlayTest
    .update(
      title = AudioPlayTitle.unsafe("Updated"),
      writers = List(Uuid.unsafe("c5c1f3b9-175c-4fa2-800d-c9c20cb44539")),
      externalResources =
        List(ExternalResource(Purchase, URI.create("https://test.org/1"))),
    )
    .getOrElse(throw new IllegalStateException())

  "contains method " - {
    "should " - {
      "return false for non-existent audio play" in stand { repo =>
        for exists <- repo.contains(audioPlayTest.id)
        yield exists shouldBe false
      }

      "return true for existent audio play" in stand { repo =>
        for
          _ <- repo.persist(audioPlayTest)
          exists <- repo.contains(audioPlayTest.id)
        yield exists shouldBe true
      }
    }
  }

  "get method " - {
    "should " - {
      "retrieve audio plays with writers and resources" in stand { repo =>
        for
          _ <- repo.persist(audioPlayTest)
          audio <- repo.get(audioPlayTest.id)
        yield audio shouldBe Some(audioPlayTest)
      }

      "retrieve audio plays without resources" in stand { repo =>
        val without = audioPlayTest
          .update(externalResources = Nil)
          .toOption
          .get
        for
          _ <- repo.persist(without)
          audio <- repo.get(without.id)
        yield audio shouldBe Some(without)
      }

      "retrieve audio plays without writers" in stand { repo =>
        val without = audioPlayTest.update(writers = Nil).toOption.get
        for
          _ <- repo.persist(without)
          audio <- repo.get(without.id)
        yield audio shouldBe Some(without)
      }

      "return `None` for non-existent audio play" in stand { repo =>
        for audio <- repo.get(audioPlayTest.id)
        yield audio shouldBe None
      }
    }
  }

  "persist method " - {
    "should " - {
      "throw error if an audio play exists" in stand { repo =>
        for
          _ <- repo.persist(audioPlayTest)
          result <- repo.persist(updatedAudioPlayTest).attempt
        yield result shouldBe Left(
          ConstraintViolation(AudioPlayConstraint.UniqueId))
      }

      "throw error if an audio play series info is not unique" in stand {
        repo =>
          val differentId = audioPlayTest
            .update(id = Uuid.unsafe("6e6abdfb-3010-4748-a721-eef1d87ca392"))
            .getOrElse(throw new IllegalStateException())

          for
            _ <- repo.persist(audioPlayTest)
            result <- repo.persist(differentId).attempt
          yield result shouldBe Left(
            ConstraintViolation(AudioPlayConstraint.UniqueSeriesInfo))
      }
    }
  }

  "update method" - {
    "should " - {
      "update audio plays" in stand { repo =>
        for
          _ <- repo.persist(audioPlayTest)
          updated <- repo.update(updatedAudioPlayTest)
        yield updated shouldBe updatedAudioPlayTest
      }

      "throw error for non-existent audio plays" in stand { repo =>
        for updated <- repo.update(audioPlayTest).attempt
        yield updated shouldBe Left(FailedPrecondition)
      }

      "throw error if an audio play series info is not unique" in stand {
        repo =>
          val updated = AudioPlays.audioPlay2
            .update(
              seriesId = audioPlayTest.seriesId,
              seriesSeason = audioPlayTest.seriesSeason,
              seriesNumber = audioPlayTest.seriesNumber,
              episodeType = audioPlayTest.episodeType,
            )
            .getOrElse(throw new IllegalStateException())

          for
            _ <- repo.persist(audioPlayTest)
            _ <- repo.persist(AudioPlays.audioPlay2)
            result <- repo.update(updated).attempt
          yield result shouldBe Left(
            ConstraintViolation(AudioPlayConstraint.UniqueSeriesInfo))
      }

      "be idempotent" in stand { repo =>
        for
          _ <- repo.persist(audioPlayTest)
          updated <- repo.update(updatedAudioPlayTest)
          updated <- repo.update(updatedAudioPlayTest)
        yield updated shouldBe updatedAudioPlayTest
      }
    }
  }

  "delete method " - {
    "should " - {
      "delete audio plays" in stand { repo =>
        for
          _ <- repo.persist(audioPlayTest)
          result <- repo.delete(audioPlayTest.id)
        yield result shouldBe ()
      }

      "be idempotent" in stand { repo =>
        for
          _ <- repo.persist(audioPlayTest)
          _ <- repo.delete(audioPlayTest.id)
          result <- repo.delete(audioPlayTest.id)
        yield result shouldBe ()
      }
    }
  }

  "list method " - {
    "should " - {
      "return empty list if no audio play's available" in stand { repo =>
        for audios <- repo.list(10, None)
        yield audios shouldBe Nil
      }

      "return no more than asked" in stand { repo =>
        for
          _ <- persistAudios(repo)
          audios <- repo.list(2, None)
        yield audios shouldBe audioPlayTests.take(2)
      }

      "use filter" in stand { repo =>
        for
          _ <- persistAudios(repo)
          first <- repo.list(1, None).map(_.head)
          filter = Condition(
            AudioPlayFilterField.Id,
            GreaterThan,
            Literal(first.id.toString))
          second <- repo.list(1, Some(filter)).map(_.head)
        yield List(first, second) shouldBe audioPlayTests.take(2)
      }
    }
  }

  "search method " - {
    "should " - {
      "return matching elements" in stand { repo =>
        val element = AudioPlays.audioPlay2
        val query = NonEmptyString.unsafe(element.title)
        for
          _ <- persistAudios(repo)
          result <- repo.search(query, 3)
        yield result should contain(element)
      }

      "not return elements when none of them match" in stand { repo =>
        for
          _ <- persistAudios(repo)
          result <- repo.search(NonEmptyString.unsafe("nothing"), 1)
        yield result shouldBe Nil
      }

      "return elements in order of likeness" in stand { repo =>
        for
          _ <- persistAudios(repo)
          result <- repo.search(NonEmptyString.unsafe("test thing"), 3)
        yield result shouldBe List(AudioPlays.audioPlay3, AudioPlays.audioPlay2)
      }

      "throw InvalidArgument when given non-positive limit" in stand { repo =>
        for result <- repo.search(NonEmptyString.unsafe("something"), 0).attempt
        yield result shouldBe InvalidArgument.asLeft
      }
    }
  }

  private def persistAudios(repo: AudioPlayRepository[IO]) =
    audioPlayTests.foldLeft(IO.unit) { (io, audio) =>
      io >> repo.persist(audio).void
    }

end AudioPlayRepositoryImplTest
