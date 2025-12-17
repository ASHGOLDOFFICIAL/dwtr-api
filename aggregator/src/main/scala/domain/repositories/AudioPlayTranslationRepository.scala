package org.aulune.aggregator
package domain.repositories


import domain.model.audioplay.AudioPlay
import domain.model.audioplay.translation.{
  AudioPlayTranslation,
  AudioPlayTranslationFilterField,
}
import domain.repositories.AudioPlayTranslationRepository.AudioPlayTranslationCursor

import org.aulune.commons.filter.Filter
import org.aulune.commons.pagination.{CursorDecoder, CursorEncoder}
import org.aulune.commons.repositories.{GenericRepository, PaginatedList}
import org.aulune.commons.types.Uuid

import java.nio.charset.StandardCharsets
import java.nio.charset.StandardCharsets.UTF_8
import java.util.Base64
import scala.util.Try


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
  )

  given CursorDecoder[AudioPlayTranslationCursor] = token =>
    Try {
      val decoded = Base64.getUrlDecoder.decode(token)
      val rawId = new String(decoded, UTF_8)
      val id = Uuid[AudioPlayTranslation](rawId).get
      AudioPlayTranslationCursor(id)
    }.toOption

  given CursorEncoder[AudioPlayTranslationCursor] = token =>
    val raw = token.id.toString
    Base64.getUrlEncoder.withoutPadding.encodeToString(raw.getBytes(UTF_8))
