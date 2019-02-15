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

    private String field;
    private String comparator;
    private String fieldValue;

    private GlobMatcher stringGlobMatcher = null;
    
    /**
     * Initialize empty instance of EventRuleCondition.
     * @return Empty instance of EventRuleCondition.
     */
    public EventRuleCondition(){}

    /**
     * Initialize instance of EventRuleCondition.
     * @param  field      String of fild to check.
     * @param  comparator String of operator
     * @param  fieldValue String of field value.
     * @return Instance of EventRuleCondition.
     */
    public EventRuleCondition( String field, String comparator, String fieldValue )
    {
        this.field = field;
        this.comparator = comparator;
        this.fieldValue = fieldValue;
    }

    /**
     * Return field value.
     * @return String of field.
     */
    public String getField() { return field; }
    /**
     * Specify field value.
     * @param newValue String of field.
     */
    public void setField( String newValue ) { this.field = newValue; }

    /**
     * Return comparator value.
     * @return String of comparator.
     */
    public String getComparator() { return comparator; }
    /**
     * Specify comparator value.
     * @param newValue String of comparator.
     */
    public void setComparator( String newValue ) { this.comparator = newValue; }

    /**
     * Return field value to check.
     * @return String of field value to check.
     */
    public String getFieldValue() { return fieldValue; }
    /**
     * Specify field value to check.
     * @param newValue String of field value to check.
     */
    public void setFieldValue( String newValue ) { this.fieldValue = newValue; }

    /**
     * From specified object, determine if event matches.
     * @param  obj JSONObject, its field/value combination using the comperator.
     * @return     boolean if true, obj matches, otherwise no match.
     */
    public boolean isMatch( JSONObject obj )
    {
        if ( field == null || comparator == null || fieldValue == null )
            return false;

        Object valueObj = getAttribute( obj, field );
        if ( valueObj == null ) {
            if (logger.isDebugEnabled()) logger.debug("DEBUG missing field: " + field + " value: " + valueObj );
            return false;
        }

        /**
         * Handle all the different types
         * Number is handled specially > and < operators
         */
        if ( valueObj instanceof Number ) {
            if (logger.isDebugEnabled()) logger.debug("DEBUG number eval: " + valueObj);

            try {
                int specifiedValue = Integer.parseInt( fieldValue );
                int value = (int) valueObj;
                if (logger.isDebugEnabled()) logger.debug("DEBUG integer check: " + specifiedValue + " against " + valueObj );

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
                if (logger.isDebugEnabled()) logger.debug("DEBUG Exception",e );
            }

            try {
                long specifiedValue = Long.parseLong( fieldValue );
                long value = (long) valueObj;
                if (logger.isDebugEnabled()) logger.debug("DEBUG long check: " + specifiedValue + " against " + valueObj );

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
                if (logger.isDebugEnabled()) logger.debug("DEBUG Exception",e );
            }

            try {
                double specifiedValue = Double.parseDouble( fieldValue );
                double value = (double)valueObj;
                if (logger.isDebugEnabled()) logger.debug("DEBUG double check: " + specifiedValue + " against " + valueObj );

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
                if (logger.isDebugEnabled()) logger.debug("DEBUG Exception",e );
            }

            try {
                float specifiedValue = Float.parseFloat( fieldValue );
                float value = (float)valueObj;
                if (logger.isDebugEnabled()) logger.debug("DEBUG float check: " + specifiedValue + " against " + valueObj );

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
                if (logger.isDebugEnabled()) logger.debug("DEBUG Exception",e );
            }

            return false;
        }


        /**
         * If its not a number treat it as a string
         */
        if ( ! ( "=".equals( comparator ) || "!=".equals( comparator ) ) ) // String only supports "=" or "!=" operator
            return false;
        String valueStr = valueObj.toString().toLowerCase();
        if (logger.isDebugEnabled()) logger.debug("DEBUG string check: " + valueStr + " against " + fieldValue );

        if ( this.stringGlobMatcher == null ){
            this.stringGlobMatcher = GlobMatcher.getMatcher(fieldValue);
        }
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

    /**
     * Retreive an attribute value using the attribute name from the object.
     * @param  obj           JSONObject to search.
     * @param  attributeName String of key to find.
     * @return               Object of matching value.  Null if not found.
     */
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

        // There has been some ambiguity between different JSON serializers over whether the first
        // character is capitalized or not
        // if the field is not found, try switching the case of the first letter and checking again
        if ( value == null && java.lang.Character.isLowerCase(fieldName.charAt(0)) ) {
            String altFieldName = fieldName.substring(0,1).toUpperCase() + fieldName.substring(1);
            try { value = obj.get( altFieldName ); } catch (Exception e) {}
        }
        if ( value == null && java.lang.Character.isUpperCase(fieldName.charAt(0)) ) {
            String altFieldName = fieldName.substring(0,1).toLowerCase() + fieldName.substring(1);
            try { value = obj.get( altFieldName ); } catch (Exception e) {}
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
