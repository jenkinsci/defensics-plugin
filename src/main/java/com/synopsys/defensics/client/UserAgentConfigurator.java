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

package com.synopsys.defensics.client;

import okhttp3.OkHttpClient.Builder;
import okhttp3.Request;

/**
 * Configures OkHttp client User-Agent header.
 */
public class UserAgentConfigurator {

  /**
   * Configures OkHttp client builder to include given User-Agent string in each request.
   *
   * @param builder OkHttpClient builder
   * @param userAgentString User-agent string
   */
  public static void configureUserAgent(Builder builder, final String userAgentString) {
    builder.addInterceptor(chain -> {
      Request original = chain.request();
      Request newReq = original.newBuilder()
          .addHeader("User-Agent", userAgentString)
          .method(original.method(), original.body())
          .build();
      return chain.proceed(newReq);
    });
  }
}
