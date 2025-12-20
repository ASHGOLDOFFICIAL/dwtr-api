package org.aulune.aggregator
package adapters.service


import adapters.service.errors.SeriesServiceErrorResponses
import adapters.service.mappers.SeriesMapper
import application.SeriesService
import application.dto.series.{
  BatchGetSeriesRequest,
  BatchGetSeriesResponse,
  CreateSeriesRequest,
  DeleteSeriesRequest,
  GetSeriesRequest,
  ListSeriesRequest,
  ListSeriesResponse,
  SearchSeriesRequest,
  SearchSeriesResponse,
  SeriesResource,
}
import domain.model.series.{Series, SeriesName}

import cats.Applicative
import cats.syntax.all.given
import org.aulune.commons.errors.ErrorResponse
import org.aulune.commons.service.auth.User
import org.aulune.commons.types.Uuid

import java.util.UUID


/** [[Series]] objects to use in tests. */
private[aggregator] object WorkSeriesStubs:
  /** ''Mega Series'' series. */
  val series1: Series = Series
    .unsafe(
      id = Uuid.unsafe("3669ae36-b459-448e-a51e-f8bbc3a41b79"),
      name = SeriesName.unsafe("Mega Series"),
    )

  /** ''Super-soap-drama'' series. */
  val series2: Series = Series
    .unsafe(
      id = Uuid.unsafe("8dddf9f1-3f59-41bb-b9b4-c97c861913c2"),
      name = SeriesName.unsafe("Super-soap-drama-series"),
    )

  /** ''Super Series'' series. */
  val series3: Series = Series
    .unsafe(
      id = Uuid.unsafe("dfaf0048-7d42-4fe5-b221-aec7aa5da90c"),
      name = SeriesName.unsafe("Super Series"),
    )

  val resourceById: Map[UUID, SeriesResource] =
    val elements = List(series1, series2, series3)
    elements.map(s => s.id -> SeriesMapper.toResponse(s)).toMap

  /** Stub [[SeriesService]] implementation that supports only `get` and
   *  `batchGet` operation.
   *
   *  Contains only series given in [[WorkSeriesStubs]] object.
   *
   *  @tparam F effect type.
   */
  def service[F[_]: Applicative]: SeriesService[F] = new SeriesService[F]:
    override def get(
        request: GetSeriesRequest,
    ): F[Either[ErrorResponse, SeriesResource]] = resourceById
      .get(request.name)
      .toRight(SeriesServiceErrorResponses.seriesNotFound)
      .pure[F]

    override def batchGet(
        request: BatchGetSeriesRequest,
    ): F[Either[ErrorResponse, BatchGetSeriesResponse]] =
      val series = request.names.mapFilter(resourceById.get)
      BatchGetSeriesResponse(series).asRight.pure[F]

    override def list(
        request: ListSeriesRequest,
    ): F[Either[ErrorResponse, ListSeriesResponse]] =
      throw new UnsupportedOperationException()

    override def search(
        request: SearchSeriesRequest,
    ): F[Either[ErrorResponse, SearchSeriesResponse]] =
      throw new UnsupportedOperationException()

    override def create(
        user: User,
        request: CreateSeriesRequest,
    ): F[Either[ErrorResponse, SeriesResource]] =
      throw new UnsupportedOperationException()

    override def delete(
        user: User,
        request: DeleteSeriesRequest,
    ): F[Either[ErrorResponse, Unit]] =
      throw new UnsupportedOperationException()
