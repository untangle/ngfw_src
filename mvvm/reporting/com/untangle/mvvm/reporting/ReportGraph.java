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

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

import net.sf.jasperreports.engine.JRAbstractSvgRenderer;
import net.sf.jasperreports.engine.JRDefaultScriptlet;
import net.sf.jasperreports.engine.JRScriptletException;
import org.jfree.chart.JFreeChart;
import org.jfree.ui.Drawable;


public abstract class ReportGraph
{
    // General ones
    public static final String PARAM_REPORT_START_DATE = "ReportStartDate";
    public static final String PARAM_REPORT_END_DATE = "ReportEndDate";
    public static final String PARAM_REPORT_TYPE = "ReportType";

    protected static final Font TITLE_FONT = new Font("Arial", Font.BOLD, 14);
    protected static final Font LABEL_FONT = new Font("Arial", Font.PLAIN, 10);

    protected static long DAY_INTERVAL = 24*60*60*1000;
    protected static long MINUTE_INTERVAL = 60*1000;

    private JRDefaultScriptlet ourScriptlet;

    // General ones
    protected Timestamp startDate;
    protected Timestamp endDate;
    protected int type;

    protected Map<String,Object> extraParams;

    protected void setExtraParams(Map<String,Object> extraParams){
        this.extraParams = new HashMap<String,Object>(extraParams); // Make a copy just in case
    }

    protected ReportGraph()
    {
    }

    public void doIt(JRDefaultScriptlet ourScriptlet) throws JRScriptletException
    {
        Connection con = null;
        try {
            Class.forName("org.postgresql.Driver");
            con = DriverManager.getConnection("jdbc:postgresql://localhost/mvvm",
                                              "postgres", "foo");
            JFreeChart chart = doInternal(con, ourScriptlet);
            ourScriptlet.setVariableValue("Traffic", new JCommonDrawableRenderer(chart));
        } catch (SQLException x) {
            x.printStackTrace();
            throw new JRScriptletException(x);
        } catch (ClassNotFoundException x) {
            x.printStackTrace();
            throw new JRScriptletException(x);
        } finally {
            try { if (con != null) con.close(); } catch (SQLException x) { }
        }
    }

    public JFreeChart doInternal(Connection con, JRDefaultScriptlet ourScriptlet) throws JRScriptletException, SQLException,
                                                                                         ClassNotFoundException
    {
        this.ourScriptlet = ourScriptlet;
        initParams();
        return doChart(con);
    }

    // Get the parameters.  Set up default values for everything.
    protected void initParams()
    {
        long now = System.currentTimeMillis();

        startDate = (Timestamp) gpv(PARAM_REPORT_START_DATE);
        if (startDate == null)
            startDate = new Timestamp(now - DAY_INTERVAL);
        endDate = (Timestamp) gpv(PARAM_REPORT_END_DATE);
        if (endDate == null)
            endDate = new Timestamp(now);
        type = (Integer) gpv(PARAM_REPORT_TYPE);
    }

    protected abstract JFreeChart doChart(Connection con) throws JRScriptletException, SQLException;

    protected Object gpv(String name)
    {
        try {
            return ourScriptlet.getParameterValue(name);
        } catch (JRScriptletException x) {
            return  null;
        }
    }


    /**
     * A wrapper for the Drawable interface in the JCommon library: you will need the
     * JCommon classes in your classpath to compile this class. In particular this can be
     * used to allow JFreeChart objects to be included in the output report in vector form.
     *
     * @author Teodor Danciu (teodord@users.sourceforge.net)
     * @version $Id$
     */
    static class JCommonDrawableRenderer extends JRAbstractSvgRenderer
    {
        private Drawable drawable = null;

        public JCommonDrawableRenderer(Drawable drawable)
        {
            this.drawable = drawable;
        }

        public void render(Graphics2D grx, Rectangle2D rectangle)
        {
            if (drawable != null) {
                drawable.draw(grx, rectangle);
            }
        }
    }
}
