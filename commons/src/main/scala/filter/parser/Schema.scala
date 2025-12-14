package org.aulune.commons
package filter.parser


import filter.parser.RawFilter.Operation
import types.NonEmptyString


/** Describes a type that can be used in filter string, how to parse from string
 *  and what operations it allows.
 *  @tparam A described type.
 */
sealed trait Schema[A]:
  /** Tries to parse string to type.
   *  @param raw string to parse.
   */
  def parse(raw: NonEmptyString): Option[A]

  /** Checks if this type supports given operation.
   *  @param op operation to check.
   */
  def allows(op: RawFilter.Operation): Boolean


object Schema:
  /** Summons the schema for given type.
   *  @tparam A need type.
   */
  transparent inline def apply[A: Schema]: Schema[A] = summon

  /** Makes schema for new type using already existing schema.
   *  @param f a way to get a value of new type from value of old type.
   *  @tparam S old type.
   *  @tparam T new type.
   */
  def from[S: Schema, T](f: S => Option[T]): Schema[T] = new Schema[T]:
    override def parse(raw: NonEmptyString): Option[T] =
      Schema[S].parse(raw).flatMap(f)
    override def allows(op: Operation): Boolean = Schema[S].allows(op)

  /** Schema for boolean. */
  private final class BooleanSchema extends Schema[Boolean]:
    override def parse(value: NonEmptyString): Option[Boolean] = value match
      case "true"  => Some(true)
      case "false" => Some(false)
      case _       => None

    override def allows(op: RawFilter.Operation): Boolean = op match
      case RawFilter.Operation.Eq(_) | RawFilter.Operation.Ne(_) => true
      case _                                                     => false

  /** Schema for integer. */
  private final class IntSchema extends Schema[Int]:
    override def parse(raw: NonEmptyString): Option[Int] = raw.toIntOption

    override def allows(op: RawFilter.Operation): Boolean = op match
      case Operation.Eq(value)  => true
      case Operation.Ne(value)  => true
      case Operation.Gt(value)  => true
      case Operation.Lt(value)  => true
      case Operation.Ge(value)  => true
      case Operation.Le(value)  => true
      case Operation.In(values) => true

  /** Schema for string. */
  private final class StringSchema extends Schema[String]:
    override def parse(raw: NonEmptyString): Option[String] = Some(raw)

    override def allows(op: RawFilter.Operation): Boolean = op match
      case Operation.Eq(value)  => true
      case Operation.Ne(value)  => true
      case Operation.Gt(value)  => true
      case Operation.Lt(value)  => true
      case Operation.Ge(value)  => true
      case Operation.Le(value)  => true
      case Operation.In(values) => true

  given forBoolean: Schema[Boolean] = BooleanSchema()
  given forInt: Schema[Int] = IntSchema()
  given forString: Schema[String] = StringSchema()
