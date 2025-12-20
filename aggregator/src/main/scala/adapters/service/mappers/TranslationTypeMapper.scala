package org.aulune.aggregator
package adapters.service.mappers


import application.dto.translation.TranslationTypeDTO
import application.dto.translation.TranslationTypeDTO.*
import domain.model.translation.TranslationType


/** Mapper between external [[TranslationTypeDTO]] and domain's
 *  [[TranslationType]].
 *
 *  @note Should not be used outside `service` package to not expose domain
 *    type.
 */
private[service] object TranslationTypeMapper:
  private val toType = Map(
    Transcript -> TranslationType.Transcript,
    Subtitles -> TranslationType.Subtitles,
    VoiceOver -> TranslationType.VoiceOver,
  )
  private val fromType = toType.map(_.swap)

  /** Convert [[TranslationTypeDTO]] to [[TranslationType]].
   *
   *  @param dto external layer object.
   *  @return mapped domain object.
   */
  def toDomain(dto: TranslationTypeDTO): TranslationType = toType(dto)

  /** Convert [[TranslationType]] to [[TranslationTypeDTO]].
   *
   *  @param domain inner domain object.
   *  @return mapped external object.
   */
  def fromDomain(
      domain: TranslationType,
  ): TranslationTypeDTO = fromType(domain)
