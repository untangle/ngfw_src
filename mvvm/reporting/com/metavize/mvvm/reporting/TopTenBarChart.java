/*
 * Copyright (c) 2003-2006 Untangle Networks, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.mvvm.reporting;


import java.sql.*;
import java.sql.*;
import java.sql.*;



import net.sf.jasperreports.engine.JRDefaultScriptlet;



public abstract class TopTenBarChart extends ReportGraph
{
    // Bar chart specific
    public static final String PARAM_MAX_BARS = "MaxBars";

    // Bar chart specific
    protected int maxBars;

    protected TopTenBarChart()
    {

    }

    // Get the parameters
    protected void initParams()
    {
        super.initParams();

        Integer maxBarsInt = (Integer) gpv(PARAM_MAX_BARS);
        if (maxBarsInt == null)
            maxBars = 11;  // Ten plus other, if necessary.
        else
            maxBars = maxBarsInt.intValue();
    }
}



