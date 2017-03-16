/**
 * $Id$
 */
package com.untangle.app.application_control_lite;

import java.io.Serializable;

/**
 * Rule for proto filter patterns
 *
 */
@SuppressWarnings("serial")
public class ApplicationControlLitePattern implements Serializable
{
    private Long id;
    private String protocol = "none";
    private String description = "None";
    private String category = "None";
    private String definition = "";
    private String quality = "Bad";
    private boolean blocked = false;
    private boolean alert = false;
    private boolean log = false;

    public ApplicationControlLitePattern() { }

    public ApplicationControlLitePattern(int mvid, String protocol, String category,
                                         String description, String definition,  String quality,
                                         boolean blocked, boolean alert, boolean log)
    {
        //this.mvid = mvid;
        this.protocol = protocol;
        this.category = category;
        this.description = description;
        this.definition = definition;
        this.quality = quality;
        this.blocked = blocked;
        this.alert = alert;
        this.log = log;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    /**
     * Protocol name
     */
    public String getProtocol() { return this.protocol; }
    public void setProtocol(String s) { this.protocol = s; }

    /**
     * Description name
     */
    public String getDescription() { return this.description; }
    public void setDescription(String s) { this.description = s; }

    /**
     * Category of the rule
     */
    public String getCategory() { return this.category; }
    public void setCategory(String s) { this.category = s; }

    /**
     * Definition (Regex) of the rule
     */
    public String getDefinition() { return this.definition; }

    public void setDefinition(String s)
    {
        this.definition = s;
    }

    /**
     * Flag that indicates if the traffic should be quality
     */
    public String getQuality() { return this.quality; }
    public void setQuality(String s) { this.quality = s; }

    /**
     * Flag that indicates if the traffic should be blocked
     */
    public boolean isBlocked() { return this.blocked; }
    public void setBlocked(boolean b) { this.blocked = b; }

    /**
     * Should admin be alerted.
     *
     * @return true if alerts should be sent.
     */
    public boolean getAlert()
    {
        return alert;
    }
    public void setAlert(boolean alert)
    {
        this.alert = alert;
    }

    /**
     * Should admin be logged.
     *
     * @return true if should be logged.
     */
    public boolean getLog()
    {
        return log;
    }
    public void setLog(boolean log)
    {
        this.log = log;
    }

    public void updateRule(ApplicationControlLitePattern pattern)
    {
        this.protocol = pattern.protocol;
        this.category = pattern.category;
        this.description = pattern.description;
        this.definition = pattern.definition;
        this.blocked = pattern.blocked;
        this.alert = pattern.alert;
        this.log = pattern.log;
    }
}
