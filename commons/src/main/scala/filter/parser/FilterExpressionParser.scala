package org.aulune.commons
package filter.parser


import filter.Filter
import filter.parser.FilterParsingError.{
  InvalidFormat,
  TypeMismatch,
  UnknownField,
  UnsupportedOperation,
}
import types.NonEmptyString

import cats.Id
import cats.syntax.all.given

import scala.util.parsing.combinator.RegexParsers


/** Parses strings to filter expression trees.
 *  @tparam F effect type.
 *  @tparam Fields type of fields to use in expression tree.
 */
trait FilterExpressionParser[F[_], Fields[_]]:
  /** Parses given string to filter expression tree.
   *  @param input input string with filter expression.
   *  @return expression tree if successful, otherwise error.
   */
  def parse(
      input: NonEmptyString,
  ): F[Either[FilterParsingError, Filter[Fields]]]


object FilterExpressionParser:
  /** Makes parser for given field definitions.
   *  @param fields field definitions.
   *  @tparam F field type.
   *  @return parser for given field type.
   */
  def make[F[_]](fields: Set[FieldDef[F]]): FilterExpressionParser[Id, F] =
    Impl(fields)

  /** Implementation of parser.
   *  @param fields field definitions to use.
   *  @tparam F effect type.
   */
  private final class Impl[F[_]](fields: Set[FieldDef[F]])
      extends FilterExpressionParser[Id, F]:

    override def parse(
        input: NonEmptyString,
    ): Either[FilterParsingError, Filter[F]] =
      val parser = RawFilterParser()
      parser.parseAll(parser.expr, input) match
        case parser.Success(rawExpression, _) =>
          makeStronglyTyped(fields, rawExpression)
        case failure => Left(InvalidFormat)

  /** Type unsafe parser of filter expressions. */
  private final class RawFilterParser extends RegexParsers:
    override val skipWhitespace = true

    /** Parser for filter expression. */
    def expr: Parser[RawFilter] = andExpr

    /** Parser for `AND` expression. */
    private def andExpr: Parser[RawFilter] = orExpr * (
      "AND" ^^^ { (a: RawFilter, b: RawFilter) => RawFilter.And(a, b) }
    )

    /** Parser for `OR` expression. */
    private def orExpr: Parser[RawFilter] = notExpr * (
      "OR" ^^^ { (a: RawFilter, b: RawFilter) => RawFilter.Or(a, b) }
    )

    /** Parser for `NOT` expression. */
    private def notExpr: Parser[RawFilter] =
      ("NOT" ~> simpleExpr).map(RawFilter.Not.apply) |
        simpleExpr

    /** Parser for condition or expression in parentheses. */
    private def simpleExpr: Parser[RawFilter] = "(" ~> expr <~ ")" | condition

    /** Parser for condition. */
    private def condition: Parser[RawFilter] = fieldName ~ operation ^^ {
      case f ~ op => RawFilter.Condition(f, op)
    }

    /** Parser for operations. */
    private def operation: Parser[RawFilter.Operation] = comparison | inclusion

    /** Parser for comparison operations. */
    private def comparison: Parser[RawFilter.Operation] =
      ("=" ~> value) ^^ RawFilter.Operation.Eq.apply |
        ("!=" ~> value) ^^ RawFilter.Operation.Ne.apply |
        ("<=" ~> value) ^^ RawFilter.Operation.Le.apply |
        ("<" ~> value) ^^ RawFilter.Operation.Lt.apply |
        (">=" ~> value) ^^ RawFilter.Operation.Ge.apply |
        (">" ~> value) ^^ RawFilter.Operation.Gt.apply

    /** Parser for inclusion operations. */
    private def inclusion: Parser[RawFilter.Operation] =
      "IN" ~> "(" ~> repsep(value, ",") <~ ")" ^^
        (vs => RawFilter.Operation.In(vs))

    /** Parser for values. */
    private def value: Parser[NonEmptyString] =
      stringLiteral | intLiteral | booleanLiteral

    /** Parser for boolean values. */
    private def booleanLiteral: Parser[NonEmptyString] =
      RegularExpression.boolean ^^ NonEmptyString.unsafe

    /** Parser for integer values. */
    private def intLiteral: Parser[NonEmptyString] =
      RegularExpression.integer ^^ NonEmptyString.unsafe.apply

    /** Parser for string values. */
    private def stringLiteral: Parser[NonEmptyString] =
      "\"" ~> RegularExpression.string <~ "\"" ^^ NonEmptyString.unsafe.apply

    /** Parser for field names. */
    private def fieldName: Parser[NonEmptyString] =
      RegularExpression.fieldName ^^ NonEmptyString.unsafe

  /** Coerces raw filter expression to type safe expression.
   *  @param fields field definitions.
   *  @param raw raw filter expression tree.
   *  @tparam F type of fields.
   *  @return type safe expression tree or error.
   */
  private def makeStronglyTyped[F[_]](
      fields: Set[FieldDef[F]],
      raw: RawFilter,
  ): Either[FilterParsingError, Filter[F]] = raw match
    case RawFilter.Condition(name, op) =>
      for
        fieldDef <- fields.find(_.name == name).toRight(UnknownField(name))
        _ <- op.asRight
          .ensure(UnsupportedOperation(name, op))(fieldDef.schema.allows)
        typedOperation <- buildOperation(fieldDef.schema, op)
          .toRight(TypeMismatch(name, op))
      yield Filter.Condition(fieldDef.field, typedOperation)

    case RawFilter.And(l, r) =>
      for
        left <- makeStronglyTyped(fields, l)
        right <- makeStronglyTyped(fields, r)
      yield Filter.And(left, right)

    case RawFilter.Or(l, r) =>
      for
        left <- makeStronglyTyped(fields, l)
        right <- makeStronglyTyped(fields, r)
      yield Filter.Or(left, right)

    case RawFilter.Not(inner) =>
      makeStronglyTyped(fields, inner).map(Filter.Not.apply)

  /** Makes type safe operation out of raw one.
   *  @param schema schema for value type.
   *  @param raw raw operation.
   *  @tparam A type of value.
   *  @return type safe operation expression if value can be parsed.
   */
  private def buildOperation[A](
      schema: Schema[A],
      raw: RawFilter.Operation,
  ): Option[Filter.Operation[A]] = raw match
    case RawFilter.Operation.Eq(v) =>
      schema.parse(v).map(Filter.Operation.Equal.apply)
    case RawFilter.Operation.Ne(v) =>
      schema.parse(v).map(Filter.Operation.NotEqual.apply)
    case RawFilter.Operation.Gt(v) =>
      schema.parse(v).map(Filter.Operation.GreaterThan.apply)
    case RawFilter.Operation.Lt(v) =>
      schema.parse(v).map(Filter.Operation.LessThan.apply)
    case RawFilter.Operation.Ge(v) =>
      schema.parse(v).map(Filter.Operation.GreaterThanOrEqual.apply)
    case RawFilter.Operation.Le(v) =>
      schema.parse(v).map(Filter.Operation.LessThanOrEqual.apply)
    case RawFilter.Operation.In(values) =>
      values.traverse(schema.parse).map(Filter.Operation.In.apply)
