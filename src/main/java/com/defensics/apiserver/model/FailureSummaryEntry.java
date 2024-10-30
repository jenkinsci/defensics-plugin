/*
 * Copyright © 2020-2021 Synopsys, Inc.
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

package com.defensics.apiserver.model;

/**
 * Entry for a single failure summary.
 */
public class FailureSummaryEntry {

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
  public FailureSummaryEntry(String source, FailureLevel level,
      int count) {
    this.source = source;
    this.count = count;
    this.level = level;
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
}
