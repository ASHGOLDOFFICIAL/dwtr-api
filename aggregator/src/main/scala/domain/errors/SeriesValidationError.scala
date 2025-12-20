package org.aulune.aggregator
package domain.errors

import scala.util.control.NoStackTrace


/** Errors that can occur during series validation. */
enum SeriesValidationError extends NoStackTrace:
  /** Some given arguments are invalid */
  case InvalidArguments
