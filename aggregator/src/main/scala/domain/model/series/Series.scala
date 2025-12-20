package org.aulune.aggregator
package domain.model.series


import domain.errors.SeriesValidationError
import domain.model.series.Series.ValidationResult

import cats.data.{Validated, ValidatedNec}
import cats.syntax.all.given
import org.aulune.commons.types.Uuid


/** Series representation.
 *  @param id series ID.
 *  @param name series name.
 */
final case class Series private (
    id: Uuid[Series],
    name: SeriesName,
):
  /** Copies with validation. */
  def update(
      id: Uuid[Series] = id,
      name: SeriesName = name,
  ): ValidationResult[Series] = Series(
    id = id,
    name = name,
  )


object Series:
  private type ValidationResult[A] = ValidatedNec[SeriesValidationError, A]

  /** Creates a series with state validation.
   *  @param id series ID.
   *  @param name series name.
   *  @return series validation result.
   */
  def apply(
      id: Uuid[Series],
      name: SeriesName,
  ): ValidationResult[Series] = new Series(id, name).validNec

  /** Unsafe constructor to use inside always-valid boundary.
   *
   *  @param id series ID.
   *  @param name series name.
   *  @throws SeriesValidationError if given params are invalid.
   */
  def unsafe(
      id: Uuid[Series],
      name: SeriesName,
  ): Series = Series(id, name) match
    case Validated.Valid(value)  => value
    case Validated.Invalid(errs) => throw errs.head
