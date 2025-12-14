package org.aulune.commons
package filter.parser


/** Mixin trait that implements [[FieldDef]].
 *  @tparam F type of field.
 *  @tparam A type of field values.
 */
trait FilterField[F[_], A: Schema] extends FieldDef[F]:
  self: F[A] =>
  override type Type = A
  override val field: F[A] = self
  override val schema: Schema[A] = Schema[Type]
