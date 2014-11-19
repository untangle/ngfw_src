/**
 * $Id: AlertRuleMatcherField.java 37267 2014-02-26 23:42:19Z dmorris $
 */
package com.untangle.node.reporting;

import org.json.JSONObject;

import org.apache.log4j.Logger;

import com.untangle.uvm.node.RuleMatcher;
import com.untangle.uvm.node.GlobMatcher;

/**
 * This is a matching criteria for a Alert Control Rule
 *
 * A AlertRule has a set of these to determine what traffic to match
 */
@SuppressWarnings("serial")
public class AlertRuleMatcherField
{
    private static final Logger logger = Logger.getLogger( AlertRuleMatcherField.class );

    private String field;
    private String comparator;
    private String value;

    private GlobMatcher stringGlobMatcher = null;
    
    public AlertRuleMatcherField() { }

    public AlertRuleMatcherField( String field, String comparator, String value )
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

    public boolean isMatch( JSONObject obj )
    {
        if ( field == null || comparator == null || value == null )
            return false;
        if ( ! obj.has(  field ) )
            return false;
        Object actualValueObj;
        try {
            actualValueObj = obj.get( field );
            if ( actualValueObj == null )
                return false;
        } catch (Exception e) {
            return false;
        }

        /**
         * Handle all the different types
         * Number is handled specially > and < operators
         */
        if ( actualValueObj instanceof Number ) {
            try {
                long specifiedValue = Long.parseLong( value );
                long actualValue = (long) actualValueObj;
                if ( "=".equals(comparator) ) {
                    return (actualValue == specifiedValue);
                } else if ( ">".equals(comparator) ) {
                    return (actualValue > specifiedValue);
                } else if ( "<".equals(comparator) ) {
                    return (actualValue < specifiedValue);
                }
            } catch ( Exception e ) {}

            try {
                double specifiedValue = Double.parseDouble( value );
                double actualValue = (double)actualValueObj;
                if ( "=".equals(comparator) ) {
                    return (actualValue == specifiedValue);
                } else if ( ">".equals(comparator) ) {
                    return (actualValue > specifiedValue);
                } else if ( "<".equals(comparator) ) {
                    return (actualValue < specifiedValue);
                }
            } catch ( Exception e ) {}

            return false;
        }


        /**
         * If its not a number treat it as a string
         */
        if ( ! "=".equals( comparator ) ) // String only supports "=" operator
            return false;
        String actualValueStr = actualValueObj.toString().toLowerCase();
        //logger.warn("XXX check: " + actualValueStr + " against " + value );

        if ( this.stringGlobMatcher == null )
            this.stringGlobMatcher = new GlobMatcher( value );
        if ( this.stringGlobMatcher == null )
            return false;

        boolean result = this.stringGlobMatcher.isMatch( actualValueStr );

        //logger.warn("XXX check: " + actualValueStr + " against " + value + " result: " + result );
        return result;

    }

}
