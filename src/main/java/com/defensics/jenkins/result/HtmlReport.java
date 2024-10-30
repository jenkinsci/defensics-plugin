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

package com.defensics.jenkins.result;

import hudson.FilePath;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.channels.ClosedByInterruptException;
import org.apache.commons.lang.exception.ExceptionUtils;

public class HtmlReport {

  private final FilePath reportFolder;
  private final FilePath reportFile;
  private final FilePath reportCssFile;
  private final String reportTitle;

  /**
   * Constructor.
   *
   * @param reportFolder   The reportFolder where the report file is/should be.
   * @param jobId       The id of the job. Used in the report filename.
   * @param reportTitle The title of this report. It is used as the tab title when viewing the
   *                    report.
   * @throws IOException          If creating missing folders fails.
   * @throws InterruptedException If creating missing folders is interrupted.
   */
  public HtmlReport(FilePath reportFolder, String jobId, String reportTitle)
      throws IOException, InterruptedException {
    this.reportFolder = reportFolder;

    reportFile = this.reportFolder.child("report-" + jobId + ".html");
    FilePath originalReportFile = this.reportFolder.child("report.html");

    try {
      reportFile.copyFrom(originalReportFile);
    } catch (Exception e) {
      // NOTE: The copyFrom can wrap Interrupted exceptions into IOExceptions so
      // throw interrupted exception in that case to match previous code.
      final Exception cause = (Exception) e.getCause();

      // Check if there was interruption, and if yes, map to InterruptedException
      if (cause != null
          && (ExceptionUtils.indexOfType(cause, InterruptedIOException.class) >= 0
          || ExceptionUtils.indexOfType(cause, ClosedByInterruptException.class) >= 0
          || ExceptionUtils.indexOfType(cause, InterruptedException.class) >= 0)
      ) {
        throw new InterruptedException(e.getCause().getMessage());
      } else {
        throw e;
      }
    }
    originalReportFile.delete();

    reportCssFile = this.reportFolder.child("style.css");
    this.reportTitle = reportTitle;
  }

  /**
   * Get the path of the result directory, relative to workspace.
   *
   * @return Path to result directory. Empty string if reports reside in the root folder of
   *         workspace.
   */
  public String getResultDirectory() {
    if (reportFile == null) {
      return null;
    }

    // Handle spotbugs NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE issue for @CheckForNull annotated
    // method. In theory multiple getParent() calls may not necessarily return the same
    // value, so use final local variable here to lock the value.
    // https://github.com/spotbugs/spotbugs/issues/782
    final FilePath parent = reportFile.getParent();
    if (parent == null) {
      return null;
    }

    return parent.getRemote();
  }

  /**
   * Check if this local HTML report file exists.
   *
   * @return True if a file exists, otherwise false.
   * @throws IOException          If checking existence fails.
   * @throws InterruptedException If checking existence is interrupted.
   */
  public boolean exists() throws IOException, InterruptedException {
    return reportFile.exists();
  }

  /**
   * Get the filename of this report.
   *
   * @return Filename without path.
   */
  public String getFileName() {
    return reportFile.getName();
  }

  /**
   * Get the title of this report.
   *
   * @return Report title.
   */
  public String getReportTitle() {
    return reportTitle;
  }

  /**
   * Delete HTML report file.
   *
   * @throws IOException          If deleting fails.
   * @throws InterruptedException If deleting is interrupted
   */
  public void delete() throws IOException, InterruptedException {
    reportFile.delete();
    reportCssFile.delete();
    reportFolder.delete();
  }
}
