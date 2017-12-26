/**
 * $Id$
 */
package com.untangle.app.application_control_lite;

import java.util.LinkedList;

/**
 * Settings for the ApplicationControlLite app
 */
@SuppressWarnings("serial")
public class ApplicationControlLiteSettings implements java.io.Serializable, org.json.JSONString
{
    private int version = 1;
    private int byteLimit  = 2048;
    private int chunkLimit = 10;
    private String unknownString = "[unknown]";
    private boolean stripZeros = true;
    private LinkedList<ApplicationControlLitePattern> patterns = null;

    public ApplicationControlLiteSettings()
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

    public LinkedList<ApplicationControlLitePattern> getPatterns() { return patterns; }
    public void setPatterns( LinkedList<ApplicationControlLitePattern> newValue ) { this.patterns = newValue; }

    public String toJSONString()
    {
        org.json.JSONObject jO = new org.json.JSONObject(this);
        return jO.toString();
    }
}
