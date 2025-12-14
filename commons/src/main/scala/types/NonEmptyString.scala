package org.aulune.commons
package types

import scala.compiletime.error

/** Non-empty string. */
opaque type NonEmptyString <: String = String


object NonEmptyString:
  /** Makes [[NonEmptyString]] out of given string with compile time checks.
   *  @return given string as [[NonEmptyString]] or compilation error.
   */
  inline def apply(string: String): NonEmptyString =
    inline if string != "" then string
    else error("String must be non-empty")

  /** Validates string to be non-empty.
   *  @param string string.
   *  @return [[NonEmptyString]] if string is non-empty.
   */
  def from(string: String): Option[NonEmptyString] =
    Option.when(string.nonEmpty)(string)

  /** Unsafe constructor to use inside always-valid boundary.
   *  @param string string.
   *  @throws IllegalArgumentException when empty string was given.
   */
  def unsafe(string: String): NonEmptyString = from(string) match
    case Some(value) => value
    case None        => throw new IllegalArgumentException()
