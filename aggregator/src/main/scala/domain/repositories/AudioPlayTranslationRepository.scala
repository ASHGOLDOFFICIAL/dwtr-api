package org.aulune.aggregator
package domain.repositories


import domain.model.audioplay.translation.{
  AudioPlayTranslation,
  AudioPlayTranslationFilterField,
}

import org.aulune.commons.filter.Filter
import org.aulune.commons.pagination.cursor.{CursorDecoder, CursorEncoder}
import org.aulune.commons.repositories.{FilterList, GenericRepository}
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
    ]
    with FilterList[F, AudioPlayTranslation, AudioPlayTranslationFilterField]
