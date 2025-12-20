package org.aulune.aggregator
package domain.model.series

/** Series name. */
opaque type SeriesName <: String = String


object SeriesName:
  /** Returns [[SeriesName]] if argument is valid.
   *
   *  To be valid string should not be empty and should not consist of
   *  whitespaces only. All whitespaces are being stripped.
   *
   *  @param name name.
   */
  def apply(name: String): Option[SeriesName] =
    val stripped = name.strip()
    Option.when(stripped.nonEmpty)(stripped)

  /** Unsafe constructor to use inside always-valid boundary.
   *  @param name series name.
   *  @throws IllegalArgumentException if given params are invalid.
   */
  def unsafe(name: String): SeriesName = SeriesName(name) match
    case Some(value) => value
    case None        => throw new IllegalArgumentException()
