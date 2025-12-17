package org.aulune.aggregator
package adapters.jdbc.postgres


import adapters.service.AudioPlayTranslations
import domain.errors.TranslationConstraint
import domain.model.audioplay.AudioPlay
import domain.model.audioplay.translation.{
  AudioPlayTranslation,
  AudioPlayTranslationFilterField,
}
import domain.model.shared.TranslatedTitle
import domain.repositories.AudioPlayTranslationRepository
import domain.repositories.AudioPlayTranslationRepository.AudioPlayTranslationCursor

import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import org.aulune.commons.filter.Filter
import org.aulune.commons.filter.Filter.Literal.StringLiteral
import org.aulune.commons.filter.Filter.Operator.Equal
import org.aulune.commons.repositories.RepositoryError.{
  ConstraintViolation,
  FailedPrecondition,
}
import org.aulune.commons.testing.PostgresTestContainer
import org.aulune.commons.types.Uuid
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers
import org.typelevel.log4cats.slf4j.Slf4jFactory
import org.typelevel.log4cats.{LoggerFactory, SelfAwareStructuredLogger}


/** Tests for [[AudioPlayTranslationRepositoryImpl]]. */
final class AudioPlayTranslationRepositoryImplTest
    extends AsyncFreeSpec
    with AsyncIOSpec
    with Matchers
    with PostgresTestContainer:

  private given loggerFactory: LoggerFactory[IO] = Slf4jFactory.create[IO]

  private def stand = makeStand(AudioPlayTranslationRepositoryImpl.build[IO])

  private val translationTest = AudioPlayTranslations.translation1
  private val translationTests = List(
    AudioPlayTranslations.translation1,
    AudioPlayTranslations.translation2,
    AudioPlayTranslations.translation3)
  private val updatedTranslationTest = translationTest
    .update(
      title = TranslatedTitle.unsafe("Updated"),
    )
    .getOrElse(throw new IllegalStateException())

  "contains method " - {
    "should " - {
      "return false for non-existent translations" in stand { repo =>
        for exists <- repo.contains(translationTest.id)
        yield exists shouldBe false
      }

      "return true for existent translation" in stand { repo =>
        for
          _ <- repo.persist(translationTest)
          exists <- repo.contains(translationTest.id)
        yield exists shouldBe true
      }
    }
  }

  "get method " - {
    "should " - {
      "retrieve translations" in stand { repo =>
        for
          _ <- repo.persist(translationTest)
          audio <- repo.get(translationTest.id)
        yield audio shouldBe Some(translationTest)
      }

      "return `None` for non-existent translation" in stand { repo =>
        for audio <- repo.get(translationTest.id)
        yield audio shouldBe None
      }
    }
  }

  "persist method " - {
    "should " - {
      "throw error if an translation exists" in stand { repo =>
        for
          _ <- repo.persist(translationTest)
          result <- repo.persist(updatedTranslationTest).attempt
        yield result shouldBe Left(
          ConstraintViolation(TranslationConstraint.UniqueId))
      }
    }
  }

  "update method" - {
    "should " - {
      "update translations" in stand { repo =>
        for
          _ <- repo.persist(translationTest)
          updated <- repo.update(updatedTranslationTest)
        yield updated shouldBe updatedTranslationTest
      }

      "throw error for non-existent translations" in stand { repo =>
        for updated <- repo.update(translationTest).attempt
        yield updated shouldBe Left(FailedPrecondition)
      }

      "be idempotent" in stand { repo =>
        for
          _ <- repo.persist(translationTest)
          updated <- repo.update(updatedTranslationTest)
          updated <- repo.update(updatedTranslationTest)
        yield updated shouldBe updatedTranslationTest
      }
    }
  }

  "delete method " - {
    "should " - {
      "delete translations" in stand { repo =>
        for
          _ <- repo.persist(translationTest)
          result <- repo.delete(translationTest.id)
        yield result shouldBe ()
      }

      "be idempotent" in stand { repo =>
        for
          _ <- repo.persist(translationTest)
          _ <- repo.delete(translationTest.id)
          result <- repo.delete(translationTest.id)
        yield result shouldBe ()
      }
    }
  }

  "list method " - {
    "should " - {
      "return empty list if no translation's available" in stand { repo =>
        for audios <- repo.list(10, None, None)
        yield audios shouldBe Nil
      }

      "return no more than asked" in stand { repo =>
        for
          _ <- persistTranslations(repo)
          audios <- repo.list(2, None, None)
        yield audios shouldBe translationTests.take(2)
      }

      "continue listing if token is given" in stand { repo =>
        for
          _ <- persistTranslations(repo)
          first <- repo.list(1, None, None).map(_.head)
          cursor = AudioPlayTranslationCursor(first.id)
          rest <- repo.list(1, Some(cursor), None)
        yield rest.head shouldBe translationTests(1)
      }

      "filter elements" in stand { repo =>
        val filter = Filter.Condition[AudioPlayTranslationFilterField](
          AudioPlayTranslationFilterField.OriginalId,
          Equal,
          StringLiteral(translationTest.originalId.toString),
        )

        for
          _ <- persistTranslations(repo)
          result <- repo.list(10, None, Some(filter))
        yield result shouldBe List(translationTest)
      }
    }
  }

  private def persistTranslations(repo: AudioPlayTranslationRepository[IO]) =
    translationTests.foldLeft(IO.unit) { (io, audio) =>
      io >> repo.persist(audio).void
    }

end AudioPlayTranslationRepositoryImplTest
