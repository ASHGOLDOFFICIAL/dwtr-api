package org.aulune.aggregator
package domain.model.series


import org.aulune.commons.filter.{Filter, FilterField}
import org.aulune.commons.types.NonEmptyString


/** Fields of [[Series]] that are allowed to be filtered on. */
enum SeriesField:
  case Id


object SeriesField:
  /** [[FilterField]] instance for filtering. */
  private object Field extends FilterField[SeriesField]:
    override def fromName(
        name: NonEmptyString,
    ): Option[SeriesField] = name match
      case s if s == "id" => Some(Id)
      case _              => None

    override def allows(
        a: SeriesField,
    )(operation: Filter.Operator, value: Filter.Literal): Boolean = a match
      case SeriesField.Id => FilterField.stringAllows(operation, value)

  given FilterField[SeriesField] = Field
