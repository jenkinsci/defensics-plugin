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

import com.synopsys.defensics.apiserver.model.Run;
import com.synopsys.defensics.jenkins.Logger;
import com.synopsys.defensics.jenkins.result.HtmlReportPublisherTarget.HtmlReportAction;
import com.synopsys.defensics.jenkins.util.DefensicsUtils;
import htmlpublisher.HtmlPublisher;
import htmlpublisher.HtmlPublisherTarget;
import hudson.FilePath;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ResultPublisher {

  public static final String REPORT_NAME = "Defensics Results";

  /**
   * Publish HTML report from file in workspace.
   *
   * @param jenkinsRun   The Jenkins run to publish report for.
   * @param defensicsRun The defensics run for which results are to be published.
   * @param report       The html report to publish
   * @param logger       The Defensics Plugin logger.
   * @param workspace    The Jenkins workspace.
   * @throws InterruptedException If publishing is interrupted.
   */
  public void publishResults(hudson.model.Run<?, ?> jenkinsRun, Run defensicsRun,
      HtmlReport report, Logger logger, FilePath workspace)
      throws InterruptedException {
    publishReportAction(jenkinsRun, workspace, logger, report, defensicsRun.getId());
    publishBuildResultAction(jenkinsRun, DefensicsUtils.countRunFailures(defensicsRun));
  }

  private void publishReportAction(hudson.model.Run<?, ?> run, FilePath workspace,
      Logger logger, HtmlReport report, String jobId)
      throws InterruptedException {
    List<HtmlPublisherTarget> targets = new ArrayList<>();
    List<HtmlReportAction> deletedActions = deleteDuplicateActions(run);

    HtmlReportPublisherTarget target = new HtmlReportPublisherTarget(
        REPORT_NAME,
        report.getResultDirectory(),
        getOtherFileNames(deletedActions) + report.getFileName(),
        getOtherTitles(deletedActions) + report.getReportTitle(),
        jobId);
    targets.add(target);
    HtmlPublisher.publishReports(
        run, workspace, logger.getListener(), targets, HtmlPublisher.class);
    target.customizeActionForDefensics(run);
  }

  private List<HtmlReportAction> deleteDuplicateActions(hudson.model.Run<?, ?> run) {
    List<HtmlReportAction> actions = run.getActions(HtmlReportAction.class);

    for (HtmlReportAction action : actions) {
      run.removeAction(action);
    }
    return actions;
  }

  private String getOtherTitles(List<HtmlReportAction> deletedActions) {
    return joinAsCommaDelimitedString(
        deletedActions.stream().map(action -> action.getHTMLTarget().getReportTitles()));
  }

  private String getOtherFileNames(List<HtmlReportAction> deletedActions) {
    return joinAsCommaDelimitedString(
        deletedActions.stream().map(action -> action.getHTMLTarget().getReportFiles()));
  }

  /**
   * Join the string in the stream into a comma delimited string. Note! If the result is not an
   * empty string, also adds a comma to the beginning so this can be easily appended to another
   * string.
   *
   * @param strings The stream of strings to join together.
   * @return A comma delimited string starting with a comma and containing all the string from the
   *         stream. Or an empty string if there was nothing to join.
   */
  private String joinAsCommaDelimitedString(Stream<String> strings) {
    String joinedString = strings.collect(Collectors.joining(","));
    return joinedString.isEmpty() ? joinedString : joinedString + ",";
  }

  private void publishBuildResultAction(hudson.model.Run<?, ?> jenkinsRun, long failureCount) {
    BuildResultAction buildResultAction = jenkinsRun.getAction(BuildResultAction.class);
    if (buildResultAction == null) {
      HtmlReportAction reportAction = jenkinsRun.getAction(HtmlReportAction.class);
      buildResultAction = new BuildResultAction(reportAction.getUrlName(), failureCount);
    } else {
      final long prevFailureCount = buildResultAction.getFailureCount();
      buildResultAction.setFailureCount(prevFailureCount + failureCount);
    }
    jenkinsRun.addOrReplaceAction(buildResultAction);
  }
}
