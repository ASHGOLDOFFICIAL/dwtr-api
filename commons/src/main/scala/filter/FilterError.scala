package org.aulune.commons
package filter

import types.NonEmptyString


/** Errors that can occur during filter string parsing. */
enum FilterError:
  /** String can not be converted to expression tree. */
  case SyntaxError

  /** String references unknown field. */
  case UnknownField(field: NonEmptyString)

  /** Operation isn't supported for this field. */
  case UnsupportedOperation[A: FilterField](
      field: A,
      operation: Filter.Operator,
      value: Filter.Literal,
  )
