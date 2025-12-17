package org.aulune.aggregator
package domain.model.audioplay.translation


import org.aulune.commons.filter.{Filter, FilterField}
import org.aulune.commons.types.NonEmptyString


/** Fields of [[AudioPlayTranslation]] that are allowed to be filtered on. */
enum AudioPlayTranslationFilterField:
  case OriginalId


object AudioPlayTranslationFilterField:
  /** [[FilterField]] instance for filtering. */
  private object Field extends FilterField[AudioPlayTranslationFilterField]:
    override def fromName(
        name: NonEmptyString,
    ): Option[AudioPlayTranslationFilterField] = name match
      case s if s == "original_id" => Some(OriginalId)
      case _                       => None

    override def allows(
        a: AudioPlayTranslationFilterField,
    )(operation: Filter.Operator, value: Filter.Literal): Boolean = a match
      case AudioPlayTranslationFilterField.OriginalId =>
        FilterField.stringAllows(operation, value)

  given FilterField[AudioPlayTranslationFilterField] = Field
