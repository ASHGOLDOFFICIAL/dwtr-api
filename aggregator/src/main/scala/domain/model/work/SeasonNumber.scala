package org.aulune.aggregator
package domain.model.work

/** Piece of media season number. */
opaque type SeasonNumber <: Int = Int


object SeasonNumber:
  /** Returns [[SeasonNumber]] if argument is valid.
   *
   *  @param season season number.
   */
  def apply(season: Int): Option[SeasonNumber] = Option.when(season > 0)(season)

  /** Unsafe constructor to use inside always-valid boundary.
   *  @param season season number.
   *  @throws IllegalArgumentException if given params are invalid.
   */
  def unsafe(season: Int): SeasonNumber = SeasonNumber(season) match
    case Some(value) => value
    case None        => throw new IllegalArgumentException()
