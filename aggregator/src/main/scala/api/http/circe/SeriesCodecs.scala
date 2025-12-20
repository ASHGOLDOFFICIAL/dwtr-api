package org.aulune.aggregator
package api.http.circe


import application.dto.series.{
  BatchGetSeriesRequest,
  BatchGetSeriesResponse,
  CreateSeriesRequest,
  ListSeriesRequest,
  ListSeriesResponse,
  SearchSeriesRequest,
  SearchSeriesResponse,
  SeriesResource,
}

import io.circe.generic.extras.semiauto.{
  deriveConfiguredDecoder,
  deriveConfiguredEncoder,
}
import io.circe.{Decoder, Encoder}
import org.aulune.commons.adapters.circe.CirceUtils.config


/** [[Encoder]] and [[Decoder]] instances for series DTOs. */
private[api] object SeriesCodecs:
  given Encoder[SeriesResource] = deriveConfiguredEncoder
  given Decoder[SeriesResource] = deriveConfiguredDecoder

  given Encoder[BatchGetSeriesResponse] = deriveConfiguredEncoder
  given Decoder[BatchGetSeriesResponse] = deriveConfiguredDecoder

  given Encoder[BatchGetSeriesRequest] = deriveConfiguredEncoder
  given Decoder[BatchGetSeriesRequest] = deriveConfiguredDecoder

  given Encoder[CreateSeriesRequest] = deriveConfiguredEncoder
  given Decoder[CreateSeriesRequest] = deriveConfiguredDecoder

  given Encoder[ListSeriesResponse] = deriveConfiguredEncoder
  given Decoder[ListSeriesResponse] = deriveConfiguredDecoder

  given Encoder[ListSeriesRequest] = deriveConfiguredEncoder
  given Decoder[ListSeriesRequest] = deriveConfiguredDecoder

  given Encoder[SearchSeriesRequest] = deriveConfiguredEncoder
  given Decoder[SearchSeriesRequest] = deriveConfiguredDecoder

  given Encoder[SearchSeriesResponse] = deriveConfiguredEncoder
  given Decoder[SearchSeriesResponse] = deriveConfiguredDecoder
