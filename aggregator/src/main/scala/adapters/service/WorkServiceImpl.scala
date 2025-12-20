package org.aulune.aggregator
package adapters.service


import adapters.service.WorkServiceImpl.{Cursor, PngExtension, PngMimeType}
import adapters.service.errors.WorkServiceErrorResponses as ErrorResponses
import adapters.service.mappers.WorkMapper
import application.AggregatorPermission.{Modify, SeeSelfHostedLocation}
import application.dto.person.{BatchGetPersonsRequest, PersonResource}
import application.dto.series.{
  BatchGetSeriesRequest,
  GetSeriesRequest,
  SeriesResource,
}
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
import application.errors.{PersonServiceError, SeriesServiceError}
import application.{
  AggregatorPermission,
  PersonService,
  SeriesService,
  WorkService,
}
import domain.errors.{WorkConstraint, WorkValidationError}
import domain.model.series.Series
import domain.model.shared.ImageUri
import domain.model.work.{Work, WorkField}
import domain.repositories.{CoverImageStorage, WorkRepository}

import cats.MonadThrow
import cats.data.EitherT
import cats.effect.Async
import cats.effect.std.UUIDGen
import cats.syntax.all.given
import fs2.Stream
import org.aulune.commons.errors.{ErrorInfo, ErrorResponse}
import org.aulune.commons.filter.Filter
import org.aulune.commons.filter.Filter.Operator.GreaterThan
import org.aulune.commons.filter.Filter.{Condition, Literal}
import org.aulune.commons.pagination.cursor.{CursorDecoder, CursorEncoder}
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


/** [[WorkService]] implementation. */
object WorkServiceImpl:
  /** Builds a service.
   *
   *  @param pagination pagination config.
   *  @param repo work repository.
   *  @param seriesService [[SeriesService]] implementation to retrieve series.
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
      repo: WorkRepository[F],
      coverStorage: CoverImageStorage[F],
      seriesService: SeriesService[F],
      personService: PersonService[F],
      permissionService: PermissionClientService[F],
      imageConverter: ImageConverter[F],
  ): F[WorkService[F]] =
    given Logger[F] = LoggerFactory[F].getLogger
    val paginationParserO = PaginationParamsParser
      .build[Cursor](pagination.default, pagination.max)
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
    yield new WorkServiceImpl[F](
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

  /** Cursor to resume pagination.
   *  @param id identity of [[Work]].
   */
  private final case class Cursor(id: Uuid[Work])
      derives CursorEncoder,
        CursorDecoder:
    /** Converts cursor to filter AST. */
    def toFilter: Filter[WorkField] =
      Condition(WorkField.Id, GreaterThan, Literal(id.toString))

end WorkServiceImpl


private final class WorkServiceImpl[F[
    _,
]: Async: SortableUUIDGen: LoggerFactory] private (
    paginationParser: PaginationParamsParser[Cursor],
    searchParser: SearchParamsParser,
    imageLimits: AggregatorConfig.ImageLimits,
    repo: WorkRepository[F],
    coverStorage: CoverImageStorage[F],
    seriesService: SeriesService[F],
    personService: PersonService[F],
    permissionService: PermissionClientService[F],
    imageConverter: ImageConverter[F],
) extends WorkService[F]:

  private val maximumImageSize = imageLimits.maxSize
  private given Logger[F] = LoggerFactory[F].getLogger
  private given PermissionClientService[F] = permissionService

  override def get(
      request: GetWorkRequest,
  ): F[Either[ErrorResponse, WorkResource]] =
    val uuid = Uuid[Work](request.name)
    (for
      _ <- eitherTLogger.info(s"Get request: $request.")
      elem <- EitherT
        .fromOptionF(repo.get(uuid), ErrorResponses.workNotFound)
        .leftSemiflatTap(_ => warn"Couldn't find element with ID: $request")
      response <- makeResponse(elem)
    yield response).value.handleErrorWith(handleInternal)

  override def list(
      request: ListWorksRequest,
  ): F[Either[ErrorResponse, ListWorksResponse]] =
    val paramsV = paginationParser.parse(request.pageSize, request.pageToken)
    (for
      _ <- eitherTLogger.info(s"List request: $request.")
      params <- EitherT
        .fromOption(paramsV.toOption, ErrorResponses.invalidPaginationParams)
        .leftSemiflatTap(_ => warn"Invalid pagination params are given.")
      elems <-
        EitherT.liftF(repo.list(params.pageSize, params.cursor.map(_.toFilter)))
      series <- EitherT(batchGetSeries(elems))
      persons <- EitherT(batchGetPersons(elems))
      resources = elems.map { e =>
        WorkMapper.makeResource(e, e.seriesId.map(series), persons)
      }
      token = makePaginationToken(elems.lastOption)
      response = ListWorksResponse(resources, token)
    yield response).value.handleErrorWith(handleInternal)

  override def search(
      request: SearchWorksRequest,
  ): F[Either[ErrorResponse, SearchWorksResponse]] =
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
        WorkMapper.makeResource(e, e.seriesId.map(series), persons)
      }
      response = SearchWorksResponse(resources)
    yield response).value.handleErrorWith(handleInternal)

  override def create(
      user: User,
      request: CreateWorkRequest,
  ): F[Either[ErrorResponse, WorkResource]] =
    requirePermissionOrDeny(Modify, user) {
      val seriesId = request.seriesId.map(Uuid[Series])
      val uuid = SortableUUIDGen.randomTypedUUID[F, Work]
      (for
        _ <- eitherTLogger.info(s"Create request $request from $user.")
        series <- EitherT(getSeries(seriesId))
        allPersonIds = (request.writers ++ request.cast.map(_.actor)).distinct
        persons <- EitherT(getPersons(allPersonIds))
        id <- EitherT.liftF(uuid)
        audio <- EitherT
          .fromEither(makeWork(request, id))
          .leftSemiflatTap(_ => warn"Request to create bad element: $request.")
        persisted <- EitherT(repo.persist(audio).map(_.asRight).recoverWith {
          case RepositoryError.ConstraintViolation(
                 WorkConstraint.UniqueSeriesInfo) =>
            ErrorResponses.duplicateSeriesInfo.asLeft.pure[F]
        })
        response = WorkMapper.makeResource(persisted, series, persons)
      yield response).value
    }.handleErrorWith(handleInternal)

  override def delete(
      user: User,
      request: DeleteWorkRequest,
  ): F[Either[ErrorResponse, Unit]] = requirePermissionOrDeny(Modify, user) {
    val uuid = Uuid[Work](request.name)
    info"Delete request $request from $user" >> repo.delete(uuid).map(_.asRight)
  }.handleErrorWith(handleInternal)

  override def uploadCover(
      user: User,
      request: UploadWorkCoverRequest,
  ): F[Either[ErrorResponse, WorkResource]] =
    requirePermissionOrDeny(Modify, user) {
      val id = Uuid[Work](request.name)
      (for
        _ <- eitherTLogger.info(s"Upload cover request for $id from $user")
        elem <- EitherT
          .fromOptionF(repo.get(id), ErrorResponses.workNotFound)
        uri <- uploadImage(request.cover)
        coverUri = ImageUri(uri)
        _ <- eitherTLogger.info(s"Cover URI: $coverUri")
        updated <- EitherT
          .fromEither(elem.update(coverUrl = coverUri).toEither)
          .leftMap(ErrorResponses.invalidWork)
        persisted <- EitherT.liftF(repo.update(updated))
        response <- makeResponse(persisted)
      yield response).value
    }.handleErrorWith(handleInternal)

  override def getLocation(
      user: User,
      request: GetWorkLocationRequest,
  ): F[Either[ErrorResponse, WorkLocationResource]] =
    requirePermissionOrDeny(SeeSelfHostedLocation, user) {
      val uuid = Uuid[Work](request.name)
      (for
        _ <- eitherTLogger.info(s"Get location request: $request.")
        elem <- EitherT
          .fromOptionF(repo.get(uuid), ErrorResponses.workNotFound)
          .leftSemiflatTap(_ => warn"Couldn't find element with ID: $request")
        link <- EitherT
          .fromOption(elem.selfHostedLocation, ErrorResponses.notSelfHosted)
        response = WorkLocationResource(link)
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

  /** Populates work with resources retrieved from respective services.
   *  @param element work to use as base.
   */
  private def makeResponse(
      element: Work,
  ): EitherT[F, ErrorResponse, WorkResource] =
    for
      series <- EitherT(getSeries(element.seriesId))
      allPersonIds = (element.writers ++ element.cast.map(_.actor)).distinct
      persons <- EitherT(getPersons(allPersonIds))
      response = WorkMapper.makeResource(element, series, persons)
    yield response

  /** Retrieves person resources for a list of works. */
  private def batchGetPersons(
      xs: List[Work],
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
      elems: List[Work],
  ): F[Either[ErrorResponse, Map[UUID, SeriesResource]]] = elems match
    case Nil => Map.empty.asRight.pure[F]
    case _   =>
      val ids = elems.mapFilter(_.seriesId)
      seriesService
        .batchGet(BatchGetSeriesRequest(ids))
        .map {
          case Right(response) =>
            response.works.map(s => s.id -> s).toMap.asRight
          case Left(err) => err.details.info match
              case Some(err)
                   if err.reason == SeriesServiceError.SeriesNotFound =>
                ErrorResponses.seriesNotFound.asLeft
              case _ => err.asLeft
        }

  /** Returns [[SeriesResource]] if ID is not `None` and there exists a series
   *  with it. If ID is not `None` but there's no [[SeriesResource]] found with
   *  it, then it will result in error response.
   *
   *  @param seriesId series ID.
   */
  private def getSeries(
      seriesId: Option[Uuid[Series]],
  ): F[Either[ErrorResponse, Option[SeriesResource]]] = seriesId match
    case None     => None.asRight.pure[F]
    case Some(id) => seriesService.get(GetSeriesRequest(name = id)).map {
        case Right(response) => response.some.asRight
        case Left(err)       => err.details.info match
            case Some(err) if err.reason == SeriesServiceError.SeriesNotFound =>
              ErrorResponses.seriesNotFound.asLeft
            case _ => err.asLeft
      }

  /** Makes work from given creation request, assigned ID and series.
   *  @param request creation request.
   *  @param id ID assigned to this work.
   *  @note It's only purpose is to improve readability of [[create]] method.
   */
  private def makeWork(
      request: CreateWorkRequest,
      id: Uuid[Work],
  ) = WorkMapper
    .fromRequest(request, id)
    .leftMap(ErrorResponses.invalidWork)
    .toEither

  /** Converts list of domain objects to one list response.
   *  @param last last sent element.
   */
  private def makePaginationToken(
      last: Option[Work],
  ): Option[String] = last.map { elem =>
    val cursor = Cursor(elem.id)
    CursorEncoder[Cursor].encode(cursor)
  }

  /** Logs any error and returns internal error response. */
  private def handleInternal[A](e: Throwable) =
    for _ <- Logger[F].error(e)("Uncaught exception.")
    yield ErrorResponses.internal.asLeft[A]
