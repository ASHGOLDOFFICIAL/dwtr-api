package org.aulune.aggregator
package application


import application.dto.work.{
  WorkLocationResource,
  WorkResource,
  CreateWorkRequest,
  DeleteWorkRequest,
  GetWorkLocationRequest,
  GetWorkRequest,
  ListWorksRequest,
  ListWorksResponse,
  SearchWorksRequest,
  SearchWorksResponse,
  UploadWorkCoverRequest,
}
import application.errors.WorkServiceError.{
  WorkNotFound,
  SeriesNotFound,
  CoverTooBig,
  DuplicateSeriesInfo,
  InvalidWork,
  InvalidCoverImage,
  NotSelfHosted,
}

import org.aulune.commons.errors.ErrorResponse
import org.aulune.commons.service.auth.User


/** Service managing pieces of media.
 *  @tparam F effect type.
 */
trait WorkService[F[_]]:
  /** Get piece of media by given ID.
   *
   *  Domain error [[WorkNotFound]] will be returned if piece is not found.
   *
   *  @param request request to get a piece.
   *  @return requested piece if found.
   */
  def get(
      request: GetWorkRequest,
  ): F[Either[ErrorResponse, WorkResource]]

  /** Get some pieces of media.
   *  @param request request to list pieces.
   *  @return list of pieces if success, otherwise error.
   */
  def list(
      request: ListWorksRequest,
  ): F[Either[ErrorResponse, ListWorksResponse]]

  /** Search pieces by some query.
   *  @param request request with search information.
   *  @return response with matched pieces if success, otherwise error.
   */
  def search(
      request: SearchWorksRequest,
  ): F[Either[ErrorResponse, SearchWorksResponse]]

  /** Create a new piece of media.
   *
   *  Domain errors:
   *    - [[InvalidWork]] will be returned when trying to create invalid piece.
   *    - [[DuplicateSeriesInfo]] will be returned when adding work with already
   *      used series info.
   *    - [[SeriesNotFound]] will be returned when trying to create piece with
   *      ID of non-existent series.
   *
   *  @param user user who performs this action.
   *  @param request piece creation request.
   *  @return created piece if success, otherwise error.
   *  @note user must have [[AggregatorPermission.Modify]] permission.
   */
  def create(
      user: User,
      request: CreateWorkRequest,
  ): F[Either[ErrorResponse, WorkResource]]

  /** Deletes existing piece of media.
   *  @param user user who performs this action.
   *  @param request request to delete a piece of media.
   *  @return `Unit` if success, otherwise error.
   *  @note user must have [[AggregatorPermission.Modify]] permission.
   */
  def delete(
      user: User,
      request: DeleteWorkRequest,
  ): F[Either[ErrorResponse, Unit]]

  /** Uploads image as cover for piece of media.
   *
   *  Domain errors:
   *    - [[WorkNotFound]] will be returned if piece is not found.
   *    - [[CoverTooBig]] will be returned if cover image exceeds maximum
   *      allowed restrictions.
   *    - [[InvalidCoverImage]] will be returned when trying to upload invalid
   *      image.
   *    - [[InvalidWork]] will be returned when result of the operation leads to
   *      invalid piece.
   *
   *  @param user user who performs this action.
   *  @param request request to upload cover.
   *  @return changed resource if success, otherwise error.
   *  @note user must have [[AggregatorPermission.Modify]] permission.
   */
  def uploadCover(
      user: User,
      request: UploadWorkCoverRequest,
  ): F[Either[ErrorResponse, WorkResource]]

  /** Gets piece of media self-hosted location.
   *
   *  Domain errors:
   *    - [[WorkNotFound]] will be returned if work is not found.
   *    - [[NotSelfHosted]] will be returned if work is not self-hosted.
   *
   *  @param user user who performs this action.
   *  @param request request information.
   *  @return response with URI if everything is OK, otherwise error.
   *  @note user must have [[AggregatorPermission.SeeSelfHostedLocation]]
   *    permission.
   */
  def getLocation(
      user: User,
      request: GetWorkLocationRequest,
  ): F[Either[ErrorResponse, WorkLocationResource]]
