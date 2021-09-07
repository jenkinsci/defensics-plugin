/*
 * Copyright © 2020-2021 Synopsys, Inc.
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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.io.ByteStreams;
import com.synopsys.defensics.apiserver.model.BaseSettings;
import com.synopsys.defensics.apiserver.model.HealthCheckResult;
import com.synopsys.defensics.apiserver.model.Item;
import com.synopsys.defensics.apiserver.model.ItemArray;
import com.synopsys.defensics.apiserver.model.Result;
import com.synopsys.defensics.apiserver.model.Run;
import com.synopsys.defensics.apiserver.model.RunTestConfiguration;
import com.synopsys.defensics.apiserver.model.Setting;
import com.synopsys.defensics.apiserver.model.SettingCliArgs;
import com.synopsys.defensics.apiserver.model.Suite;
import com.synopsys.defensics.apiserver.model.SuiteInstance;
import com.synopsys.defensics.apiserver.model.SuiteInstanceRequest;
import com.synopsys.defensics.apiserver.model.VersionInformation;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Request.Builder;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Defensics API v2 client. Client is configured to use token in all requests so new client
 * has to be created if token is to be changed. Server-provided pagination and other root-level
 * information is stripped away and methods return bare plain object and list of objects. If
 * pagination information is needed, it'd require new client interface so better to do after
 * JSON:API has been removed if needed.
 *
 * <p>Now most of exceptions (e.g. InterruptedExceptions, IOExceptions) are
 * collected as a cause in the DefensicsClientException but the exception handling is subject to
 * change when client code evolves.
 */
public class DefensicsApiV2Client implements DefensicsApiClient {
  private final OkHttpClient okHttpClient;
  private final HttpUrl apiBaseUrl;
  private final ObjectMapper objectMapper;

  /** Default content used in action requests. */
  private static final RequestBody ACTION_NO_CONTENT = RequestBody.create(
      "{}",
      MediaType.parse("application/json")
  );

  /**
   * Default constructor.
   *
   * @param apiBaseUri Defensics API Server address
   * @param token Token used to authenticate
   */
  public DefensicsApiV2Client(URI apiBaseUri, String token) {
    this(apiBaseUri, token, null);
  }

  /**
   * Constructor for configuring OkHttpClient further - allows to e.g. customize timeouts
   * or to modify TLS configuration.
   *
   * @param apiBaseUri Defensics API server address
   * @param token Token used to authenticate
   * @param clientConfigurator OkHttpClientBuilder consumer. Do not call builder.build() in this
   *        method.
   */
  public DefensicsApiV2Client(
      URI apiBaseUri,
      String token,
      Consumer<OkHttpClient.Builder> clientConfigurator) {
    objectMapper = new ObjectMapper();
    configureObjectMapper(objectMapper);

    // Don't fail if unknown fields are detected on JSON
    objectMapper.configure(
        DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
        false
    );
    final OkHttpClient.Builder okHttpBuilder = new OkHttpClient.Builder();

    // Increase default timeout since some operations like report generation can take
    // more than 10 seconds. If needed, timeout can be overridden in clientConfigurator.
    // Note that the readTimeout isn't the maximum allowed response time but maximum allowed
    // inactivity between packets when reading response (according to Baeldung post, didn't yet
    // find good official OkHttp reference)
    okHttpBuilder.readTimeout(60, TimeUnit.SECONDS);

    if (clientConfigurator != null) {
      clientConfigurator.accept(okHttpBuilder);
    }


    // Register each requests to contain given token in Authorization header
    this.setToken(token, okHttpBuilder);

    this.okHttpClient = okHttpBuilder.build();
    this.apiBaseUrl = HttpUrl.get(apiBaseUri);
  }

  @Override
  public void uploadTestPlan(String configurationId, InputStream testplanStream) {
    final HttpUrl url = apiBaseUrl.newBuilder()
        .addPathSegment("runs")
        .addPathSegment(configurationId)
        .addPathSegment("configuration")
        .addPathSegment("upload-plan")
        .build();
    try {
      final RequestBody body = RequestBody.create(
          ByteStreams.toByteArray(testplanStream),
          MediaType.parse("application/zip")
      );

      postNoResponse(url, body, "upload test plan");
    } catch (IOException e) {
      throw new DefensicsClientException("Could not read configuration file: " + e.getMessage(), e);
    }
  }

  @Override
  public void setTestConfigurationSettings(String runId, SettingCliArgs settings) {
    final HttpUrl uploadUrl = apiBaseUrl.newBuilder()
        .addPathSegment("runs")
        .addPathSegment(runId)
        .addPathSegment("configuration")
        .addPathSegment("arguments")
        .build();

    final String operation = "update test configuration";
    try {
      final RequestBody body = RequestBody.create(
          objectMapper.writeValueAsString(settings),
          MediaType.parse("application/json")
      );
      postNoResponse(uploadUrl, body, operation);
    } catch (IOException e) {
      throw new DefensicsClientException("Could not " + operation + ": " + e.getMessage(), e);
    }
  }

  @Override
  public List<Setting> setTestConfigurationSettings(String runId, BaseSettings settings) {
    final HttpUrl uploadUrl = apiBaseUrl.newBuilder()
        .addPathSegment("runs")
        .addPathSegment(runId)
        .addPathSegment("configuration")
        .addPathSegment("settings")
        .build();

    final String operation = "update test configuration";
    try {
      final RequestBody body = RequestBody.create(
          objectMapper.writeValueAsString(settings),
          MediaType.parse("application/json")
      );
      return postAndGetArrayResponse(
          uploadUrl,
          body,
          operation,
          new TypeReference<ItemArray<Setting>>(){}
      );
    } catch (IOException e) {
      throw new DefensicsClientException("Could not " + operation + ": " + e.getMessage(), e);
    }
  }

  @Override
  public List<Setting> getTestConfigurationSettings(String runId) {
    final HttpUrl url = apiBaseUrl.newBuilder()
        .addPathSegment("runs")
        .addPathSegment(runId)
        .addPathSegment("configuration")
        .addPathSegment("settings")
        .build();
    final String operation = "get test configuration";
    return getArrayItem(url, operation, new TypeReference<ItemArray<Setting>>() {});
  }

  @Override
  public Run createTestRun() {
    final HttpUrl url = apiBaseUrl.newBuilder()
        .addPathSegment("runs")
        .build();
    final RequestBody body = RequestBody.create(
        "{}",
        MediaType.parse("application/json")
    );
    return post(url, body, "create test run", new TypeReference<Item<Run>>(){});
  }

  @Override
  public Optional<RunTestConfiguration> getRunConfiguration(String configurationId) {
    final HttpUrl url = apiBaseUrl.newBuilder()
        .addPathSegment("runs")
        .addPathSegment(configurationId)
        .addPathSegment("configuration")
        .build();
    return getSingleItem(
        url,
        "get run configuration",
        new TypeReference<Item<RunTestConfiguration>>(){}
    );
  }

  @Override
  public Optional<Run> getRun(String runId) {
    // Match APIv1 client functionality. Include failure-summary by default.
    // If removed, check Jenkins plugin functionality
    return getRun(runId, "include=failure-summary");
  }

  public Optional<Run> getRun(String runId, String query) {
    final HttpUrl.Builder builder = apiBaseUrl.newBuilder()
        .addPathSegment("runs")
        .addPathSegment(runId);

    if (query != null) {
      builder.query(query);
    }

    final HttpUrl url = builder.build();
    return getSingleItem(url, "get run", new TypeReference<Item<Run>>(){});
  }

  @Override
  public List<Run> getRuns() {
    final HttpUrl url = apiBaseUrl.newBuilder()
        .addPathSegment("runs")
        .build();

    return getArrayItem(url, "get runs", new TypeReference<ItemArray<Run>>() {});
  }

  @Override
  public List<Run> getRuns(String filter, String sort, Long offset, Long limit) {
    final HttpUrl.Builder builder = apiBaseUrl.newBuilder();

    builder.addPathSegment("runs");

    if (filter != null) {
      builder.addQueryParameter("filter", filter);
    }
    if (sort != null) {
      builder.addQueryParameter("sort", sort);
    }
    if (offset != null) {
      builder.addQueryParameter("offset", offset.toString());
    }
    if (limit != null) {
      builder.addQueryParameter("limit", limit.toString());
    }

    final HttpUrl url = builder.build();

    return getArrayItem(url, "get runs", new TypeReference<ItemArray<Run>>() {});
  }

  @Override
  public List<Result> getResults() {
    return getResults("");
  }

  @Override
  public List<Result> getResults(String query) {
    final HttpUrl url = apiBaseUrl.newBuilder()
        .addPathSegment("results")
        .query(query)
        .build();

    return getArrayItem(url, "get results", new TypeReference<ItemArray<Result>>() { });
  }

  @Override
  public void deleteRun(String runId) {
    final HttpUrl uploadUrl = apiBaseUrl.newBuilder()
        .addPathSegment("runs")
        .addPathSegment(runId)
        .build();
    delete(uploadUrl, "delete test run");
  }

  @Override
  public void deleteRunPreserveSuite(String runId) {
    final HttpUrl uploadUrl = apiBaseUrl.newBuilder()
        .addPathSegment("runs")
        .addPathSegment(runId)
        .addQueryParameter("unload-suite", "false")
        .build();
    delete(uploadUrl, "delete test run (preserve suite)");
  }


  @Override
  public void deleteResult(String resultId) {
    final HttpUrl uploadUrl = apiBaseUrl.newBuilder()
        .addPathSegment("results")
        .addPathSegment(resultId)
        .build();
    delete(uploadUrl, "delete result");
  }


  @Override
  public boolean healthcheck() {
    return getHealthChecks().values().stream().allMatch(HealthCheckResult::isHealthy);
  }

  @Override
  public Map<String, HealthCheckResult> getHealthChecks() {
    final HttpUrl healthcheckUrl = apiBaseUrl.newBuilder()
        .addPathSegment("healthcheck")
        .build();

    Request request = new Builder()
        .url(healthcheckUrl)
        .build();

    final String baseErrorMessage = String.format(
        "Unable to connect Defensics server health check at address %s. "
            + "Please check you are using the correct token and Defensics API server is running",
        healthcheckUrl
    );

    final String content;
    try (Response response = okHttpClient.newCall(request).execute()) {
      if (response.code() >= 400 && response.code() != 500) {
        final String errorMessage = errorMessageForFailingJaxRsRequest(
            baseErrorMessage,
            response
        );
        throw new DefensicsClientException(errorMessage);
      }

      final ResponseBody body = response.body();
      if (body == null) {
        throw new DefensicsClientException(baseErrorMessage + ". Server response empty");
      }
      content = body.string();
    } catch (IOException e) {
      throw new DefensicsClientException(baseErrorMessage + ": " + e.getMessage(), e);
    }

    try {
      final Item<Map<String, HealthCheckResult>> healthChecks = objectMapper.readValue(
          content,
          new TypeReference<Item<Map<String, HealthCheckResult>>>() {}
      );
      return healthChecks.getData();
    } catch (JsonProcessingException e) {
      throw new DefensicsClientException(
          "Could not parse health check response: " + content, e
      );
    }
  }

  @Override
  public Optional<VersionInformation> getServerVersion() {
    final HttpUrl versionUrl = apiBaseUrl.newBuilder()
        .addPathSegment("version")
        .build();

    return getSingleItem(
        versionUrl,
        "get version information",
        new TypeReference<Item<VersionInformation>>() {}
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
    final Request request = new Builder().url(reportUri).build();

    try {
      final Response response = okHttpClient.newCall(request).execute();

      if (response.code() >= 400) {
        final String errorMessage = errorMessageForFailingJaxRsRequest(
            "Could not download results report",
            response
        );
        throw new DefensicsClientException(errorMessage);
      }

      return Optional.of(response)
          .map(Response::body)
          .map(ResponseBody::byteStream)
          .orElseThrow(
              () -> new DefensicsClientException("Could not download report. Response empty.")
          );
    } catch (IOException e) {
      throw new DefensicsClientException("Could not download result report: " + e.getMessage(), e);
    }
  }

  @Override
  public InputStream downloadResultPackage(String resultId) {
    HttpUrl.Builder uriBuilder = apiBaseUrl
        .newBuilder()
        .addPathSegment("results")
        .addPathSegment("result-package")
        .addQueryParameter("resultId", resultId);


    final HttpUrl resultPackageUri = uriBuilder.build();

    Request request = new Builder().url(resultPackageUri).build();

    try {
      final Response response = okHttpClient.newCall(request).execute();
      if (response.code() >= 400) {
        final String errorMessage = errorMessageForFailingJaxRsRequest(
            "Could not download result package.",
            response
        );
        throw new DefensicsClientException(errorMessage);
      }
      return Optional.of(response)
          .map(Response::body)
          .map(ResponseBody::byteStream)
          .orElseThrow(
              () -> new DefensicsClientException(
                  "Could not download result package. Response empty"
              )
          );
    } catch (IOException e) {
      throw new DefensicsClientException("Could not download result package: " + e.getMessage(), e);
    }
  }

  @Override
  public Optional<SuiteInstance> getRunSuiteInstance(String runId) {
    final HttpUrl url = apiBaseUrl.newBuilder()
        .addPathSegment("runs")
        .addPathSegment(runId)
        .addPathSegment("configuration")
        .addPathSegment("suite-instance")
        .build();

    return getSingleItem(
        url,
        "get suite for configuration",
        new TypeReference<Item<SuiteInstance>>() {}
    );
  }

  @Override
  public List<SuiteInstance> getSuiteInstances() {
    final HttpUrl url = apiBaseUrl.newBuilder()
        .addPathSegment("suite-instances")
        .build();

    return getArrayItem(
        url,
        "get suite instances",
        new TypeReference<ItemArray<SuiteInstance>>() {}
    );
  }

  @Override
  public Optional<SuiteInstance> getSuiteInstance(String suiteInstanceId) {
    final HttpUrl url = apiBaseUrl.newBuilder()
        .addPathSegment("suite-instances")
        .addPathSegment(suiteInstanceId)
        .build();

    return getSingleItem(url, "get suite instance", new TypeReference<Item<SuiteInstance>>() {});
  }

  @Override
  public void startRun(String runId) {
    final HttpUrl url = apiBaseUrl.newBuilder()
        .addPathSegment("runs")
        .addPathSegment(runId)
        .addPathSegment("start")
        .build();

    postAction(url, "start run");
  }

  @Override
  public void stopRun(String runId) {
    final HttpUrl url = apiBaseUrl.newBuilder()
        .addPathSegment("runs")
        .addPathSegment(runId)
        .addPathSegment("stop")
        .build();

    postAction(url, "stop run");
  }

  @Override
  public void pauseRun(String runId) {
    final HttpUrl url = apiBaseUrl.newBuilder()
        .addPathSegment("runs")
        .addPathSegment(runId)
        .addPathSegment("pause")
        .build();

    postAction(url, "pause run");
  }

  @Override
  public void resumeRun(String runId) {
    final HttpUrl url = apiBaseUrl.newBuilder()
        .addPathSegment("runs")
        .addPathSegment(runId)
        .addPathSegment("resume")
        .build();

    postAction(url, "stop run");
  }

  @Override
  public List<Suite> getSuites() {
    final HttpUrl url = apiBaseUrl.newBuilder()
        .addPathSegment("suites")
        .build();

    return getArrayItem(url, "get suites", new TypeReference<ItemArray<Suite>>() {});
  }

  @Override
  public Optional<Suite> getSuite(String suiteFeature, String suiteVersion) {
    final HttpUrl url = apiBaseUrl.newBuilder()
        .addPathSegment("suites")
        .addPathSegment(suiteFeature)
        .addPathSegment(suiteVersion)
        .build();
    return getSingleItem(url, "get suite", new TypeReference<Item<Suite>>() {});
  }

  @Override
  public SuiteInstance loadSuite(String suiteFeature) {
    final HttpUrl url = apiBaseUrl.newBuilder()
        .addPathSegment("suites")
        .addPathSegment(suiteFeature)
        .addPathSegment("load")
        .build();

    return post(
        url,
        ACTION_NO_CONTENT,
        "load suite with configuration",
        new TypeReference<Item<SuiteInstance>>() {}
    );
  }

  @Override
  public SuiteInstance loadSuite(String suiteFeature, String suiteVersion) {
    final HttpUrl url = apiBaseUrl.newBuilder()
        .addPathSegment("suites")
        .addPathSegment(suiteFeature)
        .addPathSegment(suiteVersion)
        .addPathSegment("load")
        .build();

    return post(
        url,
        ACTION_NO_CONTENT,
        "load suite with configuration",
        new TypeReference<Item<SuiteInstance>>() {}
    );
  }

  @Override
  public void unloadSuiteInstance(String suiteInstanceId) {
    final HttpUrl url = apiBaseUrl.newBuilder()
        .addPathSegment("suite-instances")
        .addPathSegment(suiteInstanceId)
        .build();

    delete(url, "unload suite");
  }

  @Override
  public void assignSuiteToRun(String suiteInstanceId, String runId) {
    final HttpUrl url = apiBaseUrl.newBuilder()
        .addPathSegment("runs")
        .addPathSegment(runId)
        .addPathSegment("configuration")
        .addPathSegment("assign-suite")
        .build();
    String operation = "assign suite to run";
    try {
      final SuiteInstanceRequest suiteInstanceRequest = new SuiteInstanceRequest(suiteInstanceId);

      final RequestBody suiteConfigRequestBody = RequestBody.create(
          objectMapper.writeValueAsString(suiteInstanceRequest),
          MediaType.parse("application/json")
      );

      postNoResponse(url, suiteConfigRequestBody, operation);
    } catch (IOException e) {
      throw new DefensicsClientException("Could not " + operation + ": e.getMessage()", e);
    }
  }

  @Override
  public Optional<Result> getResult(String resultId) {
    final HttpUrl.Builder builder = apiBaseUrl.newBuilder()
        .addPathSegment("results")
        .addPathSegment(resultId);

    final HttpUrl url = builder.build();
    return getSingleItem(url, "get result", new TypeReference<Item<Result>>(){});
  }

  private <T> Optional<T> getSingleItem(
      HttpUrl uploadUrl,
      String operationString,
      TypeReference<Item<T>> typeReference
  ) {
    final Request request = new Builder().url(uploadUrl).get().build();
    try (Response response = okHttpClient.newCall(request).execute()) {
      if (response.code() == 404) {
        return Optional.empty();
      }
      if (response.code() >= 400) {
        String message = errorMessageForFailingJaxRsRequest(
            "Could not " + operationString,
            response
        );

        throw new DefensicsClientException(message);
      }

      return Optional.of(response)
          .map(Response::body)
          .map(ResponseBody::byteStream)
          .map(stream -> {
            try {
              return objectMapper.readValue(stream, typeReference);
            } catch (IOException e) {
              throw new DefensicsClientException("Could not parse response: " + e.getMessage(), e);
            }
          })
          .map(Item::getData)
          .map(Optional::of)
          .orElseThrow(() -> new DefensicsClientException("Server response empty"));
    } catch (IOException e) {
      throw new DefensicsClientException(
          String.format("Could not %s: %s", operationString, e.getMessage()),
          e
      );
    }
  }

  private <T> List<T> getArrayItem(
      HttpUrl url,
      String operationString,
      TypeReference<ItemArray<T>> typeReference
  ) {
    final Request request = new Builder().url(url).get().build();
    try (final Response response = okHttpClient.newCall(request).execute()) {
      if (response.code() >= 400) {
        String message = errorMessageForFailingJaxRsRequest(
            "Could not " + operationString,
            response
        );

        throw new DefensicsClientException(message);
      }

      return Optional.of(response)
          .map(Response::body)
          .map(ResponseBody::byteStream)
          .map(stream -> {
            try {
              return objectMapper.readValue(stream, typeReference);
            } catch (IOException e) {
              throw new DefensicsClientException("Could not parse response: " + e.getMessage(), e);
            }
          })
          .map(ItemArray::getData)
          .orElseThrow(() -> new DefensicsClientException("Server response empty"));
    } catch (IOException e) {
      throw new DefensicsClientException(
          String.format("Could not %s: %s", operationString, e.getMessage()), e
      );
    }
  }

  private void postAction(HttpUrl startUrl, String operation) {
    post(startUrl, ACTION_NO_CONTENT, operation, null);
  }

  private <T> void postNoResponse(
      HttpUrl uploadUrl,
      RequestBody body,
      String operationString
  ) {
    post(uploadUrl, body, operationString, null);
  }

  private void delete(HttpUrl uploadUrl, String operation) {
    final Request request = new Builder().url(uploadUrl).delete().build();
    try (final Response response = okHttpClient.newCall(request).execute()) {
      if (response.code() >= 400) {
        String message = errorMessageForFailingJaxRsRequest(
            "Could not " + operation,
            response
        );

        throw new DefensicsClientException(message);
      }
    } catch (IOException e) {
      throw new DefensicsClientException("Could not " + operation + ": " + e.getMessage(), e);
    }
  }


  /**
   * Make HTTP POST request to given url with given RequestBody. Read response if
   * typeReference is non-null, otherwise null is returned.
   *
   * @param url URL
   * @param body Request body
   * @param operationString String used in eg. error conditions. In format "upload testplan"
   * @param typeReference Typereference where to de-serialize response. If null, response is not
   *     read and null is returned.
   * @param <T> Expected result type. Note that Item unwrapping is defined already.
   * @return De-serialized response or null
   */
  private <T> T post(
      HttpUrl url,
      RequestBody body,
      String operationString,
      TypeReference<Item<T>> typeReference
  ) {
    final Request request = new Builder()
        .url(url)
        .post(body)
        .build();
    try (final Response response = okHttpClient.newCall(request).execute()) {
      if (response.code() >= 400) {
        String message = errorMessageForFailingJaxRsRequest(
            "Could not " + operationString,
            response
        );

        throw new DefensicsClientException(message);
      }
      if (typeReference != null) {
        return Optional.of(response)
            .map(Response::body)
            .map(ResponseBody::byteStream)
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
    } catch (IOException e) {
      throw new DefensicsClientException("Could not " + operationString + ": " + e.getMessage(), e);
    }
  }

  /**
   * Make HTTP POST request to given url with given RequestBody. Read itemArray type of response.
   *
   * @param url URL
   * @param body Request body
   * @param operationString String used in eg. error conditions. In format "upload testplan"
   * @param typeReference Type reference where to de-serialize response.
   * @param <T> Expected result type. Note that Item unwrapping is defined already.
   * @return De-serialized response
   */
  private <T> List<T> postAndGetArrayResponse(
      HttpUrl url,
      RequestBody body,
      String operationString,
      TypeReference<ItemArray<T>> typeReference
  ) {
    final Request request = new Builder()
        .url(url)
        .post(body)
        .build();
    try (final Response response = okHttpClient.newCall(request).execute()) {
      if (response.code() >= 400) {
        String message = errorMessageForFailingJaxRsRequest(
            "Could not " + operationString,
            response
        );

        throw new DefensicsClientException(message);
      }
      if (response.code() == 202) {
        // means that there is not response yet.
        return null;
      }
      return Optional.of(response)
          .map(Response::body)
          .map(ResponseBody::byteStream)
          .map(stream -> {
            try {
              return objectMapper.readValue(stream, typeReference);
            } catch (IOException e) {
              throw new DefensicsClientException(
                  "Could not parse response: " + e.getMessage(), e
              );
            }
          })
          .map(ItemArray::getData)
          .orElseThrow(() -> new DefensicsClientException("Server response empty"));
    } catch (IOException e) {
      throw new DefensicsClientException("Could not " + operationString + ": " + e.getMessage(), e);
    }
  }

  /**
   * Makes all HTTP requests to contain given token in the Authorization header.
   * NOTE/TODO: Do not call twice. Use OkHttpClient authenticator instead?
   *
   * @param token Token to use
   */
  private void setToken(String token, OkHttpClient.Builder builder) {
    builder.addInterceptor(
        (Interceptor.Chain chain) -> {
          Request original = chain.request();
          Request newReq = original.newBuilder().addHeader(
              "Authorization", String.format("Bearer %s", token)
          ).method(original.method(), original.body())
              .build();
          return chain.proceed(newReq);
        }
    );
  }

  /**
   * Configures object mapper to use dashes in field names and Java 8 time format, etc.
   *
   * @param mapper ObjectMapper to configure
   */
  static void configureObjectMapper(ObjectMapper mapper) {
    mapper.setPropertyNamingStrategy(PropertyNamingStrategies.LOWER_CAMEL_CASE);
    mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    mapper.registerModule(new JavaTimeModule());
  }

  /**
   * Creates error message for failing requests done to JAX-RS endpoints. Returned error message
   * format:
   *
   * <code>Base message. HTTP status code: 123, message: error response body</code>
   *
   * <p>NOTE: Error message formatting and handling is unstable and subject to change in future
   * versions.
   *
   * @param baseMessage Generic message describing failure
   * @param response failing response
   * @return Formatted error message containing baseMessage, HTTP code and response body if
   *     available
   */
  private String errorMessageForFailingJaxRsRequest(String baseMessage, Response response) {

    final StringBuilder messageBuilder = new StringBuilder();
    messageBuilder.append(baseMessage);

    if (!baseMessage.trim().endsWith(".")) {
      messageBuilder.append(".");
    }

    messageBuilder.append(" HTTP status code: ").append(response.code());

    String errorBody = Optional.of(response)
        .map(Response::body)
        .map(e -> {
          try {
            return e.string();
          } catch (IOException ioException) {
            // Could not read response - return null message -> eventually mapped to ""
            return null;
          }
        }).orElse("");

    if (!errorBody.isEmpty()) {
      // Some responses have numeric status code, remove that from message
      // to prevent duplication.
      String statusCode = String.valueOf(response.code());
      if (errorBody.startsWith(statusCode)) {
        errorBody = errorBody.replaceFirst(statusCode + " ", "");
      }
      messageBuilder.append(", message: ").append(errorBody);
    }
    return messageBuilder.toString();
  }
}
