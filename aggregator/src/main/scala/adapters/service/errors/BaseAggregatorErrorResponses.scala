package org.aulune.aggregator
package adapters.service.errors


import org.aulune.commons.errors.ErrorStatus.{Internal, InvalidArgument}
import org.aulune.commons.errors.{ErrorDetails, ErrorResponse}


/** Error responses shared among aggregator services. */
trait BaseAggregatorErrorResponses:
  protected val domain = "org.aulune.aggregator"

  val internal: ErrorResponse = ErrorResponse(
    status = Internal,
    message = "Internal error.",
    details = ErrorDetails(),
  )

  val emptyBatchGet: ErrorResponse = ErrorResponse(
    status = InvalidArgument,
    message = "Empty batch get request is given",
    details = ErrorDetails(),
  )

  /** Maximum allowed elements for batch get request is exceeded.
   *  @param max maximum allowed.
   */
  def maxExceededBatchGet(max: Int): ErrorResponse = ErrorResponse(
    status = InvalidArgument,
    message = s"Too many elements, max allowed: $max",
    details = ErrorDetails(),
  )

  val invalidSearchParams: ErrorResponse = ErrorResponse(
    status = InvalidArgument,
    message = "Query should be non-empty",
    details = ErrorDetails(),
  )

  val invalidPaginationParams: ErrorResponse = ErrorResponse(
    status = InvalidArgument,
    message = "Given pagination params are invalid.",
    details = ErrorDetails(),
  )

  val invalidFilter: ErrorResponse = ErrorResponse(
    status = InvalidArgument,
    message = "Given filter is not valid.",
    details = ErrorDetails(),
  )
