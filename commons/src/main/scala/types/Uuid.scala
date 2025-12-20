package org.aulune.commons
package types


import pagination.cursor.{ByteDecoder, ByteEncoder}

import java.util.UUID
import scala.util.Try


/** UUID for given [[A]]. Use for type safety.
 *  @tparam A type of object this UUID identifies.
 */
opaque type Uuid[A] <: UUID = UUID


object Uuid:
  /** Returns [[Uuid]] from given UUID.
   *  @tparam A type of object this UUID identifies.
   *  @param uuid UUID.
   */
  def apply[A](uuid: UUID): Uuid[A] = uuid

  /** Validates string to be valid UUID.
   *  @param uuid UUID.
   *  @tparam A type of object this UUID identifies.
   *  @return [[Uuid]] if string is valid UUID.
   */
  def apply[A](uuid: String): Option[Uuid[A]] =
    Try(UUID.fromString(uuid)).toOption

  /** Unsafe constructor to use inside always-valid boundary.
   *  @param uuid UUID string.
   *  @tparam A type of object this UUID identifies.
   */
  def unsafe[A](uuid: String): Uuid[A] = UUID.fromString(uuid)

  given byteEncoder[A]: ByteEncoder[Uuid[A]] =
    ByteEncoder.forString.map(_.toString)

  given byteDecoder[A]: ByteDecoder[Uuid[A]] =
    ByteDecoder.forString.map(s => Some(UUID.fromString(s)))
