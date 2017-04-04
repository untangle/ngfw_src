/**
 * $Id: EventRuleConditionField.java 37267 2014-02-26 23:42:19Z dmorris $
 */
package com.untangle.uvm.event;

import java.util.Iterator;

import org.json.JSONObject;

import org.apache.log4j.Logger;

import com.untangle.uvm.app.RuleCondition;
import com.untangle.uvm.app.GlobMatcher;

/**
 * DEPRECATED
 * DEPRECATED
 * DEPRECATED
 * This file can be removed in 13.1
 * DEPRECATED
 * DEPRECATED
 * DEPRECATED
 *
 * This is a matching criteria for a Event Control Rule
 *
 * A EventRule has a set of these to determine what traffic to match
 */
@SuppressWarnings("serial")
public class EventRuleConditionField
{
    private static final Logger logger = Logger.getLogger( EventRuleConditionField.class );

    private String field;
    private String comparator;
    private String value;

    private GlobMatcher stringGlobMatcher = null;
    
    public EventRuleConditionField() { }

    public EventRuleConditionField( String field, String comparator, String value )
    {
        this.field = field;
        this.comparator = comparator;
        this.value = value;
    }

    public String getField() { return field; }
    public void setField( String newValue ) { this.field = newValue; }

    public String getComparator() { return comparator; }
    public void setComparator( String newValue ) { this.comparator = newValue; }

    public String getValue() { return value; }
    public void setValue( String newValue ) { this.value = newValue; }

}
