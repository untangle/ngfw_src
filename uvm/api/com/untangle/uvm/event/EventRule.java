/**
 * $Id: EventRule.java,v 1.00 2014/11/05 15:17:53 dmorris Exp $
 */

package com.untangle.uvm.event;

import java.util.List;
import java.io.Serializable;
import java.net.InetAddress;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Iterator;

import org.json.JSONObject;
import org.json.JSONString;
import org.apache.log4j.Logger;

import com.untangle.uvm.vnet.NodeSession;
import com.untangle.uvm.util.Load;

/**
 * This in the implementation of a Event Rule
 * 
 * A rule is basically a collection of EventRuleConditions (matchers) and what
 * to do if the matchers match (log, email, etc)
 */
@SuppressWarnings("serial")
public class EventRule implements JSONString, Serializable
{
    private static final Logger logger = Logger.getLogger( EventRule.class );

    private static final int LOAD_STATE_CACHE_MAX_SIZE = 100;
    
    private Integer ruleId;
    private Boolean enabled;
    private Boolean log;
    private Boolean event;
    private Boolean eventLimitFrequency = false;
    private Integer eventLimitFrequencyMinutes = 0;
    private Boolean thresholdEnabled;
    private Double  thresholdLimit;
    private Integer thresholdTimeframeSec;
    private String  thresholdGroupingField;

    private String description;

    private long lastEventTime = 0; /* stores the last time this rule sent an event */
    private Map<String,Load> loadStateCache = null;
    
    private List<EventRuleCondition> conditions;

    public EventRule()
    {
    }

    public EventRule( boolean enabled, List<EventRuleCondition> conditions, boolean log, boolean event, String description, boolean frequencyLimit, int frequencyMinutes,
                      Boolean thresholdEnabled, Double thresholdLimit, Integer thresholdTimeframeSec, String thresholdGroupingField )
    {
        this.setEnabled( enabled );
        this.setConditions( conditions );
        this.setLog( log );
        this.setEvent( event );
        this.setDescription( description );
        this.setEventLimitFrequency( frequencyLimit );
        this.setEventLimitFrequencyMinutes( frequencyMinutes );
        this.thresholdEnabled = thresholdEnabled;
        this.thresholdLimit = thresholdLimit;
        this.thresholdTimeframeSec = thresholdTimeframeSec;
        this.thresholdGroupingField = thresholdGroupingField;
    }
    
    public EventRule( boolean enabled, List<EventRuleCondition> conditions, boolean log, boolean event, String description, boolean frequencyLimit, int frequencyMinutes )
    {
        this( enabled, conditions, log, event, description, frequencyLimit, frequencyMinutes, null, null, null, null );
    }

    public List<EventRuleCondition> getConditions() { return this.conditions; }
    public void setConditions( List<EventRuleCondition> newValue ) { this.conditions = newValue; }

    public Integer getRuleId() { return this.ruleId; }
    public void setRuleId( Integer newValue ) { this.ruleId = newValue; }

    public Boolean getEnabled() { return enabled; }
    public void setEnabled( Boolean newValue ) { this.enabled = newValue; }

    public Boolean getLog() { return log; }
    public void setLog( Boolean newValue ) { this.log = newValue; }

    public Boolean getEvent() { return event; }
    public void setEvent( Boolean newValue ) { this.event = newValue; }

    public Boolean getEventLimitFrequency() { return eventLimitFrequency; }
    public void setEventLimitFrequency( Boolean newValue ) { this.eventLimitFrequency = newValue; }

    public Integer getEventLimitFrequencyMinutes() { return eventLimitFrequencyMinutes; }
    public void setEventLimitFrequencyMinutes( Integer newValue ) { this.eventLimitFrequencyMinutes = newValue; }

    public Boolean getThresholdEnabled() { return this.thresholdEnabled; }
    public void setThresholdEnabled( Boolean newValue ) { this.thresholdEnabled = newValue; }

    public Double getThresholdLimit() { return this.thresholdLimit; }
    public void setThresholdLimit( Double newValue ) { this.thresholdLimit = newValue; }

    public Integer getThresholdTimeframeSec() { return this.thresholdTimeframeSec; }
    public void setThresholdTimeframeSec( Integer newValue ) { this.thresholdTimeframeSec = newValue; }

    public String getThresholdGroupingField() { return this.thresholdGroupingField; }
    public void setThresholdGroupingField( String newValue ) { this.thresholdGroupingField = newValue; }
        
    public String getDescription() { return description; }
    public void setDescription( String newValue ) { this.description = newValue; }

    public long lastEventTime()
    {
        return this.lastEventTime;
    }

    public void updateEventTime()
    {
        this.lastEventTime = System.currentTimeMillis();
    }
    
    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }

    public boolean isMatch( JSONObject obj )
    {
        for ( EventRuleCondition condition : conditions ) {
            if ( ! condition.isMatch ( obj ) ) {
                return false;
            }
        }

        if ( thresholdEnabled() ) {
            Double load = incrementLoad( obj );
            Double limit = getThresholdLimit();

            if ( limit != null ) {
                if ( load == null ) {
                    return false;
                }
                if ( load < limit ) {
                    return false;
                }
            }
        }
        
        return true;
    }

    /**
     * This is called when this event rule matches to update the new load computations
     */
    private Double incrementLoad( JSONObject obj )
    {
        if ( ! thresholdEnabled() )
            return null;

        String groupingField = null;
        if ( this.thresholdGroupingField != null )
            groupingField = this.thresholdGroupingField;
        String groupingFieldValue = "global"; //the default if no field is specified
        try {
            if ( groupingField != null ) {
                Object o = obj.get( groupingField );
                if ( o != null )
                    groupingFieldValue = o.toString();
            }
        } catch (Exception e) {/* if the key doesnt exist we do not care*/}

        if ( this.loadStateCache == null )
            this.loadStateCache = createLoadCache( LOAD_STATE_CACHE_MAX_SIZE );

        Load loadState = this.loadStateCache.get( groupingFieldValue );
        if ( loadState == null ) {
            loadState = new Load( this.thresholdTimeframeSec );
            this.loadStateCache.put( groupingFieldValue, loadState );
        }

        return loadState.incrementLoad();
    }

    private boolean thresholdEnabled()
    {
        if ( thresholdEnabled == null || Boolean.FALSE == thresholdEnabled )
            return false;
        if ( thresholdLimit == null || thresholdTimeframeSec == null )
            return false;
        return true;
    }
        
    private static <K,V> Map<K,V> createLoadCache(final int maxSize)
    {
        return new LinkedHashMap<K,V>(maxSize*4/3, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K,V> eldest)
            {
                return size() > maxSize;
            }
        };
    }
}
