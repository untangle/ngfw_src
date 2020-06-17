/**
 * $Id$
 */
package com.untangle.app.threat_prevention;

/**
 * Reason a Request or Response was blocked.
 */
public enum ThreatPreventionReason
{
// THIS IS FOR ECLIPSE - @formatter:off

    PASS_SITE('I', "in Site Pass list"),
    RULE('F', "matched Rule list"),
    DEFAULT('N', "no rule applied");

// THIS IS FOR ECLIPSE - @formatter:on

    private final char key;
    private final String reason;

    /**
     * Constructor
     * 
     * @param key
     *        The key
     * @param reason
     *        The reason
     */
    private ThreatPreventionReason(char key, String reason)
    {
        this.key = key;
        this.reason = reason;
    }

    /**
     * Get the key
     * 
     * @return The key
     */
    public char getKey()
    {
        return key;
    }

    /**
     * Get the reason
     * 
     * @return The reason
     */
    public String getReason()
    {
        return reason;
    }

    /**
     * Get the reason for an argumented key
     * 
     * @param key
     *        The search key
     * @return The corresponding reason or null
     */
    public static ThreatPreventionReason getInstance(char key)
    {
        ThreatPreventionReason[] values = values();
        for (int i = 0; i < values.length; i++) {
            if (values[i].getKey() == key) {
                return values[i];
            }
        }
        return null;
    }

}
