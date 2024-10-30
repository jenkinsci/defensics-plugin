/*
 * Copyright © 2020-2021 Synopsys, Inc.
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

package com.defensics.apiserver.model;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;

public enum RunState {
    IDLE,
    LOADING,
    LOADED,
    STARTING,
    RUNNING,
    PAUSING,
    PAUSED,
    STOPPING,
    COMPLETED,
    ERROR,
    FATAL,
    UNLOADING,
    // Unknown enum values are mapped to this on client side (Allows client
    // to handle cases where client and server version don't match and server
    // has added some new values to the enum)
    @JsonEnumDefaultValue
    UNKNOWN_VALUE
}
