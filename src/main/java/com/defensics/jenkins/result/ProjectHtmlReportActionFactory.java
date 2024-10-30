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

import com.defensics.jenkins.result.HtmlReportPublisherTarget.HtmlReportAction;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.AbstractItem;
import hudson.model.Action;
import hudson.model.Job;
import hudson.model.Run;
import java.util.Collection;
import java.util.Collections;
import jenkins.model.TransientActionFactory;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.DoNotUse;

/**
 * This class provides a link to the left sidebar menu for freestyle projects. For pipeline
 * projects, this is handled by WorkflowActionsFactory calling {@link
 * HtmlReportPublisherTarget#getProjectAction(AbstractItem)}.
 */
@Restricted(DoNotUse.class)
@Extension
public class ProjectHtmlReportActionFactory extends TransientActionFactory<Job> {

  @Override
  public Class<Job> type() {
    return Job.class;
  }

  @Override
  @NonNull
  public Collection<? extends Action> createFor(@NonNull Job project) {
    final Run<?,?> lastCompletedBuild = project.getLastCompletedBuild();
    if (lastCompletedBuild != null) {
      return lastCompletedBuild.getActions(HtmlReportAction.class);
    } else {
      return Collections.emptyList();
    }
  }
}
