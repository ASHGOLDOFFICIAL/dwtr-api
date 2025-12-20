package org.aulune.aggregator
package application


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
import application.errors.SeriesServiceError.{InvalidSeries, SeriesNotFound}

import org.aulune.commons.errors.ErrorResponse
import org.aulune.commons.service.auth.User


trait SeriesService[F[_]]:
  /** Get series by given ID.
   *
   *  Domain error [[SeriesNotFound]] will be returned if series is not found.
   *
   *  @param request request to get a series.
   *  @return requested series if found.
   */
  def get(
      request: GetSeriesRequest,
  ): F[Either[ErrorResponse, SeriesResource]]

  /** Gets series by their identities in batches.
   *
   *  Persons are returned in the same order as in request.
   *
   *  Domain error [[SeriesNotFound]] will be returned if any of the series are
   *  not found.
   *
   *  @param request request with IDs.
   *  @return resources for every given ID or error.
   */
  def batchGet(
      request: BatchGetSeriesRequest,
  ): F[Either[ErrorResponse, BatchGetSeriesResponse]]

  /** Get a portion of series.
   *  @param request request to list series.
   *  @return list of series if success, otherwise error.
   */
  def list(
      request: ListSeriesRequest,
  ): F[Either[ErrorResponse, ListSeriesResponse]]

  /** Search series by some query.
   *  @param request request with search information.
   *  @return response with matched series if success, otherwise error.
   */
  def search(
      request: SearchSeriesRequest,
  ): F[Either[ErrorResponse, SearchSeriesResponse]]

  /** Create new series.
   *
   *  Domain error [[InvalidSeries]] will be returned when trying to create
   *  invalid series.
   *
   *  @param user user who performs this action.
   *  @param request request to create new series.
   *  @return created series if success, otherwise error.
   *  @note user must have [[AggregatorPermission.Modify]] permission.
   */
  def create(
      user: User,
      request: CreateSeriesRequest,
  ): F[Either[ErrorResponse, SeriesResource]]

  /** Deletes existing series.
   *  @param user user who performs this action.
   *  @param request request to delete a series.
   *  @return `Unit` if success, otherwise error.
   *  @note user must have [[AggregatorPermission.Modify]] permission.
   */
  def delete(
      user: User,
      request: DeleteSeriesRequest,
  ): F[Either[ErrorResponse, Unit]]
