/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.untangle.mvvm.reporting;


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
