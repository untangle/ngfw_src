/**
 * $Id: ReportEntry.java,v 1.00 2015/02/24 15:19:32 dmorris Exp $
 */
package com.untangle.app.reports;

import java.io.Serializable;
import java.util.List;

import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.json.JSONString;

/**
 * The settings for an individual report entry (graph)
 */
@SuppressWarnings("serial")
public class EmailTemplate implements JSONString, Serializable
{
    private static final Logger logger = Logger.getLogger( EmailTemplate.class );

    private Integer templateId;
    private String title;
    private String description;
    private Integer interval;
    private Integer intervalWeekStart = 0;
    private Boolean mobile;
    private Boolean readOnly = null; /* If the rule is read-only (built-in) */
    private List<String> enabledConfigIds = null;
    private List<String> enabledAppIds = null;

    public EmailTemplate()
    {
    }

    public EmailTemplate( String title, String description, Integer interval, Boolean mobile, List<String> enabledConfigIds, List<String> enabledAppIds)
    {
        this.setTitle( title );
        this.setDescription( description );
        this.setInterval( interval );
        this.setMobile( mobile );
        this.setEnabledConfigIds( enabledConfigIds );
        this.setEnabledAppIds( enabledAppIds );
    }
    
    public Integer getTemplateId() { return this.templateId; }
    public void setTemplateId( Integer newValue ) { this.templateId = newValue; }

    public String getTitle() { return this.title; }
    public void setTitle( String newValue ) { this.title = newValue; }

    public String getDescription() { return this.description; }
    public void setDescription( String newValue ) { this.description = newValue; }

    public Integer getInterval() { return this.interval; }
    public void setInterval( Integer newValue ) { this.interval = newValue; }

    public Integer getIntervalWeekStart() { return this.intervalWeekStart; }
    public void setIntervalWeekStart( Integer newValue ) { this.intervalWeekStart = newValue; }

    public Boolean getMobile() { return this.mobile; }
    public void setMobile( Boolean newValue ) { this.mobile = newValue; }

    public Boolean getReadOnly() { return this.readOnly; }
    public void setReadOnly( Boolean newValue ) { this.readOnly = newValue; }
    
     public List<String> getEnabledConfigIds() { return this.enabledConfigIds; }
    public void setEnabledConfigIds( List<String> newValue ) { this.enabledConfigIds = newValue; }

    public List<String> getEnabledAppIds() { return this.enabledAppIds; }
    public void setEnabledAppIds( List<String> newValue ) { this.enabledAppIds = newValue; }

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }

    
}
