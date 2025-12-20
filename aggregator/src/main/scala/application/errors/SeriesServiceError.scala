package org.aulune.aggregator
package application.errors


import application.SeriesService

import org.aulune.commons.errors.ErrorReason


/** Errors that can occur in [[SeriesService]].
 *
 *  @param reason string representation of error.
 */
enum SeriesServiceError(val reason: String) extends ErrorReason(reason):
  /** No series with given ID is found. */
  case SeriesNotFound extends SeriesServiceError("SERIES_NOT_FOUND")

  /** Given series is not valid. */
  case InvalidSeries extends SeriesServiceError("INVALID_SERIES")
