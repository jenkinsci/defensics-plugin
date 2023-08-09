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

package com.synopsys.defensics.apiserver.client;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;

/**
 * Stub HttpUrl to imitate OkHttp HttpUrl builder behaviour.
 * TODO: Eventually this should be replaced with proper UriBuilder like javax.ws.rs.core.UriBuilder,
 * but that'd require bringing external dependency.
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
   * Builder to construct new URL. Replace with 3rd implementation if there's some lightweight
   * version.
   */
  public static class Builder {
    private final URI originalUri;
    private String currentPath;
    private String currentQuery;

    /**
     * Constructor.
     *
     * @param uri Base URI to be augmented
     */
    public Builder(URI uri) {
      this.originalUri = uri;
      this.currentPath = originalUri.getPath();
      this.currentQuery = originalUri.getQuery();
    }

    /**
     * Adds query param to constructed URL.
     *
     * @param key Query param key
     * @param value Query param valu
     * @return builder
     */
    public Builder addQueryParameter(String key, String value) {
      if (currentQuery == null) {
        currentQuery = "";
      }
      if (!currentQuery.isEmpty()) {
        currentQuery += "&";
      }

      currentQuery += String.format(
          "%s=%s",
          URLEncoder.encode(key, UTF_8),
          URLEncoder.encode(value, UTF_8)
      );
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

      if (currentPath == null) {
        currentPath = "";
      }

      if (!currentPath.endsWith("/")) {
        currentPath = currentPath + "/";
      }
      currentPath = currentPath + URLEncoder.encode(segment, UTF_8);
      return this;
    }

    /**
     * Builds the HttpUrl with constructed URL.
     *
     * @return new HttpUrl.
     */
    public HttpUrl build() {
      try {
        StringBuilder sb = new StringBuilder();
        if (originalUri.getScheme() != null) {
          sb.append(originalUri.getScheme());
          sb.append("://");
        }
        if (originalUri.getAuthority() != null) {
          sb.append(originalUri.getAuthority());
        }
        if (currentPath != null) {
          sb.append(currentPath);
        }
        if (currentQuery != null) {
          sb.append("?").append(currentQuery);
        }
        URI finalUri = new URI(sb.toString());
        return new HttpUrl(finalUri);
      } catch (URISyntaxException e) {
        throw new IllegalArgumentException("Invalid URI: " + e.getMessage(), e);
      }
    }
  }
}
