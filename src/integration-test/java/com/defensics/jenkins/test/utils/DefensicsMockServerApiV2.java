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

package com.defensics.jenkins.test.utils;

import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.JsonBody.json;
import static org.mockserver.model.NottableString.not;
import static org.mockserver.model.NottableString.string;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.defensics.apiserver.model.HealthCheckResult;
import com.defensics.apiserver.model.Item;
import com.defensics.apiserver.model.Run;
import com.defensics.apiserver.model.RunState;
import com.defensics.apiserver.model.RunVerdict;
import com.defensics.apiserver.model.SuiteInstance;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.matchers.Times;
import org.mockserver.model.HttpResponse;

/**
 * Mock server for API v2. Returns plain JSON.
 */
public class DefensicsMockServerApiV2 {
  public static final String RUN_ID = "72c5c70f-102c-489c-a7cc-6625d47c5ab6";
  public static final String RESULT_ID = "9ee36d3c-2f7d-4ae7-8ed9-974ca0518b2a";
  public static final String SUITE_INSTANCE_ID = "8f4992ae-59e9-41af-bd5c-6402fd4d781c";
  public static final long TOTAL = 5000;
  private static final String AUTHENTICATION_TOKEN = "Bearer test-token";
  private static final String REPORT_ZIP_PATH =
      "src/integration-test/resources/com/defensics/jenkins/test/report.zip";
  private static final String RESULT_PACKAGE_PATH =
      "src/integration-test/resources/com/defensics/jenkins/test/result-package.zip";

  private static final String EXPECTED_USER_AGENT_REGEX = "Defensics-Jenkins-Plugin.*";
  private static final String CONTENT_TYPE_JSON = "application/json";
  private static final String REPORT_FORMAT = "cloud-html";
  private final RunVerdict verdict;
  private final RunState endState;
  private final boolean authentication;

  /** Object mapper to serialize API response. */
  private final ObjectMapper objectMapper;

  public DefensicsMockServerApiV2(
      boolean authentication,
      RunVerdict verdict,
      RunState endState
  ) {
    this.authentication = authentication;
    this.verdict = verdict;
    this.endState = endState;

    // Configure objectmapper to match API server configuration (check from DefensicsApiV2Client)
    this.objectMapper = new ObjectMapper();
    this.objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.LOWER_CAMEL_CASE);
    this.objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    this.objectMapper.registerModule(new JavaTimeModule());
  }

  /**
   * Initializes functional (succesfull) mock server with authentication enabled. All needed queries
   * are supported.
   *
   * @param server Server instance to be initialized.
   */
  public void initServer(ClientAndServer server) {
    initHealthCheck(server, true);
    initCreateRun(server);
    initUploadTestplan(server);
    initSetConfigurationSettings(server);
    initGetSuiteInstance(server);
    initStartRun(server);
    initStopRun(server);
    initGetRun(server);
    initGetReport(server);
    initRemoveRun(server);
    initGetResultPackage(server);
    if (authentication) {
      initUnauthorized(server);
    }
  }

  public static void initHealthCheck(ClientAndServer server, boolean healthy) {
    Map<String, HealthCheckResult> healthCheckResult = new HashMap<>();
    healthCheckResult.put("apiServer", new HealthCheckResult(healthy, "Health check message"));

    // Return 500 Internal server error if there's unhealthy health check
    int statusCode = healthy ? 200 : 500;

    server
        .when(
            request()
                .withMethod("GET")
                .withHeader("User-Agent", EXPECTED_USER_AGENT_REGEX)
                .withHeader("Authorization", AUTHENTICATION_TOKEN)
                .withPath("/api/v2/healthcheck"))
        .respond(HttpResponse.response()
            .withHeader("Content-Type", CONTENT_TYPE_JSON)
            .withBody(json(new Item<>(healthCheckResult)))
            .withStatusCode(statusCode));
  }

  private void initCreateRun(ClientAndServer server) {
    server
        .when(
            request()
                .withMethod("POST")
                .withHeader("Content-Type", CONTENT_TYPE_JSON)
                .withHeader("User-Agent", EXPECTED_USER_AGENT_REGEX)
                .withPath("/api/v2/runs"))
        .respond(HttpResponse.response()
            // Add brief delay added to check client timeout handling
            .withDelay(TimeUnit.MILLISECONDS, 5)
            .withHeader("Content-Type", CONTENT_TYPE_JSON)
            .withBody(json(getRunJson(null, RunState.IDLE, 0)))
            .withStatusCode(201));
  }

  private void initUploadTestplan(ClientAndServer server) {
    server
        .when(
            request()
                .withMethod("POST")
                .withHeader("User-Agent", EXPECTED_USER_AGENT_REGEX)
                .withPath("/api/v2/runs/" + RUN_ID + "/configuration/upload-plan"))
        .respond(HttpResponse.response().withBody("").withStatusCode(204));
  }

  private static void initSetConfigurationSettings(ClientAndServer server) {
    server
        .when(
            request()
                .withMethod("POST")
                .withHeader("User-Agent", EXPECTED_USER_AGENT_REGEX)
                .withPath("/api/v2/runs/" + RUN_ID + "/configuration/arguments"))
        .respond(HttpResponse.response().withBody("Success").withStatusCode(200));
  }

  private void initGetSuiteInstance(ClientAndServer server) {
    server
        .when(
            request()
                .withMethod("GET")
                .withHeader("User-Agent", EXPECTED_USER_AGENT_REGEX)
                .withPath("/api/v2/runs/" + RUN_ID + "/configuration/suite-instance"),
            Times.exactly(1)) //First state is LOADING
        .respond(HttpResponse.response()
            .withBody(json(getSuiteInstanceJson("LOADING"))).withStatusCode(200));

    server
        .when(
            request()
                .withMethod("GET")
                .withHeader("User-Agent", EXPECTED_USER_AGENT_REGEX)
                .withPath("/api/v2/runs/" + RUN_ID + "/configuration/suite-instance"),
            Times.unlimited()) // Next state is LOADED
        .respond(HttpResponse.response()
            .withBody(json(getSuiteInstanceJson("LOADED"))).withStatusCode(200));
  }

  private void initStartRun(ClientAndServer server) {
    server
        .when(
            request()
                .withMethod("POST")
                .withHeader("User-Agent", EXPECTED_USER_AGENT_REGEX)
                .withPath("/api/v2/runs/" + RUN_ID + "/start"))
        .respond(HttpResponse
            .response()
            .withBody("").withStatusCode(204));
  }

  private void initUnauthorized(ClientAndServer server) {
    server
        .when(
            request()
                .withHeader("User-Agent", EXPECTED_USER_AGENT_REGEX)
                .withHeader(string("Authorization"), not(AUTHENTICATION_TOKEN))
        )
        .respond(
            HttpResponse
                .response()
                .withHeader("Content-Type", CONTENT_TYPE_JSON)
                .withBody("Unauthorized. No authentication credentials found in request.")
                .withStatusCode(401));
  }

  /**
   * Responds to sequential requests with job status as follows: "STARTING", "RUNNING", and
   * "COMPLETED".
   *
   * @param server server to be initialized.
   */
  private void initGetRun(ClientAndServer server) {
    server
        .when(
            request()
                .withMethod("GET")
                .withHeader("User-Agent", EXPECTED_USER_AGENT_REGEX)
                .withPath("/api/v2/runs/" + RUN_ID)
                .withQueryStringParameter("include", "failure-summary"),
            Times.exactly(1)) //First response is "STARTING"
        .respond(HttpResponse.response()
            .withHeader("Content-Type", CONTENT_TYPE_JSON)
            .withBody(json(getRunJson(RunVerdict.PASS, RunState.STARTING, 0)))
            .withStatusCode(200));

    server
        .when(
            request()
                .withMethod("GET")
                .withHeader("User-Agent", EXPECTED_USER_AGENT_REGEX)
                .withPath("/api/v2/runs/" + RUN_ID)
                .withQueryStringParameter("include", "failure-summary"),
            Times.exactly(1)) //Following response is "RUNNING"
        .respond(HttpResponse.response()
            .withHeader("Content-Type", CONTENT_TYPE_JSON)
            .withBody(json(getRunJson(RunVerdict.PASS, RunState.RUNNING, 300)))
            .withStatusCode(200));
    server
        .when(
            request()
                .withMethod("GET")
                .withHeader("User-Agent", EXPECTED_USER_AGENT_REGEX)
                .withPath("/api/v2/runs/" + RUN_ID)
                .withQueryStringParameter("include", "failure-summary"),
            Times.unlimited()) //After this job is "COMPLETED"
        .respond(HttpResponse.response()
            .withHeader("Content-Type", CONTENT_TYPE_JSON)
            .withBody(json(getRunJson(verdict, endState, TOTAL)))
            .withStatusCode(200));
  }

  private void initStopRun(ClientAndServer server) {
    server
        .when(
            request()
                .withMethod("POST")
                .withHeader("User-Agent", EXPECTED_USER_AGENT_REGEX)
                .withPath("/api/v2/runs/" + RUN_ID + "/stop"))
        .respond(HttpResponse.response()
            .withHeader("Content-Type", CONTENT_TYPE_JSON)
            .withBody(json(""))
            .withStatusCode(204));
  }

  private void initGetReport(ClientAndServer server) {
    try {
      server.when(
          request()
              .withMethod("GET")
              .withHeader("User-Agent", EXPECTED_USER_AGENT_REGEX)
              .withPath("/api/v2/results/report")
              .withQueryStringParameter("resultId", RESULT_ID)
              .withQueryStringParameter("format", REPORT_FORMAT))

          .respond(HttpResponse.response()
              .withStatusCode(200)
              .withBody(Files.readAllBytes(Paths.get(REPORT_ZIP_PATH))));
    } catch (IOException ioe) {
      throw new RuntimeException(ioe);
    }
  }

  private void initGetResultPackage(ClientAndServer server) {
    try {
      server.when(
          request()
              .withMethod("GET")
              .withHeader("User-Agent", EXPECTED_USER_AGENT_REGEX)
              .withPath("/api/v2/results/result-package")
              .withQueryStringParameter("resultId", RESULT_ID))
          .respond(HttpResponse.response()
              .withStatusCode(200)
              .withBody(Files.readAllBytes(Paths.get(RESULT_PACKAGE_PATH))));
    } catch (IOException ioe) {
      throw new RuntimeException(ioe);
    }
  }

  private void initRemoveRun(ClientAndServer server) {
    server
        .when(
            request()
                .withMethod("DELETE")
                .withHeader("User-Agent", EXPECTED_USER_AGENT_REGEX)
                .withPath("/api/v2/runs/" + RUN_ID))
        .respond(HttpResponse.response().withStatusCode(204));
  }

  private String getRunJson(RunVerdict verdict, RunState runState, long runIndex) {
    Run run = new Run(RUN_ID);
    run.setState(runState);
    run.setRunIndex((int)runIndex);
    run.setCaseIndex((int)runIndex);
    run.setCasesToBeExecuted((int)TOTAL);
    if (verdict != null) {
      run.setVerdict(verdict);
    }
    run.setFailureSummary(Collections.emptyList());
    run.setResultId(RESULT_ID);

    return toJsonString(new Item<Run>(run));
  }


  private String getSuiteInstanceJson(String suiteInstanceState) {
    SuiteInstance suiteInstance = new SuiteInstance(
        SUITE_INSTANCE_ID,
        RunState.valueOf(suiteInstanceState),
        null,
        "suite-feature",
        "suite-version"
    );

    return toJsonString(new Item<SuiteInstance>(suiteInstance));
  }

  /**
   * Serialize given object into JSON string which can be returned as the mocked API response.
   *
   * @param object to serialize
   * @return JSON presentation
   * @throws IllegalArgumentException if object could not be serialized
   */
  private String toJsonString(Object object) {
    try {
      return objectMapper.writeValueAsString(object);
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException("Cannot serialize object", e);
    }
  }
}
