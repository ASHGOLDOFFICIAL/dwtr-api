package org.aulune.aggregator
package domain.repositories


import domain.model.audioplay.series.AudioPlaySeries
import domain.model.audioplay.{AudioPlay, AudioPlayFilterField}

import org.aulune.commons.repositories.{
  FilterList,
  GenericRepository,
  TextSearch,
}
import org.aulune.commons.types.Uuid


/** Repository for [[AudioPlay]] objects.
 *  @tparam F effect type.
 */
trait AudioPlayRepository[F[_]]
    extends GenericRepository[F, AudioPlay, Uuid[AudioPlay]]
    with FilterList[F, AudioPlay, AudioPlayFilterField]
    with TextSearch[F, AudioPlay]
