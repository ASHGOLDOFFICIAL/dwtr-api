package org.aulune.aggregator
package adapters.service


import adapters.service.AudioPlayServiceImpl.{PngExtension, PngMimeType}
import adapters.service.errors.AudioPlayServiceErrorResponses as ErrorResponses
import adapters.service.mappers.AudioPlayMapper
import application.AggregatorPermission.{Modify, SeeSelfHostedLocation}
import application.dto.audioplay.series.{
  AudioPlaySeriesResource,
  BatchGetAudioPlaySeriesRequest,
  GetAudioPlaySeriesRequest,
}
import application.dto.audioplay.{
  AudioPlayLocationResource,
  AudioPlayResource,
  CreateAudioPlayRequest,
  DeleteAudioPlayRequest,
  GetAudioPlayLocationRequest,
  GetAudioPlayRequest,
  ListAudioPlaysRequest,
  ListAudioPlaysResponse,
  SearchAudioPlaysRequest,
  SearchAudioPlaysResponse,
  UploadAudioPlayCoverRequest,
}
import application.dto.person.{BatchGetPersonsRequest, PersonResource}
import application.errors.{AudioPlaySeriesServiceError, PersonServiceError}
import application.{
  AggregatorPermission,
  AudioPlaySeriesService,
  AudioPlayService,
  PersonService,
}
import domain.errors.{AudioPlayConstraint, AudioPlayValidationError}
import domain.model.audioplay.AudioPlay
import domain.model.audioplay.series.AudioPlaySeries
import domain.model.shared.ImageUri
import domain.repositories.AudioPlayRepository.{AudioPlayCursor, given}
import domain.repositories.{AudioPlayRepository, CoverImageStorage}

import cats.MonadThrow
import cats.data.EitherT
import cats.effect.Async
import cats.effect.std.UUIDGen
import cats.syntax.all.given
import fs2.Stream
import org.aulune.commons.errors.{ErrorInfo, ErrorResponse}
import org.aulune.commons.pagination.cursor.CursorEncoder
import org.aulune.commons.pagination.params.PaginationParamsParser
import org.aulune.commons.repositories.RepositoryError
import org.aulune.commons.search.SearchParamsParser
import org.aulune.commons.service.auth.User
import org.aulune.commons.service.permission.PermissionClientService
import org.aulune.commons.service.permission.PermissionClientService.requirePermissionOrDeny
import org.aulune.commons.typeclasses.SortableUUIDGen
import org.aulune.commons.types.{NonEmptyString, Uuid}
import org.aulune.commons.utils.imaging.ImageFormat.PNG
import org.aulune.commons.utils.imaging.{ImageConversionError, ImageConverter}
import org.typelevel.log4cats.Logger.eitherTLogger
import org.typelevel.log4cats.syntax.LoggerInterpolator
import org.typelevel.log4cats.{Logger, LoggerFactory}

import java.net.URI
import java.util.UUID


/** [[AudioPlayService]] implementation. */
object AudioPlayServiceImpl:
  /** Builds a service.
   *  @param pagination pagination config.
   *  @param repo audio play repository.
   *  @param seriesService [[AudioPlaySeriesService]] implementation to retrieve
   *    audio play series.
   *  @param personService [[PersonService]] implementation to retrieve cast and
   *    writers.
   *  @param permissionService [[PermissionClientService]] implementation to
   *    perform permission checks.
   *  @tparam F effect type.
   *  @throws IllegalArgumentException if pagination params are invalid.
   */
  def build[F[_]: Async: SortableUUIDGen: LoggerFactory](
      pagination: AggregatorConfig.PaginationParams,
      search: AggregatorConfig.SearchParams,
      imageLimits: AggregatorConfig.ImageLimits,
      repo: AudioPlayRepository[F],
      coverStorage: CoverImageStorage[F],
      seriesService: AudioPlaySeriesService[F],
      personService: PersonService[F],
      permissionService: PermissionClientService[F],
      imageConverter: ImageConverter[F],
  ): F[AudioPlayService[F]] =
    given Logger[F] = LoggerFactory[F].getLogger
    val paginationParserO = PaginationParamsParser
      .build[AudioPlayCursor](pagination.default, pagination.max)
    val searchParserO = SearchParamsParser
      .build(search.default, search.max)

    for
      _ <- info"Building service."
      paginationParser <- MonadThrow[F]
        .fromOption(paginationParserO, new IllegalArgumentException())
        .onError(_ => error"Invalid pagination parser parameters are given.")
      searchParser <- MonadThrow[F]
        .fromOption(searchParserO, new IllegalArgumentException())
        .onError(_ => error"Invalid search parser parameters are given.")
      _ <- permissionService.registerPermission(Modify)
      _ <- permissionService.registerPermission(SeeSelfHostedLocation)
    yield new AudioPlayServiceImpl[F](
      paginationParser,
      searchParser,
      imageLimits,
      repo,
      coverStorage,
      seriesService,
      personService,
      permissionService,
      imageConverter,
    )

  private val PngExtension = NonEmptyString("png")
  private val PngMimeType = NonEmptyString("image/png")

end AudioPlayServiceImpl


private final class AudioPlayServiceImpl[F[
    _,
]: Async: SortableUUIDGen: LoggerFactory](
    paginationParser: PaginationParamsParser[AudioPlayCursor],
    searchParser: SearchParamsParser,
    imageLimits: AggregatorConfig.ImageLimits,
    repo: AudioPlayRepository[F],
    coverStorage: CoverImageStorage[F],
    seriesService: AudioPlaySeriesService[F],
    personService: PersonService[F],
    permissionService: PermissionClientService[F],
    imageConverter: ImageConverter[F],
) extends AudioPlayService[F]:

  private val maximumImageSize = imageLimits.maxSize
  private given Logger[F] = LoggerFactory[F].getLogger
  private given PermissionClientService[F] = permissionService

  override def get(
      request: GetAudioPlayRequest,
  ): F[Either[ErrorResponse, AudioPlayResource]] =
    val uuid = Uuid[AudioPlay](request.name)
    (for
      _ <- eitherTLogger.info(s"Get request: $request.")
      elem <- EitherT
        .fromOptionF(repo.get(uuid), ErrorResponses.audioPlayNotFound)
        .leftSemiflatTap(_ => warn"Couldn't find element with ID: $request")
      response <- makeResponse(elem)
    yield response).value.handleErrorWith(handleInternal)

  override def list(
      request: ListAudioPlaysRequest,
  ): F[Either[ErrorResponse, ListAudioPlaysResponse]] =
    val paramsV = paginationParser.parse(request.pageSize, request.pageToken)
    (for
      _ <- eitherTLogger.info(s"List request: $request.")
      params <- EitherT
        .fromOption(paramsV.toOption, ErrorResponses.invalidPaginationParams)
        .leftSemiflatTap(_ => warn"Invalid pagination params are given.")
      elems <- EitherT.liftF(repo.list(params.cursor, params.pageSize))
      series <- EitherT(batchGetSeries(elems))
      persons <- EitherT(batchGetPersons(elems))
      resources = elems.map { e =>
        AudioPlayMapper.makeResource(e, e.seriesId.map(series), persons)
      }
      token = makePaginationToken(elems.lastOption)
      response = ListAudioPlaysResponse(resources, token)
    yield response).value.handleErrorWith(handleInternal)

  override def search(
      request: SearchAudioPlaysRequest,
  ): F[Either[ErrorResponse, SearchAudioPlaysResponse]] =
    val paramsV = searchParser.parse(request.query, request.limit)
    (for
      _ <- eitherTLogger.info(s"Search request: $request.")
      params <- EitherT
        .fromOption(paramsV.toOption, ErrorResponses.invalidSearchParams)
        .leftSemiflatTap(_ => warn"Invalid search params are given.")
      elems <- EitherT.liftF(repo.search(params.query, params.limit))
      series <- EitherT(batchGetSeries(elems))
      persons <- EitherT(batchGetPersons(elems))
      resources = elems.map { e =>
        AudioPlayMapper.makeResource(e, e.seriesId.map(series), persons)
      }
      response = SearchAudioPlaysResponse(resources)
    yield response).value.handleErrorWith(handleInternal)

  override def create(
      user: User,
      request: CreateAudioPlayRequest,
  ): F[Either[ErrorResponse, AudioPlayResource]] =
    requirePermissionOrDeny(Modify, user) {
      val seriesId = request.seriesId.map(Uuid[AudioPlaySeries])
      val uuid = SortableUUIDGen.randomTypedUUID[F, AudioPlay]
      (for
        _ <- eitherTLogger.info(s"Create request $request from $user.")
        series <- EitherT(getSeries(seriesId))
        allPersonIds = (request.writers ++ request.cast.map(_.actor)).distinct
        persons <- EitherT(getPersons(allPersonIds))
        id <- EitherT.liftF(uuid)
        audio <- EitherT
          .fromEither(makeAudioPlay(request, id))
          .leftSemiflatTap(_ => warn"Request to create bad element: $request.")
        persisted <- EitherT(repo.persist(audio).map(_.asRight).recoverWith {
          case RepositoryError.ConstraintViolation(
                 AudioPlayConstraint.UniqueSeriesInfo) =>
            ErrorResponses.duplicateSeriesInfo.asLeft.pure[F]
        })
        response = AudioPlayMapper.makeResource(persisted, series, persons)
      yield response).value
    }.handleErrorWith(handleInternal)

  override def delete(
      user: User,
      request: DeleteAudioPlayRequest,
  ): F[Either[ErrorResponse, Unit]] = requirePermissionOrDeny(Modify, user) {
    val uuid = Uuid[AudioPlay](request.name)
    info"Delete request $request from $user" >> repo.delete(uuid).map(_.asRight)
  }.handleErrorWith(handleInternal)

  override def uploadCover(
      user: User,
      request: UploadAudioPlayCoverRequest,
  ): F[Either[ErrorResponse, AudioPlayResource]] =
    requirePermissionOrDeny(Modify, user) {
      val id = Uuid[AudioPlay](request.name)
      (for
        _ <- eitherTLogger.info(s"Upload cover request for $id from $user")
        elem <- EitherT
          .fromOptionF(repo.get(id), ErrorResponses.audioPlayNotFound)
        uri <- uploadImage(request.cover)
        coverUri = ImageUri(uri)
        _ <- eitherTLogger.info(s"Cover URI: $coverUri")
        updated <- EitherT
          .fromEither(elem.update(coverUrl = coverUri).toEither)
          .leftMap(ErrorResponses.invalidAudioPlay)
        persisted <- EitherT.liftF(repo.update(updated))
        response <- makeResponse(persisted)
      yield response).value
    }.handleErrorWith(handleInternal)

  override def getLocation(
      user: User,
      request: GetAudioPlayLocationRequest,
  ): F[Either[ErrorResponse, AudioPlayLocationResource]] =
    requirePermissionOrDeny(SeeSelfHostedLocation, user) {
      val uuid = Uuid[AudioPlay](request.name)
      (for
        _ <- eitherTLogger.info(s"Get location request: $request.")
        elem <- EitherT
          .fromOptionF(repo.get(uuid), ErrorResponses.audioPlayNotFound)
          .leftSemiflatTap(_ => warn"Couldn't find element with ID: $request")
        link <- EitherT
          .fromOption(elem.selfHostedLocation, ErrorResponses.notSelfHosted)
        response = AudioPlayLocationResource(link)
      yield response).value
    }.handleErrorWith(handleInternal)

  /** Upload image to external service with conversion to PNG.
   *  @param imageBytes image as bytes.
   *  @return URI of uploaded image.
   */
  private def uploadImage(
      imageBytes: IArray[Byte],
  ): EitherT[F, ErrorResponse, URI] =
    for
      imageStream <- EitherT.cond(
        imageBytes.length <= maximumImageSize,
        Stream.emits[F, Byte](imageBytes),
        ErrorResponses.coverTooBigImage(maximumImageSize))
      convertF = imageConverter.convert(imageStream, PNG)
      image <- EitherT(convertF).leftSemiflatMap {
        case ImageConversionError.UnknownFormat =>
          ErrorResponses.invalidCoverImage.pure[F]
        case e => MonadThrow[F].raiseError(e)
      }
      convertedStream = Stream.emits(image)
      objectId <- EitherT.liftF(UUIDGen.randomUUID[F])
      name = NonEmptyString.unsafe(s"$objectId.$PngExtension")
      _ <- EitherT
        .liftF(coverStorage.put(convertedStream, name, PngMimeType.some))
      uri <- EitherT.liftF(coverStorage.issueURI(name)).semiflatMap {
        case Some(value) => value.pure[F]
        case None        => MonadThrow[F].raiseError(new IllegalStateException(
            "Couldn't issue URI for just added object."))
      }
    yield uri

  /** Populates audio play with resources retrieved from respective services.
   *  @param element audio play to use as base.
   */
  private def makeResponse(
      element: AudioPlay,
  ): EitherT[F, ErrorResponse, AudioPlayResource] =
    for
      series <- EitherT(getSeries(element.seriesId))
      allPersonIds = (element.writers ++ element.cast.map(_.actor)).distinct
      persons <- EitherT(getPersons(allPersonIds))
      response = AudioPlayMapper.makeResource(element, series, persons)
    yield response

  /** Retrieves person resources for a list of audio plays. */
  private def batchGetPersons(
      xs: List[AudioPlay],
  ): F[Either[ErrorResponse, Map[UUID, PersonResource]]] =
    val uuids = xs.flatMap(e => (e.writers ++ e.cast.map(_.actor)).distinct)
    getPersons(uuids)

  /** Retrieves person resources for given IDs. */
  private def getPersons(
      ids: List[UUID],
  ): F[Either[ErrorResponse, Map[UUID, PersonResource]]] = ids match
    case Nil => Map.empty.asRight.pure[F]
    case _   => personService
        .batchGet(BatchGetPersonsRequest(ids))
        .map {
          case Right(response) =>
            response.persons.map(p => p.id -> p).toMap.asRight
          case Left(err) => err.details.info match
              case Some(err)
                   if err.reason == PersonServiceError.PersonNotFound =>
                ErrorResponses.personNotFound.asLeft
              case _ => err.asLeft
        }

  private def batchGetSeries(
      elems: List[AudioPlay],
  ): F[Either[ErrorResponse, Map[UUID, AudioPlaySeriesResource]]] = elems match
    case Nil => Map.empty.asRight.pure[F]
    case _   =>
      val ids = elems.mapFilter(_.seriesId)
      seriesService
        .batchGet(BatchGetAudioPlaySeriesRequest(ids))
        .map {
          case Right(response) =>
            response.audioPlaySeries.map(s => s.id -> s).toMap.asRight
          case Left(err) => err.details.info match
              case Some(err)
                   if err.reason == AudioPlaySeriesServiceError.SeriesNotFound =>
                ErrorResponses.audioPlaySeriesNotFound.asLeft
              case _ => err.asLeft
        }

  /** Returns [[AudioPlaySeriesResource]] if ID is not `None` and there exists
   *  audio play series with it. If ID is not `None` but there's no
   *  [[AudioPlaySeriesResource]] found with it, then it will result in error
   *  response.
   *  @param seriesId audio play series ID.
   */
  private def getSeries(
      seriesId: Option[Uuid[AudioPlaySeries]],
  ): F[Either[ErrorResponse, Option[AudioPlaySeriesResource]]] = seriesId match
    case None     => None.asRight.pure[F]
    case Some(id) =>
      seriesService.get(GetAudioPlaySeriesRequest(name = id)).map {
        case Right(response) => response.some.asRight
        case Left(err)       => err.details.info match
            case Some(err)
                 if err.reason == AudioPlaySeriesServiceError.SeriesNotFound =>
              ErrorResponses.audioPlaySeriesNotFound.asLeft
            case _ => err.asLeft
      }

  /** Makes audio play from given creation request, assigned ID and series.
   *  @param request creation request.
   *  @param id ID assigned to this audio play.
   *  @note It's only purpose is to improve readability of [[create]] method.
   */
  private def makeAudioPlay(
      request: CreateAudioPlayRequest,
      id: Uuid[AudioPlay],
  ) = AudioPlayMapper
    .fromRequest(request, id)
    .leftMap(ErrorResponses.invalidAudioPlay)
    .toEither

  /** Converts list of domain objects to one list response.
   *  @param last last sent element.
   */
  private def makePaginationToken(
      last: Option[AudioPlay],
  ): Option[String] = last.map { elem =>
    val cursor = AudioPlayCursor(elem.id)
    CursorEncoder[AudioPlayCursor].encode(cursor)
  }

  /** Logs any error and returns internal error response. */
  private def handleInternal[A](e: Throwable) =
    for _ <- Logger[F].error(e)("Uncaught exception.")
    yield ErrorResponses.internal.asLeft[A]
