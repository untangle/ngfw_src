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

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PiePlot3D;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.time.Hour;
import org.jfree.data.time.Minute;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.util.Rotation;
import org.jfree.ui.Drawable;

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.sql.*;
import java.util.Calendar;
import java.util.GregorianCalendar;

import net.sf.jasperreports.engine.JRAbstractSvgRenderer;
import net.sf.jasperreports.engine.JRDefaultScriptlet;
import net.sf.jasperreports.engine.JRScriptletException;

import com.untangle.mvvm.util.PortServiceNames;


public abstract class DayByMinuteTimeSeriesGraph extends TimeSeriesGraph
{
    protected DayByMinuteTimeSeriesGraph()
    {
    }
}
