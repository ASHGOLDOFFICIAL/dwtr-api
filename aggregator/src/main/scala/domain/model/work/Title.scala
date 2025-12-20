package org.aulune.aggregator
package domain.model.work

/** Piece of media title. */
opaque type Title <: String = String


object Title:
  /** Returns [[Title]] if argument is valid.
   *
   *  To be valid string should not be empty and should not consist of
   *  whitespaces only. All whitespaces are being stripped.
   *
   *  @param title piece of media title.
   */
  def apply(title: String): Option[Title] =
    val stripped = title.strip()
    Option.when(stripped.nonEmpty)(stripped)

  /** Unsafe constructor to use inside always-valid boundary.
   *  @param title piece of media title.
   *  @throws IllegalArgumentException if given params are invalid.
   */
  def unsafe(title: String): Title = Title(title) match
    case Some(value) => value
    case None        => throw new IllegalArgumentException()
