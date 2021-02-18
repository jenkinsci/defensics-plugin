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

package com.synopsys.defensics.jenkins.util;

import com.synopsys.defensics.apiserver.model.FailureSummaryEntry;
import com.synopsys.defensics.apiserver.model.Run;
import com.synopsys.defensics.jenkins.configuration.PluginConfiguration;
import hudson.Plugin;
import hudson.PluginWrapper;
import java.util.Optional;
import jenkins.model.Jenkins;

public class DefensicsUtils {

  /**
   * Counts the total number of failures for a run from {@code FailureSummaryEntry} objects.
   *
   * @param run Defensics run object
   * @return total count of all failures in the run
   */
  public static int countRunFailures(Run run) {
    return run.getFailureSummary().stream().mapToInt(FailureSummaryEntry::getCount).sum();
  }

  /**
   * Creates User-agent header value, e.g. Defensics-Jenkins-Plugin/1.2.3. or if version information
   * was not available, only product name, e.g. Defensics-Jenkins-Plugin.
   *
   * @return User-agent header value.
   */
  public String createUserAgentString() {
    final StringBuilder userAgentBuilder = new StringBuilder();
    userAgentBuilder.append("Defensics-Jenkins-Plugin");

    getPluginVersion(PluginConfiguration.DEFENSICS_PLUGIN_NAME)
        .ifPresent(version -> {
          userAgentBuilder.append("/").append(version);
        });

    return userAgentBuilder.toString();
  }

  /**
   * Gets plugin's version.
   *
   * @param pluginName Short name of the plugin
   * @return Version information wrapped in Optional, or empty optional if version was not available
   */
  public Optional<String> getPluginVersion(String pluginName) {
    return Optional.ofNullable(Jenkins.getInstanceOrNull())
        .map(e -> e.getPlugin(pluginName))
        .map(Plugin::getWrapper)
        .map(PluginWrapper::getVersion);
  }
}
