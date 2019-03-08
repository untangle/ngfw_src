/**
 * $Id$
 */
package com.untangle.app.application_control_lite;

/**
 * Rule/Signature (regex pattern) for application control lite patterns
 */
@SuppressWarnings("serial")
public class ApplicationControlLitePattern implements java.io.Serializable, org.json.JSONString
{
    private Long id;
    private String protocol = "none";
    private String description = "None";
    private String category = "None";
    private String definition = "";
    private String quality = "Bad";
    private boolean blocked = false;
    private boolean log = false;

    public ApplicationControlLitePattern() { }

    public ApplicationControlLitePattern(int mvid, String protocol, String category,
                                         String description, String definition,  String quality,
                                         boolean blocked, boolean log)
    {
        //this.mvid = mvid;
        this.protocol = protocol;
        this.category = category;
        this.description = description;
        this.definition = definition;
        this.quality = quality;
        this.blocked = blocked;
        this.log = log;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getProtocol() { return this.protocol; }
    public void setProtocol(String s) { this.protocol = s; }

    public String getDescription() { return this.description; }
    public void setDescription(String s) { this.description = s; }

    public String getCategory() { return this.category; }
    public void setCategory(String s) { this.category = s; }

    public String getDefinition() { return this.definition; }
    public void setDefinition(String s) { this.definition = s; }

    public String getQuality() { return this.quality; }
    public void setQuality(String s) { this.quality = s; }

    public boolean isBlocked() { return this.blocked; }
    public void setBlocked(boolean b) { this.blocked = b; }

    public boolean getLog() { return log; }
    public void setLog(boolean log) { this.log = log; }

    public void updateRule(ApplicationControlLitePattern pattern)
    {
        this.protocol = pattern.protocol;
        this.category = pattern.category;
        this.description = pattern.description;
        this.definition = pattern.definition;
        this.blocked = pattern.blocked;
        this.log = pattern.log;
    }

    public String toJSONString()
    {
        org.json.JSONObject jO = new org.json.JSONObject(this);
        return jO.toString();
    }
}
