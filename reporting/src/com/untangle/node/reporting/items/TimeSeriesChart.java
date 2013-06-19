/**
 * $Id: TimeSeriesChart.java,v 1.00 2012/01/08 20:14:47 dmorris Exp $h
 */
package com.untangle.node.reporting.items;

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
import org.jfree.chart.axis.DateTickUnit;
import org.jfree.chart.axis.DateTickUnitType;
import org.jfree.chart.axis.ValueAxis;
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
    private static final DateFormat DF = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    private final String xLabel;
    private final String yLabel;
    private final String yAxisLowerBound;

    private final Logger logger = Logger.getLogger(getClass());

    public TimeSeriesChart(String title, String xLabel, String yLabel, String majorFormatter, String yAxisLowerBound)
    {
        super(title);

        this.xLabel = xLabel;
        this.yLabel = yLabel;
        this.yAxisLowerBound = yAxisLowerBound;
    }

    private Date parseTimeStamp(String timeStr)
    {
        Date date = null;
        try {
            date = DF.parse(timeStr);
        } catch (java.text.ParseException exn) {
            logger.warn("Couldn't parse time for row key: " + timeStr, exn);
        }
        return date;
    }

    /*
     * Format the X-axis according to the data present on it.
     */
    @SuppressWarnings("unused")
    private void formatDateAxis(DateAxis da)
    {
        Date min = da.getMinimumDate();
        Date max = da.getMaximumDate();
        // range in seconds
        long range = (max.getTime() - min.getTime()) / 1000;

        int trunc; // where to truncate the boundaries
        String dateFormatStr; // the format for X-axix ticks
        logger.debug(min + " -> " + max + " (" + range + ")");
        if (range < 45 * 60) { // less than 45min
            trunc = Calendar.MINUTE;
            dateFormatStr = "mm:ss";
        } else if (range < 18 * 60 * 60) { // less than 18h
            trunc = Calendar.HOUR;
            dateFormatStr = "HH:mm";
        } else {
            trunc = Calendar.DATE;
            dateFormatStr = "MM-dd";
        }
        min = truncateDate(min, trunc, true);
        max = truncateDate(max, trunc, false);
        logger.debug("... adapted to: " + min + " -> " + max);
        da.setMinimumDate(min);
        da.setMaximumDate(max);

        da.setDateFormatOverride(new SimpleDateFormat(dateFormatStr));
        //     da.setTickUnit(new DateTickUnit(DateTickUnitType.DAY, 1));
    }

    /*
     * Format the X-axis according to given min & max dates
     */
    private void formatDateAxis(DateAxis da, File csvPath) {
        //        logger.debug("About to format date axis for: " + csvPath);
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
        String dateFormatStr; // the format for X-axix ticks
        DateTickUnitType tickUnit = null;
        int tickFrequency = -1;
        logger.debug(min + " -> " + max + " (" + range + ")");
        if (range < 45 * 60) { // less than 45min
            trunc = Calendar.MINUTE;
            dateFormatStr = "mm:ss";
        } else if (range < 18 * 60 * 60) { // less than 18h
            trunc = Calendar.HOUR;
            dateFormatStr = "HH:mm";
        } else if (range <= 24 * 60 * 60) { // less than 24h
            trunc = Calendar.DATE;
            dateFormatStr = "HH:mm";
            //             tickUnit = DateTickUnitType.HOUR;
            //             tickFrequency = 4;
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

        min = truncateDate(min, trunc, true);
        max = truncateDate(max, trunc, true);
        logger.debug("... adapted to: " + min + " -> " + max + " (tickUnit=" + tickUnit + ", tickFrequency=" + tickFrequency);
        da.setMinimumDate(min);
        da.setMaximumDate(max);

        da.setDateFormatOverride(new SimpleDateFormat(dateFormatStr));
        if (tickUnit != null) 
            da.setTickUnit(new DateTickUnit(tickUnit, tickFrequency));
    }

    @SuppressWarnings("unchecked")
    public void generate(String reportBase, String csvUrl, String imageUrl)
        throws IOException
    {
        String path = reportBase + "/" + csvUrl;
        CategoryDataset cd = Chart.readFromCsv(path);

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

        String title = getTitle();
        JFreeChart jfChart = ChartFactory.createTimeSeriesChart(title, this.xLabel, this.yLabel, tsc, true, true, false);
        jfChart.setTitle(new TextTitle(title, TITLE_FONT));
        jfChart.setBackgroundPaint(CHART_BACKGROUND_COLOR);
        jfChart.setBorderPaint(CHART_BORDER_COLOR);
        jfChart.setBorderVisible(true);

        XYPlot plot = (XYPlot)jfChart.getPlot();
        XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer)plot.getRenderer();

        if (cd.getColumnCount() == 1) { 
            // no legend if there's only one serie
            logger.debug("Disabling legend");
            renderer.setBaseItemLabelsVisible(false);
            renderer.setBaseSeriesVisibleInLegend(false);
            renderer.setBaseItemLabelsVisible(false);
        }

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

        formatDateAxis(da, new File(path));
        plot.setOutlinePaint(Color.gray);
        plot.setOutlineVisible(true);
        plot.setBackgroundPaint(java.awt.Color.white);
        plot.setDomainGridlinePaint(CHART_BACKGROUND_COLOR);
        plot.setDomainGridlineStroke(new java.awt.BasicStroke());
        plot.setRangeGridlinePaint(CHART_BACKGROUND_COLOR);
        plot.setRangeGridlineStroke(new java.awt.BasicStroke());
        plot.setDomainGridlinesVisible(true);
        plot.setRangeGridlinesVisible(true);
        plot.setAxisOffset (new org.jfree.ui.RectangleInsets (0, 0, 0, 0));
        
        ChartUtilities.saveChartAsPNG(new File(reportBase + "/" + imageUrl), jfChart, CHART_WIDTH, CHART_HEIGHT, null, false, CHART_COMPRESSION_PNG);

    }

    private static Date truncateDate(Date date, int where, boolean down)
    {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
    
        int count = -1;
        if (where == Calendar.DATE) {
            count = 4;
        } else if (where == Calendar.HOUR) {
            count = 3;
        } else if (where == Calendar.MINUTE) {
            count = 2;
        }
        
        for (int i = 0 ; i <= count ; i++) {
            cal.set(Calendar.MILLISECOND - i, 0);
        }

        if (!down)
            cal.add(where, 1);

        return cal.getTime();
    }

}