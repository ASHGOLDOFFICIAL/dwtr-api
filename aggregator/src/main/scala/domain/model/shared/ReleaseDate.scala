package org.aulune.aggregator
package domain.model.shared


import domain.model.shared.ReleaseDate.DateAccuracy

import java.time.LocalDate


/** Release date of something.
 *  @param date date itself.
 *  @param accuracy degree of accuracy.
 */
final case class ReleaseDate private (date: LocalDate, accuracy: DateAccuracy)


object ReleaseDate:
  /** Returns [[ReleaseDate]] for given [[LocalDate]] and accuracy.
   *
   *  @param date release date as [[LocalDate]].
   *  @param accuracy degree of date accuracy.
   *  @return validation result.
   */
  def apply(
      date: LocalDate,
      accuracy: DateAccuracy,
  ): Option[ReleaseDate] = Some(new ReleaseDate(date, accuracy))

  /** Unsafe constructor to use inside always-valid boundary.
   *
   *  @param date release date.
   *  @param accuracy degree of date accuracy.
   *  @throws IllegalArgumentException if given params are invalid.
   */
  def unsafe(date: LocalDate, accuracy: DateAccuracy): ReleaseDate =
    ReleaseDate(date, accuracy) match
      case Some(value) => value
      case None        => throw new IllegalArgumentException()

  /** Tells how accurate given date is. */
  enum DateAccuracy:
    /** Date is unknown. */
    case Unknown

    /** Everything up to day is correct. */
    case Day

    /** Everything up to month is correct. */
    case Month

    /** Everything up to year is correct. */
    case Year
