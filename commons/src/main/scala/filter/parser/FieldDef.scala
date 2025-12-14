package org.aulune.commons
package filter.parser

import types.NonEmptyString


/** Definition of a field used in filter expressions.
 *  @tparam F type of field.
 */
trait FieldDef[F[_]]:
  /** Type of values associated with this field. */
  type Type

  /** Field name. */
  val name: NonEmptyString

  /** Associated model. */
  val field: F[Type]

  /** Schema used to parse field values. */
  val schema: Schema[Type]

  override final def equals(other: Any): Boolean = other match
    case that: FieldDef[?] => this.name == that.name
    case _                 => false

  override final def hashCode(): Int = name.hashCode()


object FieldDef:
  /** Makes field definition for given argument.
   *  @param fa object to make field definition for.
   *  @tparam F type of field.
   *  @tparam A type of this field values.
   *  @return field definition for this object.
   */
  def make[F[_], A: Schema](fa: F[A], name: NonEmptyString): FieldDef[F] =
    Impl(fa, name)

  /** Implementation of [[FieldDef]].
   *  @param field associated model.
   *  @param name field name.
   *  @tparam F type of field.
   *  @tparam A type of field values.
   */
  private case class Impl[F[_], A: Schema](
      override val field: F[A],
      override val name: NonEmptyString,
  ) extends FieldDef[F]:
    override type Type = A
    override val schema: Schema[Type] = Schema[A]
