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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.defensics.apiserver.client.DefensicsApiClient.DefensicsClientException;
import com.defensics.apiserver.model.HealthCheckResult;
import com.defensics.apiserver.model.Item;
import com.defensics.apiserver.model.ItemArray;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Helper class for client requests.
 */
public class DefensicsApiClientConnect {
  /**
   * User-Agent used in requests.
   */
  private String userAgent = "Defensics-API-client";

  /**
   * Client for the requests.
   */
  private final HttpClient httpClient;

  /**
   * For de-serializing the responses.
   */
  private final ObjectMapper objectMapper;

  // Token used in requests. If null, authorization header is omitted.
  private final char[] token;

  /**
   * Constructor.
   *
   * @param httpClient Java HTTP client.
   * @param objectMapper Object mapper.
   * @param token Token to use in request. Can be null.
   */
  public DefensicsApiClientConnect(HttpClient httpClient, ObjectMapper objectMapper, String token) {
    this.httpClient = httpClient;
    this.objectMapper = objectMapper;
    this.token = token != null ? token.toCharArray() : null;
  }

  public void setUserAgent(String userAgentString) {
    this.userAgent = userAgentString;
  }

  /**
   * GET given URL and return response as a stream. Note: caller needs to close the stream.
   *
   * @param url             URL to GET
   * @param operationString Operation tried
   * @return Response as a stream
   */
  protected InputStream getInputStream(HttpUrl url, String operationString) {
    HttpRequest request = HttpRequest.newBuilder(url.getUri())
        .GET()
        .headers(getCommonHeaders())
        .build();
    try {
      HttpResponse<InputStream> response = httpClient.send(request, BodyHandlers.ofInputStream());
      if (response.statusCode() >= 400) {
        try (InputStream autoCloseResponse = response.body()) {
          String message = DefensicsApiClientUtility.errorMessageForFailingJaxRsRequest(
              "Could not " + operationString,
              response
          );
          throw new DefensicsClientException(message);
        }
      }
      return new BufferedInputStream(response.body());
    } catch (IOException | InterruptedException e) {
      throw new DefensicsClientException(
          String.format("Could not %s: %s", operationString, e.getMessage()), e
      );
    }
  }

  /**
   * Calls {@link #post(HttpUrl, RequestBody, String, TypeReference)}.
   *
   * @param startUrl the target url.
   * @param operation name of the operation.
   */
  protected void postAction(HttpUrl startUrl, String operation) {
    RequestBody body = new RequestBody(
        BodyPublishers.ofString("{}"),
        DefensicsApiV2Client.CONTENT_TYPE_JSON
    );
    post(startUrl, body, operation, null);
  }

  /**
   * Calls post(url, body, operationString, null).
   *
   * @param url the target url.
   * @param body Body to send.
   * @param operationString name of the operation.
   */
  protected void postNoResponse(
      HttpUrl url,
      RequestBody body,
      String operationString
  ) {
    post(url, body, operationString, null);
  }

  /**
   * Do a HTTP DELETE.
   *
   * @param url the target URL.
   * @param operation operation for logging.
   */
  protected void delete(HttpUrl url, String operation) {
    HttpRequest request = HttpRequest.newBuilder(url.getUri())
        .DELETE()
        .headers(getCommonHeaders())
        .build();
    try {
      HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());
      if (response.statusCode() >= 400) {
        String message = DefensicsApiClientUtility.errorMessageForFailingJaxRsRequest(
            "Could not " + operation,
            response
        );
        throw new DefensicsClientException(message);
      }
    } catch (IOException | InterruptedException e) {
      throw new DefensicsClientException("Could not " + operation + ": " + e.getMessage(), e);
    }
  }

  /**
   * Make HTTP POST request to given url with given RequestBody. Read response if typeReference is
   * non-null, otherwise null is returned.
   *
   * @param url             URL
   * @param body            Request body
   * @param operationString String used in e.g. error conditions. In format "upload testplan"
   * @param typeReference   Type reference where to de-serialize response. If null, response is not
   *                        read and null is returned.
   * @param <T>             Expected result type. Note that Item unwrapping is defined already.
   * @return De-serialized response or null
   */
  protected <T> T post(
      HttpUrl url,
      RequestBody body,
      String operationString,
      TypeReference<Item<T>> typeReference
  ) {
    HttpRequest request = HttpRequest.newBuilder(url.getUri())
        .POST(body.getBodyPublisher())
        .headers(getCommonHeaders())
        .header("Content-Type", body.getContentType())
        .build();
    try {
      HttpResponse<byte[]> response = httpClient.send(request, BodyHandlers.ofByteArray());
      if (response.statusCode() >= 400) {
        String message = DefensicsApiClientUtility.errorMessageForFailingJaxRsRequest(
            "Could not " + operationString,
            response
        );

        throw new DefensicsClientException(message);
      }
      if (typeReference != null) {
        return Optional.of(response)
            .map(HttpResponse::body)
            .map(stream -> {
              try {
                return objectMapper.readValue(stream, typeReference);
              } catch (IOException e) {
                throw new DefensicsClientException(
                    "Could not parse response: " + e.getMessage(), e
                );
              }
            })
            .map(Item::getData)
            .orElseThrow(() -> new DefensicsClientException("Server response empty"));
      } else {
        return null;
      }
    } catch (IOException | InterruptedException e) {
      throw new DefensicsClientException("Could not " + operationString + ": " + e.getMessage(), e);
    }
  }

  /**
   * Getter for array item.
   *
   * @param url             the target url.
   * @param operationString name of the operation for logging.
   * @param typeReference   type of the response.
   * @return List of objects.
   */
  protected <T> List<T> getArrayItem(
      HttpUrl url,
      String operationString,
      TypeReference<ItemArray<T>> typeReference
  ) {
    HttpRequest request = HttpRequest.newBuilder(url.getUri())
        .GET()
        .headers(getCommonHeaders())
        .build();
    try {
      HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());
      if (response.statusCode() >= 400) {
        String message = DefensicsApiClientUtility.errorMessageForFailingJaxRsRequest(
            "Could not " + operationString,
            response
        );

        throw new DefensicsClientException(message);
      }

      return Optional.of(response)
          .map(HttpResponse::body)
          .map(stream -> {
            try {
              return objectMapper.readValue(stream, typeReference);
            } catch (IOException e) {
              throw new DefensicsClientException("Could not parse response: " + e.getMessage(), e);
            }
          })
          .map(ItemArray::getData)
          .orElseThrow(() -> new DefensicsClientException("Server response empty"));
    } catch (IOException | InterruptedException e) {
      throw new DefensicsClientException(
          String.format("Could not %s: %s", operationString, e.getMessage()), e
      );
    }
  }

  /**
   * Get single JSON item.
   *
   * @param url             the target url.
   * @param operationString name of the operation for logging.
   * @param typeReference   type of the response.
   * @return Item wrapped in Optional.
   */
  protected <T> Optional<T> getSingleItem(
      HttpUrl url,
      String operationString,
      TypeReference<Item<T>> typeReference
  ) {
    HttpRequest request = HttpRequest.newBuilder(url.getUri())
        .GET()
        .headers(getCommonHeaders())
        .build();
    try {
      HttpResponse<byte[]> response = httpClient.send(request, BodyHandlers.ofByteArray());
      if (response.statusCode() == 404) {
        return Optional.empty();
      }
      if (response.statusCode() >= 400) {
        String message = DefensicsApiClientUtility.errorMessageForFailingJaxRsRequest(
            "Could not " + operationString,
            response
        );

        throw new DefensicsClientException(message);
      }

      return Optional.of(response)
          .map(HttpResponse::body)
          .map(body -> {
            try {
              return objectMapper.readValue(body, typeReference);
            } catch (IOException e) {
              throw new DefensicsClientException("Could not parse response: " + e.getMessage(), e);
            }
          })
          .map(Item::getData)
          .map(Optional::of)
          .orElseThrow(() -> new DefensicsClientException("Server response empty"));
    } catch (IOException | InterruptedException e) {
      throw new DefensicsClientException(
          String.format("Could not %s: %s", operationString, e.getMessage()),
          e
      );
    }
  }

  /**
   * Returns authorization header for the request. Later on, this can be changed to
   * getCommonHeaders() to return also e.g. User-Agent.
   *
   * @return Returns key-value pair for Authorization header if token is known, empty set if
   *     token is null.
   */
  private String[] getCommonHeaders() {
    ArrayList<String> headers = new ArrayList<>();
    if (userAgent != null) {
      headers.addAll(List.of("User-Agent", userAgent));
    }

    if (token != null) {
      headers.addAll(List.of("Authorization", "Bearer " + new String(token)));
    }
    return headers.toArray(new String[0]);
  }

  public Optional<Map<String, HealthCheckResult>> getHealthCheck(HttpUrl healthcheckUrl) {
    final String baseErrorMessage = String.format(
        "Unable to connect Defensics server health check at address %s. "
            + "Please check you are using the correct token and Defensics API server is running",
        healthcheckUrl.getUri()
    );

    HttpRequest request = HttpRequest.newBuilder(healthcheckUrl.getUri())
        .GET()
        .headers(getCommonHeaders())
        .build();
    try {
      HttpResponse<byte[]> response = httpClient.send(request, BodyHandlers.ofByteArray());
      if (response.statusCode() == 404) {
        return Optional.empty();
      }
      // Return the healthcheck response JSON on 500 instead of error message object.
      if (response.statusCode() >= 400 && response.statusCode() != 500) {
        String message = DefensicsApiClientUtility.errorMessageForFailingJaxRsRequest(
            baseErrorMessage,
            response
        );

        throw new DefensicsClientException(message);
      }

      return Optional.of(response)
          .map(HttpResponse::body)
          .map(body -> {
            try {
              return objectMapper.readValue(
                  body,
                  new TypeReference<Item<Map<String, HealthCheckResult>>>() {}
              );
            } catch (IOException e) {
              throw new DefensicsClientException("Could not parse response: " + e.getMessage(), e);
            }
          })
          .map(Item::getData)
          .map(Optional::of)
          .orElseThrow(() -> new DefensicsClientException(baseErrorMessage + ". Server response empty"));
    } catch (IOException | InterruptedException e) {
      throw new DefensicsClientException(
          baseErrorMessage + ": " + e.getMessage(),
          e
      );
    }
  }
}
