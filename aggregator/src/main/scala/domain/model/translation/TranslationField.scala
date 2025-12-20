package org.aulune.aggregator
package domain.model.translation


import org.aulune.commons.filter.{Filter, FilterField}
import org.aulune.commons.types.NonEmptyString


/** Fields of [[Translation]] that are allowed to be filtered on. */
enum TranslationField:
  case Id
  case OriginalId


object TranslationField:
  /** [[FilterField]] instance for filtering. */
  private object Field extends FilterField[TranslationField]:
    override def fromName(
        name: NonEmptyString,
    ): Option[TranslationField] = name match
      case s if s == "id"          => Some(Id)
      case s if s == "original_id" => Some(OriginalId)
      case _                       => None

    override def allows(
        a: TranslationField,
    )(operation: Filter.Operator, value: Filter.Literal): Boolean = a match
      case TranslationField.Id => FilterField.stringAllows(operation, value)
      case TranslationField.OriginalId =>
        FilterField.stringAllows(operation, value)

  given FilterField[TranslationField] = Field
