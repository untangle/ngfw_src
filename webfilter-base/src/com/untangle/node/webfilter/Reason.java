/*
 * $Id$
 */
package com.untangle.node.webfilter;

/**
 * Reason a Request or Response was blocked.
 */
public enum Reason
{
    BLOCK_CATEGORY('D', "in Categories Block list"),
    BLOCK_URL('U', "in URLs Block list"),
    BLOCK_EXTENSION('E', "in File Extensions Block list"),
    BLOCK_MIME('M', "in MIME Types Block list"),
    BLOCK_ALL('A', "blocking all traffic"), // XXX removed in 6.0
    BLOCK_IP_HOST('H', "hostname is an IP address"),
    PASS_URL('I', "in URLs Pass list"),
    PASS_CLIENT('C', "in Clients Pass list"),
    PASS_UNBLOCK('B', "in Bypass list"),
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
