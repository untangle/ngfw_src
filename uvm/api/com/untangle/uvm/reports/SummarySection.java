/**
 * $Id: SummarySection.java,v 1.00 2012/06/11 14:57:40 dmorris Exp $
 */
package com.untangle.uvm.reports;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("serial")
public class SummarySection extends Section implements Serializable
{
    private final List<SummaryItem> summaryItems = new ArrayList<SummaryItem>();

    public SummarySection(String name, String title)
    {
        super(name, title);
    }

    public List<SummaryItem> getSummaryItems()
    {
        return summaryItems;
    }

    public List<Highlight> getHighlights()
    {
	List<Highlight> list = new ArrayList<Highlight>();

	for (SummaryItem i : getSummaryItems()) {
	    if (i instanceof Highlight) {
		list.add((Highlight)i);
	    }
	}

        return list;
    }

    public void addSummaryItem(SummaryItem item)
    {
        summaryItems.add(item);
    }


}
