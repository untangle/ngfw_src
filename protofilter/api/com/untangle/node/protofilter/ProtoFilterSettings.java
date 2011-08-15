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

package com.untangle.node.protofilter;

import java.util.HashSet;
import java.util.Set;

/**
 * Settings for the ProtoFilter node.
 *
 * @author <a href="mailto:mahotz@untangle.com">Michael Hotz</a>
 * @version 2.0
 */

@SuppressWarnings("serial")
public class ProtoFilterSettings implements java.io.Serializable
{
    private int byteLimit  = 2048;
    private int chunkLimit = 10;
    private String unknownString = "[unknown]";
    private boolean stripZeros = true;
    private HashSet<ProtoFilterPattern> patterns = null;

    public ProtoFilterSettings()
    {
        this.patterns = new HashSet<ProtoFilterPattern>();
    }

    public int getByteLimit()
    { return this.byteLimit; }

    public void setByteLimit(int i)
    { this.byteLimit = i; }

    public int getChunkLimit()
    { return this.chunkLimit; }

    public void setChunkLimit(int i)
    { this.chunkLimit = i; }

    public String getUnknownString()
    { return this.unknownString; }

    public void setUnknownString(String s)
    { this.unknownString = s; }

    public boolean isStripZeros()
    { return this.stripZeros; }

    public void setStripZeros(boolean b)
    { this.stripZeros = b; }

    public HashSet<ProtoFilterPattern> getPatterns()
    { return patterns; }

    public void setPatterns(HashSet<ProtoFilterPattern> s)
    { this.patterns = s; }
}
