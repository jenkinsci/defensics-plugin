/*
 * Copyright 2024 Black Duck Software, Inc. All rights reserved.
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

import java.net.http.HttpRequest;

/**
 * A helper class to contain information about the request body - content-type and BodyPublisher
 * to get the actual content. Now the methods match the OkHttp's same-named class but can be changed
 * if needed.
 */
public class RequestBody {
  private final HttpRequest.BodyPublisher bodyPublisher;
  private final String contentType;

  public RequestBody(HttpRequest.BodyPublisher bodyPublisher, String contentType) {
    this.bodyPublisher = bodyPublisher;
    this.contentType = contentType;
  }

  public static RequestBody create(HttpRequest.BodyPublisher bodyPublisher, String contentType) {
    return new RequestBody(bodyPublisher, contentType);
  }

  public HttpRequest.BodyPublisher getBodyPublisher() {
    return bodyPublisher;
  }

  public String getContentType() {
    return contentType;
  }
}
