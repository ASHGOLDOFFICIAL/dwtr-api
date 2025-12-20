package org.aulune.commons
package pagination.cursor


import java.util.Base64
import scala.deriving.Mirror


/** Encoder of cursor [[A]] to string.
 *  @tparam A type of objects to encode.
 */
trait CursorEncoder[A]:
  /** Encodes given [[A]] into string.
   *  @param a [[A]] object.
   *  @return encoded string.
   */
  def encode(a: A): String


object CursorEncoder:
  /** Summons an instance of [[CursorEncoder]]. */
  final transparent inline def apply[A: CursorEncoder]: CursorEncoder[A] =
    summon

  /** Makes [[CursorEncoder]] from [[ByteEncoder]].
   *  @tparam A type to encode.
   */
  given fromByteEncoder[A: ByteEncoder]: CursorEncoder[A] = a =>
    val array = IArray.genericWrapArray(ByteEncoder[A].encode(a)).toArray
    Base64.getEncoder.encodeToString(array)

  /** Derives [[CursorEncoder]] by deriving [[ByteEncoder]].
   *  @tparam A type to encode.
   */
  inline given derived[A: Mirror.Of]: CursorEncoder[A] =
    fromByteEncoder(ByteEncoder.derived[A])
