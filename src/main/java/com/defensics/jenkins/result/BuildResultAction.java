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

package com.defensics.jenkins.result;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import hudson.model.Action;

public class BuildResultAction implements Action {

  private final String reportUrl;
  private long failureCount;

  /**
   * Helps displaying Defensics run result in build page.
   *
   * @param reportUrl    url to Defensics html report
   * @param failureCount failure count to display as a summary
   */
  public BuildResultAction(String reportUrl, long failureCount) {
    this.reportUrl = reportUrl;
    this.failureCount = failureCount;
  }

  @CheckForNull
  @Override
  public String getIconFileName() {
    return null;
  }

  @CheckForNull
  @Override
  public String getDisplayName() {
    return "Defensics Results";
  }

  @CheckForNull
  @Override
  public String getUrlName() {
    return null;
  }

  public String getReportUrl() {
    return reportUrl;
  }

  public long getFailureCount() {
    return failureCount;
  }

  public void setFailureCount(long failureCount) {
    this.failureCount = failureCount;
  }
}
