package org.aulune.aggregator
package adapters.service


import adapters.service.testdata.SeriesStubs
import adapters.service.mappers.SeriesMapper
import application.AggregatorPermission.Modify
import application.SeriesService
import application.dto.series.{
  BatchGetSeriesRequest,
  CreateSeriesRequest,
  DeleteSeriesRequest,
  GetSeriesRequest,
  ListSeriesRequest,
  SearchSeriesRequest,
  SeriesResource,
}
import application.errors.SeriesServiceError.{InvalidSeries, SeriesNotFound}
import domain.model.series.{Series, SeriesField}
import domain.repositories.SeriesRepository

import cats.data.NonEmptyList
import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import cats.syntax.all.given
import org.aulune.commons.errors.ErrorResponse
import org.aulune.commons.errors.ErrorStatus.PermissionDenied
import org.aulune.commons.filter.Filter.Operator.GreaterThan
import org.aulune.commons.filter.Filter.{Condition, Literal}
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
import org.aulune.commons.testing.instances.UUIDGenInstances
import org.aulune.commons.testing.syntax.{getInvalid, getRight}
import org.aulune.commons.typeclasses.SortableUUIDGen
import org.aulune.commons.types.{NonEmptyString, Uuid}
import org.scalamock.scalatest.AsyncMockFactory
import org.scalatest.Assertion
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers
import org.typelevel.log4cats.LoggerFactory
import org.typelevel.log4cats.noop.NoOpFactory
import org.typelevel.log4cats.slf4j.Slf4jFactory

import java.util.UUID


/** Tests for [[SeriesServiceImpl]]. */
final class SeriesServiceImplSpec
    extends AsyncFlatSpec
    with AsyncIOSpec
    with Matchers
    with AsyncMockFactory:

  import Samples.*

  private given LoggerFactory[IO] = NoOpFactory.impl[IO]
  private given SortableUUIDGen[IO] = UUIDGenInstances.makeFixedUuidGen()

  private val mockRepo = mock[SeriesRepository[IO]]
  private val mockPermissions = mock[PermissionClientService[IO]]

  private def stand(
      testCase: SeriesService[IO] => IO[Assertion],
  ): IO[Assertion] =
    val _ = (mockPermissions.registerPermission _)
      .expects(*)
      .returning(().asRight.pure)
      .anyNumberOfTimes()

    SeriesServiceImpl
      .build(
        2,
        AggregatorConfig.PaginationParams(2, 1),
        AggregatorConfig.SearchParams(2, 1),
        mockRepo,
        mockPermissions)
      .flatMap(testCase)
  end stand

  behavior of "get"

  it should "find series if it's present in repository" in stand { service =>
    val _ = mockGet(series.some.pure)
    for result <- service.get(getRequest)
    yield result shouldBe resource.asRight
  }

  it should "result in SeriesNotFound if series doesn't exist" in stand {
    service =>
      val _ = mockGet(None.pure)
      val find = service.get(getRequest)
      assertDomainError(find)(SeriesNotFound)
  }

  behavior of "batchGet"
  
  "batchGet method " - {
    val elements = NonEmptyList.of(SeriesStubs.series1, SeriesStubs.series2)
    val ids = elements.map(_.id)
    val request = BatchGetSeriesRequest(names = ids.toList)

    "should " - {
      "return batch of elements when all are found" in stand { service =>
        val _ = (mockRepo.batchGet _)
          .expects(ids)
          .returning(elements.toList.pure)

        for result <- service.batchGet(request)
        yield result.getRight.works.map(_.id) shouldBe ids.toList
      }

      "return SeriesNotFound if at least one series is not found" in stand {
        service =>
          val _ = (mockRepo.batchGet _)
            .expects(ids)
            .returning(List(SeriesStubs.series1).pure)

          val batchGet = service.batchGet(request)
          assertDomainError(batchGet)(SeriesNotFound)
      }
    }
  }

  "list method " - {
    "should " - {
      "list elements" in stand { service =>
        val request = ListSeriesRequest(
          pageSize = 1.some,
          pageToken = None,
        )
        val _ = (mockRepo.list _).expects(1, None).returning(List(series).pure)
        for result <- service.list(request)
        yield result.getRight.series shouldBe List(resource)
      }

      "return next page when asked" in stand { service =>
        val request = ListSeriesRequest(
          pageSize = 1.some,
          pageToken = None,
        )

        val _ = (mockRepo.list _)
          .expects(1, None)
          .returning(List(SeriesStubs.series1).pure)

        val filter = Condition(
          SeriesField.Id,
          GreaterThan,
          Literal(SeriesStubs.series1.id.toString))
        val _ = (mockRepo.list _)
          .expects(1, Some(filter))
          .returning(List(SeriesStubs.series1).pure)

        val response = SeriesMapper.toResponse(SeriesStubs.series1)

        for
          first <- service.list(request)
          newRequest = request.copy(pageToken = first.getRight.nextPageToken)
          second <- service.list(newRequest)
        yield second.getRight.series shouldBe List(response)
      }
    }
  }

  "search method " - {
    val query = NonEmptyString.unsafe("thing")
    val request = SearchSeriesRequest(
      query = query,
      limit = 2.some,
    )

    "should " - {
      "return list of elements" in stand { service =>
        val elements = List(
          SeriesStubs.series3,
          SeriesStubs.series2,
        )
        val _ = (mockRepo.search _)
          .expects(query, 2)
          .returning(elements.pure)

        val expected = elements.map(SeriesMapper.toResponse)
        for result <- service.search(request)
        yield result.getRight.series shouldBe expected
      }
    }
  }

  "create method " - {
    val request = CreateSeriesRequest(
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
    val request = DeleteSeriesRequest(series.id)

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

  private def mockPersist(returning: IO[Series]) =
    (mockRepo.persist _).expects(newSeries).returning(returning)

  private def mockGet(returning: IO[Option[Series]]) =
    (mockRepo.get _).expects(series.id).returning(returning)

  private def mockDelete(returning: IO[Unit]) =
    (mockRepo.delete _).expects(series.id).returning(returning)

  private def mockHasPermission(
      permission: Permission,
      returning: IO[Either[ErrorResponse, Boolean]],
  ) = (mockPermissions.hasPermission _)
    .expects(user, permission)
    .returning(returning)

  object Samples:
    val user: User = User(
      id = UUID.fromString("f04eb510-229c-4cdd-bd7b-9691c3b28ae1"),
      username = "username",
    )

    val seriesList: NonEmptyList[Series] =
      NonEmptyList.of(SeriesStubs.series1, SeriesStubs.series2)
    val series: Series = seriesList.head

    val resource: SeriesResource = SeriesResource(
      id = series.id,
      name = series.name,
    )
    val newUuid: Uuid[Series] = Uuid[Series](UUIDGenInstances.fixedUuid)
    val newSeries: Series = series
      .update(id = newUuid)
      .getOrElse(throw new IllegalStateException())
    val newResource: SeriesResource = resource.copy(
      id = newSeries.id,
      name = newSeries.name,
    )
    val getRequest: GetSeriesRequest = GetSeriesRequest(series.id)

end SeriesServiceImplSpec
