package org.aulune.aggregator
package application.dto.translation


/** Values to be used as translation type. */
enum TranslationTypeDTO:
  /** Translated as document. */
  case Transcript

  /** Synchronized subtitles. */
  case Subtitles

  /** Voice-over. */
  case VoiceOver
