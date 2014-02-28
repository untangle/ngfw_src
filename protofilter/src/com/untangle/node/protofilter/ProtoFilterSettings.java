/**
 * $Id$
 */
package com.untangle.node.protofilter;

import java.util.LinkedList;

/**
 * Settings for the ProtoFilter node.
 *
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

    public int getVersion() { return this.version; }
    public void setVersion( int newValue ) { this.version = newValue; }
    
    public int getByteLimit() { return this.byteLimit; }
    public void setByteLimit( int newValue ) { this.byteLimit = newValue; }

    public int getChunkLimit() { return this.chunkLimit; }
    public void setChunkLimit( int newValue ) { this.chunkLimit = newValue; }

    public String getUnknownString() { return this.unknownString; }
    public void setUnknownString( String newValue ) { this.unknownString = newValue; }

    public boolean isStripZeros() { return this.stripZeros; }
    public void setStripZeros( boolean newValue ) { this.stripZeros = newValue; }

    public LinkedList<ProtoFilterPattern> getPatterns() { return patterns; }
    public void setPatterns( LinkedList<ProtoFilterPattern> newValue ) { this.patterns = newValue; }
}
