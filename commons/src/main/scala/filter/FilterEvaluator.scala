package org.aulune.commons
package filter


/** Evaluates filter expression defined on [[F]] to some type of [[R]].
 *  @tparam F fields used in expression.
 *  @tparam R evaluation result type.
 */
trait FilterEvaluator[F, +R]:
  /** Evaluates expression.
   *  @param filter filter expression to evaluate.
   *  @return evaluation result.
   */
  def eval(filter: Filter[F]): R
