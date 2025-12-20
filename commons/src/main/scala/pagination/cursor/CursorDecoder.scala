package org.aulune.commons
package pagination.cursor


import java.util.Base64
import scala.deriving.Mirror


/** Decoder of token string to cursor.
 *  @tparam A type of cursor.
 */
trait CursorDecoder[A]:
  /** Decodes given string into cursor of type [[A]].
   *  @param cursor token as encoded string.
   *  @return [[A]] if decoding is successful.
   */
  def decode(cursor: String): Option[A]


object CursorDecoder:
  /** Summons an instance of [[CursorDecoder]]. */
  final transparent inline def apply[A: CursorDecoder]: CursorDecoder[A] =
    summon

  /** Makes [[CursorDecoder]] from [[ByteDecoder]].
   *  @tparam A type to encode.
   */
  given fromByteDecoder[A: ByteDecoder]: CursorDecoder[A] = cursor =>
    val array = Base64.getDecoder.decode(cursor)
    ByteDecoder[A].decode(IArray.unsafeFromArray(array))

  /** Derives [[CursorDecoder]] by deriving [[ByteDecoder]].
   *  @tparam A type to decode.
   */
  inline given derived[A: Mirror.Of]: CursorDecoder[A] =
    fromByteDecoder(ByteDecoder.derived[A])
