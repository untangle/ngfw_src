/**
 * $Id: EventRuleCondition.java 37267 2014-02-26 23:42:19Z dmorris $
 */
package com.untangle.uvm.event;

import org.json.JSONObject;

import org.apache.log4j.Logger;

import com.untangle.uvm.app.RuleCondition;
import com.untangle.uvm.app.GlobMatcher;

/**
 * This is a matching criteria for a Event Control Rule
 *
 * A EventRule has a set of these to determine what traffic to match
 */
@SuppressWarnings("serial")
public class EventRuleCondition
{
    private static final Logger logger = Logger.getLogger( EventRuleCondition.class );

    //private EventRuleConditionField value;
    private String field;
    private String comparator;
    private String value;

    private GlobMatcher stringGlobMatcher = null;
    
    public EventRuleCondition()
    {
    }

    public EventRuleCondition( String field, String comparator, String value )
    {
        this.field = field;
        this.comparator = comparator;
        this.value = value;
    }

    public String getField() { return field; }
    public void setField( String newValue ) { this.field = newValue; }

    public String getComparator() { return comparator; }
    public void setComparator( String newValue ) { this.comparator = newValue; }

    /**
     * This returns an Object instead of a string so its a proper getter/setter pair
     */
    public Object getValue() { return value; }

    /**
     * The setter takes a Object instead of a string so it accepts the old <13.0 format
     */
    public void setValue( Object newValue )
    {
        if ( newValue instanceof String ) {
            this.value = (String)newValue;
            return;
        }
        /**
         * 13.0 conversion
         * Prior to 13.0 we stored these inside a "sub-object" EventRuleConditionField
         * This will dynamically converter the old format to the new format
         * We now store the field, comparator, and value in the EventRuleCondition
         */
        if ( newValue instanceof EventRuleConditionField ) {
            EventRuleConditionField field = (EventRuleConditionField) newValue;
            this.field = field.getField();
            this.comparator = field.getComparator();
            this.value = field.getValue();
        }
    }

    public boolean isMatch( JSONObject obj )
    {
        if ( field == null || comparator == null || value == null )
            return false;


        Object actualValueObj = null;
        try { actualValueObj = obj.get( field ); } catch (Exception e) {}

        if ( actualValueObj == null && "class".equals(field) ) {
            try { actualValueObj = obj.get( "javaClass" ); } catch (Exception e) {}
        }
        if ( actualValueObj == null && "javaClass".equals(field) ) {
            try { actualValueObj = obj.get( "class" ); } catch (Exception e) {}
        }
        if ( actualValueObj == null ) {
            //logger.warn("DEBUG missing field: " + field + " value: " + actualValueObj );
            return false;
        }

        /**
         * Handle all the different types
         * Number is handled specially > and < operators
         */
        if ( actualValueObj instanceof Number ) {
            //logger.warn("DEBUG number eval: " + actualValueObj);

            try {
                int specifiedValue = Integer.parseInt( value );
                int actualValue = (int) actualValueObj;
                //logger.warn("DEBUG integer check: " + specifiedValue + " against " + actualValueObj );

                if ( "=".equals(comparator) ) {
                    return (actualValue == specifiedValue);
                } else if ( "!=".equals(comparator) ) {
                    return (actualValue != specifiedValue);
                } else if ( ">".equals(comparator) ) {
                    return (actualValue > specifiedValue);
                } else if ( ">=".equals(comparator) ) {
                    return (actualValue >= specifiedValue);
                } else if ( "<".equals(comparator) ) {
                    return (actualValue < specifiedValue);
                } else if ( "<=".equals(comparator) ) {
                    return (actualValue <= specifiedValue);
                }
            } catch ( Exception e ) {
                //logger.warn("DEBUG Exception",e );
            }

            try {
                long specifiedValue = Long.parseLong( value );
                long actualValue = (long) actualValueObj;
                //logger.warn("DEBUG long check: " + specifiedValue + " against " + actualValueObj );

                if ( "=".equals(comparator) ) {
                    return (actualValue == specifiedValue);
                } else if ( "!=".equals(comparator) ) {
                    return (actualValue != specifiedValue);
                } else if ( ">".equals(comparator) ) {
                    return (actualValue > specifiedValue);
                } else if ( ">=".equals(comparator) ) {
                    return (actualValue >= specifiedValue);
                } else if ( "<".equals(comparator) ) {
                    return (actualValue < specifiedValue);
                } else if ( "<=".equals(comparator) ) {
                    return (actualValue <= specifiedValue);
                }
            } catch ( Exception e ) {
                //logger.warn("DEBUG Exception",e );
            }

            try {
                double specifiedValue = Double.parseDouble( value );
                double actualValue = (double)actualValueObj;
                //logger.warn("DEBUG double check: " + specifiedValue + " against " + actualValueObj );

                if ( "=".equals(comparator) ) {
                    return (actualValue == specifiedValue);
                } else if ( "!=".equals(comparator) ) {
                    return (actualValue != specifiedValue);
                } else if ( ">".equals(comparator) ) {
                    return (actualValue > specifiedValue);
                } else if ( ">=".equals(comparator) ) {
                    return (actualValue >= specifiedValue);
                } else if ( "<".equals(comparator) ) {
                    return (actualValue < specifiedValue);
                } else if ( "<=".equals(comparator) ) {
                    return (actualValue <= specifiedValue);
                }
            } catch ( Exception e ) {
                //logger.warn("DEBUG Exception",e );
            }

            try {
                float specifiedValue = Float.parseFloat( value );
                float actualValue = (float)actualValueObj;
                //logger.warn("DEBUG float check: " + specifiedValue + " against " + actualValueObj );

                if ( "=".equals(comparator) ) {
                    return (actualValue == specifiedValue);
                } else if ( "!=".equals(comparator) ) {
                    return (actualValue != specifiedValue);
                } else if ( ">".equals(comparator) ) {
                    return (actualValue > specifiedValue);
                } else if ( ">=".equals(comparator) ) {
                    return (actualValue >= specifiedValue);
                } else if ( "<".equals(comparator) ) {
                    return (actualValue < specifiedValue);
                } else if ( "<=".equals(comparator) ) {
                    return (actualValue <= specifiedValue);
                }
            } catch ( Exception e ) {
                //logger.warn("DEBUG Exception",e );
            }

            return false;
        }


        /**
         * If its not a number treat it as a string
         */
        if ( ! ( "=".equals( comparator ) || "!=".equals( comparator ) ) ) // String only supports "=" or "!=" operator
            return false;
        String actualValueStr = actualValueObj.toString().toLowerCase();
        //logger.warn("DEBUG string check: " + actualValueStr + " against " + value );

        if ( this.stringGlobMatcher == null )
            this.stringGlobMatcher = new GlobMatcher( value );
        if ( this.stringGlobMatcher == null )
            return false;

        boolean match = this.stringGlobMatcher.isMatch( actualValueStr );
        if ( "=".equals(comparator) ) {
            return match;
        } else if ( "!=".equals(comparator) ) {
            return !match;
        }
        
        logger.warn("constraint failed");
        return false;
    }
}
