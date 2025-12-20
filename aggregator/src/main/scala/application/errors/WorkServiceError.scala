package org.aulune.aggregator
package application.errors


import application.WorkService

import org.aulune.commons.errors.ErrorReason


/** Errors that can occur in [[WorkService]].
 *
 *  @param reason string representation of error.
 */
enum WorkServiceError(val reason: String) extends ErrorReason(reason):
  /** Specified series is not found. */
  case SeriesNotFound extends WorkServiceError("SERIES_NOT_FOUND")

  /** Writer, or cast member, or etc. wasn't found */
  case PersonNotFound extends WorkServiceError("PERSON_NOT_FOUND")

  /** No work with given ID is found. */
  case WorkNotFound extends WorkServiceError("WORK_NOT_FOUND")

  /** Work is not self-hosted. */
  case NotSelfHosted extends WorkServiceError("NOT_SELF_HOSTED")

  /** Combination of series, season and series number is already taken by
   *  another work.
   */
  case DuplicateSeriesInfo extends WorkServiceError("DUPLICATE_SERIES_INFO")

  /** Given work is not a valid work. */
  case InvalidWork extends WorkServiceError("INVALID_WORK")

  /** Given image exceeds restrictions for cover size. */
  case CoverTooBig extends WorkServiceError("COVER_IS_TOO_BIG")

  /** Given cover image is invalid. */
  case InvalidCoverImage extends WorkServiceError("INVALID_COVER_IMAGE")
