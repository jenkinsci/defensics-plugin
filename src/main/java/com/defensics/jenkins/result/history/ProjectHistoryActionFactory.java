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

package com.defensics.jenkins.result.history;

import hudson.Extension;
import hudson.model.Action;
import hudson.model.Job;
import java.util.Collection;
import java.util.Collections;
import edu.umd.cs.findbugs.annotations.NonNull;
import jenkins.model.TransientActionFactory;

@Extension
public class ProjectHistoryActionFactory extends TransientActionFactory<Job> {

  @Override
  public Class<Job> type() {
    return Job.class;
  }

  @NonNull
  @Override
  public Collection<? extends Action> createFor(@NonNull Job target) {
    return Collections.singleton(new ProjectHistoryAction(target));
  }
}
