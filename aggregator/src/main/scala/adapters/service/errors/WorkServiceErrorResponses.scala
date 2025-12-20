package org.aulune.aggregator
package adapters.service.errors


import application.errors.WorkServiceError.{
  CoverTooBig,
  DuplicateSeriesInfo,
  InvalidCoverImage,
  InvalidWork,
  NotSelfHosted,
  PersonNotFound,
  SeriesNotFound,
  WorkNotFound,
}
import domain.errors.WorkValidationError

import cats.data.NonEmptyChain
import cats.syntax.all.given
import org.aulune.commons.errors.ErrorStatus.{
  FailedPrecondition,
  InvalidArgument,
  NotFound,
}
import org.aulune.commons.errors.{ErrorDetails, ErrorInfo, ErrorResponse}


/** Error responses for
 *  [[org.aulune.aggregator.adapters.service.WorkServiceImpl]].
 */
object WorkServiceErrorResponses extends BaseAggregatorErrorResponses:
  val seriesNotFound: ErrorResponse = ErrorResponse(
    status = FailedPrecondition,
    message = "Series with given ID was not found",
    details = ErrorDetails(
      info = ErrorInfo(
        reason = SeriesNotFound,
        domain = domain,
      ).some,
    ),
  )

  val personNotFound: ErrorResponse = ErrorResponse(
    status = FailedPrecondition,
    message = "Writer/cast member/other person wasn't found",
    details = ErrorDetails(
      info = ErrorInfo(
        reason = PersonNotFound,
        domain = domain,
      ).some,
    ),
  )

  val workNotFound: ErrorResponse = ErrorResponse(
    status = NotFound,
    message = "Work is not found.",
    details = ErrorDetails(
      info = ErrorInfo(
        reason = WorkNotFound,
        domain = domain,
      ).some,
    ),
  )

  val notSelfHosted: ErrorResponse = ErrorResponse(
    status = NotFound,
    message = "Work is not self hosted.",
    details = ErrorDetails(
      info = ErrorInfo(
        reason = NotSelfHosted,
        domain = domain,
      ).some,
    ),
  )

  val invalidCoverImage: ErrorResponse = ErrorResponse(
    status = InvalidArgument,
    message = "Invalid cover image.",
    details = ErrorDetails(
      info = ErrorInfo(
        reason = InvalidCoverImage,
        domain = domain,
      ).some,
    ),
  )

  val duplicateSeriesInfo: ErrorResponse = ErrorResponse(
    status = InvalidArgument,
    message = "Combination of series, season and series number " +
      "is already taken by another work.",
    details = ErrorDetails(
      info = ErrorInfo(
        reason = DuplicateSeriesInfo,
        domain = domain,
      ).some,
    ),
  )

  /** Cover image is too big.
   *  @param max maximum allowed size.
   */
  def coverTooBigImage(max: Long): ErrorResponse = ErrorResponse(
    status = InvalidArgument,
    message = s"Image size exceeds maximum allowed: $max bytes",
    details = ErrorDetails(
      info = ErrorInfo(
        reason = CoverTooBig,
        domain = domain,
      ).some,
    ),
  )

  def invalidWork(
      errs: NonEmptyChain[WorkValidationError],
  ): ErrorResponse = ErrorResponse(
    status = InvalidArgument,
    message = errs
      .map(representValidationError)
      .mkString_("Invalid work is given: ", ", ", "."),
    details = ErrorDetails(
      info = ErrorInfo(
        reason = InvalidWork,
        domain = domain,
      ).some,
    ),
  )

  /** Returns string representation of [[WorkValidationError]].
   *
   *  @param err validation error.
   */
  private def representValidationError(err: WorkValidationError): String =
    err match
      case WorkValidationError.InvalidTitle        => "invalid title"
      case WorkValidationError.InvalidSynopsis     => "invalid synopsis"
      case WorkValidationError.InvalidReleaseDate  => "invalid release date"
      case WorkValidationError.InvalidCast         => "invalid cast"
      case WorkValidationError.InvalidSeason       => "invalid season"
      case WorkValidationError.InvalidSeriesNumber => "invalid series number"
      case WorkValidationError.InvalidSelfHostedLocation =>
        "invalid self-hosted location"
      case WorkValidationError.WriterDuplicates =>
        "duplicate writers are not allowed"
      case WorkValidationError.CastMemberDuplicates =>
        "duplicate cast members are not allowed"
      case WorkValidationError.EpisodeTypeIsMissing =>
        "episode type is needed when series is given"
      case WorkValidationError.SeriesIsMissing =>
        "series is needed when season or series number is given"
