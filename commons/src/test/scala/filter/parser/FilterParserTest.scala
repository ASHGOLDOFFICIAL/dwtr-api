package org.aulune.commons
package filter.parser


import filter.Filter.*
import filter.Filter.Literal.StringLiteral
import filter.Filter.Operator.*
import filter.FilterError.*
import filter.{FilterField, FilterParser}
import testing.syntax.*
import types.NonEmptyString

import cats.syntax.either.given
import cats.syntax.option.given
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers


/** Tests for [[FilterParser]]. */
final class FilterParserTest extends AnyFlatSpec with Matchers:

  /** Test fields. */
  private enum UserField:
    case Age
    case Name
    case IsMarried
    case Roles

  private given FilterField[UserField] = new FilterField[UserField]:
    override def fromName(name: NonEmptyString): Option[UserField] = name match
      case s if s == NonEmptyString("age")        => UserField.Age.some
      case s if s == NonEmptyString("name")       => UserField.Name.some
      case s if s == NonEmptyString("is_married") => UserField.IsMarried.some
      case s if s == NonEmptyString("roles")      => UserField.Roles.some
      case _                                      => None

    override def allows(a: UserField)(op: Operator, l: Literal): Boolean =
      a match
        case UserField.Age       => FilterField.numberAllows(op, l)
        case UserField.Name      => FilterField.stringAllows(op, l)
        case UserField.IsMarried => FilterField.booleanAllows(op, l)
        case UserField.Roles     => op == Has && l.isInstanceOf[StringLiteral]

  private val parser: FilterParser[UserField] = FilterParser.make[UserField]

  behavior of "FilterExpressionParser.Impl.parse"

  it should "parse plain condition" in {
    val input = NonEmptyString("age >= 18")
    val expected = Condition(UserField.Age, GreaterThanOrEqual, Literal(18))
    parser.parse(input) shouldBe expected.asRight
  }

  it should "parse condition in parentheses" in {
    val input = NonEmptyString("""(name != "Ashley")""")
    val expected = Condition(UserField.Name, NotEqual, Literal("Ashley"))
    parser.parse(input) shouldBe expected.asRight
  }

  it should "parse negation of condition without parentheses" in {
    val input = NonEmptyString("""NOT is_married != true""")
    val expected = Not(Condition(UserField.IsMarried, NotEqual, Literal(true)))
    parser.parse(input) shouldBe expected.asRight
  }

  it should "parse negation of condition with parentheses" in {
    val input = NonEmptyString("""NOT (age < 10)""")
    val expected = Not(Condition(UserField.Age, LessThan, Literal(10)))
    parser.parse(input) shouldBe expected.asRight
  }

  it should "parse logical and" in {
    val input = NonEmptyString("""age != 18 AND name = "Mike"""")
    val expected = And(
      Condition(UserField.Age, NotEqual, Literal(18)),
      Condition(UserField.Name, Equal, Literal("Mike")),
    )
    parser.parse(input) shouldBe expected.asRight
  }

  it should "parse logical or" in {
    val input = NonEmptyString("""age != 18 OR name = "Mike"""")
    val expected = Or(
      Condition(UserField.Age, NotEqual, Literal(18)),
      Condition(UserField.Name, Equal, Literal("Mike")),
    )
    parser.parse(input) shouldBe expected.asRight
  }

  it should "parse has operator" in {
    val input = NonEmptyString("""roles:"abc"""")
    val expected = Condition(UserField.Roles, Has, Literal("abc"))
    parser.parse(input) shouldBe expected.asRight
  }

  it should "respect operator precedence" in {
    val input = NonEmptyString(
      """age = 18 OR NOT is_married != true AND name != "Peter"""",
    )
    val expected = And(
      Or(
        Condition(UserField.Age, Equal, Literal(18)),
        Not(Condition(UserField.IsMarried, NotEqual, Literal(true))),
      ),
      Condition(UserField.Name, NotEqual, Literal("Peter")),
    )
    parser.parse(input) shouldBe expected.asRight
  }

  it should "give OR higher precedence" in {
    val input = NonEmptyString(
      """age = 18 AND is_married != true OR name != "Peter"""",
    )
    val expected = And(
      Condition(UserField.Age, Equal, Literal(18)),
      Or(
        Condition(UserField.IsMarried, NotEqual, Literal(true)),
        Condition(UserField.Name, NotEqual, Literal("Peter")),
      ),
    )
    parser.parse(input) shouldBe expected.asRight
  }

  it should "parse complex expression" in {
    val input = NonEmptyString(
      """NOT (age = 18 AND is_married != true) OR name != "Peter"""",
    )
    val expected = Or(
      Not(
        And(
          Condition(UserField.Age, Equal, Literal(18)),
          Condition(UserField.IsMarried, NotEqual, Literal(true)),
        ),
      ),
      Condition(UserField.Name, NotEqual, Literal("Peter")),
    )
    parser.parse(input) shouldBe expected.asRight
  }

  it should "handle additional whitespaces" in {
    val input = NonEmptyString("  is_married   =   true   ")
    val expected = Condition(UserField.IsMarried, Equal, Literal(true))
    parser.parse(input) shouldBe expected.asRight
  }

  it should "handle missing whitespaces before and after parentheses" in {
    val input = NonEmptyString(
      """NOT(is_married = true AND(age <= 10))OR(name > "A")""",
    )
    val expected = Or(
      Not(
        And(
          Condition(UserField.IsMarried, Equal, Literal(true)),
          Condition(UserField.Age, LessThanOrEqual, Literal(10)),
        ),
      ),
      Condition(UserField.Name, GreaterThan, Literal("A")),
    )
    parser.parse(input) shouldBe expected.asRight
  }

  it should "handle missing whitespaces before and after operator" in {
    val input = NonEmptyString("""name<="A"""")
    val expected = Condition(UserField.Name, LessThanOrEqual, Literal("A"))
    parser.parse(input) shouldBe expected.asRight
  }

  it should "fail if operator value doesn't match field value" in {
    val input = NonEmptyString("""age = "John"""")
    parser.parse(input).getLeft shouldBe a[UnsupportedOperation[?]]
  }

  it should "fail if input references unknown field" in {
    val input = NonEmptyString("""surname = "John"""")
    parser.parse(input).getLeft shouldBe a[UnknownField]
  }

  it should "fail if condition has unsupported operation" in {
    val input = NonEmptyString("""is_married > true""")
    parser
      .parse(input)
      .getLeft shouldBe a[UnsupportedOperation[?]]
  }

  it should "fail if there's an unclosed parentheses" in {
    val input = NonEmptyString("""(is_married = true""")
    parser.parse(input).getLeft shouldBe a[SyntaxError.type]
  }

  it should "fail when given unparsable value" in {
    val input = NonEmptyString("""is_married = tru""")
    parser.parse(input).getLeft shouldBe a[SyntaxError.type]
  }
