package org.aulune.commons
package pagination.params

import scala.util.control.NoStackTrace


/** Errors that can occur during pagination params parsing. */
enum PaginationValidationError extends NoStackTrace:
  /** Given page size is not valid, i.e. is negative. */
  case InvalidPageSize

  /** Given token cannot be decoded. */
  case InvalidPageToken
