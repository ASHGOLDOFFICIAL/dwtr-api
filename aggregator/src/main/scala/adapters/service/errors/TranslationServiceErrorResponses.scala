package org.aulune.aggregator
package adapters.service.errors


import application.errors.TranslationServiceError.{
  InvalidTranslation,
  NotSelfHosted,
  OriginalNotFound,
  TranslationNotFound,
}
import domain.errors.TranslationValidationError

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
object TranslationServiceErrorResponses extends BaseAggregatorErrorResponses:

  val originalNotFound: ErrorResponse = ErrorResponse(
    status = FailedPrecondition,
    message = "Original work is not found.",
    details = ErrorDetails(
      info = ErrorInfo(
        reason = OriginalNotFound,
        domain = domain,
      ).some,
    ),
  )

  val translationNotFound: ErrorResponse = ErrorResponse(
    status = NotFound,
    message = "Translation is not found.",
    details = ErrorDetails(
      info = ErrorInfo(
        reason = TranslationNotFound,
        domain = domain,
      ).some,
    ),
  )

  def invalidTranslation(
      errs: NonEmptyChain[TranslationValidationError],
  ): ErrorResponse = ErrorResponse(
    status = InvalidArgument,
    message = errs
      .map(representValidationError)
      .mkString_("Invalid translation is given: ", ", ", "."),
    details = ErrorDetails(
      info = ErrorInfo(
        reason = InvalidTranslation,
        domain = domain,
      ).some,
    ),
  )

  val notSelfHosted: ErrorResponse = ErrorResponse(
    status = NotFound,
    message = "Translation is not self hosted.",
    details = ErrorDetails(
      info = ErrorInfo(
        reason = NotSelfHosted,
        domain = domain,
      ).some,
    ),
  )

  /** Returns string representation of [[TranslationValidationError]].
   *  @param err validation error.
   */
  private def representValidationError(
      err: TranslationValidationError,
  ): String = err match
    case TranslationValidationError.InvalidArguments => "arguments are invalid"
