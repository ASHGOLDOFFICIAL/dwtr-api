package org.aulune.aggregator
package adapters.service


import adapters.service.mappers.{
  EpisodeTypeMapper,
  ExternalResourceMapper,
  ReleaseDateMapper,
  WorkMapper,
}
import application.AggregatorPermission.{Modify, SeeSelfHostedLocation}
import application.WorkService
import application.dto.series.SeriesResource
import application.dto.work.WorkResource.CastMemberResource
import application.dto.work.{
  CastMemberDTO,
  CreateWorkRequest,
  DeleteWorkRequest,
  GetWorkLocationRequest,
  GetWorkRequest,
  ListWorksRequest,
  ListWorksResponse,
  SearchWorksRequest,
  UploadWorkCoverRequest,
  WorkResource,
}
import application.errors.WorkServiceError.{
  CoverTooBig,
  DuplicateSeriesInfo,
  InvalidCoverImage,
  InvalidWork,
  NotSelfHosted,
  SeriesNotFound,
  WorkNotFound,
}
import domain.errors.WorkConstraint
import domain.model.series.Series
import domain.model.shared.ImageUri
import domain.model.work.{Work, WorkField}
import domain.repositories.{CoverImageStorage, WorkRepository}

import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import cats.syntax.all.given
import fs2.Stream
import org.aulune.commons.errors.ErrorResponse
import org.aulune.commons.errors.ErrorStatus.PermissionDenied
import org.aulune.commons.filter.Filter.Operator.GreaterThan
import org.aulune.commons.filter.Filter.{Condition, Literal}
import org.aulune.commons.repositories.RepositoryError
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
import org.aulune.commons.testing.syntax.*
import org.aulune.commons.typeclasses.SortableUUIDGen
import org.aulune.commons.types.{NonEmptyString, Uuid}
import org.aulune.commons.utils.imaging.{
  ImageConversionError,
  ImageConverter,
  ImageFormat,
}
import org.scalamock.scalatest.AsyncMockFactory
import org.scalatest.Assertion
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers
import org.typelevel.log4cats.LoggerFactory
import org.typelevel.log4cats.slf4j.Slf4jFactory

import java.net.URI
import java.util.UUID


/** Tests for [[WorkServiceImpl]]. */
final class WorkServiceImplTest
    extends AsyncFreeSpec
    with AsyncIOSpec
    with Matchers
    with AsyncMockFactory:

  private given LoggerFactory[IO] = Slf4jFactory.create

  private val mockRepo = mock[WorkRepository[IO]]
  private val mockCoverStorage = mock[CoverImageStorage[IO]]
  private val mockSeries = WorkSeriesStubs.service[IO]
  private val mockPerson = Persons.service[IO]
  private val mockPermissions = mock[PermissionClientService[IO]]
  private val mockConverter = mock[ImageConverter[IO]]

  private val uuid = UUID.fromString("00000000-0000-0000-0000-000000000001")
  private given SortableUUIDGen[IO] = makeFixedUuidGen(uuid)

  private val user = User(
    id = UUID.fromString("f04eb510-229c-4cdd-bd7b-9691c3b28ae1"),
    username = "username",
  )

  private def stand(
      testCase: WorkService[IO] => IO[Assertion],
  ): IO[Assertion] =
    val _ = (mockPermissions.registerPermission _)
      .expects(*)
      .returning(().asRight.pure)
      .anyNumberOfTimes()
    WorkServiceImpl
      .build(
        AggregatorConfig.PaginationParams(2, 1),
        AggregatorConfig.SearchParams(2, 1),
        AggregatorConfig.ImageLimits(3),
        mockRepo,
        mockCoverStorage,
        mockSeries,
        mockPerson,
        mockPermissions,
        mockConverter,
      )
      .flatMap(testCase)
  end stand

  private val work = Works.work1

  private val resource = WorkResource(
    id = work.id,
    title = work.title,
    synopsis = work.synopsis,
    releaseDate = ReleaseDateMapper.fromDomain(work.releaseDate),
    writers = work.writers.map(Persons.resourceById),
    cast = work.cast.map(m =>
      CastMemberResource(
        actor = Persons.resourceById(m.actor),
        roles = m.roles,
        main = m.main,
      )),
    series = work.seriesId.map(WorkSeriesStubs.resourceById),
    seriesSeason = work.seriesSeason,
    seriesNumber = work.seriesNumber,
    episodeType = work.episodeType.map(EpisodeTypeMapper.fromDomain),
    coverUri = work.coverUri,
    externalResources = work.externalResources
      .map(ExternalResourceMapper.fromDomain),
  )
  private val newUuid = Uuid[Work](uuid)
  private val newWork = work
    .update(id = newUuid, coverUrl = None)
    .getOrElse(throw new IllegalStateException())
  private val newResource = resource.copy(
    id = newUuid,
    coverUri = None,
  )

  "get method " - {
    val request = GetWorkRequest(work.id)

    "should " - {
      "find elements if they're present in repository" in stand { service =>
        val _ = mockGet(work.some.pure)
        for result <- service.get(request)
        yield result shouldBe resource.asRight
      }

      "result in WorkNotFound if work doesn't exist" in stand { service =>
        val _ = mockGet(None.pure)
        val find = service.get(request)
        assertDomainError(find)(WorkNotFound)
      }

      "handle errors from repository gracefully" in stand { service =>
        val _ = mockGet(IO.raiseError(new Throwable()))
        val find = service.get(request)
        assertInternalError(find)
      }
    }
  }

  "list method " - {
    "should " - {
      "list elements" in stand { service =>
        val request = ListWorksRequest(
          pageSize = 1.some,
          pageToken = None,
        )
        val _ = (mockRepo.list _)
          .expects(1, None)
          .returning(List(work).pure)

        for result <- service.list(request)
        yield result match
          case Left(_)     => fail("Error was not expected")
          case Right(list) => list.works shouldBe List(resource)
      }

      "return next page when asked" in stand { service =>
        val request = ListWorksRequest(
          pageSize = 1.some,
          pageToken = None,
        )

        val _ = (mockRepo.list _)
          .expects(1, None)
          .returning(List(Works.work2).pure)

        val filter =
          Condition(WorkField.Id, GreaterThan, Literal(Works.work2.id.toString))
        val _ = (mockRepo.list _)
          .expects(1, Some(filter))
          .returning(List(Works.work1).pure)

        for
          first <- service.list(request)
          secondRequest = request.copy(pageToken = first.getRight.nextPageToken)
          result <- service.list(secondRequest)
        yield result.getRight.works shouldBe List(resource)
      }
    }
  }

  "search method " - {
    val query = NonEmptyString.unsafe("thing")
    val request = SearchWorksRequest(
      query = query,
      limit = 2.some,
    )

    "should " - {
      "return list of elements" in stand { service =>
        val elements = List(
          Works.work3,
          Works.work2,
        )
        val _ = (mockRepo.search _)
          .expects(query, 2)
          .returning(elements.pure)

        for result <- service.search(request)
        yield result match
          case Left(_)     => fail("Error was not expected")
          case Right(list) => list.works shouldBe elements.map { ap =>
              WorkMapper.makeResource(
                ap,
                ap.seriesId.map(WorkSeriesStubs.resourceById),
                Persons.resourceById,
              )
            }
      }
    }
  }

  "create method " - {
    val request = CreateWorkRequest(
      title = work.title,
      synopsis = work.synopsis,
      releaseDate = ReleaseDateMapper.fromDomain(work.releaseDate),
      writers = work.writers,
      cast = work.cast.map(m =>
        CastMemberDTO(
          actor = m.actor,
          roles = m.roles,
          main = m.main,
        )),
      seriesId = work.seriesId,
      seriesSeason = work.seriesSeason,
      seriesNumber = work.seriesNumber,
      episodeType = work.episodeType.map(EpisodeTypeMapper.fromDomain),
      selfHostedLocation = work.selfHostedLocation,
      externalResources = resource.externalResources,
    )

    "should " - {
      "allow users with permissions to create works if none exist" in stand {
        service =>
          val _ = mockHasPermission(Modify, true.asRight.pure)
          val _ = mockPersist(newWork.pure)
          for result <- service.create(user, request)
          yield result shouldBe newResource.asRight
      }

      "result in InvalidWork when creating invalid work" in stand { service =>
        val emptyNameRequest = request.copy(title = "")
        val _ = mockHasPermission(Modify, true.asRight.pure)
        val find = service.create(user, emptyNameRequest)
        assertDomainError(find)(InvalidWork)
      }

      "result in SeriesNotFound when creating " +
        "work of non-existent series" in stand { service =>
          val badRequest = request.copy(
            seriesId =
              UUID.fromString("a72e0458-b29f-4ac5-b19f-21197a8d18f8").some,
          )
          val _ = mockHasPermission(Modify, true.asRight.pure)
          val find = service.create(user, badRequest)
          assertDomainError(find)(SeriesNotFound)
        }

      "result in DuplicateSeriesInfo when creating " +
        "work with already taken series info" in stand { service =>
          val _ = mockHasPermission(Modify, true.asRight.pure)
          val _ = mockPersist(
            IO.raiseError(RepositoryError.ConstraintViolation(
              WorkConstraint.UniqueSeriesInfo)))
          val find = service.create(user, request)
          assertDomainError(find)(DuplicateSeriesInfo)
        }

      "result in PermissionDenied for unauthorized users" in stand { service =>
        val _ = mockHasPermission(Modify, false.asRight.pure)
        val find = service.create(user, request)
        assertErrorStatus(find)(PermissionDenied)
      }

      "handle exceptions from hasPermission gracefully" in stand { service =>
        val _ = mockHasPermission(Modify, IO.raiseError(new Throwable()))
        val find = service.create(user, request)
        assertInternalError(find)
      }

      "handle exceptions from persist gracefully" in stand { service =>
        val _ = mockHasPermission(Modify, true.asRight.pure)
        val _ = mockPersist(IO.raiseError(new Throwable()))
        val find = service.create(user, request)
        assertInternalError(find)
      }
    }
  }

  "delete method " - {
    val request = DeleteWorkRequest(work.id)

    "should " - {
      "allow users with permissions to delete existing works" in stand {
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

  "uploadCover method " - {
    val request = UploadWorkCoverRequest(
      name = work.id,
      cover = IArray[Byte](1, 2, 3),
    )
    val name = NonEmptyString.unsafe(s"$uuid.png")
    val pngMime = NonEmptyString.unsafe("image/png")
    val uri = ImageUri.unsafe(URI.create("http://new.test.org/"))
    val updated = work
      .update(coverUrl = uri.some)
      .getOrElse(throw new IllegalStateException())

    "should " - {
      "upload covers" in stand { service =>
        val _ = mockHasPermission(Modify, true.asRight.pure)
        val _ = mockGet(work.some.pure)

        val _ = (mockConverter.convert _)
          .expects(*, ImageFormat.PNG, None)
          .onCall { (s, f, size) =>
            s.compile.toVector.map(IArray.from(_).asRight[ImageConversionError])
          }

        val _ = (mockCoverStorage.put _)
          .expects(*, name, pngMime.some)
          .returning(().pure)
        val _ = (mockCoverStorage.issueURI _)
          .expects(name)
          .returning(uri.some.pure)

        val _ = (mockRepo.update _)
          .expects(updated)
          .returning(updated.pure)

        for result <- service.uploadCover(user, request)
        yield result shouldBe resource.copy(coverUri = uri.some).asRight
      }

      "result in WorkNotFound if work is not found" in stand { service =>
        val _ = mockHasPermission(Modify, true.asRight.pure)
        val _ = mockGet(None.pure)
        val result = service.uploadCover(user, request)
        assertDomainError(result)(WorkNotFound)
      }

      "result in CoverTooBig if image size exceeds maximum allowed" in stand {
        service =>
          val bigRequest = request.copy(cover = IArray(1, 2, 3, 4, 5))
          val _ = mockHasPermission(Modify, true.asRight.pure)
          val _ = mockGet(work.some.pure)
          val result = service.uploadCover(user, bigRequest)
          assertDomainError(result)(CoverTooBig)
      }

      "result in InvalidCoverImage when given not an image" in stand {
        service =>
          val _ = mockHasPermission(Modify, true.asRight.pure)
          val _ = mockGet(work.some.pure)

          val _ = (mockConverter.convert _)
            .expects(*, *, *)
            .returning(ImageConversionError.UnknownFormat.asLeft.pure)

          val result = service.uploadCover(user, request)
          assertDomainError(result)(InvalidCoverImage)
      }

      "result in PermissionDenied for unauthorized users" in stand { service =>
        val _ = mockHasPermission(Modify, false.asRight.pure)
        val result = service.uploadCover(user, request)
        assertErrorStatus(result)(PermissionDenied)
      }
    }
  }

  "getLocation method " - {
    val request = GetWorkLocationRequest(work.id)

    "should " - {
      "allow users with permissions to see self-hosted locations" in stand {
        service =>
          val _ = mockHasPermission(SeeSelfHostedLocation, true.asRight.pure)
          val _ = mockGet(work.some.pure)
          for result <- service.getLocation(user, request)
          yield result match
            case Left(_)         => fail("Error was not expected.")
            case Right(response) =>
              response.uri shouldBe work.selfHostedLocation.get
      }

      "result in WorkNotFound if work doesn't exist" in stand { service =>
        val _ = mockHasPermission(SeeSelfHostedLocation, true.asRight.pure)
        val _ = mockGet(None.pure)
        val get = service.getLocation(user, request)
        assertDomainError(get)(WorkNotFound)
      }

      "result in NotSelfHosted if work is not hosted" in stand { service =>
        val notSelfHosted = work
          .update(selfHostedLocation = None)
          .getOrElse(throw new IllegalStateException())

        val _ = mockHasPermission(SeeSelfHostedLocation, true.asRight.pure)
        val _ = mockGet(notSelfHosted.some.pure)
        val get = service.getLocation(user, request)
        assertDomainError(get)(NotSelfHosted)
      }

      "result in PermissionDenied for unauthorized users" in stand { service =>
        val _ = mockHasPermission(SeeSelfHostedLocation, false.asRight.pure)
        val result = service.getLocation(user, request)
        assertErrorStatus(result)(PermissionDenied)
      }
    }
  }

  private def mockPersist(returning: IO[Work]) =
    (mockRepo.persist _).expects(newWork).returning(returning)

  private def mockGet(returning: IO[Option[Work]]) =
    (mockRepo.get _).expects(work.id).returning(returning)

  private def mockDelete(returning: IO[Unit]) =
    (mockRepo.delete _).expects(work.id).returning(returning)

  private def mockHasPermission(
      permission: Permission,
      returning: IO[Either[ErrorResponse, Boolean]],
  ) = (mockPermissions.hasPermission _)
    .expects(user, permission)
    .returning(returning)

end WorkServiceImplTest
