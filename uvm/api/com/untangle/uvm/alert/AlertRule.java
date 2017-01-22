/**
 * $Id: AlertRule.java,v 1.00 2014/11/05 15:17:53 dmorris Exp $
 */

package com.untangle.uvm.alert;

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
 * This in the implementation of a Alert Rule
 * 
 * A rule is basically a collection of AlertRuleConditions (matchers) and what
 * to do if the matchers match (log, alert, or both)
 */
@SuppressWarnings("serial")
public class AlertRule implements JSONString, Serializable
{
    private static final Logger logger = Logger.getLogger( AlertRule.class );

    private static final int LOAD_STATE_CACHE_MAX_SIZE = 100;
    
    private Integer ruleId;
    private Boolean enabled;
    private Boolean log;
    private Boolean alert;
    private Boolean alertLimitFrequency = false;
    private Integer alertLimitFrequencyMinutes = 0;
    private Boolean thresholdEnabled;
    private Double  thresholdLimit;
    private Integer thresholdTimeframeSec;
    private String  thresholdGroupingField;

    private String description;

    private long lastAlertTime = 0; /* stores the last time this rule sent an alert */
    private Map<String,Load> loadStateCache = null;
    
    private List<AlertRuleCondition> conditions;

    public AlertRule()
    {
    }

    public AlertRule( boolean enabled, List<AlertRuleCondition> conditions, boolean log, boolean alert, String description, boolean frequencyLimit, int frequencyMinutes,
                      Boolean thresholdEnabled, Double thresholdLimit, Integer thresholdTimeframeSec, String thresholdGroupingField )
    {
        this.setEnabled( enabled );
        this.setConditions( conditions );
        this.setLog( log );
        this.setAlert( alert );
        this.setDescription( description );
        this.setAlertLimitFrequency( frequencyLimit );
        this.setAlertLimitFrequencyMinutes( frequencyMinutes );
        this.thresholdEnabled = thresholdEnabled;
        this.thresholdLimit = thresholdLimit;
        this.thresholdTimeframeSec = thresholdTimeframeSec;
        this.thresholdGroupingField = thresholdGroupingField;
    }
    
    public AlertRule( boolean enabled, List<AlertRuleCondition> conditions, boolean log, boolean alert, String description, boolean frequencyLimit, int frequencyMinutes )
    {
        this( enabled, conditions, log, alert, description, frequencyLimit, frequencyMinutes, null, null, null, null );
    }

    public List<AlertRuleCondition> getConditions() { return this.conditions; }
    public void setConditions( List<AlertRuleCondition> newValue ) { this.conditions = newValue; }

    public Integer getRuleId() { return this.ruleId; }
    public void setRuleId( Integer newValue ) { this.ruleId = newValue; }

    public Boolean getEnabled() { return enabled; }
    public void setEnabled( Boolean newValue ) { this.enabled = newValue; }

    public Boolean getLog() { return log; }
    public void setLog( Boolean newValue ) { this.log = newValue; }

    public Boolean getAlert() { return alert; }
    public void setAlert( Boolean newValue ) { this.alert = newValue; }

    public Boolean getAlertLimitFrequency() { return alertLimitFrequency; }
    public void setAlertLimitFrequency( Boolean newValue ) { this.alertLimitFrequency = newValue; }

    public Integer getAlertLimitFrequencyMinutes() { return alertLimitFrequencyMinutes; }
    public void setAlertLimitFrequencyMinutes( Integer newValue ) { this.alertLimitFrequencyMinutes = newValue; }

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

    public long lastAlertTime()
    {
        return this.lastAlertTime;
    }

    public void updateAlertTime()
    {
        this.lastAlertTime = System.currentTimeMillis();
    }
    
    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }

    public boolean isMatch( JSONObject obj )
    {
        for ( AlertRuleCondition condition : conditions ) {
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
     * This is called when this alert rule matches to update the new load computations
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
