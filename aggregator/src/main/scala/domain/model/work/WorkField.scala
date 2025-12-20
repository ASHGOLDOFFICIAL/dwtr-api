package org.aulune.aggregator
package domain.model.work


import org.aulune.commons.filter.{Filter, FilterField}
import org.aulune.commons.types.NonEmptyString


/** Fields of [[Work]] that are allowed to be filtered on. */
enum WorkField:
  case Id


object WorkField:
  /** [[FilterField]] instance for filtering. */
  private object Field extends FilterField[WorkField]:
    override def fromName(name: NonEmptyString): Option[WorkField] = name match
      case s if s == "id" => Some(Id)
      case _              => None

    override def allows(
        a: WorkField,
    )(operation: Filter.Operator, value: Filter.Literal): Boolean = a match
      case WorkField.Id => FilterField.stringAllows(operation, value)

  given FilterField[WorkField] = Field
