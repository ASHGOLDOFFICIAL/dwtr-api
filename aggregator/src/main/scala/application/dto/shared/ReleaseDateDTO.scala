package org.aulune.aggregator
package application.dto.shared


import application.dto.shared.ReleaseDateDTO.DateAccuracyDTO

import java.time.LocalDate


/** Release date of something.
 *  @param date date itself.
 *  @param accuracy degree of accuracy.
 */
final case class ReleaseDateDTO(date: LocalDate, accuracy: DateAccuracyDTO)


object ReleaseDateDTO:
  /** Tells how accurate given date is. */
  enum DateAccuracyDTO:
    /** Date is unknown. */
    case Unknown

    /** Everything up to day is correct. */
    case Day

    /** Everything up to month is correct. */
    case Month

    /** Everything up to year is correct. */
    case Year
