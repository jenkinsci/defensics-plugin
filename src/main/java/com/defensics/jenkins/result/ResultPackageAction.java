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

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.FilePath;
import hudson.model.DirectoryBrowserSupport;
import hudson.model.Run;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import jenkins.model.RunAction2;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

public class ResultPackageAction implements RunAction2 {

  public static final String URL_NAME = "defensics";
  private transient Run<?, ?> run;
  private final List<String> resultPackages = new ArrayList<>();
  /**
   * Contains the link description. Key is the resultPackage string stored into resultPackages list.
   */
  private final Map<String, String> descriptions = new HashMap<>();

  public ResultPackageAction(String resultFile) {
    resultPackages.add(resultFile);
    descriptions.put(resultFile, "");
  }

  public ResultPackageAction(String resultFile, String description) {
    resultPackages.add(resultFile);
    descriptions.put(resultFile, description);
  }

  public List<String> getResultPackages() {
    return resultPackages;
  }

  public void addResultPackage(String resultPackage, String description) {
    resultPackages.add(resultPackage);
    descriptions.put(resultPackage, description);
  }

  public String getDescription(String resultFile) {
    if (resultFile == null || descriptions == null) {
      return "";
    }

    return descriptions.getOrDefault(resultFile, "");
  }

  @Override
  public void onAttached(Run<?, ?> r) {
    run = r;
  }

  @Override
  public void onLoad(Run<?, ?> r) {
    run = r;
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

  @NonNull
  @Override
  public String getUrlName() {
    return URL_NAME;
  }

  public DirectoryBrowserSupport doDynamic(StaplerRequest req, StaplerResponse res) {
    return new DirectoryBrowserSupport(this, new FilePath(run.getRootDir()).child(getUrlName()),
        null, null, false);
  }
}
