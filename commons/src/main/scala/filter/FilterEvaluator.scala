package org.aulune.commons
package filter


/** Evaluates filter expression defined on [[A]] to some type of [[R]].
 *  @tparam A fields used in expression.
 *  @tparam R evaluation result type.
 */
trait FilterEvaluator[A, R]:
  /** Evaluates expression.
   *  @param filter filter expression to evaluate.
   *  @return evaluation result.
   */
  def eval(filter: Filter[A]): R
