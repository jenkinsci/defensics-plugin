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

package com.defensics.apiserver.model;

/**
 * Healthcheck result returned by API server.
 */
public class HealthCheckResult {
  private boolean healthy;
  private String message;

  /** Private constructor for Jackson (de)serialization. */
  private HealthCheckResult() {
  }

  /*
   * Constructor.
   *
   * @param healthy true if healthcheck is healthy
   */
  public HealthCheckResult(boolean healthy) {
    this.healthy = healthy;
    this.message = "";
  }

  /**
   * Constructor.
   *
   * @param healthy true if healthcheck is healthy
   * @param message Additional message
   */
  public HealthCheckResult(boolean healthy, String message) {
    this.healthy = healthy;
    this.message = message;
  }

  public boolean isHealthy() {
    return healthy;
  }

  public String getMessage() {
    return message;
  }

  public void setHealthy(boolean healthy) {
    this.healthy = healthy;
  }

  public void setMessage(String message) {
    this.message = message;
  }
}
