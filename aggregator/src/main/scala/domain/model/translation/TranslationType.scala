package org.aulune.aggregator
package domain.model.translation


/** Possible translation types. */
enum TranslationType:
  /** Translated as document. */
  case Transcript

  /** Synchronized subtitles. */
  case Subtitles

  /** Voice-over. */
  case VoiceOver
