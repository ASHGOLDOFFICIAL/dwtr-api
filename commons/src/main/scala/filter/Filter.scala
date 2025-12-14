package org.aulune.commons
package filter


/** Describes logical expression used to filter elements.
 *  @tparam F fields available in expression.
 */
enum Filter[F[_]]:
  /** Filter condition. It's minimal filter expression.
   *  @param field field to apply this condition to.
   *  @param operation operation of condition.
   */
  case Condition[I[_], A](
      field: I[A],
      operation: Filter.Operation[A],
  ) extends Filter[I]

  /** Logical and.
   *  @param first left-hand expression.
   *  @param second right-hand expression.
   */
  case And(first: Filter[F], second: Filter[F])

  /** Logical or.
   *  @param first left-hand expression.
   *  @param second right-hand expression.
   */
  case Or(first: Filter[F], second: Filter[F])

  /** Logical not.
   *  @param inner inner expression.
   */
  case Not(inner: Filter[F])


object Filter:
  enum Operation[+A]:
    case Equal(value: A)
    case NotEqual(value: A)
    case GreaterThan(value: A)
    case LessThan(value: A)
    case GreaterThanOrEqual(value: A)
    case LessThanOrEqual(value: A)
    case Has(value: A)
