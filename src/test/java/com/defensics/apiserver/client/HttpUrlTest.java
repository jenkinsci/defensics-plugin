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

package com.defensics.apiserver.client;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;

import java.net.URI;
import org.junit.Test;

/**
 * Unit tests for HttpURL and its builder class.
 */
public class HttpUrlTest {
  @Test
  public void testHttpUrlBuilder() {
    URI baseUri = URI.create("http://127.0.0.1:1234");
    HttpUrl.Builder builder = new HttpUrl.Builder(baseUri);

    assertThat(builder.build().getUri(), is(baseUri));

    builder.addPathSegment("foo");
    assertThat(builder.build().getUri(), is(URI.create("http://127.0.0.1:1234/foo")));

    builder.addPathSegment("bar");
    assertThat(builder.build().getUri(), is(URI.create("http://127.0.0.1:1234/foo/bar")));

    builder.addQueryParameter("first", "val1");
    assertThat(
        builder.build().getUri(),
        is(URI.create("http://127.0.0.1:1234/foo/bar?first=val1"))
    );

    builder.addQueryParameter("second", "val2");
    assertThat(
        builder.build().getUri(),
        is(URI.create("http://127.0.0.1:1234/foo/bar?first=val1&second=val2"))
    );
  }

  @Test
  public void testHttpUrlBuilder_pathEncoding() {
    URI baseUri = URI.create("http://127.0.0.1:1234");
    HttpUrl.Builder builder = new HttpUrl.Builder(baseUri);

    assertThat(builder.build().getUri(), is(baseUri));

    builder.addPathSegment("foo bar");
    assertThat(builder.build().getUri(), is(URI.create("http://127.0.0.1:1234/foo%20bar")));

    // Do not allow multiple parts in addPathSegment to reduce risk of path traversal
    assertThrows(IllegalArgumentException.class, () -> builder.addPathSegment("first/second"));

    // Do not allow up-traversal in addPathSegment to reduce risk of path traversal
    assertThrows(IllegalArgumentException.class, () -> builder.addPathSegment(".."));
  }

  @Test
  public void testHttpUrlBuilder_queryEncoding() {
    URI baseUri = URI.create("http://127.0.0.1:1234");
    HttpUrl.Builder builder = new HttpUrl.Builder(baseUri);

    assertThat(builder.build().getUri(), is(baseUri));

    builder.addQueryParameter("first", "a&b");
    assertThat(builder.build().getUri(), is(URI.create("http://127.0.0.1:1234?first=a%26b")));

    builder.addQueryParameter("second", null);
    assertThat(builder.build().getUri(), is(URI.create("http://127.0.0.1:1234?first=a%26b&second")));
  }
}
