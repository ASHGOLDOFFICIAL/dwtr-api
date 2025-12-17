package org.aulune.commons
package filter


import types.NonEmptyString

import scala.util.parsing.combinator.RegexParsers


/** Parser for filter expressions.
 *  @tparam A type of fields.
 */
trait FilterParser[A]:
  /** Converts string input to filter AST if possible.
   *  @param input string with filter expression.
   */
  def parse(input: NonEmptyString): Either[FilterError, Filter[A]]


object FilterParser:
  /** Makes new filter parser.
   *  @tparam A type of fields.
   */
  def make[A: FilterField]: FilterParser[A] = Impl[A]()

  /** [[FilterParser]] implementation.
   *  @tparam A type of fields.
   */
  private final class Impl[A: FilterField]
      extends FilterParser[A]
      with RegexParsers:

    override def parse(input: NonEmptyString): Either[FilterError, Filter[A]] =
      for
        result <- parseAll(expr, input)
          .map(resolve)
          .getOrElse(Left(FilterError.SyntaxError))
        _ <- validate(result)
      yield result

    override val skipWhitespace = true

    private def identifier: Parser[NonEmptyString] =
      """[a-zA-Z_][a-zA-Z0-9_]*""".r ^^ NonEmptyString.unsafe
    private def stringLiteral: Parser[String] = "\"" ~> """[^"]*""".r <~ "\""
    private def numberLiteral: Parser[BigDecimal] =
      """-?\d+(\.\d+)?""".r ^^ BigDecimal.apply
    private def boolLiteral: Parser[Boolean] =
      ("true" | "false") ^^ (_.toBoolean)

    private def value: Parser[Filter.Literal] =
      stringLiteral ^^ Filter.Literal.StringLiteral.apply |
        numberLiteral ^^ Filter.Literal.NumberLiteral.apply |
        boolLiteral ^^ Filter.Literal.BooleanLiteral.apply

    private def operator: Parser[Filter.Operator] =
      "=" ^^^ Filter.Operator.Equal |
        "!=" ^^^ Filter.Operator.NotEqual |
        "<=" ^^^ Filter.Operator.LessThanOrEqual |
        "<" ^^^ Filter.Operator.LessThan |
        ">=" ^^^ Filter.Operator.GreaterThanOrEqual |
        ">" ^^^ Filter.Operator.GreaterThan |
        ":" ^^^ Filter.Operator.Has

    private def condition: Parser[RawFilter] =
      identifier ~ operator ~ value ^^ { case field ~ op ~ v =>
        RawFilter.Condition(field, op, v)
      }

    private def parens: Parser[RawFilter] = "(" ~> expr <~ ")"
    private def simpleExpr: Parser[RawFilter] = condition | parens

    private def notExpr: Parser[RawFilter] =
      ("NOT" ~> simpleExpr) ^^ RawFilter.Not.apply | simpleExpr

    private def orExpr: Parser[RawFilter] = notExpr * (
      "OR" ^^^ { (a: RawFilter, b: RawFilter) => RawFilter.Or(a, b) }
    )

    private def andExpr: Parser[RawFilter] = orExpr * (
      "AND" ^^^ { (a: RawFilter, b: RawFilter) => RawFilter.And(a, b) }
    )

    private def expr: Parser[RawFilter] = andExpr

    /** Transforms raw filter AST to typed one.
     *
     *  If expression contained unknown fields, returns as error.
     *  @param raw raw filter AST.
     */
    private def resolve(raw: RawFilter): Either[FilterError, Filter[A]] =
      raw match
        case RawFilter.Condition(field, op, value) =>
          FilterField[A].fromName(field) match
            case Some(a) => Right(Filter.Condition(a, op, value))
            case None    => Left(FilterError.UnknownField(field))

        case RawFilter.And(l, r) =>
          for
            left <- resolve(l)
            right <- resolve(r)
          yield Filter.And(left, right)

        case RawFilter.Or(l, r) =>
          for
            left <- resolve(l)
            right <- resolve(r)
          yield Filter.Or(left, right)

        case RawFilter.Not(e) => resolve(e).map(Filter.Not.apply)

    /** Checks if filter expression contains only defined operation.
     *  @param expr expression to check.
     */
    private def validate(expr: Filter[A]): Either[FilterError, Unit] =
      expr match
        case Filter.Condition(field, op, value) =>
          if !FilterField[A].allows(field)(op, value)
          then Left(FilterError.UnsupportedOperation(field, op, value))
          else Right(())
        case Filter.And(l, r) => validate(l).flatMap(_ => validate(r))
        case Filter.Or(l, r)  => validate(l).flatMap(_ => validate(r))
        case Filter.Not(e)    => validate(e)

  /** Untyped filter AST. */
  private enum RawFilter:
    case Condition(
        field: NonEmptyString,
        op: Filter.Operator,
        value: Filter.Literal,
    )
    case And(left: RawFilter, right: RawFilter)
    case Or(left: RawFilter, right: RawFilter)
    case Not(expr: RawFilter)
