/*
 * $HeadURL:$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.untangle.uvm.reporting;


import java.sql.*;




public abstract class TimeSeriesGraph extends ReportGraph
{
    // Means use minutes, not hours
    public static final String PARAM_HIGH_TIME_RESOLUTION = "HighTimeResolution";

    public static final String PARAM_TIME_AXIS_LABEL = "TimeAxisLabel";
    public static final String PARAM_VALUE_AXIS_LABEL = "ValueAxisLabel";

    // Time series specific
    protected boolean highTimeResolution;
    protected String timeAxisLabel;
    protected String valueAxisLabel;

    protected int MOVING_AVERAGE_MINUTES = 6;
    protected int MINUTES_PER_BUCKET = 3;

    protected TimeSeriesGraph()
    {

    }

    // Get the parameters
    protected void initParams()
    {
        super.initParams();

        // Get the parameters
        /*
          Boolean hiResBool = (Boolean) gpv(PARAM_HIGH_TIME_RESOLUTION);
          if (hiResBool == null)
          highTimeResolution = endDate.getTime() - startDate.getTime() <= DAY_INTERVAL ? true : false;
          else
          highTimeResolution = hiResBool.booleanValue();
          timeAxisLabel = (String) gpv(PARAM_TIME_AXIS_LABEL);
          if (timeAxisLabel == null) {
          if (highTimeResolution)
          timeAxisLabel = "time of day";
          else
          timeAxisLabel = "day of week";
          }
        */
        timeAxisLabel = "Time of day";

    }
}
