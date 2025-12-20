package org.aulune.aggregator
package domain.repositories


import domain.model.audioplay.translation.{
  AudioPlayTranslation,
  AudioPlayTranslationFilterField,
}
import domain.repositories.AudioPlayTranslationRepository.AudioPlayTranslationCursor

import org.aulune.commons.filter.Filter
import org.aulune.commons.pagination.cursor.{CursorDecoder, CursorEncoder}
import org.aulune.commons.repositories.GenericRepository
import org.aulune.commons.types.Uuid


/** Repository for [[AudioPlayTranslation]] objects.
 *
 *  @tparam F effect type.
 */
trait AudioPlayTranslationRepository[F[_]]
    extends GenericRepository[
      F,
      AudioPlayTranslation,
      Uuid[AudioPlayTranslation],
    ]:

  /** Lists translation.
   *  @param count amount of elements.
   *  @param cursor cursor to last element.
   *  @param filter optional filter expression.
   */
  def list(
      count: Int,
      cursor: Option[AudioPlayTranslationCursor],
      filter: Option[Filter[AudioPlayTranslationFilterField]],
  ): F[List[AudioPlayTranslation]]


object AudioPlayTranslationRepository:
  /** Cursor to resume pagination of translations.
   *  @param id ID of this translation.
   */
  final case class AudioPlayTranslationCursor(
      id: Uuid[AudioPlayTranslation],
  ) derives CursorEncoder,
        CursorDecoder
