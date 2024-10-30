/*
 * Copyright © 2020-2023 Synopsys, Inc.
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

package com.defensics.client;

import java.net.Socket;
import java.net.http.HttpClient;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509ExtendedTrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * Helper class to configure HTTP client to use looser TLS configuration in case Jenkins JVM
 * or API Server cannot be configured to trust each other's certificates. Should be used as a last
 * resort.
 */
public class UnsafeTlsConfigurator {

  /**
   * Configures given HttpBuilder to use unsafe TLS: Trust all certificates and endpoints.
   * This allows the client to communicate with self-signed or custom certificates that are not in
   * root CA. Should be used as a last resort if Jenkins JVM or server cannot be configured to have
   * known (CA) certificates.
   *
   * @param clientBuilder builder of Http client
   */
  public static void configureUnsafeTlsHttpClient(HttpClient.Builder clientBuilder) {
    final X509TrustManager x509TrustManager = new X509ExtendedTrustManager() {
      @Override
      public void checkClientTrusted(
          X509Certificate[] chain,
          String authType,
          Socket socket
      ) {}

      @Override
      public void checkServerTrusted(
          X509Certificate[] chain,
          String authType,
          Socket socket
      ) {}

      @Override
      public void checkClientTrusted(
          X509Certificate[] chain,
          String authType,
          SSLEngine engine
      ) {}

      @Override
      public void checkServerTrusted(
          X509Certificate[] chain,
          String authType,
          SSLEngine engine
      ) {}

      @Override
      public void checkClientTrusted(X509Certificate[] chain, String authType) {}

      @Override
      public void checkServerTrusted(
          X509Certificate[] chain,
          String authType
      ) {}

      @Override
      public X509Certificate[] getAcceptedIssuers() {
        return new X509Certificate[]{};
      }
    };
    final TrustManager[] trustAllCerts = new TrustManager[]{
        x509TrustManager
    };
    try {
      final SSLContext sslContext = SSLContext.getInstance("TLS");
      sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
      clientBuilder.sslContext(sslContext);
    } catch (NoSuchAlgorithmException | KeyManagementException e) {
      throw new IllegalStateException(
          "Could not configure looser TLS configuration for the HTTP client",
          e
      );
    }
  }
}
