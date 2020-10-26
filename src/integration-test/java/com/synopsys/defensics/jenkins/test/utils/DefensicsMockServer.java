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

import com.synopsys.defensics.api.ApiService;
import com.synopsys.defensics.apiserver.model.RunState;
import org.mockserver.integration.ClientAndServer;

/**
 * Mock server returning for unit and integration tests. Add specific exceptions what requests
 * are expected and returns sample model.
 *
 * <p>Wrapper class: This delegates expectation setting either to APIv1 or APIv2 server depending
 * on ApiService flag to allow testing both APIs until v1 is removed.</p>
 */
public class DefensicsMockServer {
  public static final String RUN_ID = "72c5c70f-102c-489c-a7cc-6625d47c5ab6";
  private final String verdict;
  private final RunState endState;
  private final boolean authentication;

  public DefensicsMockServer(boolean authentication, String verdict, RunState endState) {
    this.verdict = verdict;
    this.authentication = authentication;
    this.endState = endState;
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
}
