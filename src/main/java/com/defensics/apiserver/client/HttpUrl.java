/*
 * Copyright © 2023 Synopsys, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.defensics.apiserver.client;

import java.net.URI;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriBuilder;

/**
 * Stub HttpUrl to match OkHttp HttpUrl builder behaviour. This uses Spring's UriBuilder
 * as that's provided in the Jenkins core libraries. Other option would be
 * javax.ws.rs.core.UriBuilder but that'd add a new dependency.
 */
public class HttpUrl {
  private final URI uri;

  public HttpUrl(URI apiBaseUri) {
    this.uri = apiBaseUri;
  }

  public static HttpUrl get(URI uri) {
    return new HttpUrl(uri);
  }

  public Builder newBuilder() {
    return new Builder(this.uri);
  }

  public URI getUri() {
    return uri;
  }

  /**
   * Builder to construct new HttpUrl.
   */
  public static class Builder {
    private final UriBuilder builder;

    /**
     * Constructor.
     *
     * @param uri Base URI to be augmented
     */
    public Builder(URI uri) {
      builder = new DefaultUriBuilderFactory(uri.toString()).builder();
    }

    /**
     * Adds query param to constructed URL.
     *
     * @param key Query param key
     * @param value Query param valu
     * @return builder
     */
    public Builder addQueryParameter(String key, String value) {
      builder.queryParam(key, value);
      return this;
    }

    /**
     * Adds path segment to constructed URL. NOTE: This method doesn't allow slashes or double
     * periods to prevent path traversal. Adjust and/or add a method to add multiple
     * path segments at once if those are needed.
     *
     * @param segment URL segment to add.
     * @return builder
     */
    public Builder addPathSegment(String segment) {
      if (segment.contains("/")) {
        throw new IllegalArgumentException("Segment cannot contain slashes");
      }

      if (segment.contains("..")) {
        throw new IllegalArgumentException("Segment cannot contain '..'");
      }
      builder.pathSegment(segment);
      return this;
    }

    /**
     * Builds the HttpUrl with constructed URL.
     *
     * @return new HttpUrl.
     */
    public HttpUrl build() {
      return new HttpUrl(builder.build());
    }
  }
}
