package org.aulune.aggregator
package adapters.jdbc.postgres.metas


import domain.model.shared.Language
import domain.model.translation.{TranslatedTitle, TranslationType}

import doobie.Meta


private[postgres] object TranslationMetas:
  given Meta[TranslatedTitle] = Meta[String].tiemap { str =>
    TranslatedTitle(str).toRight(
      s"Failed to decode TranslatedTitle from: $str.")
  }(identity)

  // Potentially unsafe
  given Meta[TranslationType] = Meta[Int].timap {
    case 1 => TranslationType.Transcript
    case 2 => TranslationType.Subtitles
    case 3 => TranslationType.VoiceOver
  } {
    case TranslationType.Transcript => 1
    case TranslationType.Subtitles  => 2
    case TranslationType.VoiceOver  => 3
  }

  given Meta[Language] = Meta[String].timap {
    case "rus" => Language.Russian
    case "urk" => Language.Ukrainian
  } {
    case Language.Russian   => "rus"
    case Language.Ukrainian => "urk"
  }
