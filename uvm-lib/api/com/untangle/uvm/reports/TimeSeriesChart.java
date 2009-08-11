/*
 * $HeadURL: svn://chef/branch/prod/hellokitty/work/src/uvm-lib/api/com/untangle/uvm/RemoteReportingManager.java $
 * Copyright (c) 2003-2007 Untangle, Inc.
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent modules,
 * and to copy and distribute the resulting executable under terms of your
 * choice, provided that you also meet, for each linked independent module,
 * the terms and conditions of the license of that module.  An independent
 * module is a module which is not derived from or based on this library.
 * If you modify this library, you may extend this exception to your version
 * of the library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
 */

package com.untangle.uvm.reports;

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.io.CSV;
import org.jfree.data.time.Minute;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

public class TimeSeriesChart extends Plot
{
    private final String xLabel;
    private final String yLabel;
    private final String majorFormatter;

    private final Logger logger = Logger.getLogger(getClass());

    public TimeSeriesChart(String title, String xLabel, String yLabel,
                           String majorFormatter)
    {
        super(title);

        this.xLabel = xLabel;
        this.yLabel = yLabel;
        this.majorFormatter = majorFormatter;
    }

    public void generate(String reportBase, String csvUrl, String imageUrl)
        throws IOException
    {
        CSV csv = new CSV();

        File f = new File(reportBase + "/" + csvUrl);
        if (!f.exists()) {
            logger.warn("file does not exist: " + f);
            return;
        }
        FileInputStream fis = new FileInputStream(f);
        Reader r = new InputStreamReader(fis);

        CategoryDataset cd = csv.readCategoryDataset(r);

        TimeSeriesCollection tsc = new TimeSeriesCollection();

        List<Color> seriesColors = new ArrayList<Color>();
        for (int i = 0; i < cd.getColumnCount(); i++) {
            String columnKey = (String)cd.getColumnKey(i);
            String colorStr = colors.get(columnKey);
            if (null != colorStr) {
                try {
                    Color c = Color.decode("0x" + colorStr);
                    seriesColors.add(c);
                } catch (NumberFormatException exn) {
                    logger.warn("could not decode color: " + colorStr, exn);
                }
            }

            TimeSeries ds = new TimeSeries(columnKey);
            for (int j = 0; j < cd.getRowCount(); j++) {
                Comparable rowKey = cd.getRowKey(j);
                try {
                    String timeStr = (String)rowKey;
                    String[] split = timeStr.split(":");
                    int hour = Integer.parseInt(split[0]);
                    int minute = Integer.parseInt(split[1]);

                    Minute m = new Minute(minute, hour, 1, 1, 1900);

                    double v = cd.getValue(rowKey, columnKey).doubleValue();

                    ds.add(m, v);
                } catch (IndexOutOfBoundsException exn) {
                    System.out.println(exn);
                    logger.warn("Bad row key: " + rowKey, exn);
                } catch (ClassCastException exn) {
                    System.out.println(exn);
                    logger.warn("Bad row key: " + rowKey, exn);
                } catch (NumberFormatException exn) {
                    System.out.println(exn);
                    logger.warn("Bad row key: " + rowKey, exn);
                }
            }

            tsc.addSeries(ds);
        }

        String title = getTitle();
        JFreeChart jfChart =
            ChartFactory.createTimeSeriesChart(title, this.xLabel, this.yLabel,
                                               tsc, false, true, false);
        jfChart.setTitle(new TextTitle(title, TITLE_FONT));
        XYPlot p = (XYPlot)jfChart.getPlot();
        XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer)p.getRenderer();
        for (int i = 0; i < seriesColors.size(); i++) {
            renderer.setSeriesPaint(i, seriesColors.get(i));
        }
        p.getRangeAxis().setLabelFont(AXIS_FONT);
        p.getDomainAxis().setLabelFont(AXIS_FONT);
        ChartUtilities.saveChartAsPNG(new File(reportBase + "/" + imageUrl),
                                      jfChart, CHART_WIDTH, CHART_HEIGHT,
                                      null, false, CHART_COMPRESSION_PNG);

    }
}