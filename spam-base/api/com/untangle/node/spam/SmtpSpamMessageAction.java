/*
 * $Id$
 */
package com.untangle.node.spam;

public enum SmtpSpamMessageAction
{
    PASS('P', "pass message"),
    MARK('M', "mark message"),
    DROP('B', "drop message"),
    QUARANTINE('Q', "quarantine message"),
    SAFELIST('S', "safelist message"),
    OVERSIZE('Z', "oversize message"),
    OUTBOUND('O', "outbound message");

    public static final char PASS_KEY = 'P';
    public static final char MARK_KEY = 'M';
    public static final char BLOCK_KEY = 'B';
    public static final char QUARANTINE_KEY = 'Q';
    public static final char SAFELIST_KEY = 'S'; // special pass case
    public static final char OVERSIZE_KEY = 'Z'; // special pass case
    public static final char OUTBOUND_KEY = 'O'; // special pass case

    private String name;
    private char key;

    private SmtpSpamMessageAction(char key, String name)
    {
        this.key = key;
        this.name = name;
    }

    public static SmtpSpamMessageAction getInstance(char key)
    {
        SmtpSpamMessageAction[] values = values();
        for (int i = 0; i < values.length; i++) {
            if (values[i].getKey() == key){
                return values[i];
            }
        }
        return null;
    }

    public char getKey()
    {
        return key;
    }
    
    public String getName()
    {
        return name;
    }
}
