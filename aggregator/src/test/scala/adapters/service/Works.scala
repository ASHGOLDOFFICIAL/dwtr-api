package org.aulune.aggregator
package adapters.service


import adapters.service.errors.WorkServiceErrorResponses
import adapters.service.mappers.WorkMapper
import application.WorkService
import application.dto.work.{
  CreateWorkRequest,
  DeleteWorkRequest,
  GetWorkLocationRequest,
  GetWorkRequest,
  ListWorksRequest,
  ListWorksResponse,
  SearchWorksRequest,
  SearchWorksResponse,
  UploadWorkCoverRequest,
  WorkLocationResource,
  WorkResource,
}
import domain.model.series.Series
import domain.model.shared.ExternalResourceType.{
  Download,
  Other,
  Private,
  Purchase,
  Streaming,
}
import domain.model.shared.ReleaseDate.DateAccuracy
import domain.model.shared.ReleaseDate.DateAccuracy.{Day, Year}
import domain.model.shared.{
  ExternalResource,
  ImageUri,
  ReleaseDate,
  SelfHostedLocation,
  Synopsis,
}
import domain.model.work.{
  ActorRole,
  CastMember,
  EpisodeType,
  SeasonNumber,
  SeriesNumber,
  Title,
  Work,
}

import cats.Applicative
import cats.syntax.all.given
import org.aulune.commons.errors.ErrorResponse
import org.aulune.commons.service.auth.User
import org.aulune.commons.types.Uuid

import java.net.URI
import java.time.LocalDate


/** [[Work]] objects to use in tests. */
private[aggregator] object Works:
  private def makeCoverUri(url: String): Option[ImageUri] =
    ImageUri.unsafe(URI.create(url)).some

  private def makeReleaseDate(
      year: Int,
      month: Int,
      day: Int,
      accuracy: DateAccuracy,
  ): ReleaseDate = ReleaseDate.unsafe(LocalDate.of(year, month, day), accuracy)

  /** ''Magic Mountain'' work. */
  val work1: Work = Work.unsafe(
    id = Uuid.unsafe("3f8a202e-609d-49b2-a643-907b341cea66"),
    title = Title.unsafe("Magic Mountain"),
    synopsis = Synopsis.unsafe("Synopsis"),
    writers = List(Persons.person1.id, Persons.person2.id),
    cast = List(
      CastMember.unsafe(
        actor = Persons.person3.id,
        roles = List(ActorRole.unsafe("Hero"), ActorRole.unsafe("Narator")),
        main = true,
      ),
      CastMember.unsafe(
        actor = Persons.person2.id,
        roles = List(ActorRole.unsafe("Villian")),
        main = false,
      ),
    ),
    releaseDate = makeReleaseDate(2000, 10, 10, Day),
    seriesId = WorkSeriesStubs.series1.id.some,
    seriesSeason = SeasonNumber.unsafe(1).some,
    seriesNumber = SeriesNumber.unsafe(1).some,
    episodeType = EpisodeType.Regular.some,
    coverUrl = makeCoverUri("https://imagahost.org/123"),
    selfHostedLocation = SelfHostedLocation
      .unsafe(URI.create("file:///media/example1.mp3"))
      .some,
    externalResources = List(
      ExternalResource(Purchase, URI.create("https://test.org/1")),
      ExternalResource(Download, URI.create("https://test.org/2")),
      ExternalResource(Streaming, URI.create("https://test.org/1")),
      ExternalResource(Other, URI.create("https://test.org/2")),
      ExternalResource(Private, URI.create("https://test.org/3")),
    ),
  )

  /** ''Test of Thing'' work. */
  val work2: Work = Work.unsafe(
    id = Uuid.unsafe("3f8a202e-609d-49b2-a643-907b341cea67"),
    title = Title.unsafe("Test of Thing"),
    synopsis = Synopsis.unsafe("Synopsis 1"),
    releaseDate = makeReleaseDate(1999, 10, 31, Day),
    writers = Nil,
    cast = Nil,
    seriesId = WorkSeriesStubs.series3.id.some,
    seriesSeason = None,
    seriesNumber = SeriesNumber.unsafe(2).some,
    episodeType = EpisodeType.Special.some,
    coverUrl = makeCoverUri("https://cdn.test.org/23"),
    selfHostedLocation = SelfHostedLocation
      .unsafe(URI.create("file:///media/example2.mp3"))
      .some,
    externalResources =
      List(ExternalResource(Download, URI.create("https://audio.com/1"))),
  )

  /** ''The Testing Things'' work. */
  val work3: Work = Work.unsafe(
    id = Uuid.unsafe("3f8a202e-609d-49b2-a643-907b341cea68"),
    title = Title.unsafe("The Testing Things"),
    synopsis = Synopsis.unsafe("Synopsis 2"),
    releaseDate = makeReleaseDate(2024, 12, 31, Year),
    writers = Nil,
    cast = List(
      CastMember.unsafe(
        actor = Persons.person3.id,
        roles = List(ActorRole.unsafe("Whatever")),
        main = false,
      ),
    ),
    seriesId = None,
    seriesSeason = None,
    seriesNumber = None,
    episodeType = None,
    coverUrl = None,
    selfHostedLocation = SelfHostedLocation
      .unsafe(URI.create("file:///media/example3.mp3"))
      .some,
    externalResources =
      List(ExternalResource(Streaming, URI.create("https://audio.com/2"))),
  )

  /** Stub [[WorkService]] implementation that supports only `get` and `search`
   *  operations.
   *
   *  Contains only persons given in [[Works]] object.
   *
   *  @tparam F effect type.
   */
  def service[F[_]: Applicative]: WorkService[F] = new WorkService[F]:
    private val workById: Map[Uuid[Work], Work] = Map.from(
      List(work1, work2, work3).map(p => (p.id, p)),
    )

    override def get(
        request: GetWorkRequest,
    ): F[Either[ErrorResponse, WorkResource]] = workById
      .get(Uuid[Work](request.name))
      .map { audioPlay =>
        WorkMapper.makeResource(
          audioPlay,
          audioPlay.seriesId.map(WorkSeriesStubs.resourceById),
          Persons.resourceById,
        )
      }
      .toRight(WorkServiceErrorResponses.workNotFound)
      .pure[F]

    override def list(
        request: ListWorksRequest,
    ): F[Either[ErrorResponse, ListWorksResponse]] =
      throw new UnsupportedOperationException()

    override def search(
        request: SearchWorksRequest,
    ): F[Either[ErrorResponse, SearchWorksResponse]] =
      val elements = workById.values
        .filter(a => a.title == request.query)
        .toList
      SearchWorksResponse(
        elements.map { audioPlay =>
          WorkMapper.makeResource(
            audioPlay,
            audioPlay.seriesId.map(WorkSeriesStubs.resourceById),
            Persons.resourceById,
          )
        },
      ).asRight.pure[F]

    override def create(
        user: User,
        request: CreateWorkRequest,
    ): F[Either[ErrorResponse, WorkResource]] =
      throw new UnsupportedOperationException()

    override def delete(
        user: User,
        request: DeleteWorkRequest,
    ): F[Either[ErrorResponse, Unit]] =
      throw new UnsupportedOperationException()

    override def uploadCover(
        user: User,
        request: UploadWorkCoverRequest,
    ): F[Either[ErrorResponse, WorkResource]] =
      throw new UnsupportedOperationException()

    override def getLocation(
        user: User,
        request: GetWorkLocationRequest,
    ): F[Either[ErrorResponse, WorkLocationResource]] =
      throw new UnsupportedOperationException()
