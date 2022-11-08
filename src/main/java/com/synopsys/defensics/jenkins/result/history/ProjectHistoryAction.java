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

package com.synopsys.defensics.jenkins.result.history;

import com.synopsys.defensics.jenkins.result.BuildResultAction;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import hudson.model.Action;
import hudson.model.Job;
import hudson.model.Run;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

/**
 * This action presents the defensics failure trend chart.
 */
public class ProjectHistoryAction implements Action {

  public static final int MAX_BUILDS = 20;
  private List<Run<?,?>> runs;

  /**
   * Constructor.
   *
   * @param job The job for which this action should be created. The failures of the job's each run
   *            are used to create the trend chart.
   */
  public ProjectHistoryAction(Job<?,?> job) {
    runs = new ArrayList<>();
    Run<?,?> run = job.getLastCompletedBuild();
    boolean hasDefensicsResults = false;
    for (int i = 0; i < MAX_BUILDS; i++) {
      if (run == null) {
        break;
      }

      BuildResultAction buildResultAction = run.getAction(BuildResultAction.class);
      if (buildResultAction != null) {
        hasDefensicsResults = true;
      }

      runs.add(run);
      run = run.getPreviousBuild();
    }

    if (!hasDefensicsResults) {
      runs = new ArrayList<>();
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
    return null;
  }

  @CheckForNull
  @Override
  public String getUrlName() {
    return "defensicsHistory";
  }

  public boolean shouldShowTrendGraph() {
    return runs.size() > 1;
  }

  public void doTrendGraph(StaplerRequest request, StaplerResponse response) throws IOException {
    new TrendGraph(runs).doPng(request, response);
  }

  public void doTrendMap(StaplerRequest request, StaplerResponse reponse) throws IOException {
    new TrendGraph(runs).doMap(request, reponse);
  }
}
