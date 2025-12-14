package org.aulune.commons
package filter.parser


/** Errors that can occur during filter string parsing. */
enum FilterParsingError:
  /** String can not be converted to expression tree. */
  case InvalidFormat

  /** String references unknown field. */
  case UnknownField(field: String)

  /** Value for operation doesn't match field value. */
  case TypeMismatch(field: String, operation: RawFilter.Operation)

  /** Operation isn't supported for this field. */
  case UnsupportedOperation(field: String, operation: RawFilter.Operation)
