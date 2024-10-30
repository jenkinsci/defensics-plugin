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

package com.defensics.jenkins.result.history;

import com.defensics.jenkins.result.BuildResultAction;
import hudson.model.Run;
import hudson.util.DataSetBuilder;
import hudson.util.Graph;
import hudson.util.ShiftedCategoryAxis;
import java.util.List;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.AreaRenderer;
import org.jfree.chart.urls.CategoryURLGenerator;
import org.jfree.data.category.CategoryDataset;

/**
 * This class handles the setup and drawing of a failure trend graph.
 */
public class TrendGraph extends Graph {

  private final List<Run<?,?>> runs;

  /**
   * Constructor.
   *
   * @param runs List of runs to use for the graph. A run is not required to contain Defenscis
   *             steps, for those runs 0 is shown in the chart.
   */
  TrendGraph(List<Run<?,?>> runs) {
    super(runs.get(0).getTime().getTime(), 500, 200);
    this.runs = runs;
  }

  @Override
  protected JFreeChart createGraph() {
    JFreeChart chart = ChartFactory.createAreaChart(
        null,
        null,
        "Count",
        getDataSetBuilder().build(),
        PlotOrientation.VERTICAL,
        false,
        true,
        true);

    final CategoryPlot plot = chart.getCategoryPlot();
    setUrlFormat(plot);
    setTooltipFormat(plot);
    configureXAxis(plot);
    configureYAxis(plot);

    return chart;
  }

  private DataSetBuilder<String, BuildNumber> getDataSetBuilder() {
    DataSetBuilder<String, BuildNumber> dataSetBuilder = new DataSetBuilder<>();

    for (Run<?,?> run : runs) {
      BuildResultAction buildResultAction = run.getAction(BuildResultAction.class);
      if (buildResultAction == null) {
        dataSetBuilder.add(0, "Failures", new BuildNumber(run.getNumber()));
      } else {
        dataSetBuilder.add(
            buildResultAction.getFailureCount(),
            "Failures",
            new BuildNumber(run.getNumber()));
      }
    }
    return dataSetBuilder;
  }

  private void setUrlFormat(CategoryPlot plot) {
    AreaRenderer renderer = new AreaRenderer();
    CategoryURLGenerator urlGenerator = (CategoryDataset dataset, int series, int category) -> {
      if (category > 0) {
        BuildResultAction action =
            runs.get(runs.size() - category).getAction(BuildResultAction.class);
        if (action != null) {
          return dataset.getColumnKey(category).toString().substring(1) + "/"
              + action.getReportUrl();
        } else {
          return null;
        }
      }
      return null;
    };
    renderer.setBaseItemURLGenerator(urlGenerator);
    plot.setRenderer(renderer);
  }

  private void setTooltipFormat(CategoryPlot plot) {
    plot.getRenderer().setBaseToolTipGenerator((CategoryDataset dataset, int row, int column) -> {
      BuildResultAction action =
          runs.get(runs.size() - column - 1).getAction(BuildResultAction.class);
      if (action != null) {
        return dataset.getColumnKey(column).toString() + ": "
            + action.getFailureCount() + " failures";
      } else {
        return dataset.getColumnKey(column).toString() + ": " + "Defensics not run";
      }
    });
  }

  private void configureXAxis(CategoryPlot plot) {
    CategoryAxis domainAxis = new ShiftedCategoryAxis(null);
    plot.setDomainAxis(domainAxis);
    domainAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_90);
    domainAxis.setLowerMargin(0.0);
    domainAxis.setUpperMargin(0.0);
    domainAxis.setCategoryMargin(0.0);
  }

  private void configureYAxis(CategoryPlot plot) {
    final NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
    rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
    rangeAxis.setLowerBound(0);
  }
}
