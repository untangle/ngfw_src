/**
 * $Id: ApplicationData.java,v 1.00 2012/06/11 14:57:33 dmorris Exp $
 */
package com.untangle.uvm.reports;

import java.io.Serializable;
import java.util.List;

@SuppressWarnings("serial")
public class ApplicationData implements Serializable
{
    
    private final String name;
    private final String title;
    private final List<Section> sections;

    public ApplicationData(String name, String title, List<Section> sections)
    {
        this.name = name;
        this.title = title;
        this.sections = sections;
    }

    public String getName()
    {
        return name;
    }

    public String getTitle()
    {
        return title;
    }

    public List<Section> getSections()
    {
        return sections;
    }
}