/**
 * $Id: Chart.java,v 1.00 2012/01/08 20:12:21 dmorris Exp $
 */

package com.untangle.uvm.reports;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Serializable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import org.jfree.data.category.CategoryDataset;
import org.jfree.data.io.CSV;

@SuppressWarnings("serial")
public class Chart extends SummaryItem implements Serializable
{
    private final String plotType;
    private final String imageUrl;
    private final String csvUrl;

    private static final Logger logger = Logger.getLogger(Chart.class.getName());

    private final List<KeyStatistic> keyStatistics = new ArrayList<KeyStatistic>();

    private Plot plot;

    public Chart(String name, String title, String plotType, String imageUrl,
                 String csvUrl)
    {
        super(name, title);

        this.plotType = plotType;
        this.imageUrl = imageUrl;
        this.csvUrl = csvUrl;
    }

    public void setPlot(Plot plot)
    {
        this.plot = plot;
    }

    public String getPlotType()
    {
        return plotType;
    }

    public String getImageUrl()
    {
        return imageUrl;
    }

    public String getCsvUrl()
    {
        return csvUrl;
    }

    public String getPrinterUrl()
    {
        return imageUrl;
    }

    public List<KeyStatistic> getKeyStatistics()
    {
        return keyStatistics;
    }

    public Map<String, String> getColors()
    {
        if (null == plot) {
            return new HashMap<String, String>();
        } else {
            return plot.getColors();
        }
    }

    public void addKeyStatistic(KeyStatistic ks)
    {
        keyStatistics.add(ks);
    }

    public void generate(String reportBase)
        throws IOException
    {
        Logger logger = Logger.getLogger(getClass());

        logger.debug("generating: " + imageUrl + " from: " + csvUrl);

        if (null == plot) {
            logger.warn("no plot: " + imageUrl);
        } else {
            plot.generate(reportBase, csvUrl, imageUrl);
        }
    }

    public static CategoryDataset readFromCsv(String path)
    throws IOException {
        CSV csv = new CSV(';','"');

        File f = new File(path);
        if (!f.exists()) {
            logger.warn("file does not exist: " + f);
            return null;
        }
        FileInputStream fis = new FileInputStream(f);
        Reader r = new InputStreamReader(fis);

        CategoryDataset cd = csv.readCategoryDataset(r);

        return cd;
    }
}