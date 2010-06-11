/*
 * $HeadURL: svn://chef/branch/prod/web-ui/work/src/ips/api/com/untangle/node/ips/IpsSettings.java $
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

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Transient;

/**
 * Base Settings for the Ips node.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
@Embeddable
public class IpsBaseSettings implements Serializable
{

    private int maxChunks;

    private int rulesLength;
    private int variablesLength;
    private int immutableVariablesLength;
    
    private int totalAvailable;
    private int totalBlocking;
    private int totalLogging;

    public IpsBaseSettings() { }

    @Column(name="max_chunks")
    public int getMaxChunks()
    {
        return maxChunks;
    }

    public void setMaxChunks(int maxChunks)
    {
        this.maxChunks = maxChunks;
    }

    @Transient
    public int getRulesLength()
    {
        return this.rulesLength;
    }

    public void setRulesLength(int rulesLength)
    {
        this.rulesLength = rulesLength;
    }

    @Transient
    public int getVariablesLength()
    {
        return this.variablesLength;
    }

    public void setVariablesLength(int variablesLength)
    {
        this.variablesLength = variablesLength;
    }

    @Transient
    public int getImmutableVariablesLength()
    {
        return this.immutableVariablesLength;
    }

    public void setImmutableVariablesLength(int immutableVariablesLength)
    {
        this.immutableVariablesLength = immutableVariablesLength;
    }

    @Transient
    public int getTotalAvailable() {
        return this.totalAvailable;
    }

    public void setTotalAvailable(int totalAvailable) {
        this.totalAvailable = totalAvailable;
    }

    @Transient
    public int getTotalBlocking() {
        return this.totalBlocking;
    }

    public void setTotalBlocking(int totalBlocking) {
        this.totalBlocking = totalBlocking;
    }

    @Transient
    public int getTotalLogging() {
        return this.totalLogging;
    }

    public void setTotalLogging(int totalLogging) {
        this.totalLogging = totalLogging;
    }
}