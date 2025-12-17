package org.aulune.commons
package filter.instances


import filter.instances.DoobieFragmentFilterEvaluator.UnsupportedOperation
import filter.{BaseFilterEvaluator, Filter}
import types.NonEmptyString

import cats.syntax.either.given
import doobie.*
import doobie.syntax.all.given
import doobie.util.fragment.Fragment


/** Evaluates filter AST to [[Fragment]]. Doesn't support `HAS` operation.
 *  @param columnName function that maps field elements to field names.
 *  @tparam A fields used in expression.
 */
class DoobieFragmentFilterEvaluator[A](columnName: A => NonEmptyString)
    extends BaseFilterEvaluator[A, Either[UnsupportedOperation, Fragment]]:
  private type Result = Either[UnsupportedOperation, Fragment]

  override protected def conditionTransformer(
      cond: Filter.Condition[A],
  ): Result = operator(cond.operator).map { op =>
    Fragment.const(columnName(cond.field)) ++ op ++ fr0" ${literal(cond.value)}"
  }

  override protected def andCombiner(left: Result, right: Result): Result =
    for
      l <- left
      r <- right
    yield fr0"(" ++ l ++ fr0" AND " ++ r ++ fr0")"

  override protected def orCombiner(left: Result, right: Result): Result =
    for
      l <- left
      r <- right
    yield fr0"(" ++ l ++ fr0" OR " ++ r ++ fr0")"

  override protected def notTransformer(inner: Result): Result =
    inner.map(i => fr0"(NOT " ++ i ++ fr0")")

  private def operator(
      op: Filter.Operator,
  ): Result = op match
    case Filter.Operator.Equal              => fr0"=".asRight
    case Filter.Operator.NotEqual           => fr0"<>".asRight
    case Filter.Operator.LessThan           => fr0"<".asRight
    case Filter.Operator.LessThanOrEqual    => fr0"<=".asRight
    case Filter.Operator.GreaterThan        => fr0">".asRight
    case Filter.Operator.GreaterThanOrEqual => fr0">=".asRight
    case Filter.Operator.Has                => UnsupportedOperation.asLeft

  private def literal(l: Filter.Literal): Fragment = l match
    case Filter.Literal.StringLiteral(value)  => fr0"$value"
    case Filter.Literal.NumberLiteral(value)  => fr0"$value"
    case Filter.Literal.BooleanLiteral(value) => fr0"$value"


object DoobieFragmentFilterEvaluator:
  object UnsupportedOperation
  type UnsupportedOperation = UnsupportedOperation.type
