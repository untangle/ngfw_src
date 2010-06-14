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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
// import org.jfree.chart.axis.CategoryAxis;
// import org.jfree.chart.axis.CategoryLabelPositions;
// import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.xy.XYBarDataset;
import org.jfree.data.io.CSV;
import org.jfree.data.time.Minute;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

public class StackedBarChart extends Plot
{
    private static final DateFormat DF = 
	new SimpleDateFormat("MMM-dd");

    private static final DateFormat HF = 
	new SimpleDateFormat("HH");


    private final String xLabel;
    private final String yLabel;
    private final String majorFormatter;

    private final Logger logger = Logger.getLogger(getClass());

    public StackedBarChart(String title, String xLabel, String yLabel,
                           String majorFormatter)
    {
        super(title);

        this.xLabel = xLabel;
        this.yLabel = yLabel;
        this.majorFormatter = majorFormatter;
    }


    private Date parseTimeStamp(String timeStr) {
	Date date = null;
	try {
	    date = DF.parse(timeStr);
	} catch (java.text.ParseException exn) {
            try{
                date = HF.parse(timeStr);
            } catch (java.text.ParseException exn2) {
                logger.warn("Couldn't parse time for row key: " + timeStr, exn2);
            }
	}
	return date;
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

        //        DefaultCategoryDataset rotated = new DefaultCategoryDataset();
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
		    Date d = parseTimeStamp((String)rowKey);
                    double v = cd.getValue(rowKey, columnKey).doubleValue();
                    ds.add(new Minute(d), v);
                } catch (Exception exn) {
                    logger.warn("Bad row key: " + rowKey, exn);
                }
            }
            tsc.addSeries(ds);
        }

//         for (int i = 0; i < cd.getColumnCount(); i++) {
//             String columnKey = (String)cd.getColumnKey(i);

//             for (int j = 0; j < cd.getRowCount(); j++) {
//                 Comparable rowKey = cd.getRowKey(j);

//                 rotated.addValue(cd.getValue(rowKey, columnKey),
//                                  columnKey, rowKey);
//             }
//         }

        String title = getTitle();
        // FIXME: compute bar width properly
        JFreeChart jfChart =
            ChartFactory.createXYBarChart(title, this.xLabel, true, this.yLabel,
                                          new XYBarDataset(tsc, 10000000.0),
                                          PlotOrientation.VERTICAL,
                                          true, false, false);


        jfChart.setTitle(new TextTitle(title, TITLE_FONT));

//         CategoryPlot p = (CategoryPlot)jfChart.getPlot();
//         StackedBarRenderer renderer = (StackedBarRenderer)p.getRenderer();
        XYPlot p = (XYPlot)jfChart.getPlot();
        XYBarRenderer renderer = (XYBarRenderer)p.getRenderer();
        renderer.setShadowVisible(false);
        for (int i = 0; i < seriesColors.size(); i++) {
            renderer.setSeriesPaint(i, seriesColors.get(i));
        }

//         for (String key : colors.keySet()) {
//             int i = rotated.getRowIndex(key);
//             String colorStr = colors.get(key);
//             if (null != colorStr) {
//                 try {
//                     Color c = Color.decode("0x" + colorStr);
//                     renderer.setSeriesPaint(i, c);
//                 } catch (NumberFormatException exn) {
//                     logger.warn("could not decode color: " + colorStr, exn);
//                 }
//             }
//         }

        p.getRangeAxis().setLabelFont(AXIS_FONT);
        DateAxis da = (DateAxis)p.getDomainAxis();
        da.setLabelFont(AXIS_FONT);

	//formatDateAxis(da, f);

//         CategoryAxis domainAxis = p.getDomainAxis();
//         domainAxis.setCategoryLabelPositions(CategoryLabelPositions.DOWN_45);
//         p.getRangeAxis().setLabelFont(AXIS_FONT);
//         p.getDomainAxis().setLabelFont(AXIS_FONT);

        ChartUtilities.saveChartAsPNG(new File(reportBase + "/" + imageUrl),
                                      jfChart, CHART_WIDTH, CHART_HEIGHT,
                                      null, false, CHART_COMPRESSION_PNG);
    }
}