package org.aulune.commons
package filter.parser

import types.NonEmptyString


/** Filter expression tree model without type safety. */
private[parser] enum RawFilter:
  case Condition(field: NonEmptyString, op: RawFilter.Operation)
  case And(left: RawFilter, right: RawFilter)
  case Or(left: RawFilter, right: RawFilter)
  case Not(inner: RawFilter)


private[parser] object RawFilter:
  enum Operation:
    case Eq(value: NonEmptyString)
    case Ne(value: NonEmptyString)
    case Gt(value: NonEmptyString)
    case Lt(value: NonEmptyString)
    case Ge(value: NonEmptyString)
    case Le(value: NonEmptyString)
    case In(values: List[NonEmptyString])
