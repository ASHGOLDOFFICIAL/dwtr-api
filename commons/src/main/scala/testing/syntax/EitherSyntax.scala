package org.aulune.commons
package testing.syntax


extension [A, B](either: Either[A, B])
  /** Gets left side of either or throws an exception.
   *  @throws IllegalStateException if either was right.
   */
  def getLeft: A = either match
    case right: Right[A, B] =>
      throw IllegalStateException(s"Left was expected, but got $right")
    case Left(value) => value

  /** Gets right side of either or throws an exception.
   *  @throws IllegalStateException if either was left.
   */
  def getRight: B = either match
    case left: Left[A, B] =>
      throw IllegalStateException(s"Right was expected, but got $left")
    case Right(value) => value
