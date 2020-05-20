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
 * Object for failure details.
 */
public class FailureDetail {

  /**
   * Source of the failure.
   */
  private String source;

  /**
   * Failed rounds.
   */
  private int failedRounds;

  /**
   * Details of the failure.
   */
  private String failureDetails;

  /**
   * Default constructor.
   */
  public FailureDetail() {

  }

  /**
   * Constructor.
   *
   * @param source         The failure source instrumentation.
   * @param failedRounds   Number of failed instrumentation rounds.
   * @param failureDetails Details of the failure, optional.
   */
  public FailureDetail(String source, int failedRounds, String failureDetails) {
    this.source = source;
    this.failedRounds = failedRounds;
    this.failureDetails = failureDetails;
  }

  public void setSource(String source) {
    this.source = source;
  }

  public void setFailedRounds(int failedRounds) {
    this.failedRounds = failedRounds;
  }

  public void setFailureDetails(String failureDetails) {
    this.failureDetails = failureDetails;
  }

  public String getSource() {
    return source;
  }

  public int getFailedRounds() {
    return failedRounds;
  }

  public String getFailureDetails() {
    return failureDetails;
  }
}
