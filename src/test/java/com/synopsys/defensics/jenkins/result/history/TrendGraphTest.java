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

package com.synopsys.defensics.jenkins.result.history;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.synopsys.defensics.jenkins.result.BuildResultAction;
import hudson.model.Run;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.jfree.chart.JFreeChart;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

public class TrendGraphTest {

  private List<Run<?,?>> runs;
  private TrendGraph trendGraph;

  @Rule
  public Timeout timeout = new Timeout(20, TimeUnit.SECONDS);

  @Before
  public void setup() {
    runs = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      Run<?,?> run = mock(Run.class);
      when(run.getTime()).thenReturn(new Date());
      when(run.getNumber()).thenReturn(i + 1);
      BuildResultAction buildResultAction = mock(BuildResultAction.class);
      if (i % 2 == 0) {
        when(run.getAction(BuildResultAction.class)).thenReturn(buildResultAction);
      } else {
        when(run.getAction(BuildResultAction.class)).thenReturn(null);
      }
      when(buildResultAction.getReportUrl()).thenReturn("report-url");
      when(buildResultAction.getFailureCount()).thenReturn(1234567890L + i);
      runs.add(run);
    }
    trendGraph = new TrendGraph(runs);
  }

  @Test
  public void testValueAxisLabel() {
    JFreeChart chart = trendGraph.createGraph();

    assertThat(chart.getCategoryPlot().getRangeAxis().getLabel(), is(equalTo("Count")));
  }

  @Test
  public void testDatasetSize() {
    CategoryDataset dataset = trendGraph.createGraph().getCategoryPlot().getDataset();

    assertThat(dataset.getColumnCount(), is(equalTo(10)));
    assertThat(dataset.getRowCount(), is(equalTo(1)));
  }

  @Test
  public void testDatasetRowKey() {
    CategoryDataset dataset = trendGraph.createGraph().getCategoryPlot().getDataset();

    assertThat(dataset.getRowKey(0), is(equalTo("Failures")));
  }

  @Test
  public void testDatasetColumnKeys() {
    CategoryDataset dataset = trendGraph.createGraph().getCategoryPlot().getDataset();

    assertThat(dataset.getColumnKey(0).toString(), is(equalTo("#1")));
    assertThat(dataset.getColumnKey(1).toString(), is(equalTo("#2")));
    assertThat(dataset.getColumnKey(2).toString(), is(equalTo("#3")));
    assertThat(dataset.getColumnKey(3).toString(), is(equalTo("#4")));
    assertThat(dataset.getColumnKey(4).toString(), is(equalTo("#5")));
    assertThat(dataset.getColumnKey(5).toString(), is(equalTo("#6")));
    assertThat(dataset.getColumnKey(6).toString(), is(equalTo("#7")));
    assertThat(dataset.getColumnKey(7).toString(), is(equalTo("#8")));
    assertThat(dataset.getColumnKey(8).toString(), is(equalTo("#9")));
    assertThat(dataset.getColumnKey(9).toString(), is(equalTo("#10")));
  }

  @Test
  public void testDatasetValues() {
    CategoryDataset dataset = trendGraph.createGraph().getCategoryPlot().getDataset();

    assertThat(dataset.getValue(0, 0).toString(), is(equalTo("1234567890")));
    assertThat(dataset.getValue(0, 1).toString(), is(equalTo("0")));
    assertThat(dataset.getValue(0, 2).toString(), is(equalTo("1234567892")));
    assertThat(dataset.getValue(0, 3).toString(), is(equalTo("0")));
    assertThat(dataset.getValue(0, 4).toString(), is(equalTo("1234567894")));
    assertThat(dataset.getValue(0, 5).toString(), is(equalTo("0")));
    assertThat(dataset.getValue(0, 6).toString(), is(equalTo("1234567896")));
    assertThat(dataset.getValue(0, 7).toString(), is(equalTo("0")));
    assertThat(dataset.getValue(0, 8).toString(), is(equalTo("1234567898")));
    assertThat(dataset.getValue(0, 9).toString(), is(equalTo("0")));
  }

  @Test
  public void testUrlGeneration() {
    DefaultCategoryDataset dataset = new DefaultCategoryDataset();
    dataset.addValue(0, Integer.valueOf(0), "#0");
    dataset.addValue(3, Integer.valueOf(0), "#1");
    dataset.addValue(3, Integer.valueOf(0), "#2");
    JFreeChart chart = trendGraph.createGraph();

    assertThat(
        chart.getCategoryPlot().getRenderer().getBaseItemURLGenerator().generateURL(
            dataset,
            0,
            2),
        is(equalTo("2/report-url")));
  }

  @Test
  public void testUrlGenerationDefensicsNotRun() {
    TrendGraph trendGraph = new TrendGraph(runs);
    JFreeChart chart = trendGraph.createGraph();
    DefaultCategoryDataset dataset = new DefaultCategoryDataset();
    dataset.addValue(0, Integer.valueOf(0), "#0");
    dataset.addValue(3, Integer.valueOf(0), "#1");

    assertThat(
        chart.getCategoryPlot().getRenderer().getBaseItemURLGenerator().generateURL(
            dataset,
            0,
            1),
        is(nullValue()));
  }

  @Test
  public void testTooltipGeneration() {
    JFreeChart chart = trendGraph.createGraph();
    DefaultCategoryDataset dataset = new DefaultCategoryDataset();
    dataset.addValue(0, Integer.valueOf(0), "#0");
    dataset.addValue(3, Integer.valueOf(0), "#1");

    assertThat(
        chart.getCategoryPlot().getRenderer().getBaseToolTipGenerator().generateToolTip(
            dataset,
            0,
            1),
        is(equalTo("#1: 1234567898 failures")));
  }

  @Test
  public void testTooltipGenerationDefensicsNotRun() {
    DefaultCategoryDataset dataset = new DefaultCategoryDataset();
    dataset.addValue(0, Integer.valueOf(0), "#0");
    dataset.addValue(3, Integer.valueOf(0), "#1");
    dataset.addValue(3, Integer.valueOf(0), "#2");
    JFreeChart chart = trendGraph.createGraph();

    assertThat(
        chart.getCategoryPlot().getRenderer().getBaseToolTipGenerator().generateToolTip(
            dataset,
            0,
            2),
        is(equalTo("#2: Defensics not run")));
  }
}
