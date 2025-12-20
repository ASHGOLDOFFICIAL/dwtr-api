package org.aulune.aggregator
package application.dto.work


import application.dto.shared.{ExternalResourceDTO, ReleaseDateDTO}

import java.net.URI
import java.time.LocalDate
import java.util.UUID


/** Work creation request body.
 *
 *  @param title work title.
 *  @param synopsis brief description.
 *  @param releaseDate release date.
 *  @param writers IDs of writers.
 *  @param seriesId series ID.
 *  @param seriesSeason season.
 *  @param seriesNumber number in series.
 *  @param episodeType episode type.
 *  @param selfHostedLocation link to self-hosted place.
 *  @param externalResources links to external resources.
 */
final case class CreateWorkRequest(
    title: String,
    synopsis: String,
    releaseDate: ReleaseDateDTO,
    writers: List[UUID],
    cast: List[CastMemberDTO],
    seriesId: Option[UUID],
    seriesSeason: Option[Int],
    seriesNumber: Option[Int],
    episodeType: Option[EpisodeTypeDTO],
    selfHostedLocation: Option[URI],
    externalResources: List[ExternalResourceDTO],
)
