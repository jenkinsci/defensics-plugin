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

package com.defensics.jenkins.test.utils;

public class Constants {
  public static final String NAME = "My Defensics";
  public static final String URL = "http://my.defensics";
  public static final String LOCAL_URL = "http://127.0.0.1:1080";
  public static final boolean CERTIFICATE_VALIDATION_DISABLED = true; // True disables validation
  public static final boolean CERTIFICATE_VALIDATION_ENABLED = false; // False enables validation
  public static final String CREDENTIALS_ID = "test-credentials";
  public static final String SETTING_FILE_PATH = "http.testplan";
  public static final String PIPELINE_ERROR_TEXT = "Pipeline found error";
  public static final String CONFIGURATION_OVERRIDES = "--test.setting=thisIsFakeSetting";
}
