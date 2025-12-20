package org.aulune.aggregator
package domain.repositories


import domain.model.audioplay.series.AudioPlaySeries
import domain.repositories.AudioPlaySeriesRepository.Cursor

import org.aulune.commons.pagination.cursor.{
  ByteDecoder,
  ByteEncoder,
  CursorDecoder,
  CursorEncoder,
}
import org.aulune.commons.repositories.{
  BatchGet,
  GenericRepository,
  PaginatedList,
  TextSearch,
}
import org.aulune.commons.types.Uuid


trait AudioPlaySeriesRepository[F[_]]
    extends GenericRepository[F, AudioPlaySeries, Uuid[AudioPlaySeries]]
    with BatchGet[F, AudioPlaySeries, Uuid[AudioPlaySeries]]
    with PaginatedList[F, AudioPlaySeries, Cursor]
    with TextSearch[F, AudioPlaySeries]


object AudioPlaySeriesRepository:
  /** Cursor to resume pagination.
   *  @param id identity of last entry.
   */
  final case class Cursor(id: Uuid[AudioPlaySeries])
      derives CursorEncoder,
        CursorDecoder
