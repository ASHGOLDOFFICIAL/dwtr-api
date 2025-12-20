package org.aulune.commons
package filter


/** Describes logical expression used to filter elements.
 *  @tparam A fields available in expression.
 */
enum Filter[A]:
  self =>

  case Condition(
      field: A,
      operator: Filter.Operator,
      value: Filter.Literal,
  )
  case And(left: Filter[A], right: Filter[A])
  case Or(left: Filter[A], right: Filter[A])
  case Not(expr: Filter[A])

  /** Negation operation.
   *  @return `Not` of this filter.
   */
  def unary_not: Filter[A] = Not(self)

  /** Makes logical and with another filter.
   *  @return `And` of this and other.
   */
  infix def and(other: Filter[A]): Filter[A] = And(self, other)

  /** Makes logical or with another filter.
   *  @return `Or` of this and other.
   */
  infix def or(other: Filter[A]): Filter[A] = Or(self, other)


object Filter:
  enum Operator:
    case Equal, NotEqual
    case LessThan, LessThanOrEqual, GreaterThan, GreaterThanOrEqual
    case Has

  object Operator:
    /** Equality operators. */
    val equality: Set[Operator] = Set(Equal, NotEqual)

    /** Comparison operators. */
    val comparison: Set[Operator] =
      Set(LessThan, LessThanOrEqual, GreaterThan, GreaterThanOrEqual)

    /** Inclusion check operator. */
    val inclusion: Set[Operator] = Set(Has)

    /** Operators for number types. */
    val forNumber: Set[Operator] = equality ++ comparison

    /** Operators for string types. */
    val forString: Set[Operator] = equality ++ comparison

    /** Operators for boolean types. */
    val forBoolean: Set[Operator] = equality

  enum Literal:
    case StringLiteral(value: String)
    case NumberLiteral(value: BigDecimal)
    case BooleanLiteral(value: Boolean)

  object Literal:
    /** Alias for [[StringLiteral]]. */
    def apply(string: String): StringLiteral = StringLiteral(string)

    /** Alias for [[NumberLiteral]]. */
    def apply(bigDecimal: BigDecimal): NumberLiteral = NumberLiteral(bigDecimal)

    /** Alias for [[BooleanLiteral]]. */
    def apply(boolean: Boolean): BooleanLiteral = BooleanLiteral(boolean)
