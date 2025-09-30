/**
 * $Id$
 */

package com.untangle.app.dynamic_blocklists;

import org.json.JSONObject;
import org.json.JSONString;

import java.io.Serializable;

/**
 * SourceList
 */
@SuppressWarnings("serial")
public class DynamicBlockList implements Serializable, JSONString {

    private String id;
    private boolean enabled;
    private String name;
    private String source;
    private String parsingMethod;
    private int pollingTime;
    private String pollingUnit;
    private boolean skipCertCheck;
    private String type;
    private long count;
    private long lastUpdated;

    public DynamicBlockList() { }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getParsingMethod() { return parsingMethod; }
    public void setParsingMethod(String parsingMethod) { this.parsingMethod = parsingMethod; }

    public int getPollingTime() { return pollingTime; }
    public void setPollingTime(int pollingTime) { this.pollingTime = pollingTime; }

    public String getPollingUnit() { return pollingUnit; }
    public void setPollingUnit(String pollingUnit) { this.pollingUnit = pollingUnit; }

    public boolean getSkipCertCheck() { return skipCertCheck; }
    public void setSkipCertCheck(boolean skipCertCheck) { this.skipCertCheck = skipCertCheck; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public long getCount() { return count; }
    public void setCount(long count) { this.count = count; }

    public long getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(long lastUpdated) { this.lastUpdated = lastUpdated; }

    @Override
    public String toJSONString() {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
}
