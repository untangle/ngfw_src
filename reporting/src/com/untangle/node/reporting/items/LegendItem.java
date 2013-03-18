/**
 * $Id: LegendItem.java,v 1.00 2012/06/11 14:58:32 dmorris Exp $
 */
package com.untangle.node.reporting.items;

import java.io.Serializable;

@SuppressWarnings("serial")
public class LegendItem implements Serializable
{
    private final String label;
    private final String imageUrl;

    public LegendItem(String label, String imageUrl)
    {
        this.label = label;
        this.imageUrl = imageUrl;
    }

    public String getLabel()
    {
        return label;
    }

    public String getImageUrl()
    {
        return imageUrl;
    }
}