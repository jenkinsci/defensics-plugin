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

package com.synopsys.defensics.jenkins;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.synopsys.defensics.api.ApiService;
import com.synopsys.defensics.apiserver.model.Run;
import com.synopsys.defensics.apiserver.model.RunState;
import com.synopsys.defensics.apiserver.model.RunVerdict;
import com.synopsys.defensics.apiserver.model.SuiteInstance;
import com.synopsys.defensics.client.DefensicsRequestException;
import com.synopsys.defensics.jenkins.configuration.InstanceConfiguration;
import com.synopsys.defensics.jenkins.result.ResultPublisher;
import hudson.AbortException;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Plugin;
import hudson.PluginManager;
import hudson.PluginWrapper;
import hudson.model.Result;
import hudson.util.VersionNumber;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

/**
 * Unit tests for FuzzJobRunner, allows to test corner cases and other more randomly occuring
 * scenarios. Uses lots of mocks, so modeling the full real behaviour is quite tedious so tests
 * focus mostly on certain aspect to be tested.
 */
public class FuzzJobRunnerTest {
  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Mock
  private hudson.model.Run<?, ?> jenkinsRun;

  @Mock
  private FilePath workspace;

  @Mock
  private Launcher launcher;

  @Mock
  private Logger logger;

  @Mock
  private FilePath testplan;

  @Mock
  private InstanceConfiguration instanceConfiguration;

  @Mock
  private Run defensicsRun;

  @Mock
  private SuiteInstance suiteInstance;

  @Mock
  private ApiService apiService;

  @Mock
  private Plugin htmlPublisher;

  @Mock
  private ResultPublisher resultPublisher;

  @Mock
  private PollingIntervals pollingIntervals;

  @Mock
  private PluginManager pluginManager;

  private boolean saveResultPackage = false;

  private static final String RUN_ID = "5adcf4cc-7a86-4f3c-8fa4-ba316ce686c0";

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.openMocks(this);
  }

  /**
   * Test that main code path can run with mock services. Doesn't try to model state transitions.
   */
  @Test
  public void testRun_mocksWork() throws Exception {
    final FuzzJobRunner fuzzJobRunner = createFuzzJobRunnerWithMockServices();
    setupMocks();

    when(suiteInstance.getState()).thenReturn(RunState.LOADED);
    when(defensicsRun.getState()).thenReturn(RunState.COMPLETED);
    when(defensicsRun.getVerdict()).thenReturn(RunVerdict.PASS);

    fuzzJobRunner.run(
        jenkinsRun,
        workspace,
        launcher,
        logger,
        testplan,
        "",
        instanceConfiguration,
        saveResultPackage
    );

    verify(jenkinsRun).setResult(Result.SUCCESS);
  }

  /**
   * Check that error in suite loading results in job abort.
   */
  @Test
  public void testSuiteLoadError() throws Exception {
    final FuzzJobRunner fuzzJobRunner = createFuzzJobRunnerWithMockServices();
    setupMocks();

    when(suiteInstance.getState()).thenReturn(RunState.ERROR);
    when(suiteInstance.getError()).thenReturn("Suite error XXX");

    AbortException exception = Assert.assertThrows(
        AbortException.class,
        () -> fuzzJobRunner.run(
            jenkinsRun,
            workspace,
            launcher,
            logger,
            testplan,
            "",
            instanceConfiguration,
            saveResultPackage
        )
    );
    assertThat(exception.getMessage(), is("Couldn't load suite, error: Suite error XXX"));
  }

  /**
   * Check that fatal error in suite loading results in job abort.
   */
  @Test
  public void testSuiteLoadFatalError() throws Exception {
    final FuzzJobRunner fuzzJobRunner = createFuzzJobRunnerWithMockServices();
    setupMocks();

    when(suiteInstance.getState()).thenReturn(RunState.FATAL);
    when(suiteInstance.getError()).thenReturn("Suite error XXX");

    AbortException exception = Assert.assertThrows(
        AbortException.class,
        () -> fuzzJobRunner.run(
            jenkinsRun,
            workspace,
            launcher,
            logger,
            testplan,
            "",
            instanceConfiguration,
            saveResultPackage
        )
    );
    assertThat(exception.getMessage(), is("Couldn't load suite, error: Suite error XXX"));
  }

  private void setupMocks() throws DefensicsRequestException, InterruptedException, IOException {
    when(instanceConfiguration.getUrl()).thenReturn("http://non.existent.invalid:9999");
    when(defensicsRun.getId()).thenReturn(RUN_ID);

    // Mock HTML Publisher to be recent version
    final PluginWrapper pluginWrapper = Mockito.mock(PluginWrapper.class);
    when(pluginWrapper.getVersionNumber()).thenReturn(new VersionNumber("1.22"));
    when(htmlPublisher.getWrapper()).thenReturn(pluginWrapper);

    // Make tests go fast. Zero should be OK since it's passed to Thread.sleep()
    when(pollingIntervals.getRunPollingInterval()).thenReturn(0);
    when(pollingIntervals.getTestplanLoadingInterval()).thenReturn(0);
    when(pollingIntervals.getInitialRunPollingInterval()).thenReturn(0);

    // Mock API calls
    when(apiService.createNewRun()).thenReturn(defensicsRun);
    when(apiService.getConfigurationSuite(RUN_ID)).thenReturn(Optional.of(suiteInstance));
    when(apiService.getRun(RUN_ID)).thenReturn(defensicsRun);

    // Setup HTML report download
    when(workspace.createTempDir(any(), any()))
        .thenReturn(new FilePath(temporaryFolder.newFolder()));

    doAnswer(invocation -> {
      final FilePath filePath = invocation.getArgument(1);
      Files.write(
          Paths.get(filePath.absolutize().getRemote(), "report.html"),
          "<html/>".getBytes(StandardCharsets.UTF_8)
      );
      return null;
    }).when(apiService).saveResults(any(Run.class), any(FilePath.class));
  }

  /**
   * Create FuzzJobRunner but set up its related services to be mock classes for unit testing.
   * If this is getting clumsy, the related objects could be passed in constructor or by using
   * Supplier lambdas.
   *
   * @return FuzzJobRunner with default mocks for unit tests
   */
  @NotNull
  private FuzzJobRunner createFuzzJobRunnerWithMockServices() {
    return new FuzzJobRunner() {
      @Override
      Plugin getHtmlPublisher() {
        return htmlPublisher;
      }

      @Override
      ResultPublisher getResultPublisher() {
        return resultPublisher;
      }

      @Override
      PluginManager getJenkinsPluginManager() {
        return pluginManager;
      }

      @Override
      @NotNull PollingIntervals getPollingIntervals(
          hudson.model.Run<?, ?> jenkinsRun, Launcher launcher, Logger logger
      ) {
        return pollingIntervals;
      }

      @Override
      ApiService getApiService(
          InstanceConfiguration instanceConfiguration, String authenticationToken
      ) {
        return apiService;
      }
    };
  }
}
