package org.aulune.commons
package pagination.cursor


import instances.WithDerivation
import pagination.cursor

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import scala.collection.Factory
import scala.util.Try


/** Decodes elements from bytes.
 *  @tparam A type of elements.
 */
trait ByteDecoder[A]:
  /** Decodes given bytes into type [[A]].
   *  @param bytes bytes to decode.
   *  @return [[A]] if decoding is successful.
   */
  def decode(bytes: IArray[Byte]): Option[A]

  /** Creates new decoder based on this one.
   *  @param f function to decode [[B]] from [[A]].
   *  @tparam B new type.
   *  @return decoder for new type.
   */
  final def map[B](f: A => Option[B]): ByteDecoder[B] =
    bytes => decode(bytes).flatMap(f)


object ByteDecoder extends WithDerivation[ByteDecoder]:
  transparent inline def apply[A: ByteDecoder]: ByteDecoder[A] = summon

  /** [[ByteDecoder]] for strings. */
  given forString: ByteDecoder[String] = bytes =>
    Try(
      new String(IArray.genericWrapArray(bytes).toArray, StandardCharsets.UTF_8),
    ).toOption

  /** [[ByteDecoder]] for bytes. */
  given forByte: ByteDecoder[Byte] = bytes =>
    Try {
      ByteBuffer.wrap(IArray.genericWrapArray(bytes).toArray).get()
    }.toOption

  /** [[ByteDecoder]] for shorts. */
  given forShort: ByteDecoder[Short] = bytes =>
    Try {
      ByteBuffer.wrap(IArray.genericWrapArray(bytes).toArray).getShort()
    }.toOption

  /** [[ByteDecoder]] for ints. */
  given forInt: ByteDecoder[Int] = bytes =>
    Try {
      ByteBuffer.wrap(IArray.genericWrapArray(bytes).toArray).getInt()
    }.toOption

  /** [[ByteDecoder]] for longs. */
  given forLong: ByteDecoder[Long] = bytes =>
    Try {
      ByteBuffer.wrap(IArray.genericWrapArray(bytes).toArray).getLong()
    }.toOption

  /** [[ByteDecoder]] for floats. */
  given forFloat: ByteDecoder[Float] = bytes =>
    Try {
      ByteBuffer.wrap(IArray.genericWrapArray(bytes).toArray).getFloat()
    }.toOption

  /** [[ByteDecoder]] for doubles. */
  given forDouble: ByteDecoder[Double] = bytes =>
    Try {
      ByteBuffer.wrap(IArray.genericWrapArray(bytes).toArray).getDouble()
    }.toOption

  /** [[ByteDecoder]] for booleans. */
  given forBoolean: ByteDecoder[Boolean] = forByte.map {
    case 0 => Some(false)
    case 1 => Some(true)
    case _ => None
  }

  /** [[ByteDecoder]] for iterables. */
  given forIterable[C[_] <: Iterable[_], A](using
      decoder: ByteDecoder[A],
      factory: Factory[A, C[A]],
  ): ByteDecoder[C[A]] = bytes =>
    Try {
      val array = IArray.genericWrapArray(bytes).toArray
      val buffer = ByteBuffer.wrap(array)
      val size = buffer.getInt

      val builder = factory.newBuilder
      (1 to size).foreach { _ =>
        val length = buffer.getInt
        val allocated = new Array[Byte](length)
        val _ = buffer.get(allocated)
        val iArray = IArray.unsafeFromArray(allocated)
        decoder.decode(iArray) match
          case Some(value) => builder += value
          case None        => throw Throwable()
      }
      builder.result()
    }.toOption

  override def deriveProduct[A](productType: ProductType[A]): ByteDecoder[A] =
    bytes =>
      Try {
        val array = IArray.genericWrapArray(bytes).toArray
        val buffer = ByteBuffer.wrap(array)
        val size = buffer.getInt

        val components = productType.elements.map { el =>
          val length = buffer.getInt
          val allocated = new Array[Byte](length)
          val _ = buffer.get(allocated)
          val iArray = IArray.unsafeFromArray(allocated)
          el.typeclass.decode(iArray) match
            case Some(value) => value
            case None        => throw Throwable()
        }
        productType.fromElements(components)
      }.toOption

  override def deriveSum[A](sumType: SumType[A]): ByteDecoder[A] = bytes =>
    for
      (indexBytes, elementBytes) <- Try(bytes.splitAt(4)).toOption
      index <- ByteDecoder.forInt.decode(indexBytes)
      element <- sumType.elements.find(_.idx == index)
      decoded <- element.typeclass.decode(elementBytes)
    yield decoded.asInstanceOf[A]
