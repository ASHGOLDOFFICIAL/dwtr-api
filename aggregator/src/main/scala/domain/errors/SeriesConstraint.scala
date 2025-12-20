package org.aulune.aggregator
package domain.errors


/** Constraints that exist on series as collection. */
enum SeriesConstraint:
  /** ID should be unique. */
  case UniqueId
