/**
 * $Id$
 */
package com.untangle.node.reports.items;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("serial")
public class Highlight extends SummaryItem implements Serializable
{
    private final String stringTemplate;
    private final Map<String, String> highlightValues = new HashMap<String, String>();

    public Highlight(String name, String stringTemplate)
    {
    super(name, name);
        this.stringTemplate = stringTemplate;
    }

    public String getStringTemplate()
    {
        return stringTemplate;
    }

    public void addValue(String key, String value) {
    highlightValues.put(key, value);
    }

    public Map<String, String> getHighlightValues() {
    return highlightValues;
    }

    public String toString()
    {
        String s =  "[Highlight name='" + getName() + 
        "', stringTemplate='" + stringTemplate +
        "', values={" ;

    for (String k: highlightValues.keySet()) {
        s = s + k + "=" + highlightValues.get(k) + ", ";
    }

    s = s + "}]";
    return s;
    }
}