/**
 * $Id$
 */
package com.untangle.node.ips;

import java.io.Serializable;


@SuppressWarnings("serial")
public class RuleClassification implements Serializable {

    private String name;
    private String description;
    private int priority;

    public RuleClassification(String name, String description, int priority) {
        if (name == null)
            throw new NullPointerException("Name cannot be null");
        this.name = name;
        this.description = description;
        this.priority = priority;
    }

    public String getName() {
        return name;
    }
    public void setName(String name)
    {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }
    public void setDescription(String description)
    {
        this.description = description;
    }

    public int getPriority() {
        return priority;
    }
    public void setPriority(int priority)
    {
        this.priority = priority;
    }
}
