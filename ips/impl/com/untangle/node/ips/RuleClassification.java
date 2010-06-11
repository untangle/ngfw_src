/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.untangle.node.ips;

import java.io.Serializable;


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
