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

import java.util.List;

/**
 * Root wrapper used in API responses for list of items. Includes actual data in 'data' property
 * and pagination info in 'pagination' property.
 *
 * @param <T> Contained object class
 */
public class ItemArray<T> {
  private List<T> data;
  private Pagination pagination;

  public ItemArray() {}
  public ItemArray(List<T> data) {
    this.data = data;
    this.pagination = new Pagination(0, this.data.size(), this.data.size());
  }

  public ItemArray(List<T> data, long startIndex, long endIndex, long totalCount) {
    this.data = data;
    this.pagination = new Pagination(startIndex, endIndex, totalCount);
  }

  public ItemArray(
      List<T> data,
      Pagination pagination
  ) {
    this.data = data;
    this.pagination = pagination;
  }

  public List<T> getData() {
    return data;
  }

  public Pagination getPagination() {
    return pagination;
  }

  public static class Pagination {
    long startIndex;
    long endIndex;
    long totalCount;

    public Pagination() {
    }

    public Pagination(long startIndex, long endIndex, long totalCount) {
      this.startIndex = startIndex;
      this.endIndex = endIndex;
      this.totalCount = totalCount;
    }

    public long getStartIndex() {
      return startIndex;
    }

    public long getEndIndex() {
      return endIndex;
    }

    public long getTotalCount() {
      return totalCount;
    }
  }
}
