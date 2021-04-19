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
import static org.mockito.Mockito.when;

import com.synopsys.defensics.api.ApiService;
import com.synopsys.defensics.apiserver.model.Run;
import com.synopsys.defensics.apiserver.model.RunState;
import com.synopsys.defensics.apiserver.model.RunVerdict;
import com.synopsys.defensics.apiserver.model.SuiteInstance;
import com.synopsys.defensics.apiserver.model.SuiteRunState;
import com.synopsys.defensics.client.DefensicsRequestException;
import com.synopsys.defensics.jenkins.configuration.InstanceConfiguration;
import hudson.AbortException;
import hudson.FilePath;
import hudson.Launcher;
import java.util.Optional;
import java.util.UUID;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public class FuzzJobRunnerTest {

  @Mock
  private hudson.model.Run jenkinsRun;

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
  private ApiService apiService = Mockito.mock(ApiService.class);

  private boolean saveResultPackage = false;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  public void testSuiteLoadError() throws DefensicsRequestException, InterruptedException {
    FuzzJobRunner fuzzJobRunner = new FuzzJobRunner(apiService);

    String runId = UUID.randomUUID().toString();
    when(instanceConfiguration.getUrl()).thenReturn("http://non.existent.invalid:9999");
    when(defensicsRun.getId()).thenReturn(runId);
    when(apiService.createNewRun()).thenReturn(defensicsRun);
    when(apiService.getConfigurationSuite(runId)).thenReturn(Optional.of(suiteInstance));

    when(suiteInstance.getState()).thenReturn(SuiteRunState.ERROR);
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
}
