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
    private String fieldValue;

    private GlobMatcher stringGlobMatcher = null;
    
    public EventRuleCondition()
    {
    }

    public EventRuleCondition( String field, String comparator, String fieldValue )
    {
        this.field = field;
        this.comparator = comparator;
        this.fieldValue = fieldValue;
    }

    public String getField() { return field; }
    public void setField( String newValue ) { this.field = newValue; }

    public String getComparator() { return comparator; }
    public void setComparator( String newValue ) { this.comparator = newValue; }

    public String getFieldValue() { return fieldValue; }
    public void setFieldValue( String newValue ) { this.fieldValue = newValue; }

    /**
     * 13.0 conversion
     * this can be removed in 13.1
     *
     * Prior to 13.0 we stored these inside a "sub-object" EventRuleConditionField
     * This will dynamically converter the old format to the new format
     * We now store the field, comparator, and value in the EventRuleCondition
     */
    public EventRuleConditionField getValue() { return null; }
    public void setValue( EventRuleConditionField field )
    {
        if ( field == null ) return;
        this.field = field.getField();
        this.comparator = field.getComparator();
        this.fieldValue = field.getValue();
    }

    public boolean isMatch( JSONObject obj )
    {
        if ( field == null || comparator == null || fieldValue == null )
            return false;

        Object valueObj = getAttribute( obj, field);
        if ( valueObj == null ) {
            //logger.warn("DEBUG missing field: " + field + " value: " + valueObj );
            return false;
        }

        /**
         * Handle all the different types
         * Number is handled specially > and < operators
         */
        if ( valueObj instanceof Number ) {
            //logger.warn("DEBUG number eval: " + valueObj);

            try {
                int specifiedValue = Integer.parseInt( fieldValue );
                int value = (int) valueObj;
                //logger.warn("DEBUG integer check: " + specifiedValue + " against " + valueObj );

                if ( "=".equals(comparator) ) {
                    return (value == specifiedValue);
                } else if ( "!=".equals(comparator) ) {
                    return (value != specifiedValue);
                } else if ( ">".equals(comparator) ) {
                    return (value > specifiedValue);
                } else if ( ">=".equals(comparator) ) {
                    return (value >= specifiedValue);
                } else if ( "<".equals(comparator) ) {
                    return (value < specifiedValue);
                } else if ( "<=".equals(comparator) ) {
                    return (value <= specifiedValue);
                }
            } catch ( Exception e ) {
                //logger.warn("DEBUG Exception",e );
            }

            try {
                long specifiedValue = Long.parseLong( fieldValue );
                long value = (long) valueObj;
                //logger.warn("DEBUG long check: " + specifiedValue + " against " + valueObj );

                if ( "=".equals(comparator) ) {
                    return (value == specifiedValue);
                } else if ( "!=".equals(comparator) ) {
                    return (value != specifiedValue);
                } else if ( ">".equals(comparator) ) {
                    return (value > specifiedValue);
                } else if ( ">=".equals(comparator) ) {
                    return (value >= specifiedValue);
                } else if ( "<".equals(comparator) ) {
                    return (value < specifiedValue);
                } else if ( "<=".equals(comparator) ) {
                    return (value <= specifiedValue);
                }
            } catch ( Exception e ) {
                //logger.warn("DEBUG Exception",e );
            }

            try {
                double specifiedValue = Double.parseDouble( fieldValue );
                double value = (double)valueObj;
                //logger.warn("DEBUG double check: " + specifiedValue + " against " + valueObj );

                if ( "=".equals(comparator) ) {
                    return (value == specifiedValue);
                } else if ( "!=".equals(comparator) ) {
                    return (value != specifiedValue);
                } else if ( ">".equals(comparator) ) {
                    return (value > specifiedValue);
                } else if ( ">=".equals(comparator) ) {
                    return (value >= specifiedValue);
                } else if ( "<".equals(comparator) ) {
                    return (value < specifiedValue);
                } else if ( "<=".equals(comparator) ) {
                    return (value <= specifiedValue);
                }
            } catch ( Exception e ) {
                //logger.warn("DEBUG Exception",e );
            }

            try {
                float specifiedValue = Float.parseFloat( fieldValue );
                float value = (float)valueObj;
                //logger.warn("DEBUG float check: " + specifiedValue + " against " + valueObj );

                if ( "=".equals(comparator) ) {
                    return (value == specifiedValue);
                } else if ( "!=".equals(comparator) ) {
                    return (value != specifiedValue);
                } else if ( ">".equals(comparator) ) {
                    return (value > specifiedValue);
                } else if ( ">=".equals(comparator) ) {
                    return (value >= specifiedValue);
                } else if ( "<".equals(comparator) ) {
                    return (value < specifiedValue);
                } else if ( "<=".equals(comparator) ) {
                    return (value <= specifiedValue);
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
        String valueStr = valueObj.toString().toLowerCase();
        //logger.warn("DEBUG string check: " + valueStr + " against " + value );

        if ( this.stringGlobMatcher == null )
            this.stringGlobMatcher = new GlobMatcher( fieldValue );
        if ( this.stringGlobMatcher == null )
            return false;

        boolean match = this.stringGlobMatcher.isMatch( valueStr );
        if ( "=".equals(comparator) ) {
            return match;
        } else if ( "!=".equals(comparator) ) {
            return !match;
        }
        
        logger.warn("constraint failed");
        return false;
    }

    private Object getAttribute( JSONObject obj, String attributeName )
    {
        if ( attributeName == null )
            return null;
        String[] parts = attributeName.split("\\.",2);
        if ( parts.length < 1 )
            return null;

        String fieldName = parts[0];
        
        Object value = null;
        try { value = obj.get( fieldName ); } catch (Exception e) {}

        if ( value == null && "class".equals(fieldName) ) {
            try { value = obj.get( "javaClass" ); } catch (Exception e) {}
        }
        if ( value == null && "javaClass".equals(fieldName) ) {
            try { value = obj.get( "class" ); } catch (Exception e) {}
        }

        // if the attributeName contains a "." then recurse
        // "foo.bar" should get obj['foo']['bar']
        if ( value != null && parts.length > 1 ) {
            return getAttribute( new JSONObject( value ), parts[1] );
        } else {
            return value;
        }
    }
    
}
