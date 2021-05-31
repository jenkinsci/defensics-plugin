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
import static org.mockito.internal.verification.VerificationModeFactory.times;

import com.synopsys.defensics.api.ApiService;
import com.synopsys.defensics.apiserver.client.DefensicsApiClient.DefensicsClientException;
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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.Timeout;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.stubbing.Answer;

/**
 * Unit tests for FuzzJobRunner, allows to test corner cases and other more randomly occuring
 * scenarios. Uses lots of mocks, so modeling the full real behaviour is quite tedious so tests
 * focus mostly on certain aspect to be tested. FuzzJobRunner method visibility could be opened
 * a bit to allow method-level unit testing.
 */
public class FuzzJobRunnerTest {
  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Rule
  public Timeout timeout = new Timeout(10, TimeUnit.SECONDS);

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

  @Test
  public void testInterruption_RunningRunStopped()
      throws DefensicsRequestException, IOException, InterruptedException {
    final FuzzJobRunner fuzzJobRunner = createFuzzJobRunnerWithMockServices();
    setupMocks();

    final AtomicReference<RunState> runState = new AtomicReference<>(RunState.RUNNING);

    when(suiteInstance.getState()).thenAnswer(invocation -> runState.get());

    // Stop run should succeed and set run to completed
    doAnswer(invocation -> {
      runState.set(RunState.COMPLETED);
      return null;
    }).when(apiService).stopRun(RUN_ID);

    // Cause job interrupt in 5th poll
    final AtomicInteger counter = new AtomicInteger();
    when(defensicsRun.getState()).thenAnswer((Answer<?>) invocation -> {
      if (counter.incrementAndGet() == 5) {
        throw new InterruptedException("Job interrupted");
      }
      return runState.get();
    });

    Assert.assertThrows(
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

    verify(apiService).stopRun(RUN_ID);
    verify(jenkinsRun).setResult(Result.ABORTED);
  }


  /**
   * Check that retry mechanism on run stop works. Some states do not yet allow stopping and
   * returns 409, so interrupt handler now retries stop request if first one got 409.
   * Simulate a case where run was in STARTING state and moved to RUNNING state.
   */
  @Test
  public void testInterruption_StartingRunStopped_eventually()
      throws DefensicsRequestException, IOException, InterruptedException {
    final FuzzJobRunner fuzzJobRunner = createFuzzJobRunnerWithMockServices();
    setupMocks();

    final AtomicReference<RunState> runState = new AtomicReference<>(RunState.STARTING);

    when(suiteInstance.getState()).thenAnswer(invocation -> runState.get());

    // Make run state changes happen during stopRun calls.
    doAnswer(invocation -> {
      if (runState.get().equals(RunState.STARTING)) {
        // Starting run gets 409, but let's move run state internally to running.
        // NOTE: These nested exceptions are bit cumbersome here.
        runState.set(RunState.RUNNING);
        throw new DefensicsRequestException(
            "Plugin exception",
            new DefensicsClientException("409 Conflict API client exception")
        );
      }

      // Second stop for RUNNING run should succeed
      if (runState.get().equals(RunState.RUNNING)) {
        runState.set(RunState.COMPLETED);
      }
      return null;
    }).when(apiService).stopRun(RUN_ID);

    // Cause job interrupt in 5th poll
    final AtomicInteger counter = new AtomicInteger();
    when(defensicsRun.getState()).thenAnswer((Answer<?>) invocation -> {
      if (counter.incrementAndGet() == 5) {
        throw new InterruptedException("Job interrupted");
      }
      return runState.get();
    });

    Assert.assertThrows(
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

    // STARTING state gets 409, RUNNING state gets 200
    verify(apiService, times(2)).stopRun(RUN_ID);
    verify(jenkinsRun).setResult(Result.ABORTED);
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
