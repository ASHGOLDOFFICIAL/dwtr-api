package org.aulune.aggregator
package domain.model.audioplay


import domain.model.audioplay.translation.AudioPlayTranslation

import org.aulune.commons.filter.{Filter, FilterField}
import org.aulune.commons.types.NonEmptyString


/** Fields of [[AudioPlay]] that are allowed to be filtered on. */
enum AudioPlayFilterField:
  case Id


object AudioPlayFilterField:
  /** [[FilterField]] instance for filtering. */
  private object Field extends FilterField[AudioPlayFilterField]:
    override def fromName(name: NonEmptyString): Option[AudioPlayFilterField] =
      name match
        case s if s == "id" => Some(Id)
        case _              => None

    override def allows(
        a: AudioPlayFilterField,
    )(operation: Filter.Operator, value: Filter.Literal): Boolean = a match
      case AudioPlayFilterField.Id => FilterField.stringAllows(operation, value)

  given FilterField[AudioPlayFilterField] = Field
