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

import junitx.extensions.ComparabilityTestCase;

public class BuildNumberComparabilityTestCase extends ComparabilityTestCase {

  public BuildNumberComparabilityTestCase(String name) {
    super(name);
  }

  @Override
  protected Comparable createLessInstance() throws Exception {
    return new BuildNumber(-1);
  }

  @Override
  protected Comparable createEqualInstance() throws Exception {
    return new BuildNumber(234);
  }

  @Override
  protected Comparable createGreaterInstance() throws Exception {
    return new BuildNumber(235);
  }
}
