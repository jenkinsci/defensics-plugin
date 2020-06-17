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

package com.synopsys.defensics.jenkins.test.utils;

import static org.mockserver.model.Header.header;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.JsonBody.json;
import static org.mockserver.model.NottableString.not;
import static org.mockserver.model.NottableString.string;

import com.synopsys.defensics.apiserver.model.RunState;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import javax.ws.rs.core.HttpHeaders;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.matchers.Times;
import org.mockserver.model.HttpResponse;

public class DefensicsMockServer {
  public static final String RUN_ID = "72c5c70f-102c-489c-a7cc-6625d47c5ab6";
  public static final String SUITE_INSTANCE_ID = "8f4992ae-59e9-41af-bd5c-6402fd4d781c";
  public static final long TOTAL = 5000;
  private static final String AUTHENTICATION_TOKEN = "Bearer test-token";
  private static final String REPORT_ZIP_PATH =
      "src/integration-test/resources/com/synopsys/defensics/jenkins/test/report.zip";
  private static final String RESULT_PACKAGE_PATH =
      "src/integration-test/resources/com/synopsys/defensics/jenkins/test/result-package.zip";

  private static final String EXPECTED_USER_AGENT_REGEX = "Defensics-Jenkins-Plugin.*";
  private final String verdict;
  private final RunState endState;
  private final boolean authentication;

  public DefensicsMockServer(boolean authentication, String verdict, RunState endState) {
    this.verdict = verdict;
    this.authentication = authentication;
    this.endState = endState;
  }

  /**
   * Initializes functional (succesfull) mock server with authentication enabled. All needed queries
   * are supported.
   *
   * @param server Server instance to be initialized.
   */
  public void initServer(ClientAndServer server) {
    initHealthCheck(server);
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

  private void initHealthCheck(ClientAndServer server) {
    server
        .when(
            request()
                .withMethod("GET")
                .withHeader("User-Agent", EXPECTED_USER_AGENT_REGEX)
                .withHeader(HttpHeaders.AUTHORIZATION, AUTHENTICATION_TOKEN)
                .withPath("/api/v1/healthcheck"))
        .respond(HttpResponse.response().withBody("{\"healthy\":true}").withStatusCode(200));
  }

  private void initCreateRun(ClientAndServer server) {
    server
        .when(
            request()
                .withMethod("POST")
                .withHeader("Content-Type", "application/vnd.api+json; charset=utf-8")
                .withHeader("User-Agent", EXPECTED_USER_AGENT_REGEX)
                .withPath("/api/v1/runs"))
        .respond(HttpResponse.response()
            .withHeader("Content-Type", "application/vnd.api+json")
            .withBody(json("{\n"
                    + "  \"data\": {\n"
                    + "    \"id\": \"" + RUN_ID +"\",\n"
                    + "    \"type\": \"runs\",\n"
                    + "    \"links\": {\n"
                    + "      \"self\": \"http://127.0.0.1:3150/api/v1/runs/" + RUN_ID + "\"\n"
                    + "    },\n"
                    + "    \"attributes\": {\n"
                    + "      \"cases-to-be-executed\": 0,\n"
                    + "      \"case-index\": 0,\n"
                    + "      \"run-index\": 0,\n"
                    + "      \"state\": \"IDLE\",\n"
                    + "      \"verdict\": null,\n"
                    + "      \"run-name\": null,\n"
                    + "      \"run-type\": null,\n"
                    + "      \"run-start-time\": null\n"
                    + "    },\n"
                    + "    \"relationships\": {\n"
                    + "      \"parent-configuration\": {\n"
                    + "        \"links\": {\n"
                    + "          \"self\": \"http://127.0.0.1:3150/api/v1/runs/" + RUN_ID + "/relationships/parent-configuration\",\n"
                    + "          \"related\": \"http://127.0.0.1:3150/api/v1/runs/" + RUN_ID + "/parent-configuration\"\n"
                    + "        }\n"
                    + "      },\n"
                    + "      \"configuration\": {\n"
                    + "        \"links\": {\n"
                    + "          \"self\": \"http://127.0.0.1:3150/api/v1/runs/" + RUN_ID + "/relationships/configuration\",\n"
                    + "          \"related\": \"http://127.0.0.1:3150/api/v1/runs/" + RUN_ID + "/configuration\"\n"
                    + "        }\n"
                    + "      },\n"
                    + "      \"failure-summary\" : {\n"
                    + "        \"data\" : [],\n"
                    + "        \"links\" : {\n"
                    + "          \"self\" : \"http://127.0.0.1:3150/api/v1/runs/" + RUN_ID + "/relationships/failure-summary\",\n"
                    + "          \"related\" : \"http://127.0.0.1:3150/api/v1/runs/" + RUN_ID + "/failure-summary\"\n"
                    + "        }\n"
                    + "      },\n"
                    + "      \"project\" : {\n"
                    + "        \"links\" : {\n"
                    + "          \"self\" : \"http://127.0.0.1:3150/api/v1/runs/" + RUN_ID + "/relationships/project\",\n"
                    + "          \"related\" : \"http://127.0.0.1:3150/api/v1/runs/" + RUN_ID + "/project\"\n"
                    + "        }\n"
                    + "      }"
                    + "      \n"
                    + "    }\n"
                    + "  }\n"
                    + "}")
            )
            .withStatusCode(201));
  }

  private void initUploadTestplan(ClientAndServer server) {
    server
        .when(
            request()
                .withMethod("POST")
                .withHeader("User-Agent", EXPECTED_USER_AGENT_REGEX)
                .withPath("/api/v1/runs/" + RUN_ID + "/configuration/upload-plan"))
        .respond(HttpResponse.response().withBody("").withStatusCode(204));
  }

  private static void initSetConfigurationSettings(ClientAndServer server) {
    server
        .when(
            request()
                .withMethod("POST")
                .withHeader("User-Agent", EXPECTED_USER_AGENT_REGEX)
                .withPath("/api/v1/runs/" + RUN_ID + "/configuration/arguments"))
        .respond(HttpResponse.response().withBody("Success").withStatusCode(200));
  }

  private void initGetSuiteInstance(ClientAndServer server) {
    server
        .when(
            request()
                .withMethod("GET")
                .withHeader("User-Agent", EXPECTED_USER_AGENT_REGEX)
                .withPath("/api/v1/runs/" + RUN_ID + "/configuration/suite-instance"),
            Times.exactly(1)) //First state is LOADING
        .respond(HttpResponse.response()
            .withBody(json(
                suiteInstanceContent("LOADING"))).withStatusCode(200));

    server
        .when(
            request()
                .withMethod("GET")
                .withHeader("User-Agent", EXPECTED_USER_AGENT_REGEX)
                .withPath("/api/v1/runs/" + RUN_ID + "/configuration/suite-instance"),
            Times.unlimited()) // Next state is LOADED
        .respond(HttpResponse.response()
            .withBody(json(suiteInstanceContent("LOADED"))).withStatusCode(200));
  }

  private void initStartRun(ClientAndServer server) {
    server
        .when(
            request()
                .withMethod("POST")
                .withHeader("User-Agent", EXPECTED_USER_AGENT_REGEX)
                .withPath("/api/v1/runs/" + RUN_ID + "/start"))
        .respond(HttpResponse
            .response()
            .withBody("").withStatusCode(204));
  }

  private void initUnauthorized(ClientAndServer server) {
    server
        .when(
            request()
                .withHeader("User-Agent", EXPECTED_USER_AGENT_REGEX)
                .withHeader(
                    (
                        header(
                            string(HttpHeaders.AUTHORIZATION),
                            not(AUTHENTICATION_TOKEN)))))
        .respond(
            HttpResponse
                .response()
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
                .withPath("/api/v1/runs/" + RUN_ID),
            Times.exactly(1)) //First response is "STARTING"
        .respond(HttpResponse.response()
            .withHeader("Content-Type", "application/vnd.api+json")
            .withBody(json(runContent("PASS", RunState.STARTING, 0)))
            .withStatusCode(200));

    server
        .when(
            request()
                .withMethod("GET")
                .withHeader("User-Agent", EXPECTED_USER_AGENT_REGEX)
                .withPath("/api/v1/runs/" + RUN_ID),
                Times.exactly(1)) //Following response is "RUNNING"
        .respond(HttpResponse.response()
            .withHeader("Content-Type", "application/vnd.api+json")
            .withBody(json(runContent("PASS", RunState.RUNNING, 300)))
            .withStatusCode(200));
    server
        .when(
            request()
                .withMethod("GET")
                .withHeader("User-Agent", EXPECTED_USER_AGENT_REGEX)
                .withPath("/api/v1/runs/" + RUN_ID),
                Times.unlimited()) //After this job is "COMPLETED"
        .respond(HttpResponse.response()
            .withHeader("Content-Type", "application/vnd.api+json")
            .withBody(json(runContent(verdict, endState, TOTAL)))
            .withStatusCode(200));
  }

  private void initStopRun(ClientAndServer server) {
    server
        .when(
            request()
                .withMethod("POST")
                .withHeader("User-Agent", EXPECTED_USER_AGENT_REGEX)
                .withPath("/api/v1/runs/" + RUN_ID + "/stop"))
        .respond(HttpResponse.response()
            .withHeader("Content-Type", "application/json")
            .withBody(json(""))
            .withStatusCode(204));
  }

  private void initGetReport(ClientAndServer server) {
    try {
      server.when(
          request()
              .withMethod("GET")
              .withHeader("User-Agent", EXPECTED_USER_AGENT_REGEX)
              .withPath("/api/v1/runs/" + RUN_ID + "/report"))
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
              .withPath("/api/v1/runs/" + RUN_ID + "/package"))
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
                .withPath("/api/v1/runs/" + RUN_ID))
        .respond(HttpResponse.response().withStatusCode(204));
  }

  private String runContent(String verdict, RunState runState, long runIndex) {
    return "{\n"
        + "  \"data\": {\n"
        + "    \"id\": \"" + RUN_ID + "\",\n"
        + "    \"type\": \"runs\",\n"
        + "    \"links\": {\n"
        + "      \"self\": \"http://127.0.0.1:3150/api/v1/runs/" + RUN_ID + "\"\n"
        + "    },\n"
        + "    \"attributes\": {\n"
        + "      \"cases-to-be-executed\": " + TOTAL + ",\n"
        + "      \"case-index\": " + runIndex + ",\n"
        + "      \"run-index\": " + runIndex + ",\n"
        + "      \"state\": \"" + runState + "\",\n"
        + "      \"verdict\": \"" + verdict + "\",\n"
        + "      \"run-name\": null,\n"
        + "      \"run-type\": null,\n"
        + "      \"run-start-time\": null\n"
        + "    },\n"
        + "    \"relationships\": {\n"
        + "      \"parent-configuration\": {\n"
        + "        \"links\": {\n"
        + "          \"self\": \"http://127.0.0.1:3150/api/v1/runs/" + RUN_ID + "/relationships/parent-configuration\",\n"
        + "          \"related\": \"http://127.0.0.1:3150/api/v1/runs/" + RUN_ID + "/parent-configuration\"\n"
        + "        }\n"
        + "      },\n"
        + "      \"configuration\": {\n"
        + "        \"links\": {\n"
        + "          \"self\": \"http://127.0.0.1:3150/api/v1/runs/" + RUN_ID + "/relationships/configuration\",\n"
        + "          \"related\": \"http://127.0.0.1:3150/api/v1/runs/" + RUN_ID + "/configuration\"\n"
        + "        }\n"
        + "      },\n"
        + "      \"failure-summary\" : {\n"
        + "        \"data\" : [],\n"
        + "        \"links\" : {\n"
        + "          \"self\" : \"http://127.0.0.1:3150/api/v1/runs/" + RUN_ID + "/relationships/failure-summary\",\n"
        + "          \"related\" : \"http://127.0.0.1:3150/api/v1/runs/" + RUN_ID + "/failure-summary\"\n"
        + "        }\n"
        + "      },\n"
        + "      \"project\" : {\n"
        + "        \"links\" : {\n"
        + "          \"self\" : \"http://127.0.0.1:3150/api/v1/runs/" + RUN_ID + "/relationships/project\",\n"
        + "          \"related\" : \"http://127.0.0.1:3150/api/v1/runs/" + RUN_ID + "/project\"\n"
        + "        }\n"
        + "      }"
        + "    }\n"
        + "  }\n"
        + "}";
  }

  private String suiteInstanceContent(String suiteInstanceState) {
    return "{\n"
        + "  \"data\": {\n"
        + "    \"id\": \"" + SUITE_INSTANCE_ID + "\",\n"
        + "    \"type\": \"suite-instances\",\n"
        + "    \"links\": {\n"
        + "      \"self\": \"http://127.0.0.1:3150/api/v1/suite-instances/" + SUITE_INSTANCE_ID
        + "\"\n"
        + "    },\n"
        + "    \"attributes\": {\n"
        + "      \"state\": \"" + suiteInstanceState + "\",\n"
        + "      \"error\": null\n"
        + "    },\n"
        + "    \"relationships\": {\n"
        + "      \"suite\": {\n"
        + "        \"links\": {\n"
        + "          \"self\": \"http://127.0.0.1:3150/api/v1/suite-instances/" + SUITE_INSTANCE_ID
        + "/relationships/suite\",\n"
        + "          \"related\": \"http://127.0.0.1:3150/api/v1/suite-instances/"
        + SUITE_INSTANCE_ID + "/suite\"\n"
        + "        }\n"
        + "      },\n"
        + "      \"run-test-configuration\": {\n"
        + "        \"links\": {\n"
        + "          \"self\": \"http://127.0.0.1:3150/api/v1/suite-instances/" + SUITE_INSTANCE_ID
        + "/relationships/run-test-configuration\",\n"
        + "          \"related\": \"http://127.0.0.1:3150/api/v1/suite-instances/"
        + SUITE_INSTANCE_ID + "/run-test-configuration\"\n"
        + "        }\n"
        + "      }\n"
        + "    }\n"
        + "  }\n"
        + "}";
  }
}
