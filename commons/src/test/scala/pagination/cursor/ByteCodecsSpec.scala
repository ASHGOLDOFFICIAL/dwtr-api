package org.aulune.commons
package pagination.cursor


import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers


/** Tests for [[ByteDecoder]] and [[ByteEncoder]], */
final class ByteCodecsSpec extends AnyFlatSpec with Matchers:

  /** Test sum type. */
  enum Flag derives ByteEncoder, ByteDecoder:
    case Enabled
    case Disabled

  /** Test product type. */
  private final case class User(
      age: Int,
      name: String,
      isMarried: Boolean,
      roles: List[String],
  ) derives ByteEncoder,
        ByteDecoder

  behavior of "forString"

  it should "correctly encode and decode strings" in {
    val encoded = ByteEncoder.forString.encode("abc")
    ByteDecoder.forString.decode(encoded) shouldBe Some("abc")
  }

  it should "correctly encode and decode empty strings" in {
    val encoded = ByteEncoder.forString.encode("")
    ByteDecoder.forString.decode(encoded) shouldBe Some("")
  }

  behavior of "forByte"

  it should "correctly encode and decode bytes" in {
    val encoded = ByteEncoder.forByte.encode(8)
    ByteDecoder.forByte.decode(encoded) shouldBe Some(8)
  }

  it should "correctly encode and decode min possible byte" in {
    val encoded = ByteEncoder.forByte.encode(Byte.MinValue)
    ByteDecoder.forByte.decode(encoded) shouldBe Some(Byte.MinValue)
  }

  it should "correctly encode and decode max possible byte" in {
    val encoded = ByteEncoder.forByte.encode(Byte.MaxValue)
    ByteDecoder.forByte.decode(encoded) shouldBe Some(Byte.MaxValue)
  }

  behavior of "forShort"

  it should "correctly encode and decode shorts" in {
    val encoded = ByteEncoder.forShort.encode(8)
    ByteDecoder.forShort.decode(encoded) shouldBe Some(8)
  }

  it should "correctly encode and decode min possible short" in {
    val encoded = ByteEncoder.forShort.encode(Short.MinValue)
    ByteDecoder.forShort.decode(encoded) shouldBe Some(Short.MinValue)
  }

  it should "correctly encode and decode max possible short" in {
    val encoded = ByteEncoder.forShort.encode(Short.MaxValue)
    ByteDecoder.forShort.decode(encoded) shouldBe Some(Short.MaxValue)
  }

  behavior of "forInt"

  it should "correctly encode and decode ints" in {
    val encoded = ByteEncoder.forInt.encode(8)
    ByteDecoder.forInt.decode(encoded) shouldBe Some(8)
  }

  it should "correctly encode and decode min possible int" in {
    val encoded = ByteEncoder.forInt.encode(Int.MinValue)
    ByteDecoder.forInt.decode(encoded) shouldBe Some(Int.MinValue)
  }

  it should "correctly encode and decode max possible int" in {
    val encoded = ByteEncoder.forInt.encode(Int.MaxValue)
    ByteDecoder.forInt.decode(encoded) shouldBe Some(Int.MaxValue)
  }

  behavior of "forLong"

  it should "correctly encode and decode longs" in {
    val encoded = ByteEncoder.forLong.encode(8)
    ByteDecoder.forLong.decode(encoded) shouldBe Some(8)
  }

  it should "correctly encode and decode min possible long" in {
    val encoded = ByteEncoder.forLong.encode(Long.MinValue)
    ByteDecoder.forLong.decode(encoded) shouldBe Some(Long.MinValue)
  }

  it should "correctly encode and decode max possible long" in {
    val encoded = ByteEncoder.forLong.encode(Long.MaxValue)
    ByteDecoder.forLong.decode(encoded) shouldBe Some(Long.MaxValue)
  }

  behavior of "forFloat"

  it should "correctly encode and decode floats" in {
    val encoded = ByteEncoder.forFloat.encode(8.8f)
    ByteDecoder.forFloat.decode(encoded) shouldBe Some(8.8f)
  }

  it should "correctly encode and decode min possible float" in {
    val encoded = ByteEncoder.forFloat.encode(Float.MinValue)
    ByteDecoder.forFloat.decode(encoded) shouldBe Some(Float.MinValue)
  }

  it should "correctly encode and decode min possible positive float" in {
    val encoded = ByteEncoder.forFloat.encode(Float.MinPositiveValue)
    ByteDecoder.forFloat.decode(encoded) shouldBe Some(Float.MinPositiveValue)
  }

  it should "correctly encode and decode max possible float" in {
    val encoded = ByteEncoder.forFloat.encode(Float.MaxValue)
    ByteDecoder.forFloat.decode(encoded) shouldBe Some(Float.MaxValue)
  }

  it should "correctly encode and decode NaN" in {
    val encoded = ByteEncoder.forFloat.encode(Float.NaN)
    ByteDecoder.forFloat.decode(encoded).get.isNaN shouldBe true
  }

  it should "correctly encode and decode +inf" in {
    val encoded = ByteEncoder.forFloat.encode(Float.PositiveInfinity)
    ByteDecoder.forFloat.decode(encoded).get.isPosInfinity shouldBe true
  }

  it should "correctly encode and decode -inf" in {
    val encoded = ByteEncoder.forFloat.encode(Float.NegativeInfinity)
    ByteDecoder.forFloat.decode(encoded).get.isNegInfinity shouldBe true
  }

  behavior of "forDouble"

  it should "correctly encode and decode floats" in {
    val encoded = ByteEncoder.forDouble.encode(8.8d)
    ByteDecoder.forDouble.decode(encoded) shouldBe Some(8.8d)
  }

  it should "correctly encode and decode min possible float" in {
    val encoded = ByteEncoder.forDouble.encode(Double.MinValue)
    ByteDecoder.forDouble.decode(encoded) shouldBe Some(Double.MinValue)
  }

  it should "correctly encode and decode min possible positive float" in {
    val encoded = ByteEncoder.forDouble.encode(Double.MinPositiveValue)
    ByteDecoder.forDouble.decode(encoded) shouldBe Some(Double.MinPositiveValue)
  }

  it should "correctly encode and decode max possible float" in {
    val encoded = ByteEncoder.forDouble.encode(Double.MaxValue)
    ByteDecoder.forDouble.decode(encoded) shouldBe Some(Double.MaxValue)
  }

  it should "correctly encode and decode NaN" in {
    val encoded = ByteEncoder.forDouble.encode(Double.NaN)
    ByteDecoder.forDouble.decode(encoded).get.isNaN shouldBe true
  }

  it should "correctly encode and decode +inf" in {
    val encoded = ByteEncoder.forDouble.encode(Double.PositiveInfinity)
    ByteDecoder.forDouble.decode(encoded).get.isPosInfinity shouldBe true
  }

  it should "correctly encode and decode -inf" in {
    val encoded = ByteEncoder.forDouble.encode(Double.NegativeInfinity)
    ByteDecoder.forDouble.decode(encoded).get.isNegInfinity shouldBe true
  }

  behavior of "forBoolean"

  it should "correctly encode and decode booleans" in {
    val trueEncoded = ByteEncoder.forBoolean.encode(true)
    val falseEncoded = ByteEncoder.forBoolean.encode(false)

    val trueDecoded = ByteDecoder.forBoolean.decode(trueEncoded)
    val falseDecoded = ByteDecoder.forBoolean.decode(falseEncoded)

    (trueDecoded, falseDecoded) shouldBe (Some(true), Some(false))
  }

  it should "not decode bytes other than 0s and 1s" in {
    val bytes: IArray[Byte] = IArray.from(List(2))
    ByteDecoder.forBoolean.decode(bytes) shouldBe None
  }

  behavior of "derived"

  it should "correctly encode and decode case classes" in {
    val user = User(18, "John", false, List("admin", "moderator"))
    val encoded = ByteEncoder[User].encode(user)
    ByteDecoder[User].decode(encoded) shouldBe Some(user)
  }

  it should "correctly encode and decode enums" in {
    val encodedEnabled = ByteEncoder[Flag].encode(Flag.Enabled)
    val encodedDisabled = ByteEncoder[Flag].encode(Flag.Disabled)

    val decodedEnabled = ByteDecoder[Flag].decode(encodedEnabled)
    val decodedDisabled = ByteDecoder[Flag].decode(encodedDisabled)

    (decodedEnabled, decodedDisabled) shouldBe (
      Some(Flag.Enabled),
      Some(Flag.Disabled),
    )
  }
