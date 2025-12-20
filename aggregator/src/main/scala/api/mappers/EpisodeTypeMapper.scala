package org.aulune.aggregator
package api.mappers

import application.dto.work.EpisodeTypeDTO


/** Mapper between application layer's [[EpisodeTypeDTO]] and its API
 *  representation as strings.
 */
private[api] object EpisodeTypeMapper:
  private val fromDtoMapper = EpisodeTypeDTO.values.map {
    case t @ EpisodeTypeDTO.Regular => t -> "regular"
    case t @ EpisodeTypeDTO.Special => t -> "special"
  }.toMap

  private val fromStringMapper = fromDtoMapper.map(_.swap)

  val stringValues: List[String] = fromStringMapper.keys.toList

  /** Returns string representation of [[EpisodeTypeDTO]].
   *  @param dto DTO to represent.
   */
  def toString(dto: EpisodeTypeDTO): String = fromDtoMapper(dto)

  /** Returns [[EpisodeTypeDTO]] for given string if valid.
   *  @param str string.
   *  @return [[EpisodeTypeDTO]] or `None` if given string is not mapped to any
   *    DTO object.
   */
  def fromString(str: String): Option[EpisodeTypeDTO] =
    fromStringMapper.get(str)
