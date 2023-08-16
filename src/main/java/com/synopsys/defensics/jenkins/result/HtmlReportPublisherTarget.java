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

import htmlpublisher.HtmlPublisherTarget;
import hudson.model.AbstractItem;
import hudson.model.Action;
import hudson.model.Run;
import java.util.List;
import java.util.Objects;
import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * This class exists to enable replacing the default graph icon html publisher uses
 * for html actions with our own icon.
 */
public final class HtmlReportPublisherTarget extends HtmlPublisherTarget {

  private static final boolean KEEP_ALL = true;
  private static final boolean ALWAYS_LINK_TO_LAST_BUILD = true;
  private static final boolean ALLOW_MISSING = true;

  private final String jobId;

  /**
   * Constructor.
   * @param reportName Report name. Used in action link text.
   * @param reportDir Directory from which the report can be found, relative to workspace.
   * @param reportFiles The file names of report files, separated by comma.
   * @param reportTitles The title of the report files, separated by comma. These are used as tab
   *        titles when viewing the report.
   * @param jobId The id of the Defensics job.
   */
  public HtmlReportPublisherTarget(String reportName, String reportDir, String reportFiles,
      String reportTitles, String jobId) {
    super(reportName,
        reportDir,
        reportFiles,
        KEEP_ALL,
        ALWAYS_LINK_TO_LAST_BUILD,
        ALLOW_MISSING);
    this.setReportTitles(reportTitles);
    this.jobId = jobId;
  }

  public String getJobId() {
    return jobId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof HtmlReportPublisherTarget)) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }
    HtmlReportPublisherTarget that = (HtmlReportPublisherTarget) o;
    return Objects.equals(jobId, that.jobId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), jobId);
  }

  /**
   * This is overridden to return an empty Action to avoid
   * {@link htmlpublisher.workflow.WorkflowActionsFactory}
   * creating a duplicate report link on project level.
   * This is called for Pipeline projects.
   * See {@link ProjectHtmlReportActionFactory} for Freestyle projects.
   * {@inheritDoc}
   */
  @Override
  public Action getProjectAction(AbstractItem item) {
    return EmptyAction.getInstance();
  }

  /**
   * This method replaces the {@link htmlpublisher.HtmlPublisherTarget.HTMLBuildAction} added to the
   * run by {@link htmlpublisher.HtmlPublisher} with a {@link HtmlReportAction}, a subtype.
   * The reason for this is to override the default icon used by
   * {@link htmlpublisher.HtmlPublisherTarget.BaseHTMLAction}.
   *
   * @param run The run where defensics results have been published using
   * {@link htmlpublisher.HtmlPublisher}, and where
   * {@link HTMLBuildAction} should be replaced with a
   * {@link HtmlReportAction}.
   */
  public void customizeActionForDefensics(Run<?, ?> run) {
    final List<HTMLBuildAction> actions = run.getActions(HTMLBuildAction.class);
    run.removeAction(actions.stream().filter(
        // Yes, we want to use == here instead of equals, because we want the exact same instance.
        // Equals would give true when report names are same even if the target instance is
        // different.
        action -> action.getHTMLTarget() == this).findFirst().get());
    run.addAction(createAction(run));
  }

  private HtmlReportAction createAction(Run<?, ?> run) {
    return new HtmlReportAction(run, this);
  }

  /**
   * This class exists to override the graph icon used by
   * {@link htmlpublisher.HtmlPublisherTarget.BaseHTMLAction} for builds.
   */
  public class HtmlReportAction extends HTMLBuildAction {

    public HtmlReportAction(Run<?, ?> run, HtmlPublisherTarget target) {
      super(run, target);
    }

    /**
     * Get file name for defensics logo to be used as icon for this action.
     *
     * @return Defensics logo filename. Null if action should be hidden.
     */
    @Override
    public String getIconFileName() {
      return super.getIconFileName() == null ? null
          : "/plugin/defensics/images/24x24/defensics-logo.png";
    }

    @NonNull
    @Override
    public HtmlPublisherTarget getHTMLTarget() {
      return HtmlReportPublisherTarget.this;
    }
  }
}
