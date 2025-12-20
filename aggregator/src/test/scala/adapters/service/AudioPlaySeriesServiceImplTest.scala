package org.aulune.aggregator
package adapters.service


import adapters.service.mappers.AudioPlaySeriesMapper
import application.AggregatorPermission.Modify
import application.AudioPlaySeriesService
import application.dto.audioplay.series.{
  AudioPlaySeriesResource,
  BatchGetAudioPlaySeriesRequest,
  CreateAudioPlaySeriesRequest,
  DeleteAudioPlaySeriesRequest,
  GetAudioPlaySeriesRequest,
  ListAudioPlaySeriesRequest,
  SearchAudioPlaySeriesRequest,
}
import application.errors.AudioPlaySeriesServiceError.{
  InvalidSeries,
  SeriesNotFound,
}
import domain.model.audioplay.series.AudioPlaySeries
import domain.repositories.AudioPlaySeriesRepository

import cats.data.NonEmptyList
import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import cats.syntax.all.given
import org.aulune.commons.errors.ErrorResponse
import org.aulune.commons.errors.ErrorStatus.PermissionDenied
import org.aulune.commons.pagination.cursor.CursorEncoder
import org.aulune.commons.service.auth.User
import org.aulune.commons.service.permission.{
  Permission,
  PermissionClientService,
}
import org.aulune.commons.testing.ErrorAssertions.{
  assertDomainError,
  assertErrorStatus,
  assertInternalError,
}
import org.aulune.commons.testing.instances.UUIDGenInstances.makeFixedUuidGen
import org.aulune.commons.typeclasses.SortableUUIDGen
import org.aulune.commons.types.{NonEmptyString, Uuid}
import org.scalamock.scalatest.AsyncMockFactory
import org.scalatest.Assertion
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers
import org.typelevel.log4cats.LoggerFactory
import org.typelevel.log4cats.slf4j.Slf4jFactory

import java.util.UUID


/** Tests for [[AudioPlaySeriesServiceImpl]]. */
final class AudioPlaySeriesServiceImplTest
    extends AsyncFreeSpec
    with AsyncIOSpec
    with Matchers
    with AsyncMockFactory:

  private given LoggerFactory[IO] = Slf4jFactory.create

  private val mockRepo = mock[AudioPlaySeriesRepository[IO]]
  private val mockPermissions = mock[PermissionClientService[IO]]

  private val uuid = UUID.fromString("00000000-0000-0000-0000-000000000001")
  private given SortableUUIDGen[IO] = makeFixedUuidGen(uuid)

  private val user = User(
    id = UUID.fromString("f04eb510-229c-4cdd-bd7b-9691c3b28ae1"),
    username = "username",
  )

  private def stand(
      testCase: AudioPlaySeriesService[IO] => IO[Assertion],
  ): IO[Assertion] =
    val _ = (mockPermissions.registerPermission _)
      .expects(*)
      .returning(().asRight.pure)
      .anyNumberOfTimes()
    AudioPlaySeriesServiceImpl
      .build(
        2,
        AggregatorConfig.PaginationParams(2, 1),
        AggregatorConfig.SearchParams(2, 1),
        mockRepo,
        mockPermissions)
      .flatMap(testCase)
  end stand

  private val series = AudioPlaySeriesStubs.series1
  private val resource = AudioPlaySeriesResource(
    id = series.id,
    name = series.name,
  )
  private val newUuid = Uuid[AudioPlaySeries](uuid)
  private val newSeries = series
    .update(id = newUuid)
    .getOrElse(throw new IllegalStateException())
  private val newResource = resource.copy(
    id = newSeries.id,
    name = newSeries.name,
  )

  "get method " - {
    val request = GetAudioPlaySeriesRequest(series.id)

    "should " - {
      "find series if it's present in repository" in stand { service =>
        val _ = mockGet(series.some.pure)
        for result <- service.get(request)
        yield result shouldBe resource.asRight
      }

      "result in SeriesNotFound if series doesn't exist" in stand { service =>
        val _ = mockGet(None.pure)
        val find = service.get(request)
        assertDomainError(find)(SeriesNotFound)
      }
    }
  }

  "batchGet method " - {
    val elements = NonEmptyList.of(
      AudioPlaySeriesStubs.series1,
      AudioPlaySeriesStubs.series2)
    val ids = elements.map(_.id)
    val request = BatchGetAudioPlaySeriesRequest(names = ids.toList)

    "should " - {
      "return batch of elements when all are found" in stand { service =>
        val _ = (mockRepo.batchGet _)
          .expects(ids)
          .returning(elements.toList.pure)

        for result <- service.batchGet(request)
        yield result match
          case Left(_)         => fail("Error was not expected.")
          case Right(response) =>
            response.audioPlaySeries.map(_.id) shouldBe ids.toList
      }

      "return SeriesNotFound if at least one series is not found" in stand {
        service =>
          val _ = (mockRepo.batchGet _)
            .expects(ids)
            .returning(List(AudioPlaySeriesStubs.series1).pure)

          val batchGet = service.batchGet(request)
          assertDomainError(batchGet)(SeriesNotFound)
      }
    }
  }

  "list method " - {
    "should " - {
      "list elements" in stand { service =>
        val request = ListAudioPlaySeriesRequest(
          pageSize = 1.some,
          pageToken = None,
        )
        val _ = (mockRepo.list _).expects(None, 1).returning(List(series).pure)
        for result <- service.list(request)
        yield result match
          case Left(_)     => fail("Error was not expected")
          case Right(list) => list.audioPlaySeries shouldBe List(resource)
      }

      "return next page when asked" in stand { service =>
        val cursor = AudioPlaySeriesRepository.Cursor(series.id)
        val prevToken = CursorEncoder[AudioPlaySeriesRepository.Cursor]
          .encode(cursor)

        val request = ListAudioPlaySeriesRequest(
          pageSize = 1.some,
          pageToken = prevToken.some,
        )
        val _ = (mockRepo.list _)
          .expects(cursor.some, 1)
          .returning(List(AudioPlaySeriesStubs.series2).pure)
        val response =
          AudioPlaySeriesMapper.toResponse(AudioPlaySeriesStubs.series2)

        for result <- service.list(request)
        yield result match
          case Left(_)     => fail("Error was not expected")
          case Right(list) => list.audioPlaySeries shouldBe List(response)
      }
    }
  }

  "search method " - {
    val query = NonEmptyString.unsafe("thing")
    val request = SearchAudioPlaySeriesRequest(
      query = query,
      limit = 2.some,
    )

    "should " - {
      "return list of elements" in stand { service =>
        val elements = List(
          AudioPlaySeriesStubs.series3,
          AudioPlaySeriesStubs.series2,
        )
        val _ = (mockRepo.search _)
          .expects(query, 2)
          .returning(elements.pure)

        for result <- service.search(request)
        yield result match
          case Left(_)     => fail("Error was not expected")
          case Right(list) => list.audioPlaySeries shouldBe elements.map(
              AudioPlaySeriesMapper.toResponse)
      }
    }
  }

  "create method " - {
    val request = CreateAudioPlaySeriesRequest(
      name = series.name,
    )

    "should " - {
      "allow users with permissions to create series if none exist" in stand {
        service =>
          val _ = mockHasPermission(Modify, true.asRight.pure)
          val _ = mockPersist(newSeries.pure)
          for result <- service.create(user, request)
          yield result shouldBe newResource.asRight
      }

      "result in InvalidSeries when creating invalid series" in stand {
        service =>
          val emptyNameRequest = request.copy(name = "")
          val _ = mockHasPermission(Modify, true.asRight.pure)
          val find = service.create(user, emptyNameRequest)
          assertDomainError(find)(InvalidSeries)
      }

      "result in PermissionDenied for unauthorized users" in stand { service =>
        val _ = mockHasPermission(Modify, false.asRight.pure)
        val find = service.create(user, request)
        assertErrorStatus(find)(PermissionDenied)
      }
    }
  }

  "delete method " - {
    val request = DeleteAudioPlaySeriesRequest(series.id)

    "should " - {
      "allow users with permissions to delete existing series" in stand {
        service =>
          val _ = mockHasPermission(Modify, true.asRight.pure)
          val _ = mockDelete(().pure)
          for result <- service.delete(user, request)
          yield result shouldBe ().asRight
      }

      "result in PermissionDenied for unauthorized users" in stand { service =>
        val _ = mockHasPermission(Modify, false.asRight.pure)
        val delete = service.delete(user, request)
        assertErrorStatus(delete)(PermissionDenied)
      }

      "handle exceptions from hasPermission gracefully" in stand { service =>
        val _ = mockHasPermission(Modify, IO.raiseError(new Throwable()))
        val delete = service.delete(user, request)
        assertInternalError(delete)
      }

      "handle exceptions from delete gracefully" in stand { service =>
        val _ = mockHasPermission(Modify, true.asRight.pure)
        val _ = mockDelete(IO.raiseError(new Throwable()))
        val delete = service.delete(user, request)
        assertInternalError(delete)
      }
    }
  }

  private def mockPersist(returning: IO[AudioPlaySeries]) =
    (mockRepo.persist _).expects(newSeries).returning(returning)

  private def mockGet(returning: IO[Option[AudioPlaySeries]]) =
    (mockRepo.get _).expects(series.id).returning(returning)

  private def mockDelete(returning: IO[Unit]) =
    (mockRepo.delete _).expects(series.id).returning(returning)

  private def mockHasPermission(
      permission: Permission,
      returning: IO[Either[ErrorResponse, Boolean]],
  ) = (mockPermissions.hasPermission _)
    .expects(user, permission)
    .returning(returning)

end AudioPlaySeriesServiceImplTest
