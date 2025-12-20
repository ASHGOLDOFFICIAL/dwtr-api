package org.aulune.aggregator
package domain.repositories


import domain.model.audioplay.series.{
  AudioPlaySeries,
  AudioPlaySeriesFilterField,
}

import org.aulune.commons.repositories.{
  BatchGet,
  FilterList,
  GenericRepository,
  TextSearch,
}
import org.aulune.commons.types.Uuid


trait AudioPlaySeriesRepository[F[_]]
    extends GenericRepository[F, AudioPlaySeries, Uuid[AudioPlaySeries]]
    with BatchGet[F, AudioPlaySeries, Uuid[AudioPlaySeries]]
    with FilterList[F, AudioPlaySeries, AudioPlaySeriesFilterField]
    with TextSearch[F, AudioPlaySeries]
