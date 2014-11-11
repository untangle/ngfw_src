/**
 * $Id: AlertRuleMatcherField.java 37267 2014-02-26 23:42:19Z dmorris $
 */
package com.untangle.node.reporting;

import com.untangle.uvm.node.RuleMatcher;

/**
 * This is a matching criteria for a Alert Control Rule
 *
 * A AlertRule has a set of these to determine what traffic to match
 */
@SuppressWarnings("serial")
public class AlertRuleMatcherField
{
    private String field;
    private String comparator;
    private String value;

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
}
