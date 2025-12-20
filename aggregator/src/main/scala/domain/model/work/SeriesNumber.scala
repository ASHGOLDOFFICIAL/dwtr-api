package org.aulune.aggregator
package domain.model.work

/** Piece of media series number. */
opaque type SeriesNumber <: Int = Int


object SeriesNumber:
  /** Returns [[SeriesNumber]] if argument is valid.
   *
   *  @param number series number.
   */
  def apply(number: Int): Option[SeriesNumber] = Option.when(number > 0)(number)

  /** Unsafe constructor to use inside always-valid boundary.
   *  @param number series number.
   *  @throws IllegalArgumentException if given params are invalid.
   */
  def unsafe(number: Int): SeriesNumber = SeriesNumber(number) match
    case Some(value) => value
    case None        => throw new IllegalArgumentException()
