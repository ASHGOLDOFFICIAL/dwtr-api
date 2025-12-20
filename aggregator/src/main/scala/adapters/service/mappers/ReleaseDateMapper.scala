package org.aulune.aggregator
package adapters.service.mappers


import application.dto.shared.ReleaseDateDTO
import application.dto.shared.ReleaseDateDTO.DateAccuracyDTO
import domain.model.shared.ReleaseDate
import domain.model.shared.ReleaseDate.DateAccuracy


/** Mapper between external [[ReleaseDateDTO]] and domain's [[ReleaseDate]].
 *  @note Should not be used outside `service` package to not expose domain
 *    type.
 */
private[service] object ReleaseDateMapper:
  private val mapToDomain = Map(
    DateAccuracyDTO.Unknown -> DateAccuracy.Unknown,
    DateAccuracyDTO.Year -> DateAccuracy.Year,
    DateAccuracyDTO.Month -> DateAccuracy.Month,
    DateAccuracyDTO.Day -> DateAccuracy.Day,
  )
  private val mapFromDomain = mapToDomain.map(_.swap)

  /** Convert [[ReleaseDateDTO]] to [[ReleaseDate]].
   *  @param dto external layer object.
   *  @return mapped domain object.
   */
  def toDomain(dto: ReleaseDateDTO): Option[ReleaseDate] =
    ReleaseDate(date = dto.date, accuracy = mapToDomain(dto.accuracy))

  /** Convert [[ReleaseDate]] to [[ReleaseDateDTO]].
   *  @param domain inner domain object.
   *  @return mapped external object.
   */
  def fromDomain(domain: ReleaseDate): ReleaseDateDTO = ReleaseDateDTO(
    date = domain.date,
    accuracy = mapFromDomain(domain.accuracy))
