/**
 * $Id$
 */
package com.untangle.node.protofilter;

import java.util.LinkedList;

/**
 * Settings for the ProtoFilter node.
 *
 * @author <a href="mailto:mahotz@untangle.com">Michael Hotz</a>
 * @version 2.0
 */
@SuppressWarnings("serial")
public class ProtoFilterSettings implements java.io.Serializable
{
    private int version = 1;
    private int byteLimit  = 2048;
    private int chunkLimit = 10;
    private String unknownString = "[unknown]";
    private boolean stripZeros = true;
    private LinkedList<ProtoFilterPattern> patterns = null;

    public ProtoFilterSettings()
    {
    }

    public int getVersion()
    { return this.version; }

    public void setVersion(int i)
    { this.version = i; }
    
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

    public LinkedList<ProtoFilterPattern> getPatterns()
    { return patterns; }

    public void setPatterns(LinkedList<ProtoFilterPattern> s)
    { this.patterns = s; }
}
