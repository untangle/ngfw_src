/*
 * $Id$
 */
package com.untangle.app.web_filter;

/**
 * Reason a Request or Response was blocked.
 */
public enum Reason
{
    BLOCK_CATEGORY('D', "in Categories Block list"),
    BLOCK_URL('U', "in Site Block list"),
    BLOCK_IP_HOST('H', "hostname is an IP address"),
    PASS_URL('I', "in Site Pass list"),
    PASS_REFERER_URL('R', "referer in Site Pass list"),
    PASS_CLIENT('C', "in Clients Pass list"),
    PASS_UNBLOCK('B', "in Bypass list"),
    FILTER_RULE('F', "matched Rule list"),
    DEFAULT('N', "no rule applied");

    private final char key;
    private final String reason;

    private Reason(char key, String reason)
    {
        this.key = key;
        this.reason = reason;
    }

    public char getKey(){
        return key;
    }

    public String getReason() {
        return reason;
    }

    public static Reason getInstance(char key)
    {
        Reason[] values = values();
        for (int i = 0; i < values.length; i++) {
            if (values[i].getKey() == key){
                return values[i];
            }
        }
        return null;
    }

}
