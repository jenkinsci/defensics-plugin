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

package com.synopsys.defensics.jenkins.result;

import hudson.model.Action;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.CheckForNull;

public class BuildResultAction implements Action {

  private final String reportUrl;
  private final long failureCount;
  private final List<String> resultPackages = new ArrayList<>();

  /**
   * Helps displaying Defensics run result in build page.
   *
   * @param reportUrl    url to Defensics html report
   * @param failureCount failure count to display as a summary
   * @param resultFile   result package file name
   */
  public BuildResultAction(String reportUrl, long failureCount, String resultFile) {
    this.reportUrl = reportUrl;
    this.failureCount = failureCount;
    if (resultFile != null) {
      resultPackages.add(resultFile);
    }
  }

  /**
   * Helps displaying Defensics run result in build page.
   *
   * @param reportUrl      url to Defensics html report
   * @param failureCount   failure count to display as a summary
   * @param resultPackages result package files as a list
   */
  public BuildResultAction(String reportUrl, long failureCount, List<String> resultPackages) {
    this.reportUrl = reportUrl;
    this.failureCount = failureCount;
    if (resultPackages.size() > 0) {
      this.resultPackages.addAll(resultPackages);
    }
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

  public List<String> getResultPackages() {
    return resultPackages;
  }

  /**
   * Add a result package.
   *
   * @param resultPackage if the param is null it won't be added.
   */
  public void addResultPackage(String resultPackage) {
    if (resultPackage != null) {
      resultPackages.add(resultPackage);
    }
  }
}
