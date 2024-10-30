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

package com.defensics.jenkins.configuration;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.util.ListBoxModel;
import java.util.ArrayList;
import java.util.List;
import jenkins.model.GlobalConfiguration;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;

@Extension
public class PluginConfiguration extends GlobalConfiguration {
  /** Plugin short name. Used e.g. when fetching plugin version number. */
  public static final String DEFENSICS_PLUGIN_NAME = "defensics";
  /** Display name for both configuration and steps. */
  public static final String DISPLAY_NAME = "Defensics fuzz test";

  private final transient InstanceConfigurationValidator instanceConfigurationValidator =
      new InstanceConfigurationValidator();
  private List<InstanceConfiguration> defensicsInstances = new ArrayList<>();

  public PluginConfiguration() {
    super.load();
  }

  public List<InstanceConfiguration> getDefensicsInstances() {
    return defensicsInstances;
  }

  public void setDefensicsInstances(
      List<InstanceConfiguration> defensicsInstances) {
    this.defensicsInstances = defensicsInstances;
  }

  @Override
  public boolean configure(StaplerRequest req, JSONObject formDataJson) throws FormException {
    List<InstanceConfiguration> defensicsInstances = req.bindJSONToList(
        InstanceConfiguration.class,
        formDataJson.get("defensicsInstances"));

    instanceConfigurationValidator.validate(defensicsInstances);

    setDefensicsInstances(defensicsInstances);
    save();
    return true;
  }

  @NonNull
  @Override
  public String getDisplayName() {
    return DISPLAY_NAME;
  }

  /**
   * Get the options for selecting Defensics instance for a job.
   *
   * @return The items to present in the select.
   */
  public ListBoxModel doFillDefensicsInstanceNameItems() {
    ListBoxModel items = new ListBoxModel();
    for (InstanceConfiguration instanceConfiguration : getDefensicsInstances()) {
      items.add(
          instanceConfiguration.getDisplayName(),
          instanceConfiguration.getName());
    }
    return items;
  }
}
