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
 * Response returned from the /api/v2/version endpoint. Currently returns only main monitor version
 * but later on can include other components as well.
 */
public class VersionInformation {
  private String monitorVersion;

  public VersionInformation() {
  }

  public void setMonitorVersion(String monitorVersion) {
    this.monitorVersion = monitorVersion;
  }

  public String getMonitorVersion() {
    return monitorVersion;
  }
}
