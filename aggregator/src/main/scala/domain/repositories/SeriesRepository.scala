package org.aulune.aggregator
package domain.repositories


import domain.model.series.{Series, SeriesField}

import org.aulune.commons.repositories.{
  BatchGet,
  FilterList,
  GenericRepository,
  TextSearch,
}
import org.aulune.commons.types.Uuid


trait SeriesRepository[F[_]]
    extends GenericRepository[F, Series, Uuid[Series]]
    with BatchGet[F, Series, Uuid[Series]]
    with FilterList[F, Series, SeriesField]
    with TextSearch[F, Series]
