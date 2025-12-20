package org.aulune.aggregator
package domain.repositories


import domain.model.audioplay.AudioPlay
import domain.repositories.AudioPlayRepository.AudioPlayCursor

import org.aulune.commons.pagination.cursor.{CursorDecoder, CursorEncoder}
import org.aulune.commons.repositories.{
  GenericRepository,
  PaginatedList,
  TextSearch,
}
import org.aulune.commons.types.Uuid


/** Repository for [[AudioPlay]] objects.
 *  @tparam F effect type.
 */
trait AudioPlayRepository[F[_]]
    extends GenericRepository[F, AudioPlay, Uuid[AudioPlay]]
    with PaginatedList[F, AudioPlay, AudioPlayCursor]
    with TextSearch[F, AudioPlay]


object AudioPlayRepository:
  /** Cursor to resume pagination of audio plays.
   *  @param id identity of [[AudioPlay]].
   */
  final case class AudioPlayCursor(id: Uuid[AudioPlay])
      derives CursorEncoder,
        CursorDecoder
