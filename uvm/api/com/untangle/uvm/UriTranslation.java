/**
 * $Id$
 */

package com.untangle.uvm;

import java.io.Serializable;

import org.json.JSONObject;
import org.json.JSONString;

/**
 * Uri Translation
 */
@SuppressWarnings("serial")
public class UriTranslation implements Serializable, JSONString
{
    private String uri = null;
    private String scheme = null;
    private String host = null;
    private String path = null;
    private String query = null;
    private Integer port = null;

    public UriTranslation() {}
    
    public String getUri() { return uri; }
    public void setUri( String uri ) { this.uri = uri; }

    public String getScheme() { return scheme; }
    public void setScheme( String scheme ) { this.scheme = scheme; }

    public String getHost() { return host; }
    public void setHost( String host ) { this.host = host; }

    public String getPath() { return path; }
    public void setPath( String path ) { this.path = path; }

    public String getQuery() { return query; }
    public void setQuery( String query ) { this.query = query; }

    public Integer getPort() { return port; }
    public void setPort( Integer port ) { this.port = port; }

    public String toJSONString()
    {
        org.json.JSONObject jO = new org.json.JSONObject(this);
        return jO.toString();
    }

}