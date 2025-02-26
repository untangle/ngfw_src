/**
 * $Id$
 */
package com.untangle.app.web_filter;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import org.json.JSONObject;
import org.json.JSONString;

import com.untangle.uvm.app.GenericRule;
/**
 * Global pass site urls .
 */
@SuppressWarnings("serial")
public class GlobalPassedUrls implements Serializable, JSONString{
    private int version = 1;
    private List<GenericRule> globalPassedUrls = new LinkedList<GenericRule>();

    
    /**
     * Get the version of settings
    * @return version
    */
    public int getVersion() { return version; }
    /**
     * Set the version of settings
     * @param version of settings
     */
    public void setVersion(int version) { this.version = version; }
 

    public List<GenericRule> getGlobalPassedUrls() { return globalPassedUrls; }
    public void setGlobalPassedUrls( List<GenericRule > globalPassedUrls) { this.globalPassedUrls = globalPassedUrls; }

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
}
