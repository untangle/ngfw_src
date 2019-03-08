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

    final public static char PASS_KEY = 'P';
    final public static char BLOCK_KEY = 'B';

    private final char key;
    private final String name;

    /**
     * Create an action with the specified key and name
     * Private - use getInstance to get the instance of an action
     * @param key The key
     * @param name The english name
     */
    private Action(char key, String name)
    {
        this.key = key;
        this.name = name;
    }

    /**
     * get the key character for this action
     * @return the key
     */
    public char getKey()
    {
        return key;
    }

    /**
     * get the english name for this action
     * @return the name
     */
    public String getName()
    {
        return name;
    }

    /**
     * Get the global Action instance for the specified key
     * @param key The key
     * @return the action or null if not found
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
