/*
 * Copyright © 2020 Synopsys, Inc.
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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.io.ByteStreams;
import com.synopsys.defensics.apiserver.model.HealthCheckResult;
import com.synopsys.defensics.apiserver.model.Run;
import com.synopsys.defensics.apiserver.model.RunTestConfiguration;
import com.synopsys.defensics.apiserver.model.SettingCliArgs;
import com.synopsys.defensics.apiserver.model.SuiteInstance;
import io.crnk.client.CrnkClient;
import io.crnk.client.TransportException;
import io.crnk.client.http.okhttp.OkHttpAdapter;
import io.crnk.core.engine.document.ErrorData;
import io.crnk.core.exception.CrnkException;
import io.crnk.core.exception.CrnkMappableException;
import io.crnk.core.queryspec.PathSpec;
import io.crnk.core.queryspec.QuerySpec;
import io.crnk.core.repository.OneRelationshipRepository;
import io.crnk.core.repository.ResourceRepository;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Collections;
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
 * Defensics API JSON:API Client, uses crnk JSON:API clients most of the request, and OkHttpClient
 * for non-JSON:API requests. Client is configured to use token in all requests so new client has
 * to be created if token is to be changed.
 *
 * <p>Now most of exceptions (e.g. CrnkExceptions, InterruptedExceptions, IOExceptions) are
 * collected as a cause in the DefensicsClientException but the exception handling is subject to
 * change when client code evolves.
 */
public class DefensicsJsonApiClient implements DefensicsApiClient {
  private final CrnkClient crnkClient;
  private final ResourceRepository<RunTestConfiguration, String> testConfigurationRepository;
  private final ResourceRepository<Run, String> runRepository;
  private final ResourceRepository<SuiteInstance, String> suiteInstanceRepository;
  private final OneRelationshipRepository<RunTestConfiguration, String, SuiteInstance, String>
      runConfigurationSuiteRepository;
  private final OkHttpClient okHttpClient;
  private final HttpUrl apiBaseUrl;

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
  public DefensicsJsonApiClient(URI apiBaseUri, String token) {
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
  public DefensicsJsonApiClient(
      URI apiBaseUri,
      String token,
      Consumer<OkHttpClient.Builder> clientConfigurator) {
    final CrnkClient crnkClient = new CrnkClient(apiBaseUri.toString());
    configureObjectMapper(crnkClient.getObjectMapper());

    // Makes client handle cases where new values have been added to enum on server side
    crnkClient.getObjectMapper().configure(
        DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE,
        true
    );

    // Don't fail if unknown fields are detected on JSON
    crnkClient.getObjectMapper().configure(
        DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
        false
    );

    // Use OkHttpClient for both JSON:API and plain HTTP requests
    final OkHttpAdapter httpAdapter;
    if (clientConfigurator == null) {
      httpAdapter = new OkHttpAdapter();
    } else {
      // Allow further configuration of OkHttp client (to e.g. pass custom TLS configuration)
      httpAdapter = new OkHttpAdapter() {
        private OkHttpClient impl = null;
        @Override
        public OkHttpClient getImplementation() {
          // Configure crnk's own client further by spinning new client and its configurator.
          // Check this implementations against crnk's own implementation every now and then
          // if there's cleaner way for this.
          //
          // TODO: Not thread-safe, but likely not big problem since all clients should behave
          // the same.
          if (impl == null) {
            final OkHttpClient implementation = super.getImplementation();
            final OkHttpClient.Builder builder = implementation.newBuilder();
            clientConfigurator.accept(builder);
            impl = builder.build();
          }
          return impl;
        }
      };
    }

    crnkClient.setHttpAdapter(httpAdapter);
    crnkClient.getHttpAdapter().setReceiveTimeout(60, TimeUnit.SECONDS);
    // Register each requests to contain given token in Authorization header
    this.setToken(token, httpAdapter);

    this.okHttpClient = httpAdapter.getImplementation();
    this.crnkClient = crnkClient;
    this.apiBaseUrl = HttpUrl.get(apiBaseUri);

    this.testConfigurationRepository = this.crnkClient
        .getRepositoryForType(RunTestConfiguration.class);
    this.runRepository = this.crnkClient
        .getRepositoryForType(Run.class);
    this.suiteInstanceRepository = this.crnkClient.getRepositoryForType(SuiteInstance.class);

    this.runConfigurationSuiteRepository = this.crnkClient.getOneRepositoryForType(
        RunTestConfiguration.class, SuiteInstance.class
    );
  }

  @Override
  public void uploadTestPlan(String configurationId, InputStream testplanStream) {

    final HttpUrl uploadUrl = apiBaseUrl.newBuilder()
        .addPathSegments("runs")
        .addPathSegment(configurationId)
        .addPathSegments("configuration")
        .addPathSegment("upload-plan")
        .build();
    final Request request;
    try {
      request = new Builder()
          .url(uploadUrl)
          .post(
              RequestBody.create(
                  ByteStreams.toByteArray(testplanStream),
                  MediaType.parse("application/zip")
              )
          )
          .build();
    } catch (IOException e) {
      throw new DefensicsClientException("Could not read configuration file: " + e.getMessage(), e);
    }
    try {
      final Response response = okHttpClient.newCall(request).execute();

      if (response.code() >= 400) {
        final String errorMessage = errorMessageForFailingJaxRsRequest(
            "Could not upload test configuration",
            response
        );

        throw new DefensicsClientException(errorMessage);
      }
    } catch (IOException e) {
      throw new DefensicsClientException(
          "Could not upload test configuration: " + e.getMessage(),
          e
      );
    }
  }

  @Override
  public void setTestConfigurationSettings(String runId, SettingCliArgs settings) {
    final HttpUrl uploadUrl = apiBaseUrl.newBuilder()
         .addPathSegments("runs")
         .addPathSegment(runId)
         .addPathSegments("configuration")
         .addPathSegment("arguments")
         .build();
    try {
      final ObjectMapper om = crnkClient.getObjectMapper();
      final Request request = new Builder()
           .url(uploadUrl)
           .post(
               RequestBody.create(
                   om.writeValueAsString(settings),
                   MediaType.parse("application/json")
               )
           )
           .build();
      final Response response = okHttpClient.newCall(request).execute();
      if (response.code() >= 400) {
        String message = errorMessageForFailingJaxRsRequest(
            "Could not update test configuration.",
            response
        );

        throw new DefensicsClientException(message);
      }
    } catch (IOException e) {
      throw new DefensicsClientException(
          "Could not update test configuration: " + e.getMessage(),
          e
      );
    }
  }

  @Override
  public Run createTestRun() {
    try {
      return runRepository.create(new Run());
    } catch (TransportException | CrnkException e) {
      throw new DefensicsClientException(
          errorMessageForFailingCrnkRequest("Could not create run", e),
          e
      );
    }
  }

  @Override
  public Optional<RunTestConfiguration> getRunConfiguration(String configurationId) {
    try {
      return Optional.ofNullable(
          testConfigurationRepository.findOne(configurationId,
            new QuerySpec(RunTestConfiguration.class))
      );
    } catch (TransportException | CrnkException e) {
      throw new DefensicsClientException(
          errorMessageForFailingCrnkRequest(
              "Could not get run " + configurationId + " configuration",
              e
          ),
          e
      );
    }
  }

  @Override
  public Optional<Run> getRun(String runId) {
    QuerySpec querySpec = new QuerySpec(Run.class);
    querySpec.includeRelation(PathSpec.of("failureSummary"));
    try {
      return Optional.ofNullable(
          runRepository.findOne(runId, querySpec)
      );
    } catch (TransportException | CrnkException e) {
      throw new DefensicsClientException(
          errorMessageForFailingCrnkRequest("Could not get run " + runId, e),
          e
      );
    }
  }

  @Override
  public void deleteRun(String runId) {
    try {
      runRepository.delete(runId);
    } catch (TransportException | CrnkException e) {
      throw new DefensicsClientException(
          errorMessageForFailingCrnkRequest("Could not delete run", e),
          e
      );
    }
  }

  @Override
  public boolean healthcheck() {
    final HttpUrl healthcheckUrl = apiBaseUrl.newBuilder()
        .addPathSegments("healthcheck")
        .build();

    Request request = new Builder()
        .url(healthcheckUrl)
        .build();
    final String baseErrorMessage = String.format(
        "Unable to connect Defensics server healthcheck at address %s. "
            + "Please check you are using the correct token and Defensics API server is running",
        healthcheckUrl
    );

    try (Response response = okHttpClient.newCall(request).execute()) {
      // NOTE: Some of the status codes could be mapped to unhealthy status but that'll require
      // upcoming more detailed healthcheck model so use now exception with error message.
      if (response.code() >= 400) {
        final String errorMessage = errorMessageForFailingJaxRsRequest(
            baseErrorMessage,
            response
        );
        throw new DefensicsClientException(errorMessage);
      }
      final ObjectMapper objectMapper = crnkClient.getObjectMapper();
      // Using optional/mapping since spotbugs issues NPE warning easily otherwise
      try (final InputStream contentStream = Optional.of(response)
          .map(Response::body)
          .map(ResponseBody::byteStream)
          .orElseThrow(
              () -> new DefensicsClientException(baseErrorMessage + ". Server response empty")
          )
      ) {
        final Map<String, HealthCheckResult> healthChecks = objectMapper.readValue(
            contentStream,
            new TypeReference<Map<String, HealthCheckResult>>() {}
        );

        if (healthChecks == null) {
          throw new DefensicsClientException(baseErrorMessage + ". Server response empty");
        }
        return healthChecks.values().stream().allMatch(HealthCheckResult::isHealthy);
      }
    } catch (IOException e) {
      throw new DefensicsClientException(baseErrorMessage + ": " + e.getMessage(), e);
    }
  }

  @Override
  public InputStream downloadReport(
      String runId,
      String reportType
  ) {
    HttpUrl.Builder uriBuilder = apiBaseUrl.newBuilder()
        .addPathSegment("runs")
        .addPathSegment(runId)
        .addPathSegment("report")
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
  public InputStream downloadResultPackage(String runId) {
    HttpUrl.Builder uriBuilder = apiBaseUrl
        .newBuilder()
        .addPathSegment("runs")
        .addPathSegment(runId)
        .addPathSegment("package");

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
  public Optional<SuiteInstance> getConfigurationSuite(String configurationId) {
    Map<String, SuiteInstance> result = runConfigurationSuiteRepository.findOneRelations(
        Collections.singleton(configurationId),
        "suite-instance",
        new QuerySpec(SuiteInstance.class)
    );
    try {
      return Optional.ofNullable(result.get(configurationId));
    } catch (TransportException | CrnkException e) {
      throw new DefensicsClientException(
          errorMessageForFailingCrnkRequest(
              "Could not get suite-instance for configuration " + configurationId,
              e
          ),
          e
      );
    }
  }

  @Override
  public Optional<SuiteInstance> getSuiteInstance(String suiteInstanceId) {
    try {
      return Optional.ofNullable(
          suiteInstanceRepository.findOne(suiteInstanceId, new QuerySpec(SuiteInstance.class))
      );
    } catch (TransportException | CrnkException e) {
      throw new DefensicsClientException(
          errorMessageForFailingCrnkRequest("Could not get suite instance " + suiteInstanceId, e),
          e
      );
    }
  }

  @Override
  public void startRun(String runId) {
    final HttpUrl startUrl = apiBaseUrl.newBuilder()
        .addPathSegments("runs")
        .addPathSegment(runId)
        .addPathSegment("start")
        .build();

    Request request = new Builder()
        .url(startUrl)
        .post(ACTION_NO_CONTENT).build();

    try (Response response = okHttpClient.newCall(request).execute()) {
      if (response.code() >= 400) {
        final String errorMessage = errorMessageForFailingJaxRsRequest(
            "Could not start run",
            response
        );
        throw new DefensicsClientException(errorMessage);
      }
    } catch (IOException e) {
      throw new DefensicsClientException("Could not start run: " + e.getMessage(), e);
    }
  }

  /**
   * Stop run.
   * @param runId Run ID
   */
  public void stopRun(String runId) {
    final HttpUrl stopUrl = apiBaseUrl.newBuilder()
        .addPathSegments("runs")
        .addPathSegment(runId)
        .addPathSegment("stop")
        .build();

    Request request = new Builder()
        .url(stopUrl)
        .post(ACTION_NO_CONTENT).build();

    try (Response response = okHttpClient.newCall(request).execute()) {
      if (response.code() >= 400) {
        final String errorMessage = errorMessageForFailingJaxRsRequest(
            "Could not stop run",
            response
        );
        throw new DefensicsClientException(errorMessage);
      }
    } catch (IOException e) {
      throw new DefensicsClientException("Could not stop run", e);
    }
  }

  /**
   * Pause run.
   * @param runId Run ID which is to be paused.
   */
  public void pauseRun(String runId) {
    final HttpUrl stopUrl = apiBaseUrl.newBuilder()
        .addPathSegments("runs")
        .addPathSegment(runId)
        .addPathSegment("pause")
        .build();

    Request request = new Builder()
        .url(stopUrl)
        .post(ACTION_NO_CONTENT).build();

    try (Response response = okHttpClient.newCall(request).execute()) {
      if (response.code() >= 400) {
        final String errorMessage = errorMessageForFailingJaxRsRequest(
            "Could not pause run",
             response
        );
        throw new DefensicsClientException(errorMessage);
      }
    } catch (IOException e) {
      throw new DefensicsClientException("Could not pause run", e);
    }
  }

  /**
   * Resume paused run.
   * @param runId Run ID which is to be resumed.
   */
  public void resumeRun(String runId) {
    final HttpUrl stopUrl = apiBaseUrl.newBuilder()
        .addPathSegments("runs")
        .addPathSegment(runId)
        .addPathSegment("resume")
        .build();

    Request request = new Builder()
        .url(stopUrl)
        .post(ACTION_NO_CONTENT).build();

    try (Response response = okHttpClient.newCall(request).execute()) {
      if (response.code() >= 400) {
        final String errorMessage = errorMessageForFailingJaxRsRequest(
            "Could not resume run",
            response
        );
        throw new DefensicsClientException(errorMessage);
      }
    } catch (IOException e) {
      throw new DefensicsClientException("Could not resume run", e);
    }
  }


  /**
   * Makes all HTTP requests to contain given token in the Authorization header.
   * NOTE/TODO: Do not call twice. Use OkHttpClient authenticator instead?
   *
   * @param token Token to use
   * @param httpAdapter OkHttpAdapter where to set the listener
   */
  private void setToken(String token, OkHttpAdapter httpAdapter) {
    httpAdapter.addListener(
        (OkHttpClient.Builder builder) -> {
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
    );
  }

  /**
   * Configures object mapper to use dashes in field names and Java 8 time format, etc.
   *
   * @param mapper ObjectMapper to configure
   */
  static void configureObjectMapper(ObjectMapper mapper) {
    mapper.setPropertyNamingStrategy(PropertyNamingStrategy.KEBAB_CASE);
    mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    mapper.registerModule(new JavaTimeModule());
  }

  public CrnkClient getCrnkClient() {
    return crnkClient;
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

  /**
   * Creates error message for failing requests done to JSON:API endpoints. Returned error message
   * format:
   *
   * <code>
   *   Base message. HTTP status code: 123[, message: crnk_error_title - crnk_error_details]
   *
   * [ ] part optional, depends if crnk provided that information.
   * </code>
   *
   * <p>NOTE: Error message formatting and handling is unstable and subject to change in future
   * versions.
   *
   * @param baseMessage Generic message describing failure
   * @param e Crnk exception
   * @return Formatted error message containing baseMessage, HTTP code and response body if
   *     available
   */
  private String errorMessageForFailingCrnkRequest(String baseMessage, Exception e) {
    final StringBuilder messageBuilder = new StringBuilder();
    messageBuilder.append(baseMessage);

    if (!baseMessage.trim().endsWith(".")) {
      messageBuilder.append(".");
    }
    // Mappable exceptions contains HTTP status code and further details
    if (e instanceof CrnkMappableException) {
      final CrnkMappableException mappableException = (CrnkMappableException) e;
      messageBuilder.append(" HTTP status code: ").append(mappableException.getHttpStatus());

      final ErrorData errorData = mappableException.getErrorData();
      if (errorData.getTitle() != null && !errorData.getTitle().isEmpty()) {
        messageBuilder.append(", message: ").append(mappableException.getErrorData().getTitle());

        if (errorData.getDetail() != null && !errorData.getDetail().isEmpty()) {
          messageBuilder.append(" - ").append(mappableException.getErrorData().getDetail());
        }

      }
    } else {
      messageBuilder.append(" ").append(e.getMessage());
    }
    return messageBuilder.toString();
  }
}
