/*
 * Copyright © 2023 Synopsys, Inc.
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

package com.synopsys.defensics.jenkins.test.utils;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import org.junit.rules.ExternalResource;

/**
 * Stub HTTP server used in E2E tests as a JUnit test rule.
 */
public class HttpSutRule extends ExternalResource {
  private final HttpServer httpServer;

  /** Default request handler. Returns 200 OK with short response. */
  public static final HttpHandler DEFAULT_HTTP_HANDLER = exchange -> {
    String response = "SUT response";
    exchange.sendResponseHeaders(200, response.length());
    try (OutputStream os = exchange.getResponseBody()) {
      os.write(response.getBytes(StandardCharsets.UTF_8));
    }
  };

  /**
   * Constructor.
   *
   * @param port        Listen port
   * @param httpHandler HTTP Handler.
   */
  public HttpSutRule(int port, HttpHandler httpHandler) {
    try {
      httpServer = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
      httpServer.createContext("/", httpHandler);
    } catch (IOException e) {
      throw new RuntimeException("Couldn't create HTTP server", e);
    }
  }

  @Override
  protected void before() throws Throwable {
    log("Starting HTTP SUT at " + getAddress());
    httpServer.start();
  }

  @Override
  protected void after() {
    log("Stopping HTTP SUT");
    httpServer.stop(1);
  }

  public HttpSutRule(int port) {
    this(port, DEFAULT_HTTP_HANDLER);
  }

  public URI getAddress() {
    return URI.create(
        String.format(
            "http://%s:%d",
            httpServer.getAddress().getHostName(),
            httpServer.getAddress().getPort()
        )
    );
  }

  private void log(String message) {
    System.out.println("[SUT] " + message);
  }
}
