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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.defensics.apiserver.model.HealthCheckResult;
import com.defensics.apiserver.model.Item;
import com.defensics.apiserver.model.Run;
import com.defensics.apiserver.model.SettingCliArgs;
import com.defensics.apiserver.model.Suite;
import com.defensics.apiserver.model.SuiteInstance;
import com.defensics.apiserver.model.VersionInformation;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest.BodyPublishers;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Defensics API v2 client. Client is configured to use token in all requests so new client has to
 * be created if token is to be changed. Server-provided pagination and other root-level information
 * is stripped away and methods return bare plain object and list of objects.
 *
 * <p>Now most of the exceptions (e.g. InterruptedExceptions, IOExceptions) are
 * collected as a cause in the DefensicsClientException but the exception handling is subject to
 * change when client code evolves.
 */
public class DefensicsApiV2Client implements DefensicsApiClient {

  /**
   * API path segment for runs.
   */
  public static final String RUNS = "runs";

  /**
   * API path segment for suite instances.
   */
  public static final String SUITE_INSTANCES = "suite-instances";

  public static final String CONTENT_TYPE_ZIP = "application/zip";
  public static final String CONTENT_TYPE_JSON = "application/json";
  public static final String CONTENT_TEXT_PLAIN = "text/plain";

  private final HttpUrl apiBaseUrl;
  private final ObjectMapper objectMapper = DefensicsApiClientUtility.createObjectMapper();
  private final DefensicsApiClientConnect defensicsApiClientConnect;

  /**
   * Default constructor.
   *
   * @param apiBaseUri Defensics API Server address
   * @param token      Token used to authenticate
   */
  public DefensicsApiV2Client(URI apiBaseUri, String token) {
    this(apiBaseUri, token, null);
  }

  /**
   * Constructor for configuring HttpClient further - allows to e.g. customize timeouts or to
   * modify TLS configuration.
   *
   * @param apiBaseUri         Defensics API server address
   * @param token              Token used to authenticate
   * @param clientConfigurator HttpClientBuilder consumer. Do not call builder.build() in this
   *                           method.
   */
  public DefensicsApiV2Client(
      URI apiBaseUri,
      String token,
      Consumer<HttpClient.Builder> clientConfigurator) {

    // Don't fail if unknown fields are detected on JSON
    objectMapper.configure(
        DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
        false
    );
    HttpClient.Builder httpClientBuilder = HttpClient.newBuilder();

    if (clientConfigurator != null) {
      clientConfigurator.accept(httpClientBuilder);
    }

    HttpClient httpClient = httpClientBuilder.build();

    this.apiBaseUrl = HttpUrl.get(apiBaseUri);
    this.defensicsApiClientConnect = new DefensicsApiClientConnect(httpClient, objectMapper, token);
  }

  @Override
  public void uploadTestPlan(
      String configurationId, InputStream testplanStream) {
    HttpUrl.Builder builder = apiBaseUrl.newBuilder()
        .addPathSegment(RUNS)
        .addPathSegment(configurationId)
        .addPathSegment("configuration")
        .addPathSegment("upload-plan");

    HttpUrl url = builder.build();
    try {
      final RequestBody body = RequestBody.create(
          BodyPublishers.ofByteArray(testplanStream.readAllBytes()),
          CONTENT_TYPE_ZIP
      );

      defensicsApiClientConnect.postNoResponse(url, body, "upload test plan");
    } catch (IOException e) {
      throw new DefensicsClientException(
          "Could not read configuration file: " + e.getMessage(), e
      );
    }
  }

  @Override
  public void setTestConfigurationSettings(String runId, SettingCliArgs settings) {
    final HttpUrl uploadUrl = apiBaseUrl.newBuilder()
        .addPathSegment(RUNS)
        .addPathSegment(runId)
        .addPathSegment("configuration")
        .addPathSegment("arguments")
        .build();

    final String operation = "update test configuration";
    try {
      final RequestBody body = jsonBody(settings);
      defensicsApiClientConnect.postNoResponse(uploadUrl, body, operation);
    } catch (IOException e) {
      throw new DefensicsClientException("Could not " + operation + ": " + e.getMessage(), e);
    }
  }

  @Override
  public Run createTestRun() {
    final HttpUrl url = apiBaseUrl.newBuilder()
        .addPathSegment(RUNS)
        .build();
    final RequestBody emptyJsonBody =
        RequestBody.create(BodyPublishers.ofString("{}"), CONTENT_TYPE_JSON);
    return defensicsApiClientConnect.post(
        url,
        emptyJsonBody,
        "create test run",
        new TypeReference<>() {
        }
    );
  }

  @Override
  public Optional<Run> getRun(String runId) {
    // Match APIv1 client functionality. Include failure-summary by default.
    // If removed, check Jenkins plugin functionality
    return getRun(runId, true);
  }

  /**
   * Get run with optional failureSummary embedded.
   *
   * @param runId Run ID
   * @param includeFailureSummary If true, includes failureSummary information.
   * @return Run, if present.
   */
  public Optional<Run> getRun(String runId, boolean includeFailureSummary) {
    final HttpUrl.Builder builder = apiBaseUrl.newBuilder()
        .addPathSegment(RUNS)
        .addPathSegment(runId);

    if (includeFailureSummary) {
      builder.addQueryParameter("include", "failure-summary");
    }

    final HttpUrl url = builder.build();
    return getSingleItem(url, "get run", new TypeReference<>() {
    });
  }

  @Override
  public void deleteRun(String runId) {
    final HttpUrl uploadUrl = apiBaseUrl.newBuilder()
        .addPathSegment(RUNS)
        .addPathSegment(runId)
        .build();
    defensicsApiClientConnect.delete(uploadUrl, "delete test run");
  }

  @Override
  public Map<String, HealthCheckResult> getHealthChecks() {
    final HttpUrl healthcheckUrl = apiBaseUrl.newBuilder()
        .addPathSegment("healthcheck")
        .build();

      return defensicsApiClientConnect.getHealthCheck(healthcheckUrl).orElseThrow();
  }

  @Override
  public Optional<VersionInformation> getServerVersion() {
    final HttpUrl versionUrl = apiBaseUrl.newBuilder()
        .addPathSegment("version")
        .build();

    return getSingleItem(
        versionUrl,
        "get version information",
        new TypeReference<>() {
        }
    );
  }

  @Override
  public InputStream downloadReport(
      String resultId,
      String reportType
  ) {
    HttpUrl.Builder uriBuilder = apiBaseUrl.newBuilder()
        .addPathSegment("results")
        .addPathSegment("report")
        .addQueryParameter("resultId", resultId)
        .addQueryParameter("format", reportType);

    final HttpUrl reportUri = uriBuilder.build();
    return defensicsApiClientConnect.getInputStream(reportUri, "download results report");
  }

  @Override
  public InputStream downloadResultPackage(String resultId) {
    HttpUrl.Builder uriBuilder = apiBaseUrl
        .newBuilder()
        .addPathSegment("results")
        .addPathSegment("result-package")
        .addQueryParameter("resultId", resultId);

    return defensicsApiClientConnect.getInputStream(uriBuilder.build(), "download result package");
  }

  @Override
  public Optional<SuiteInstance> getRunSuiteInstance(String runId) {
    final HttpUrl url = apiBaseUrl.newBuilder()
        .addPathSegment(RUNS)
        .addPathSegment(runId)
        .addPathSegment("configuration")
        .addPathSegment("suite-instance")
        .build();

    return getSingleItem(
        url,
        "get suite for configuration",
        new TypeReference<>() {
        }
    );
  }

  @Override
  public List<SuiteInstance> getSuiteInstances() {
    final HttpUrl url = apiBaseUrl.newBuilder()
        .addPathSegment(SUITE_INSTANCES)
        .build();

    return defensicsApiClientConnect.getArrayItem(
        url,
        "get suite instances",
        new TypeReference<>() {
        }
    );
  }

  @Override
  public void startRun(String runId) {
    final HttpUrl url = apiBaseUrl.newBuilder()
        .addPathSegment(RUNS)
        .addPathSegment(runId)
        .addPathSegment("start")
        .build();

    defensicsApiClientConnect.postAction(url, "start run");
  }

  @Override
  public void stopRun(String runId) {
    final HttpUrl url = apiBaseUrl.newBuilder()
        .addPathSegment(RUNS)
        .addPathSegment(runId)
        .addPathSegment("stop")
        .build();

    defensicsApiClientConnect.postAction(url, "stop run");
  }

  @Override
  public Optional<Suite> getSuite(String suiteFeature, String suiteVersion) {
    final HttpUrl url = apiBaseUrl.newBuilder()
        .addPathSegment("suites")
        .addPathSegment(suiteFeature)
        .addPathSegment(suiteVersion)
        .build();
    return getSingleItem(url, "get suite", new TypeReference<>() {
    });
  }

  private <T> Optional<T> getSingleItem(
      HttpUrl url,
      String operationString,
      TypeReference<Item<T>> typeReference
  ) {
    return defensicsApiClientConnect.getSingleItem(url, operationString, typeReference);
  }

  /**
   * Helper method to create JSON RequestBody object.
   *
   * @param object Object to serialize to JSON.
   * @return RequestBody
   * @throws JsonProcessingException if object could not be serialized.
   */
  private RequestBody jsonBody(Object object) throws JsonProcessingException {
    return RequestBody.create(
        BodyPublishers.ofString(objectMapper.writeValueAsString(object)),
        CONTENT_TYPE_JSON
    );
  }

  public void setUserAgent(String userAgentString) {
    defensicsApiClientConnect.setUserAgent(userAgentString);
  }
}
