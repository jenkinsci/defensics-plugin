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

package com.synopsys.defensics.jenkins.test.utils;

import com.synopsys.defensics.apiserver.model.RunState;
import com.synopsys.defensics.apiserver.model.RunVerdict;
import java.util.concurrent.TimeUnit;
import org.mockserver.configuration.ConfigurationProperties;
import org.mockserver.integration.ClientAndServer;
import org.slf4j.event.Level;

/**
 * Mock server returning for unit and integration tests. Add specific exceptions what requests
 * are expected and returns sample model.
 *
 * <p>Wrapper class: This delegates expectation setting either to APIv1 or APIv2 server depending
 * on ApiService flag to allow testing both APIs until v1 is removed.</p>
 */
public class DefensicsMockServer {
  public static final String RUN_ID = "72c5c70f-102c-489c-a7cc-6625d47c5ab6";
  private final RunVerdict verdict;
  private final RunState endState;
  private final boolean authentication;

  public DefensicsMockServer(boolean authentication, RunVerdict verdict, RunState endState) {
    this.verdict = verdict;
    this.authentication = authentication;
    this.endState = endState;
    ConfigurationProperties.logLevel(Level.WARN.toString());
  }

  /**
   * Change Mock server logging level. INFO level prints all request expectations etc., polluting
   * the run logs so level is set to WARN level by default. Note this is done via static
   * ConfigurationProperties so this will have global effect.
   *
   * @param level Log level. Uses SLF4j levels TRACE, DEBUG, INFO, WARN, ERROR, OFF (or java
   *     logger levels).
   */
  public static void setMockServerLogLevel(String level) {
    ConfigurationProperties.logLevel(level);
  }

  /**
   * Initializes functional (succesful) mock server with authentication enabled. All needed queries
   * are supported.
   *
   * @param server Server instance to be initialized.
   */
  public void initServer(ClientAndServer server) {
    // NOTE: This creates new server instance every initServer(.). If problematic, better would
    // be to change actual mockservers to inherit this, and change this class to use factory method
    // to create desired server.
    final DefensicsMockServerApiV2 apiV2Server = new DefensicsMockServerApiV2(
        authentication,
        verdict,
        endState
    );
    apiV2Server.initServer(server);
  }


  /**
   * Stop the mockServer gracefully. Issues stop and wait that the server is stopped. Note: This
   * handles the lower-level mockServer and not this DefensicsMockServer. This method could go
   * somewhere else as well but currently there isn't suitable util class so let's have this here
   * for now.
   *
   * @param mockServer to stop
   * @throws IllegalStateException if server could not be stopped in a timely manner
   */
  public static void stopMockServer(ClientAndServer mockServer) {
    mockServer.stop();
    // Windows CI builds fails sometimes due to "Unable to connect to socket" so wait
    // that previous test server has stopped.
    // Related issue https://github.com/mock-server/mockserver/issues/498
    int counter = 5;
    while(!mockServer.hasStopped(3, 500, TimeUnit.MILLISECONDS)) {
      if (counter-- <= 0) {
        throw new IllegalStateException(
            "Could not stop mockserver in port " + mockServer.getLocalPort());
      }
    }
  }
}
