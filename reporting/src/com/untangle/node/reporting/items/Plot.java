/**
 * $Id: Plot.java,v 1.00 2012/01/08 18:05:02 dmorris Exp $
 */
package com.untangle.node.reporting.items;

import java.awt.Font;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public abstract class Plot
{
    public static final int CHART_COMPRESSION_PNG = 9;  // for PNG
    public static final int CHART_WIDTH = 338;
    public static final int CHART_HEIGHT = 230;
    public static final Font TITLE_FONT = new Font("Helvetica", Font.PLAIN, 14);
    public static final Font AXIS_FONT = new Font("Helvetica", Font.PLAIN, 10);

    protected final java.awt.Color CHART_BACKGROUND_COLOR = new java.awt.Color(230,230,230);
    protected final java.awt.Color CHART_BORDER_COLOR = new java.awt.Color(180,180,180);

    protected final Map<String, String> colors = new HashMap<String, String>();

    private final String title;

    public Plot(String title)
    {
        this.title = title;
    }

    public String getTitle()
    {
        return title;
    }

    public Map<String, String> getColors()
    {
        return new HashMap<String, String>(colors);
    }

    public void setColor(String title, String value)
        throws NumberFormatException
    {
        colors.put(title, value);
    }

    public abstract void generate(String reportBase, String csvUrl, String imageUrl)
        throws IOException;
}