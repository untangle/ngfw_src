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

package com.untangle.node.ids;

import java.io.Serializable;


public class RuleClassification implements Serializable {
    private static final long serialVersionUID = -7009087970546610324L;

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
