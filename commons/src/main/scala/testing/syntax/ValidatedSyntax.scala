package org.aulune.commons
package testing.syntax

import cats.data.Validated


extension [E, A](validated: Validated[E, A])
  /** Gets valid or throws an exception.
   *  @throws IllegalStateException if either was right.
   */
  def getValid: A = validated match
    case invalid: Validated.Invalid[E] =>
      throw IllegalStateException(s"Valid was expected, but got $invalid")
    case Validated.Valid(value) => value

  /** Gets invalid of validated or throws an exception.
   *  @throws IllegalStateException if either was left.
   */
  def getInvalid: E = validated match
    case valid: Validated.Valid[A] =>
      throw IllegalStateException(s"Invalid was expected, but got $valid")
    case Validated.Invalid(value) => value
