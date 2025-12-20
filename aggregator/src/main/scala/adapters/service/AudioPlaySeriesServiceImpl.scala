package org.aulune.aggregator
package adapters.service


import adapters.service.AudioPlaySeriesServiceImpl.Cursor
import adapters.service.errors.AudioPlaySeriesServiceErrorResponses as ErrorResponses
import adapters.service.mappers.AudioPlaySeriesMapper
import application.AggregatorPermission.Modify
import application.dto.audioplay.series.{
  AudioPlaySeriesResource,
  BatchGetAudioPlaySeriesRequest,
  BatchGetAudioPlaySeriesResponse,
  CreateAudioPlaySeriesRequest,
  DeleteAudioPlaySeriesRequest,
  GetAudioPlaySeriesRequest,
  ListAudioPlaySeriesRequest,
  ListAudioPlaySeriesResponse,
  SearchAudioPlaySeriesRequest,
  SearchAudioPlaySeriesResponse,
}
import application.{AggregatorPermission, AudioPlaySeriesService}
import domain.model.audioplay.AudioPlayFilterField
import domain.model.audioplay.series.{
  AudioPlaySeries,
  AudioPlaySeriesFilterField,
}
import domain.repositories.AudioPlaySeriesRepository

import cats.MonadThrow
import cats.data.{EitherT, NonEmptyList}
import cats.syntax.all.given
import org.aulune.commons.errors.ErrorResponse
import org.aulune.commons.filter.Filter
import org.aulune.commons.filter.Filter.Operator.GreaterThan
import org.aulune.commons.filter.Filter.{Condition, Literal}
import org.aulune.commons.pagination.cursor.{CursorDecoder, CursorEncoder}
import org.aulune.commons.pagination.params.PaginationParamsParser
import org.aulune.commons.search.SearchParamsParser
import org.aulune.commons.service.auth.User
import org.aulune.commons.service.permission.PermissionClientService
import org.aulune.commons.service.permission.PermissionClientService.requirePermissionOrDeny
import org.aulune.commons.typeclasses.SortableUUIDGen
import org.aulune.commons.types.Uuid
import org.typelevel.log4cats.Logger.eitherTLogger
import org.typelevel.log4cats.syntax.LoggerInterpolator
import org.typelevel.log4cats.{Logger, LoggerFactory}

import java.util.UUID


/** [[AudioPlaySeriesService]] implementation. */
object AudioPlaySeriesServiceImpl:
  /** Builds a service.
   *  @param pagination pagination config.
   *  @param repo audio play repository.
   *  @param permissionService [[PermissionClientService]] implementation to
   *    perform permission checks.
   *  @tparam F effect type.
   *  @throws IllegalArgumentException if pagination params are invalid.
   */
  def build[F[_]: MonadThrow: SortableUUIDGen: LoggerFactory](
      maxBatchGet: Int,
      pagination: AggregatorConfig.PaginationParams,
      search: AggregatorConfig.SearchParams,
      repo: AudioPlaySeriesRepository[F],
      permissionService: PermissionClientService[F],
  ): F[AudioPlaySeriesService[F]] =
    given Logger[F] = LoggerFactory[F].getLogger
    val paginationParserO = PaginationParamsParser
      .build[Cursor](pagination.default, pagination.max)
    val searchParserO = SearchParamsParser
      .build(search.default, search.max)

    for
      _ <- info"Building service."
      _ <- MonadThrow[F]
        .raiseWhen(maxBatchGet <= 0)(new IllegalArgumentException())
        .onError(_ =>
          error"Non-positive maximum allowed number of get batch request elements.")
      paginationParser <- MonadThrow[F]
        .fromOption(paginationParserO, new IllegalArgumentException())
        .onError(_ => error"Invalid pagination parser parameters are given.")
      searchParser <- MonadThrow[F]
        .fromOption(searchParserO, new IllegalArgumentException())
        .onError(_ => error"Invalid search parser parameters are given.")
      _ <- permissionService.registerPermission(Modify)
    yield new AudioPlaySeriesServiceImpl[F](
      maxBatchGet,
      paginationParser,
      searchParser,
      repo,
      permissionService,
    )

  /** Cursor to resume pagination.
   *  @param id identity of last entry.
   */
  private final case class Cursor(id: Uuid[AudioPlaySeries])
      derives CursorEncoder,
        CursorDecoder:
    /** Converts cursor to filter AST. */
    def toFilter: Filter[AudioPlaySeriesFilterField] = Condition(
      AudioPlaySeriesFilterField.Id,
      GreaterThan,
      Literal(id.toString))

end AudioPlaySeriesServiceImpl


private final class AudioPlaySeriesServiceImpl[F[
    _,
]: MonadThrow: SortableUUIDGen: LoggerFactory] private (
    maxBatchGet: Int,
    paginationParser: PaginationParamsParser[Cursor],
    searchParser: SearchParamsParser,
    repo: AudioPlaySeriesRepository[F],
    permissionService: PermissionClientService[F],
) extends AudioPlaySeriesService[F]:

  private given Logger[F] = LoggerFactory[F].getLogger
  private given PermissionClientService[F] = permissionService

  override def get(
      request: GetAudioPlaySeriesRequest,
  ): F[Either[ErrorResponse, AudioPlaySeriesResource]] =
    val uuid = Uuid[AudioPlaySeries](request.name)
    (for
      _ <- eitherTLogger.info(s"Find request: $request.")
      elem <- EitherT
        .fromOptionF(repo.get(uuid), ErrorResponses.seriesNotFound)
        .leftSemiflatTap(_ => warn"Couldn't find element with ID: $request")
      response = AudioPlaySeriesMapper.toResponse(elem)
    yield response).value.handleErrorWith(handleInternal)

  override def batchGet(
      request: BatchGetAudioPlaySeriesRequest,
  ): F[Either[ErrorResponse, BatchGetAudioPlaySeriesResponse]] = (for
    _ <- eitherTLogger.info(s"Batch get request: $request.")
    ids <- EitherT.fromEither(parseIdList(request.names))
    elems <- EitherT.liftF(repo.batchGet(ids))
    response <- EitherT.fromEither(checkNoneIsMissing(ids, elems))
  yield response).value.handleErrorWith(handleInternal)

  override def list(
      request: ListAudioPlaySeriesRequest,
  ): F[Either[ErrorResponse, ListAudioPlaySeriesResponse]] =
    val paramsV = paginationParser.parse(request.pageSize, request.pageToken)
    (for
      _ <- eitherTLogger.info(s"List request: $request.")
      params <- EitherT
        .fromOption(paramsV.toOption, ErrorResponses.invalidPaginationParams)
        .leftSemiflatTap(_ => warn"Invalid pagination params are given.")
      elems <-
        EitherT.liftF(repo.list(params.pageSize, params.cursor.map(_.toFilter)))

      nextPageToken = elems.lastOption
        .map(elem => CursorEncoder[Cursor].encode(Cursor(elem.id)))
        .filter(_ => elems.size == params.pageSize)
      response = ListAudioPlaySeriesResponse(
        elems.map(AudioPlaySeriesMapper.toResponse),
        nextPageToken)
    yield response).value.handleErrorWith(handleInternal)

  override def search(
      request: SearchAudioPlaySeriesRequest,
  ): F[Either[ErrorResponse, SearchAudioPlaySeriesResponse]] =
    val paramsV = searchParser.parse(request.query, request.limit)
    (for
      _ <- eitherTLogger.info(s"Search request: $request.")
      params <- EitherT
        .fromOption(paramsV.toOption, ErrorResponses.invalidSearchParams)
        .leftSemiflatTap(_ => warn"Invalid search params are given.")
      searchResult = repo.search(params.query, params.limit)
      elems <- EitherT.liftF(searchResult)
      response = AudioPlaySeriesMapper.toSearchResponse(elems)
    yield response).value.handleErrorWith(handleInternal)

  override def create(
      user: User,
      request: CreateAudioPlaySeriesRequest,
  ): F[Either[ErrorResponse, AudioPlaySeriesResource]] =
    requirePermissionOrDeny(Modify, user) {
      val uuid = SortableUUIDGen.randomTypedUUID[F, AudioPlaySeries]
      (for
        _ <- eitherTLogger.info(s"Create request $request from $user.")
        id <- EitherT.liftF(uuid)
        audio <- EitherT
          .fromEither(makeAudioPlaySeries(request, id))
          .leftSemiflatTap(_ => warn"Request to create bad element: $request.")
        persisted <- EitherT.liftF(repo.persist(audio))
        response = AudioPlaySeriesMapper.toResponse(persisted)
      yield response).value
    }.handleErrorWith(handleInternal)

  override def delete(
      user: User,
      request: DeleteAudioPlaySeriesRequest,
  ): F[Either[ErrorResponse, Unit]] = requirePermissionOrDeny(Modify, user) {
    val uuid = Uuid[AudioPlaySeries](request.name)
    info"Delete request $request from $user" >> repo.delete(uuid).map(_.asRight)
  }.handleErrorWith(handleInternal)

  /** Transforms list of UUIDs to NEL of typed UUIDs if possible. Or returns the
   *  appropriate error response.
   *  @param ids list of persons IDs.
   */
  private def parseIdList(
      ids: List[UUID],
  ): Either[ErrorResponse, NonEmptyList[Uuid[AudioPlaySeries]]] =
    for
      _ <- Either.raiseWhen(ids.size > maxBatchGet)(
        ErrorResponses.maxExceededBatchGet(maxBatchGet))
      idsO = NonEmptyList.fromList(ids.map(Uuid[AudioPlaySeries]))
      parsed <- Either.fromOption(idsO, ErrorResponses.emptyBatchGet)
    yield parsed

  /** Checks that for ID a series is given.
   *  @param ids IDs of series.
   *  @param series given series.
   */
  private def checkNoneIsMissing(
      ids: NonEmptyList[Uuid[AudioPlaySeries]],
      series: List[AudioPlaySeries],
  ): Either[ErrorResponse, BatchGetAudioPlaySeriesResponse] =
    val set = series.map(_.id).toSet
    val missing = ids.filterNot(set.contains)
    for
      _ <- NonEmptyList
        .fromList(missing)
        .toLeft(())
        .leftMap(ErrorResponses.seriesNotFound)
      response = BatchGetAudioPlaySeriesResponse(
        audioPlaySeries = series.map(AudioPlaySeriesMapper.toResponse),
      )
    yield response

  /** Makes series from given creation request and assigned ID.
   *  @param request creation request.
   *  @param id ID assigned to this series.
   *  @note It's only purpose is to improve readability of [[create]] method.
   */
  private def makeAudioPlaySeries(
      request: CreateAudioPlaySeriesRequest,
      id: Uuid[AudioPlaySeries],
  ) = AudioPlaySeriesMapper
    .fromRequest(request, id)
    .leftMap(ErrorResponses.invalidSeries)
    .toEither

  /** Logs any error and returns internal error response. */
  private def handleInternal[A](e: Throwable) =
    for _ <- Logger[F].error(e)("Uncaught exception.")
    yield ErrorResponses.internal.asLeft[A]
