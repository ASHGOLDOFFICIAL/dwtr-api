package org.aulune.aggregator
package api.http.tapir.work


import api.http.tapir.person.PersonExamples
import api.http.tapir.series.SeriesExamples
import application.dto.shared.ExternalResourceTypeDTO.Purchase
import application.dto.shared.ReleaseDateDTO.DateAccuracyDTO
import application.dto.shared.{ExternalResourceDTO, ReleaseDateDTO}
import application.dto.work.EpisodeTypeDTO.Regular
import application.dto.work.WorkResource.CastMemberResource
import application.dto.work.{
  CastMemberDTO,
  CreateWorkRequest,
  ListWorksResponse,
  SearchWorksResponse,
  WorkLocationResource,
  WorkResource,
}

import java.net.URI
import java.time.LocalDate
import java.util.{Base64, UUID}


/** Examples for DTO objects for works.
 *  @note ''Though Scoundrels Are Discovered'' (''Cicero'' 1.1) is used as an
 *    example.
 */
object WorkExamples:
  private val titleExample = "Though Scoundrels Are Discovered"
  private val synopsisExample = "Rome, 80 BC. A wealthy landowner has been murdered" +
    "in the street. His son, Sextus Roscius, is accused of the crime. When every " +
    "lawyer in the city turns down his case, there's only one man who can save " +
    "Roscius from a guilty verdict and a particularly grisly execution…\n\n" +
    "Marcus Tullius Cicero: a mere twenty-six years old, but a rising star in " +
    "the Forum. Together with his brother, Quintus, Cicero must investigate the " +
    "murder of Roscius’s father and find the true culprit; but in their quest for " +
    "justice, the brothers Cicero may be about to make some very powerful " +
    "enemies indeed..."
  private val releaseDateExample = ReleaseDateDTO(
    date = LocalDate.of(2017, 2, 28),
    accuracy = DateAccuracyDTO.Full,
  )

  private val writersExample = List(PersonExamples.DavidLlewellynResource)
  private val castExample = List(
    CastMemberResource(
      actor = PersonExamples.SamuelBarnettResource,
      roles = List("Marcus Tullius Cicero"),
      main = true,
    ),
    CastMemberResource(
      actor = PersonExamples.GeorgeNaylorResource,
      roles = List("Quintus Tullius Cicero"),
      main = true,
    ),
    CastMemberResource(
      actor = PersonExamples.StephenCritchlowResource,
      roles = List("Etrucius"),
      main = false,
    ),
    CastMemberResource(
      actor = PersonExamples.YoussefKerkourResource,
      roles = List("Titus Capito"),
      main = false,
    ),
    CastMemberResource(
      actor = PersonExamples.SimonLuddersResource,
      roles = List("Sextus Roscius"),
      main = false,
    ),
    CastMemberResource(
      actor = PersonExamples.ElizabethMortonResource,
      roles = List("Caecilia Metella"),
      main = false,
    ),
  )

  private val selfHostedLocationExample = URI.create(
    "https://selfhosted.org:8096/stable/web/#/details?id=6ea6c8076b1147849b2311030699d047",
  )

  private val seriesSeasonExample = Some(1)
  private val seriesNumberExample = Some(1)
  private val episodeTypeExample = Some(Regular)

  private val coverUriExample =
    Some(URI.create("https://www.bigfinish.com/image/release/1605/large.jpg"))

  private val externalResourcesExample = List(
    ExternalResourceDTO(
      Purchase,
      URI
        .create("https://www.bigfinish.com/releases/v/cicero-episode-1-1605"),
    ),
    ExternalResourceDTO(
      Purchase,
      URI
        .create("https://www.bigfinish.com/releases/v/cicero-series-01-1777"),
    ),
  )
  private val nextPageTokenExample =
    Some(Base64.getEncoder.encodeToString(titleExample.getBytes))

  val Resource: WorkResource = WorkResource(
    id = UUID.fromString("bab591f2-e256-4969-9b79-7652d6d8430e"),
    title = titleExample,
    synopsis = synopsisExample,
    releaseDate = releaseDateExample,
    writers = writersExample,
    cast = castExample,
    series = Some(SeriesExamples.Resource),
    seriesSeason = seriesSeasonExample,
    seriesNumber = seriesNumberExample,
    episodeType = episodeTypeExample,
    coverUri = coverUriExample,
    externalResources = externalResourcesExample,
  )

  val CreateRequest: CreateWorkRequest = CreateWorkRequest(
    title = titleExample,
    synopsis = synopsisExample,
    releaseDate = releaseDateExample,
    writers = writersExample.map(_.id),
    cast = castExample.map(r =>
      CastMemberDTO(
        actor = r.actor.id,
        roles = r.roles,
        main = r.main,
      )),
    seriesId = Some(SeriesExamples.Resource.id),
    seriesSeason = seriesSeasonExample,
    seriesNumber = seriesNumberExample,
    episodeType = episodeTypeExample,
    selfHostedLocation = Some(selfHostedLocationExample),
    externalResources = externalResourcesExample,
  )

  val ListResponse: ListWorksResponse = ListWorksResponse(
    works = List(Resource),
    nextPageToken = nextPageTokenExample,
  )

  val SearchResponse: SearchWorksResponse = SearchWorksResponse(
    works = List(Resource),
  )

  val GetSelfHostedLocationResponse: WorkLocationResource =
    WorkLocationResource(
      uri = selfHostedLocationExample,
    )
