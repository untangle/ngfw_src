/*
 * $HeadURL: svn://chef/branch/prod/hellokitty/work/src/uvm/api/com/untangle/uvm/ReportingManager.java $
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
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.DateTickUnit;
import org.jfree.chart.axis.DateTickUnitType;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.chart.renderer.xy.StackedXYBarRenderer;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.io.CSV;
import org.jfree.data.time.SimpleTimePeriod;
import org.jfree.data.time.TimePeriod;
import org.jfree.data.time.TimeTableXYDataset;

import com.untangle.uvm.util.DateTruncator;

public class StackedBarChart extends Plot
{
    private static DateFormat DF = new SimpleDateFormat("yyyy-MM-dd");
    private static DateFormat HF = new SimpleDateFormat("yyyy-MM-dd HH");
    static {
        DF.setLenient(false);
        HF.setLenient(false);
    }

    // duplicated from com.uvm.engine.ReportingManagerImpl
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    private final String xLabel;
    private final String yLabel;
    private final String yAxisLowerBound;
    //unused// private final String majorFormatter;

    private final Logger logger = Logger.getLogger(getClass());

    public StackedBarChart(String title, String xLabel, String yLabel,
                           String majorFormatter, String yAxisLowerBound)
    {
        super(title);

        this.xLabel = xLabel;
        this.yLabel = yLabel;
        this.yAxisLowerBound = yAxisLowerBound;
        //this.majorFormatter = majorFormatter;
    }

    private int getInterval(String timeStr) {
    	try {
            HF.parse(timeStr);
            return 1;
    	} catch (java.text.ParseException exn) {
            try{
    		DF.parse(timeStr);
                return 8;
            } catch (java.text.ParseException exn2) {
                logger.warn("Couldn't get time interval for row key: " + timeStr, exn2);
            }
    	}
    	return 1;
    }

    private void formatDateAxis(DateAxis da, File csvPath) {
        logger.debug("About to format date axis for: " + csvPath);
        File dir = csvPath.getParentFile().getParentFile();

        String maxString = dir.getParentFile().getName();
	Date max = null;
	try { // 1st, assume this is a generic report
	    max = DATE_FORMAT.parse(maxString);
	} catch (java.text.ParseException exn) {
            try { // try to see if it's a per-{user,host,email} report
                dir = dir.getParentFile().getParentFile();
                maxString = dir.getParentFile().getName();
                max = DATE_FORMAT.parse(maxString);
            } catch (java.text.ParseException e) {
                logger.warn("Couldn't parse time for: " + maxString, e);
                return;
            }
	}

        String length = dir.getName();
        String[] split = length.split("-");
        int numDays = Integer.parseInt(split[0]);

        Calendar c = Calendar.getInstance();
        c.setTime(max);
        c.add(Calendar.DATE, -numDays);
        Date min = c.getTime();

	// range in seconds
	long range = (max.getTime() - min.getTime()) / 1000;

	int trunc; // where to truncate the boundaries
	String dateFormatStr; // the format for X-axis ticks
	DateTickUnitType tickUnit = null;
        int tickFrequency = -1;
	logger.debug(min + " -> " + max + " (" + range + " seconds)");
	if (range < 45 * 60) { // less than 45min
	    trunc = Calendar.MINUTE;
	    dateFormatStr = "mm:ss";
	} else if (range < 18 * 60 * 60) { // less than 18h
	    trunc = Calendar.HOUR;
	    dateFormatStr = "HH:mm";
	} else if (range <= 24 * 60 * 60) { // less than 24h
	    trunc = Calendar.DATE;
	    dateFormatStr = "HH:mm";
            tickUnit = DateTickUnitType.HOUR;
            tickFrequency = 4;
        } else if (range <= 7 * 24 * 60 * 60 ) { // less than 7 days
	    trunc = Calendar.DATE;
	    dateFormatStr = "MMM-d";
            tickUnit = DateTickUnitType.DAY;
            tickFrequency = 2;
	} else {
	    trunc = Calendar.DATE;
	    dateFormatStr = "MMM-d";
            tickUnit = DateTickUnitType.DAY;
            tickFrequency = 7;
        }

	min = DateTruncator.truncateDate(min, trunc, true);
	max = DateTruncator.truncateDate(max, trunc, true);
	logger.debug("... adapted to: " + min + " -> " + max + " (tickUnit=" + tickUnit + ", tickFrequency=" + tickFrequency + ")");
 	da.setMinimumDate(min);
 	da.setMaximumDate(max);

        da.setDateFormatOverride(new SimpleDateFormat(dateFormatStr));
        if (tickUnit != null)
            da.setTickUnit(new DateTickUnit(tickUnit, tickFrequency));
    }

    private Date parseTimeStamp(String timeStr) 
    {
    	Date date = null;
    	try {
            date = HF.parse(timeStr);
    	} catch (java.text.ParseException exn) {
            try{
    		date = DF.parse(timeStr);
            } catch (java.text.ParseException exn2) {
                logger.warn("Couldn't parse time for row key: " + timeStr, exn2);
            }
    	}
    	return date;
    }

    @SuppressWarnings("unchecked")
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

        TimeTableXYDataset dataset = new TimeTableXYDataset();

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

            for (int j = 0; j < cd.getRowCount(); j++) {
                Comparable rowKey = cd.getRowKey(j);
                try {
                    int interval = getInterval((String)rowKey);
                    Date d = parseTimeStamp((String)rowKey);
                    double v = cd.getValue(rowKey, columnKey).doubleValue();

                    Calendar cal = Calendar.getInstance();
                    cal.setTime(d);
                    cal.add(Calendar.HOUR, interval);

                    TimePeriod period = new SimpleTimePeriod(d, cal.getTime());
                    dataset.add(period, v, columnKey);
                } catch (Exception exn) {
                    logger.warn("Bad row key: " + rowKey, exn);
                }
            }
        }

        String title = getTitle();

        XYBarRenderer renderer = new StackedXYBarRenderer();
        renderer.setDrawBarOutline(false);
        renderer.setShadowVisible(false);

        if (cd.getColumnCount() == 1) { 
            // no legend if there's only one serie
            logger.debug("Disabling legend");
            renderer.setBaseItemLabelsVisible(false);
            renderer.setBaseSeriesVisibleInLegend(false);
            renderer.setBaseItemLabelsVisible(false);
        }

        XYPlot plot = new XYPlot(dataset,
                                 new DateAxis(this.xLabel),
                                 new NumberAxis(this.yLabel),
                                 renderer);

        JFreeChart jfChart = new JFreeChart(plot);      

        jfChart.setTitle(new TextTitle(title, TITLE_FONT));

        for (int i = 0; i < seriesColors.size(); i++) {
            renderer.setSeriesPaint(i, seriesColors.get(i));
        }

        ValueAxis va = plot.getRangeAxis();
        va.setLabelFont(AXIS_FONT);
        if (this.yAxisLowerBound != null) {
            va.setLowerBound(Double.parseDouble(this.yAxisLowerBound));
            va.setUpperBound(va.getUpperBound() * 1.05);
        }

        DateAxis da = (DateAxis)plot.getDomainAxis();
        da.setLabelFont(AXIS_FONT);

	formatDateAxis(da, f);

        ChartUtilities.saveChartAsPNG(new File(reportBase + "/" + imageUrl),
                                      jfChart, CHART_WIDTH, CHART_HEIGHT,
                                      null, false, CHART_COMPRESSION_PNG);
    }
}