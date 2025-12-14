package org.aulune.commons
package filter

import filter.FilterEvaluator.ConditionEvaluator


/** Generic implementation of [[FilterEvaluator]]
 *
 *  @param conditionEvaluator used to handle conditions.
 *  @param andCombiner used to handle AND.
 *  @param orCombiner used to handle OR.
 *  @param notTransformer used to handle NOT.
 *  @tparam F fields used in expression.
 *  @tparam R evaluation result type.
 */
open class GenericFilterEvaluator[F[_], +R](
    conditionEvaluator: ConditionEvaluator[F, R],
    andCombiner: (R, R) => R,
    orCombiner: (R, R) => R,
    notTransformer: R => R,
) extends FilterEvaluator[F, R]:

  override def eval(filter: Filter[F]): R = filter match
    case Filter.And(first, second)      => evalAnd(first, second)
    case Filter.Or(first, second)       => evalOr(first, second)
    case Filter.Not(inner)              => evalNot(inner)
    case field @ Filter.Condition(_, _) => conditionEvaluator.eval(field)

  private inline def evalAnd(first: Filter[F], second: Filter[F]): R =
    andCombiner(eval(first), eval(second))

  private inline def evalOr(first: Filter[F], second: Filter[F]): R =
    orCombiner(eval(first), eval(second))

  private inline def evalNot(inner: Filter[F]): R = notTransformer(eval(inner))
