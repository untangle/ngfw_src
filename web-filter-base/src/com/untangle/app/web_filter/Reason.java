/**
 * $Id$
 */

package com.untangle.app.web_filter;

/**
 * Reason a Request or Response was blocked.
 */
public enum Reason
{
// THIS IS FOR ECLIPSE - @formatter:off

    BLOCK_CATEGORY('D', "in Categories Block list"),
    BLOCK_URL('U', "in Site Block list"),
    BLOCK_SEARCH_TERM('T', "in Search Term list"),
    BLOCK_IP_HOST('H', "hostname is an IP address"),
    PASS_URL('I', "in Site Pass list"),
    PASS_REFERER_URL('R', "referer in Site Pass list"),
    PASS_CLIENT('C', "in Clients Pass list"),
    PASS_UNBLOCK('B', "in Bypass list"),
    FILTER_RULE('F', "matched Rule list"),
    REDIRECT_KIDS('K', "redirected to kid-friendly search engine"),
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
    private Reason(char key, String reason)
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
    public static Reason getInstance(char key)
    {
        Reason[] values = values();
        for (int i = 0; i < values.length; i++) {
            if (values[i].getKey() == key) {
                return values[i];
            }
        }
        return null;
    }

}
