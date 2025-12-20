package org.aulune.aggregator
package adapters.service.mappers


import application.dto.work.EpisodeTypeDTO
import application.dto.shared.{ExternalResourceDTO, ExternalResourceTypeDTO}
import domain.model.work.EpisodeType
import domain.model.shared.{ExternalResource, ExternalResourceType}


/** Mapper between external [[EpisodeTypeDTO]] and domain's [[EpisodeType]].
 *  @note Should not be used outside `service` package to not expose domain
 *    type.
 */
private[service] object EpisodeTypeMapper:
  private val mapToDomain = Map(
    EpisodeTypeDTO.Regular -> EpisodeType.Regular,
    EpisodeTypeDTO.Special -> EpisodeType.Special,
  )
  private val mapFromDomain = mapToDomain.map(_.swap)

  /** Convert [[EpisodeTypeDTO]] to [[EpisodeType]].
   *  @param dto external layer object.
   *  @return mapped domain object.
   */
  def toDomain(dto: EpisodeTypeDTO): EpisodeType = mapToDomain(dto)

  /** Convert [[EpisodeType]] to [[EpisodeTypeDTO]].
   *  @param domain inner domain object.
   *  @return mapped external object.
   */
  def fromDomain(domain: EpisodeType): EpisodeTypeDTO = mapFromDomain(domain)
