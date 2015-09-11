package com.untangle.node.web_cache; // API

import java.io.Serializable;

import org.json.JSONObject;
import org.json.JSONString;

@SuppressWarnings("serial")
public class WebCacheRule implements JSONString, Serializable
{
    private int id;
    private boolean live;
    private String hostname;

    public WebCacheRule()
    {
    }

    public WebCacheRule(String hostname,boolean live)
    {
        this.hostname = hostname;
        this.live = live;
    }

    public int getId()
    {
        return(id);
    }

    public void setId(int id)
    {
        this.id = id;
    }

    public boolean isLive()
    {
        return(live);
    }

    public void setLive( boolean live )
    {
        this.live = live;
    }

    public String getHostname()
    {
        return(hostname);
    }

    public void setHostname( String hostname )
    {
        this.hostname = hostname;
    }

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
}
