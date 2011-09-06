/*
 * $Id: ShieldRule.java,v 1.00 2011/09/06 14:25:45 dmorris Exp $
 */
package com.untangle.node.shield;

import java.util.Map;
import java.util.HashMap;
import java.net.UnknownHostException;

import com.untangle.uvm.node.IpMaskedAddressRule;
import com.untangle.uvm.node.ParseException;

/**
 * Rule for the shield
 */
@SuppressWarnings("serial")
public class ShieldRule extends IpMaskedAddressRule implements java.io.Serializable
{
    /* divider for this rule (between0 and whatever, not inclusive) */
    private Float divider = DIVIDER_VALUES[0];

    /* Enumeration of all of the possible dividers */
    private static final String DIVIDER_ENUMERATION[];
    private static final Float  DIVIDER_VALUES[];

    private static final Map<Float,String> DIVIDER_MAP_FLOAT_TO_STRING = new HashMap<Float,String>();
    private static final Map<String,Float> DIVIDER_MAP_STRING_TO_FLOAT = new HashMap<String,Float>();

    public ShieldRule() { }

    /**
     * Divider up to which this applies, 0 is the highest and not recommended.
     *
     * @return the port to redirect to.
     */
    public Float getDivider()
    {
        /* This will "fix" dividers that have values that are not the
         * enumeration */
        getDividerString();
        return this.divider;
    }

    public void setDivider(Float divider)
    {
        this.divider = divider;
        getDividerString();
    }

    /* These are all guarded to guarantee the user doesn't set to an
     * invalid value */
    public String getDividerString()
    {
        String dividerString = DIVIDER_MAP_FLOAT_TO_STRING
            .get(this.divider);
        if (dividerString == null) {
            this.divider  = DIVIDER_VALUES[0];
            dividerString = DIVIDER_ENUMERATION[0];
        }

        return dividerString;
    }

    public void setDivider(String divider)
    {
        Float dividerValue = DIVIDER_MAP_STRING_TO_FLOAT.get(divider);
        if (dividerValue == null) {
            this.divider = DIVIDER_VALUES[0];
        } else {
            this.divider = dividerValue;
        }
    }

    public static String[] getDividerEnumeration()
    {
        return DIVIDER_ENUMERATION;
    }

    public static String getDividerDefault()
    {
        return DIVIDER_ENUMERATION[0];
    }

    static
    {
        DIVIDER_ENUMERATION = new String[] {
            "5 users",
            "25 users",
            "50 users",
            "100 users",
            "unlimited"
        };

        /* A little bit of a tapering off starting at 50 for 100
         * (capped in netcap to the value NC_SHIELD_DIVIDER_MAX */
        DIVIDER_VALUES = new Float[] { 5.0f, 25.0f, 40.0f, 75.0f, -1.0f };

        for (int c = 0 ; c < DIVIDER_ENUMERATION.length ; c++) {
            DIVIDER_MAP_FLOAT_TO_STRING.put(DIVIDER_VALUES[c], DIVIDER_ENUMERATION[c]);
            DIVIDER_MAP_STRING_TO_FLOAT.put(DIVIDER_ENUMERATION[c], DIVIDER_VALUES[c]);
        }
    }
}
