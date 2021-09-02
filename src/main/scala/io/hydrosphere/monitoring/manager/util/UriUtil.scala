package io.hydrosphere.monitoring.manager.util

import sttp.model.Uri

object UriUtil {
  def combine(baseUri: Uri, segment: Uri): Uri =
    baseUri.addPathSegments(segment.pathSegments.segments)
}
