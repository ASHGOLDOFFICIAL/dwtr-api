package org.aulune.commons
package filter


/** Base abstract implementation of [[FilterEvaluator]].
 *  @tparam A fields used in expression.
 *  @tparam R evaluation result type.
 */
abstract class BaseFilterEvaluator[A, R] extends FilterEvaluator[A, R]:

  /** Defines how to evaluate condition.
   *  @param cond condition to evaluate.
   */
  protected def conditionTransformer(cond: Filter.Condition[A]): R

  /** Defines how to combine two sides of AND operator.
   *  @param left left side of an operator.
   *  @param right right side of an operator.
   */
  protected def andCombiner(left: R, right: R): R

  /** Defines how to combine two sides of OR operator.
   *  @param left left side of an operator.
   *  @param right right side of an operator.
   */
  protected def orCombiner(left: R, right: R): R

  /** Defines how to evaluate NOT operator.
   *  @param inner NOT argument.
   */
  protected def notTransformer(inner: R): R

  override def eval(filter: Filter[A]): R = filter match
    case Filter.And(first, second)  => evalAnd(first, second)
    case Filter.Or(first, second)   => evalOr(first, second)
    case Filter.Not(inner)          => evalNot(inner)
    case field: Filter.Condition[A] => conditionTransformer(field)

  private inline def evalAnd(first: Filter[A], second: Filter[A]): R =
    andCombiner(eval(first), eval(second))

  private inline def evalOr(first: Filter[A], second: Filter[A]): R =
    orCombiner(eval(first), eval(second))

  private inline def evalNot(inner: Filter[A]): R = notTransformer(eval(inner))
