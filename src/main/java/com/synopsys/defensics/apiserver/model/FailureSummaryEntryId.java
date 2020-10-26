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

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.io.Serializable;
import java.util.Objects;

/**
 * ID class for {@link FailureSummaryEntry}.
 */
@JsonSerialize(using = ToStringSerializer.class)
public class FailureSummaryEntryId implements Serializable {

  /**
   * Due some Crnk API limitations, each resource id must be representable as single String.
   * Apparently official workaround for resources with composite ids is to specify way to
   * combine id components to single string and specify 'JSON serialization' for the id class
   * that produces that single representation.
   * (Note that as far as I can see, this serialization is never actually used from the pov of
   * outside API user or our own repository code, it's just something Crnk internal code uses)
   */
  private static final String ID_STRING_FORMAT = "%s:%s";

  private String id;

  private String runId;

  public FailureSummaryEntryId() {
  }

  public FailureSummaryEntryId(String id, String runId) {
    this.id = id;
    this.runId = runId;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getRunId() {
    return runId;
  }

  public void setRunId(String runId) {
    this.runId = runId;
  }

  /**
   * Deserialize a FailureSummaryEntryId from a JSON-string. See also: ID_STRING_FORMAT.
   * @param idString The JSON string.
   */
  public FailureSummaryEntryId(String idString) {
    if (idString == null || idString.isEmpty()) {
      throw new IllegalArgumentException("idString must have non-empty value");
    }
    String[] parts = idString.split(":");
    if (parts.length != 2) {
      throw new IllegalArgumentException(String.format("Invalid idString specified %s", idString));
    }
    this.runId = parts[0];
    this.id = parts[1];
  }

  @Override
  public String toString() {
    return String.format(ID_STRING_FORMAT, runId, id);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    FailureSummaryEntryId that = (FailureSummaryEntryId) o;
    return Objects.equals(id, that.id)
        && Objects.equals(runId, that.runId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id,
        runId);
  }
}
