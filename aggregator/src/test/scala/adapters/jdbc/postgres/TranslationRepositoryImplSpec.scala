package org.aulune.aggregator
package adapters.jdbc.postgres

import domain.errors.TranslationConstraint
import domain.model.translation.{TranslatedTitle, Translation, TranslationField}
import domain.model.work.Work
import domain.repositories.TranslationRepository

import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import org.aulune.aggregator.adapters.service.testdata.Translations
import org.aulune.commons.filter.Filter
import org.aulune.commons.filter.Filter.Operator.GreaterThan
import org.aulune.commons.filter.Filter.{Condition, Literal}
import org.aulune.commons.repositories.RepositoryError.{
  ConstraintViolation,
  FailedPrecondition,
}
import org.aulune.commons.testing.PostgresTestContainer
import org.aulune.commons.testing.syntax.{getLeft, getValid}
import org.aulune.commons.types.Uuid
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers
import org.typelevel.log4cats.LoggerFactory
import org.typelevel.log4cats.noop.NoOpFactory


/** Tests for [[TranslationRepositoryImpl]]. */
final class TranslationRepositoryImplSpec
    extends AsyncFlatSpec
    with AsyncIOSpec
    with Matchers
    with PostgresTestContainer:

  import Samples.*

  private given loggerFactory: LoggerFactory[IO] = NoOpFactory.impl[IO]
  private def stand = makeStand(TranslationRepositoryImpl.build[IO])

  behavior of "contains"

  it should "return false for non-existent translations" in stand { repo =>
    for exists <- repo.contains(translation.id)
    yield exists shouldBe false
  }

  it should "return true for existent translation" in stand { repo =>
    for
      _ <- repo.persist(translation)
      exists <- repo.contains(translation.id)
    yield exists shouldBe true
  }

  behavior of "get"

  it should "retrieve translations" in stand { repo =>
    for
      _ <- repo.persist(translation)
      audio <- repo.get(translation.id)
    yield audio shouldBe Some(translation)
  }

  it should "return `None` for non-existent translation" in stand { repo =>
    for audio <- repo.get(translation.id)
    yield audio shouldBe None
  }

  behavior of "persist"

  it should "throw error if an translation exists" in stand { repo =>
    for
      _ <- repo.persist(translation)
      result <- repo.persist(updatedTranslation).attempt
    yield result.getLeft shouldBe ConstraintViolation(
      TranslationConstraint.UniqueId)
  }

  behavior of "update"

  it should "update translations" in stand { repo =>
    for
      _ <- repo.persist(translation)
      updated <- repo.update(updatedTranslation)
    yield updated shouldBe updatedTranslation
  }

  it should "throw error for non-existent translations" in stand { repo =>
    for updated <- repo.update(translation).attempt
    yield updated shouldBe Left(FailedPrecondition)
  }

  it should "be idempotent" in stand { repo =>
    for
      _ <- repo.persist(translation)
      updated <- repo.update(updatedTranslation)
      updated <- repo.update(updatedTranslation)
    yield updated shouldBe updatedTranslation
  }

  behavior of "delete"

  it should "delete translations" in stand { repo =>
    for
      _ <- repo.persist(translation)
      result <- repo.delete(translation.id)
    yield result shouldBe ()
  }

  it should "be idempotent" in stand { repo =>
    for
      _ <- repo.persist(translation)
      _ <- repo.delete(translation.id)
      result <- repo.delete(translation.id)
    yield result shouldBe ()
  }

  behavior of "list"

  it should "return empty list if no translation's available" in stand { repo =>
    for audios <- repo.list(10, None)
    yield audios shouldBe Nil
  }

  it should "return no more than asked" in stand { repo =>
    for
      _ <- persistTestData(repo)
      audios <- repo.list(2, None)
    yield audios shouldBe translationList.take(2)
  }

  it should "use filter" in stand { repo =>
    for
      _ <- persistTestData(repo)
      first <- repo.list(1, None).map(_.head)
      filter =
        Condition(TranslationField.Id, GreaterThan, Literal(first.id.toString))
      second <- repo.list(1, Some(filter)).map(_.head)
    yield List(first, second) shouldBe translationList.take(2)
  }

  object Samples:
    val translation: Translation = Translations.translation1
    val updatedTranslation: Translation = translation
      .update(
        title = TranslatedTitle.unsafe("Updated"),
      )
      .getValid

    val translationList: List[Translation] = List(
      Translations.translation1,
      Translations.translation2,
      Translations.translation3,
    ).sortBy(_.id.toString)

  private def persistTestData(repo: TranslationRepository[IO]) =
    translationList.foldLeft(IO.unit) { (io, audio) =>
      io >> repo.persist(audio).void
    }

end TranslationRepositoryImplSpec
