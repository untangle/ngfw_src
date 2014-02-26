/**
 * $Id$
 */
package com.untangle.node.reporting.items;

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