/**
 * $Id: PieChart.java,v 1.00 2012/01/08 16:45:39 dmorris Exp $
 */
package com.untangle.uvm.reports;

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

import org.apache.log4j.Logger;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.io.CSV;
import org.jfree.util.SortOrder;

public class PieChart extends Plot
{
    private final int displayLimit;

    private final Logger logger = Logger.getLogger(getClass());

    public PieChart(String title, String xLabel, String yLabel, String majorFormatter, int displayLimit)
    {
        super(title);

        this.displayLimit = displayLimit;
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

        DefaultPieDataset dpd = new DefaultPieDataset();
        int othersCount = 0;
        
        for (int i = 0; i < cd.getColumnCount(); i++) {
            String columnKey = (String)cd.getColumnKey(i);

            for (int j = 0; j < cd.getRowCount(); j++) {
                String rowKey = cd.getRowKey(j).toString();
                Number n = cd.getValue(rowKey, columnKey);
                if (displayLimit >= 0 && j >= displayLimit-1) {
                    othersCount += n.intValue();
                } else {
                    dpd.setValue(rowKey, n);
                }
            }
        }
        
        dpd.sortByValues(SortOrder.DESCENDING);
        if ( othersCount != 0 ) {
            dpd.insertValue(dpd.getItemCount(),"others",othersCount);
        }

        String title = getTitle();
        JFreeChart jfChart = ChartFactory.createPieChart(title, dpd, false, false, false);
        jfChart.setTitle(new TextTitle(title, TITLE_FONT));
        jfChart.setBackgroundPaint(CHART_BACKGROUND_COLOR);
        jfChart.setBorderPaint(CHART_BORDER_COLOR);
        jfChart.setBorderVisible(true);

        PiePlot plot = (PiePlot)jfChart.getPlot();
        for (String key : colors.keySet()) {
            String colorStr = colors.get(key);
            if (null != colorStr) {
                try {
                    Color c = Color.decode("0x" + colorStr);
                    if (key.equals("others")) {
                        //logger.warn("setColor( " + key + " , " + Color.lightGray + " )");
                        //plot.setSectionPaint(key, Color.lightGray);
                        plot.setSectionPaint(key, c);
                        plot.setExplodePercent(key, .10);
                    }
                    else {
                        //logger.warn("setColor( " + key + " , " + c + " )");
                        plot.setSectionPaint(key, c);
                    }
                } catch (NumberFormatException exn) {
                    logger.warn("could not decode color: " + colorStr, exn);
                }
            }
        }
        plot.setLabelGenerator(null);
        plot.setOutlineVisible(false);
        plot.setBackgroundPaint(CHART_BACKGROUND_COLOR);
        
        ChartUtilities.saveChartAsPNG(new File(reportBase + "/" + imageUrl), jfChart, CHART_WIDTH, CHART_HEIGHT, null, false, CHART_COMPRESSION_PNG);
    }
}