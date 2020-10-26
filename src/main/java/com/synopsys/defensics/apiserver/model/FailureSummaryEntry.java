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

package com.synopsys.defensics.apiserver.model;

/**
 * Entry for a single failure summary.
 */
public class FailureSummaryEntry {

  // For nested resource with to-many relation, we must use id-class
  private FailureSummaryEntryId id;

  /**
   * Source instrumentation of the failure summary.
   */
  private String source;

  /**
   * The level of the failure (ERROR/WARNING).
   */
  private FailureLevel level;

  /**
   * The number of failure events.
   */
  private int count;

  private String runId;

  /**
   * The default constructor.
   */
  public FailureSummaryEntry() {
  }

  /**
   * Constructor.
   *
   * @param source The failure source instrumentation.
   * @param count  The number of failure events.
   */
  public FailureSummaryEntry(String id, String runId, String source, FailureLevel level,
      int count) {
    this.id = new FailureSummaryEntryId(id, runId);
    this.source = source;
    this.count = count;
    this.level = level;
    this.runId = runId;
  }

  public FailureSummaryEntryId getId() {
    return id;
  }

  public void setId(FailureSummaryEntryId id) {
    this.id = id;
  }

  public int getCount() {
    return count;
  }

  public void setCount(int count) {
    this.count = count;
  }

  public String getSource() {
    return source;
  }

  public void setSource(String source) {
    this.source = source;
  }

  public FailureLevel getLevel() {
    return level;
  }

  public void setLevel(FailureLevel level) {
    this.level = level;
  }

  public String getRunId() {
    return runId;
  }

  public void setRunId(String runId) {
    this.runId = runId;
  }
}
