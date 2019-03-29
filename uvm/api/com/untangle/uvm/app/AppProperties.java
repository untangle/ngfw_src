/**
 * $Id$
 */
package com.untangle.uvm.app;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.Arrays;

import org.json.JSONObject;
import org.json.JSONString;

/**
 * The immutable properties of a App
 */
@SuppressWarnings("serial")
public class AppProperties implements Serializable, JSONString, Comparable<AppProperties>
{
    private String name = null;
    private String displayName = null;
    private String className = null;
    private String appBase = null;
    private String daemon = null;

    public enum Type {
        FILTER,
        SERVICE,
        UNKNOWN
    }
    private Type type;
    
    private boolean hasPowerButton = true;
    private boolean autoStart = true;
    private boolean autoLoad = false;
    private boolean invisible = false;
    private int     viewPosition = -1;

    private List<String> parents = new LinkedList<>();

    private List<String> supportedArchitectures = Arrays.asList("any");
    private Long         minimumMemory;

    public AppProperties() {}
    
    /**
     * Internal name of the app.
     */
    public String getName() { return name; }
    public void setName( String newValue ) { this.name = newValue; }

    /**
     * Name of the main app Class.
     */
    public String getClassName() { return className; }
    public void setClassName( String newValue ) { this.className = newValue; }

    /**
     * The parent app, usually a casing.
     */
    public List<String> getParents() { return parents; }
    public void setParents( List<String> newValue ) { this.parents = newValue; }

    /**
     * Get supported architectures
     */
    public List<String> getSupportedArchitectures() { return supportedArchitectures; }
    public void setSupportedArchitectures( List<String> newValue ) { this.supportedArchitectures = newValue; }

    /**
     * Get minimum memory requirements (null if none)
     */
    public Long getMinimumMemory() { return minimumMemory; }
    public void setMinimumMemory( Long newValue ) { this.minimumMemory = newValue; }

    /**
     * The name of the app, for display purposes.
     */
    public String getDisplayName() { return displayName; }
    public void setDisplayName( String newValue ) { this.displayName = newValue; }
    
    /**
     * The appBase is the name of the base app. For example
     * clam-app's appBase is untangle-base-virus-blocker.
     */
    public String getAppBase() { return appBase; }
    public void setAppBase( String newValue ) { this.appBase = newValue; }

    /**
     * The dawmon is the optional name of an associated daemon.
     */
    public String getDaemon() { return daemon; }
    public void setDaemon( String newValue ) { this.daemon = newValue; }

    /**
     * The type is the type of app
     */
    public Type getType() { return type; }
    public void setType( Type newValue ) { this.type = newValue; }

    /**
     * The view position in the rack
     */
    public int getViewPosition() { return viewPosition; }
    public void setViewPosition( int newValue ) { this.viewPosition = newValue; }
    
    /**
     * True if this app can be turned on and off.  False, otherwise.
     */
    public boolean getHasPowerButton() { return hasPowerButton; }
    public void setHasPowerButton( boolean newValue ) { this.hasPowerButton = newValue; }

    /**
     * True if this app should be started automatically (once loaded).
     */
    public boolean getAutoStart() { return autoStart; }
    public void setAutoStart( boolean newValue ) { this.autoStart = newValue; }

    /**
     * True if this app should be loaded automatically.
     */
    public boolean getAutoLoad() { return autoLoad; }
    public void setAutoLoad( boolean newValue ) { this.autoLoad = newValue; }
    
    /**
     * True if this app should be started automatically.
     */
    public boolean getInvisible() { return invisible; }
    public void setInvisible( boolean newValue ) { this.invisible = newValue; }
    
    public boolean equals(Object o)
    {
        if (!(o instanceof AppProperties)) {
            return false;
        }

        AppProperties td = (AppProperties)o;

        return getName().equals( td.getName() );
    }

    public String toString()
    {
        return toJSONString();
    }

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }

    public int compareTo( AppProperties a )
    {
        return new Integer(getViewPosition()).compareTo(a.getViewPosition());
    }

    public int hashCode( )
    {
        if ( getClassName() != null )
            return getClassName().hashCode();
        else
            return 0;
    }
}
