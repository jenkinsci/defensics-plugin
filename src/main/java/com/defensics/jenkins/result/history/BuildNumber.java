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

import java.util.Objects;

/**
 * This class represents Jenkins build numbers for trend chart dataset. It is needed so build
 * numbers can be compared as integers but presented as strings with additional # in the beginning.
 * This class was created to fix a bug: the builds were in the wrong order in trend chart, i.e. 1,
 * 10, 2, 3, etc.
 */
public class BuildNumber implements Comparable<BuildNumber> {

  private final int buildNumber;

  public BuildNumber(int buildNumber) {
    this.buildNumber = buildNumber;
  }

  public int getBuildNumber() {
    return buildNumber;
  }

  @Override
  public final boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof BuildNumber)) {
      return false;
    }
    BuildNumber that = (BuildNumber) o;
    return Objects.equals(buildNumber, that.getBuildNumber());
  }

  @Override
  public final int hashCode() {
    return Objects.hash(buildNumber);
  }

  @Override
  public int compareTo(BuildNumber other) {
    if (this.buildNumber > other.getBuildNumber()) {
      return 1;
    } else if (this.buildNumber < other.getBuildNumber()) {
      return -1;
    } else {
      return 0;
    }
  }

  @Override
  public String toString() {
    return "#" + buildNumber;
  }
}
