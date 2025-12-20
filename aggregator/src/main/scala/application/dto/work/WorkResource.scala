package org.aulune.aggregator
package application.dto.work


import application.dto.person.PersonResource
import application.dto.series.SeriesResource
import application.dto.shared.{ExternalResourceDTO, ReleaseDateDTO}
import application.dto.work.WorkResource.CastMemberResource

import java.net.URI
import java.time.LocalDate
import java.util.UUID


/** Work response body.
 *  @param id work ID.
 *  @param title work title.
 *  @param synopsis brief description.
 *  @param releaseDate release date.
 *  @param writers writers of this work.
 *  @param series series.
 *  @param seriesSeason season.
 *  @param seriesNumber number in series.
 *  @param episodeType type of episode in relation to series.
 *  @param coverUri link to cover image.
 *  @param externalResources links to external resources.
 */
final case class WorkResource(
    id: UUID,
    title: String,
    synopsis: String,
    releaseDate: ReleaseDateDTO,
    writers: List[PersonResource],
    cast: List[CastMemberResource],
    series: Option[SeriesResource],
    seriesSeason: Option[Int],
    seriesNumber: Option[Int],
    episodeType: Option[EpisodeTypeDTO],
    coverUri: Option[URI],
    externalResources: List[ExternalResourceDTO],
)


object WorkResource:
  /** Cast member representation.
   *
   *  @param actor actor (cast member).
   *  @param roles roles this actor performed.
   *  @param main is this cast member considered part of main cast.
   */
  final case class CastMemberResource(
      actor: PersonResource,
      roles: List[String],
      main: Boolean,
  )
