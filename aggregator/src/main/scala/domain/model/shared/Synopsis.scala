package org.aulune.aggregator
package domain.model.shared

/** Synopsis for pieces of media. */
opaque type Synopsis <: String = String


object Synopsis:
  /** Returns [[Synopsis]] if argument is valid.
   *
   *  To be valid string should not be empty and should not consist of
   *  whitespaces only. All whitespaces are being stripped.
   *
   *  @param synopsis synopsis.
   */
  def apply(synopsis: String): Option[Synopsis] =
    val stripped = synopsis.strip()
    Option.when(stripped.nonEmpty)(stripped)

  /** Unsafe constructor to use inside always-valid boundary.
   *  @param synopsis synopsis string.
   *  @throws IllegalArgumentException if given params are invalid.
   */
  def unsafe(synopsis: String): Synopsis = Synopsis(synopsis) match
    case Some(value) => value
    case None        => throw new IllegalArgumentException()
