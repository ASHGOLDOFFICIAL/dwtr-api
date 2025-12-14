package org.aulune.commons
package filter.parser


import filter.Filter.*
import filter.Filter.Operation.*
import filter.parser.FilterParsingError.*
import testing.syntax.*
import types.NonEmptyString

import cats.Id
import cats.syntax.either.given
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers


/** Tests for [[FilterExpressionParser]]. */
final class FilterExpressionParserTest extends AnyFlatSpec with Matchers:
  /** Test fields. */
  private enum UserField[A: Schema](override val name: NonEmptyString)
      extends FilterField[UserField, A]:
    case Age extends UserField[Int](NonEmptyString("age"))
    case Name extends UserField[String](NonEmptyString("name"))
    case IsMarried extends UserField[Boolean](NonEmptyString("is_married"))

  private val parser: FilterExpressionParser[Id, UserField] =
    FilterExpressionParser.make[UserField](UserField.values.toSet)

  behavior of "FilterExpressionParser.Impl.parse"

  it should "parse plain condition" in {
    val input = NonEmptyString("age >= 18")
    val expected = Condition(UserField.Age, GreaterThanOrEqual(18))
    parser.parse(input) shouldBe expected.asRight
  }

  it should "parse condition in parentheses" in {
    val input = NonEmptyString("""(name != "Ashley")""")
    val expected = Condition(UserField.Name, NotEqual("Ashley"))
    parser.parse(input) shouldBe expected.asRight
  }

  it should "parse negation of condition without parentheses" in {
    val input = NonEmptyString("""NOT is_married != true""")
    val expected = Not(Condition(UserField.IsMarried, NotEqual(true)))
    parser.parse(input) shouldBe expected.asRight
  }

  it should "parse negation of condition with parentheses" in {
    val input = NonEmptyString("""NOT (age < 10)""")
    val expected = Not(Condition(UserField.Age, LessThan(10)))
    parser.parse(input) shouldBe expected.asRight
  }

  it should "parse logical and" in {
    val input = NonEmptyString("""age != 18 AND name = "Mike"""")
    val expected = And(
      Condition(UserField.Age, NotEqual(18)),
      Condition(UserField.Name, Equal("Mike")),
    )
    parser.parse(input) shouldBe expected.asRight
  }

  it should "parse logical or" in {
    val input = NonEmptyString("""age != 18 OR name = "Mike"""")
    val expected = Or(
      Condition(UserField.Age, NotEqual(18)),
      Condition(UserField.Name, Equal("Mike")),
    )
    parser.parse(input) shouldBe expected.asRight
  }

  it should "respect operator precedence" in {
    val input = NonEmptyString(
      """age = 18 OR NOT is_married != true AND name != "Peter"""",
    )
    val expected = And(
      Or(
        Condition(UserField.Age, Equal(18)),
        Not(Condition(UserField.IsMarried, NotEqual(true))),
      ),
      Condition(UserField.Name, NotEqual("Peter")),
    )
    parser.parse(input) shouldBe expected.asRight
  }

  it should "give OR higher precedence" in {
    val input = NonEmptyString(
      """age = 18 AND is_married != true OR name != "Peter"""",
    )
    val expected = And(
      Condition(UserField.Age, Equal(18)),
      Or(
        Condition(UserField.IsMarried, NotEqual(true)),
        Condition(UserField.Name, NotEqual("Peter")),
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
          Condition(UserField.Age, Equal(18)),
          Condition(UserField.IsMarried, NotEqual(true)),
        ),
      ),
      Condition(UserField.Name, NotEqual("Peter")),
    )
    parser.parse(input) shouldBe expected.asRight
  }

  it should "handle additional whitespaces" in {
    val input = NonEmptyString("""  is_married   =   true   """)
    val expected = Condition(UserField.IsMarried, Equal(true))
    parser.parse(input) shouldBe expected.asRight
  }

  it should "fail if operator value doesn't match field value" in {
    val input = NonEmptyString("""age = "John"""")
    parser.parse(input).getLeft shouldBe a[TypeMismatch]
  }

  it should "fail if input references unknown field" in {
    val input = NonEmptyString("""surname = "John"""")
    parser.parse(input).getLeft shouldBe a[UnknownField]
  }

  it should "fail if condition has unsupported operation" in {
    val input = NonEmptyString("""is_married > true""")
    parser
      .parse(input)
      .getLeft shouldBe a[UnsupportedOperation]
  }

  it should "fail if there's an unclosed parentheses" in {
    val input = NonEmptyString("""(is_married = true""")
    parser.parse(input).getLeft shouldBe a[InvalidFormat.type]
  }

  it should "fail when given unparsable value" in {
    val input = NonEmptyString("""is_married = tru""")
    parser.parse(input).getLeft shouldBe a[InvalidFormat.type]
  }
