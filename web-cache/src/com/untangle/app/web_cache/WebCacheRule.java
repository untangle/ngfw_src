/**
 * $Id$
 */

package com.untangle.app.web_cache;

import java.io.Serializable;

import org.json.JSONObject;
import org.json.JSONString;

/**
 * This is the implementation of a web cache rule which are used to specify
 * hosts for which content should never be cached.
 * 
 * @author mahotz
 * 
 */
@SuppressWarnings("serial")
public class WebCacheRule implements JSONString, Serializable
{
    private int id;
    private boolean live;
    private String hostname;

    public WebCacheRule()
    {
    }

    public WebCacheRule(String hostname, boolean live)
    {
        this.hostname = hostname;
        this.live = live;
    }

    public int getId()
    {
        return (id);
    }

    public void setId(int id)
    {
        this.id = id;
    }

    public boolean isLive()
    {
        return (live);
    }

    public void setLive(boolean live)
    {
        this.live = live;
    }

    public String getHostname()
    {
        return (hostname);
    }

    public void setHostname(String hostname)
    {
        this.hostname = hostname;
    }

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
}
