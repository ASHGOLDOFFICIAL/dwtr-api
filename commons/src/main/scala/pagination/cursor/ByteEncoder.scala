package org.aulune.commons
package pagination.cursor


import instances.WithDerivation

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets


/** Encodes elements to bytes.
 *  @tparam A type of elements.
 */
trait ByteEncoder[A]:
  /** Encodes given [[A]] to bytes.
   *  @param x element to encode.
   *  @return encoded as bytes.
   */
  def encode(x: A): IArray[Byte]

  /** Creates new encoder based on this one.
   *  @param f function to transform [[B]] to [[A]].
   *  @tparam B new type.
   *  @return encoder for new type.
   */
  final def map[B](f: B => A): ByteEncoder[B] = x => encode(f(x))


object ByteEncoder extends WithDerivation[ByteEncoder]:
  transparent inline def apply[A: ByteEncoder]: ByteEncoder[A] = summon

  /** [[ByteEncoder]] for strings. */
  given forString: ByteEncoder[String] =
    x => IArray.unsafeFromArray(x.getBytes(StandardCharsets.UTF_8))

  /** [[ByteEncoder]] for bytes. */
  given forByte: ByteEncoder[Byte] =
    x => IArray.unsafeFromArray(ByteBuffer.allocate(1).put(x).array)

  /** [[ByteEncoder]] for shorts. */
  given forShort: ByteEncoder[Short] =
    x => IArray.unsafeFromArray(ByteBuffer.allocate(2).putShort(x).array)

  /** [[ByteEncoder]] for ints. */
  given forInt: ByteEncoder[Int] =
    x => IArray.unsafeFromArray(ByteBuffer.allocate(4).putInt(x).array)

  /** [[ByteEncoder]] for longs. */
  given forLong: ByteEncoder[Long] =
    x => IArray.unsafeFromArray(ByteBuffer.allocate(8).putLong(x).array)

  /** [[ByteEncoder]] for floats. */
  given forFloat: ByteEncoder[Float] =
    x => IArray.unsafeFromArray(ByteBuffer.allocate(4).putFloat(x).array)

  /** [[ByteEncoder]] for doubles. */
  given forDouble: ByteEncoder[Double] =
    x => IArray.unsafeFromArray(ByteBuffer.allocate(8).putDouble(x).array)

  /** [[ByteEncoder]] for booleans. */
  given forBoolean: ByteEncoder[Boolean] = forByte.map {
    case true  => 1.toByte
    case false => 0.toByte
  }

  /** [[ByteEncoder]] for iterables. */
  given forIterable[C[_], A](using
      ev: C[A] <:< Iterable[A],
      encoder: ByteEncoder[A],
  ): ByteEncoder[C[A]] = x =>
    val encoded = ev(x).map(encoder.encode).toVector
    val totalSize = encoded.foldLeft(4)((acc, bytes) => acc + 4 + bytes.length)

    val buffer = ByteBuffer.allocate(totalSize)
    buffer.putInt(encoded.size)
    encoded.foreach { bytes =>
      buffer.putInt(bytes.length)
      buffer.put(IArray.genericWrapArray(bytes).toArray)
    }

    IArray.unsafeFromArray(buffer.array())

  override def deriveProduct[A](productType: ProductType[A]): ByteEncoder[A] =
    (x: A) =>
      val encoded =
        productType.elements.map(p => p.typeclass.encode(p.getValue(x)))
      val totalSize =
        encoded.foldLeft(4)((acc, bytes) => acc + 4 + bytes.length)
      val buffer = ByteBuffer.allocate(totalSize)
      buffer.putInt(encoded.size)
      encoded.foreach { bytes =>
        buffer.putInt(bytes.length)
        buffer.put(IArray.genericWrapArray(bytes).toArray)
      }
      IArray.unsafeFromArray(buffer.array())

  override def deriveSum[A](sumType: SumType[A]): ByteEncoder[A] = (x: A) =>
    val elem = sumType.getElement(x)
    val encodedIndex = ByteEncoder.forInt.encode(elem.idx)
    val encodedContent = elem.typeclass.encode(elem.cast(x))
    val size = encodedIndex.length + encodedContent.length
    val buffer = ByteBuffer.allocate(size)
    buffer.put(IArray.genericWrapArray(encodedIndex).toArray)
    buffer.put(IArray.genericWrapArray(encodedContent).toArray)
    IArray.unsafeFromArray(buffer.array())
