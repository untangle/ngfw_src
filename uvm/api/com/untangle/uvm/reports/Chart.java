/*
 * $HeadURL: svn://chef/branch/prod/hellokitty/work/src/uvm/api/com/untangle/uvm/RemoteReportingManager.java $
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

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

@SuppressWarnings("serial")
public class Chart extends SummaryItem implements Serializable
{
    private final String plotType;
    private final String imageUrl;
    private final String csvUrl;

    private final List<KeyStatistic> keyStatistics =
        new ArrayList<KeyStatistic>();

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
}