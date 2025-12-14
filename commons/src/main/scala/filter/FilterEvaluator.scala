package org.aulune.commons
package filter


/** Evaluates filter expression defined on [[F]] to some type of [[R]].
 *  @tparam F fields used in expression.
 *  @tparam R evaluation result type.
 */
trait FilterEvaluator[F[_], +R]:
  /** Evaluates expression.
   *  @param filter filter expression to evaluate.
   *  @return evaluation result.
   */
  def eval(filter: Filter[F]): R


object FilterEvaluator:
  /** Evaluates condition expression.
   *  @tparam F fields used in expression.
   *  @tparam R result type.
   */
  trait ConditionEvaluator[F[_], R]:
    /** Evaluates condition expression.
     *  @param condition literal to evaluate.
     *  @tparam A type of literal field.
     */
    def eval[A](condition: Filter.Condition[F, A]): R
