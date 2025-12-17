package org.aulune.commons
package filter


import filter.Filter.Literal.{BooleanLiteral, NumberLiteral, StringLiteral}
import filter.Filter.{Literal, Operator}
import types.NonEmptyString


/** Witnesses that A can be used as a field in filter expression.
 *  @tparam A type for typeclass.
 */
trait FilterField[A]:
  /** Returns [[A]] that corresponds to given field name. if any.
   *  @param name field name.
   */
  def fromName(name: NonEmptyString): Option[A]

  /** Checks if field allows operation on given value.
   *  @param a object to use.
   */
  def allows(a: A)(
      operation: Operator,
      value: Literal,
  ): Boolean


object FilterField:
  /** Summons type class. */
  transparent inline def apply[A: FilterField]: FilterField[A] = summon

  /** Operations allowed for numbers. */
  def numberAllows(operation: Operator, value: Literal): Boolean = value match
    case _: NumberLiteral => Operator.forNumber(operation)
    case _                => false

  /** Operations allowed for strings. */
  def stringAllows(operation: Operator, value: Literal): Boolean = value match
    case _: StringLiteral => Operator.forString(operation)
    case _                => false

  /** Operations allowed for booleans. */
  def booleanAllows(operation: Operator, value: Literal): Boolean = value match
    case _: BooleanLiteral => Operator.forBoolean(operation)
    case _                 => false
