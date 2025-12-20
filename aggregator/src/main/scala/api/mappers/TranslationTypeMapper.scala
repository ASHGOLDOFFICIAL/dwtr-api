package org.aulune.aggregator
package api.mappers

import application.dto.translation.TranslationTypeDTO


/** Mapper between application layer's [[TranslationTypeDTO]] and its API
 *  representation as strings.
 */
private[api] object TranslationTypeMapper:
  private val fromStringMapper = Map(
    "transcript" -> TranslationTypeDTO.Transcript,
    "subtitles" -> TranslationTypeDTO.Subtitles,
    "voiceover" -> TranslationTypeDTO.VoiceOver,
  )
  private val fromDtoMapper = fromStringMapper.map(_.swap)

  val stringValues: List[String] = fromStringMapper.keys.toList

  /** Returns string representation of [[TranslationTypeDTO]].
   *
   *  @param dto DTO to represent.
   */
  def toString(dto: TranslationTypeDTO): String = fromDtoMapper(dto)

  /** Returns [[TranslationTypeDTO]] for given string if valid.
   *
   *  @param str string.
   *  @return [[TranslationTypeDTO]] or `None` if given string is not mapped to
   *    any DTO object.
   */
  def fromString(str: String): Option[TranslationTypeDTO] =
    fromStringMapper.get(str)
