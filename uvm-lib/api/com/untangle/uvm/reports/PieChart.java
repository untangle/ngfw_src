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
	//unused// private final String xLabel;
	//unused// private final String yLabel;
    //unused// private final String majorFormatter;
    private final int displayLimit;

    private final Logger logger = Logger.getLogger(getClass());

    public PieChart(String title, String xLabel, String yLabel, String majorFormatter, int displayLimit)
    {
        super(title);

        //unused// this.xLabel = xLabel;
        //unused// this.yLabel = yLabel;
        //unused// this.majorFormatter = majorFormatter;
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

        for (int i = 0; i < cd.getColumnCount(); i++) {
            String columnKey = (String)cd.getColumnKey(i);

            for (int j = 0; j < cd.getRowCount(); j++) {
                String rowKey = cd.getRowKey(j).toString();
                Number n = cd.getValue(rowKey, columnKey);
                if (displayLimit >= 0 && j >= displayLimit-1) {
                    Number m = dpd.getIndex("others") == -1 ? 0 : dpd.getValue("others");
                    dpd.setValue("others", m.intValue() + n.intValue());
                } else {
                    dpd.setValue(rowKey, n);
                }
            }
        }

        dpd.sortByValues(SortOrder.DESCENDING);

        String title = getTitle();
        JFreeChart jfChart
            = ChartFactory.createPieChart(title, dpd, false, false, false);
        jfChart.setTitle(new TextTitle(title, TITLE_FONT));
        PiePlot plot = (PiePlot)jfChart.getPlot();
        for (String key : colors.keySet()) {
            String colorStr = colors.get(key);
            if (null != colorStr) {
                try {
                    Color c = Color.decode("0x" + colorStr);
                    plot.setSectionPaint(key, c);
                } catch (NumberFormatException exn) {
                    logger.warn("could not decode color: " + colorStr, exn);
                }
            }
        }
        plot.setLabelGenerator(null);

        ChartUtilities.saveChartAsPNG(new File(reportBase + "/" + imageUrl),
                                      jfChart, CHART_WIDTH, CHART_HEIGHT,
                                      null, false, CHART_COMPRESSION_PNG);
    }
}