/**
 * $Id$
 */
package com.untangle.app.ad_blocker;

/**
 * An enumeration of the possible actions
 */
public enum Action
{
    PASS('P', "pass"),
    BLOCK('B', "block");

    public static char PASS_KEY = 'P';
    public static char BLOCK_KEY = 'B';

    private final char key;
    private final String name;

    private Action(char key, String name)
    {
        this.key = key;
        this.name = name;
    }

    /**
     * get the key character for this action
     * @returns the key
     */
    public char getKey()
    {
        return key;
    }

    /**
     * get the english name for this action
     * @returns the name
     */
    public String getName()
    {
        return name;
    }

    /**
     * Get the global Action instance for the specified key
     * @returns the action or null if not found
     */
    public static Action getInstance(char key)
    {
        Action[] values = values();
        for (int i = 0; i < values.length; i++) {
            if (values[i].getKey() == key){
                return values[i];
            }
        }
        return null;
    }
}
