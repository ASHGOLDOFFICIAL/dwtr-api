package org.aulune.aggregator
package domain.model.audioplay.series


import org.aulune.commons.filter.{Filter, FilterField}
import org.aulune.commons.types.NonEmptyString


/** Fields of [[AudioPlaySeries]] that are allowed to be filtered on. */
enum AudioPlaySeriesFilterField:
  case Id


object AudioPlaySeriesFilterField:
  /** [[FilterField]] instance for filtering. */
  private object Field extends FilterField[AudioPlaySeriesFilterField]:
    override def fromName(
        name: NonEmptyString,
    ): Option[AudioPlaySeriesFilterField] = name match
      case s if s == "id" => Some(Id)
      case _              => None

    override def allows(
        a: AudioPlaySeriesFilterField,
    )(operation: Filter.Operator, value: Filter.Literal): Boolean = a match
      case AudioPlaySeriesFilterField.Id =>
        FilterField.stringAllows(operation, value)

  given FilterField[AudioPlaySeriesFilterField] = Field
