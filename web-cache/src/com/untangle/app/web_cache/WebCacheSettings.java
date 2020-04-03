/**
 * $Id$
 */

package com.untangle.app.web_cache;

import java.util.HashSet;
import java.util.LinkedList;
import org.json.JSONString;
import org.json.JSONObject;

/**
 * This is the implementation of the web cache settings.
 * 
 * @author mahotz
 * 
 */
@SuppressWarnings("serial")
public class WebCacheSettings implements java.io.Serializable, JSONString
{
    private LinkedList<WebCacheRule> ruleList = null;
    private HashSet<String> ruleHash = null;
    private Float loadLimit = (float) 3.5;

    public LinkedList<WebCacheRule> getRules()
    {
        return (ruleList);
    }

    public void setRules(LinkedList<WebCacheRule> ruleList)
    {
        this.ruleList = ruleList;
        this.ruleHash = new HashSet<String>(ruleList.size());

        // add all the active rules to the hashset
        for (int x = 0; x < ruleList.size(); x++) {
            WebCacheRule curr = ruleList.get(x);
            if (curr.isLive() == true) ruleHash.add(curr.getHostname().toLowerCase());
        }
    }

    public Float getLoadLimit()
    {
        return (loadLimit);
    }

    public void setLoadLimit(Float loadLimit)
    {
        this.loadLimit = loadLimit;
    }

    public boolean checkRules(String hostName)
    {
        if (ruleHash.contains(hostName) == true) return (true);
        return (false);
    }

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
}
