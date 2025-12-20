package org.aulune.aggregator
package domain.errors

import scala.util.control.NoStackTrace


/** Errors that can occur during work validation. */
enum WorkValidationError extends NoStackTrace:
  // Argument validation.

  /** Work title is invalid. */
  case InvalidTitle

  /** Synopsis is invalid. */
  case InvalidSynopsis

  /** Invalid release date. */
  case InvalidReleaseDate

  /** Invalid cast. */
  case InvalidCast

  /** Invalid season. */
  case InvalidSeason

  /** Invalid series number. */
  case InvalidSeriesNumber

  /** Invalid self-hosted location. */
  case InvalidSelfHostedLocation

  // State validation.

  /** Some writers are listed more than once. */
  case WriterDuplicates

  /** Some cast members are listed more than once. */
  case CastMemberDuplicates

  /** Series is specified but episode type is empty. */
  case EpisodeTypeIsMissing

  /** Season, series number or episode was given without series ID. */
  case SeriesIsMissing
