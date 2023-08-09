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

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

/**
 * Utilities for the Defensics client.
 */
public class DefensicsApiClientUtility {
  /**
   * Creates error message for failing requests done to JAX-RS endpoints. Returned error message
   * format:
   * <code>Base message. HTTP status code: 123, message: error response body</code>
   *
   * <p>NOTE: Error message formatting and handling is unstable and subject to change in future
   * versions.
   *
   * @param baseMessage Generic message describing failure
   * @param response    failing response
   * @return Formatted error message containing baseMessage, HTTP code and response body if
   *         available
   */
  protected static String errorMessageForFailingJaxRsRequest(
      String baseMessage, HttpResponse<?> response) {

    final StringBuilder messageBuilder = new StringBuilder();
    messageBuilder.append(baseMessage);

    if (!baseMessage.trim().endsWith(".")) {
      messageBuilder.append(".");
    }

    messageBuilder.append(" HTTP status code: ").append(response.statusCode());

    String contentType = response.headers().firstValue("Content-Type").orElse(null);
    final List<String> textualContentTypes = List.of(
        DefensicsApiV2Client.CONTENT_TYPE_JSON,
        DefensicsApiV2Client.CONTENT_TEXT_PLAIN
    );

    String errorBody = Optional.of(response)
        .map(resp -> {
          Object body = resp.body();
          if (body != null && contentType != null && textualContentTypes.contains(contentType)) {
            if (body instanceof String) {
              return (String) body;
            } else if (body instanceof byte[]) {
              return new String((byte[]) body, StandardCharsets.UTF_8);
            } else if (body instanceof InputStream) {
              try (InputStream bodyStream = (InputStream) body) {
                return new String(bodyStream.readAllBytes(), StandardCharsets.UTF_8);
              } catch (IOException e) {
                return "Could not read response";
              }
            }
          }
          return "";
        })
        .orElse("");

    if (!errorBody.isEmpty()) {
      // Some responses have numeric status code, remove that from message
      // to prevent duplication.
      String statusCode = String.valueOf(response.statusCode());
      if (errorBody.startsWith(statusCode)) {
        errorBody = errorBody.replaceFirst(statusCode + " ", "");
      }
      messageBuilder.append(", message: ").append(errorBody);
    }
    return messageBuilder.toString();
  }

  /**
   * Configures object mapper to use dashes in field names and Java 8 time format, etc.
   *
   * @param mapper ObjectMapper to configure
   */
  protected static ObjectMapper createObjectMapper() {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.LOWER_CAMEL_CASE);
    objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    objectMapper.registerModule(new JavaTimeModule());
    // Don't fail if unknown fields are detected on JSON
    objectMapper.configure(
        DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
        false
    );
    return objectMapper;
  }
}
