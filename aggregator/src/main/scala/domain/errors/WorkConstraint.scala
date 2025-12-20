package org.aulune.aggregator
package domain.errors


/** Constraints that exist on pieces of media as collection. */
enum WorkConstraint:
  /** ID should be unique. */
  case UniqueId

  /** Series info (series, season, number, episode type) should be unique. */
  case UniqueSeriesInfo
