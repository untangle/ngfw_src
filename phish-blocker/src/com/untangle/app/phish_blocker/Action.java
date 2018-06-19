/**
 * $Id$
 */

package com.untangle.app.phish_blocker;

/**
 * Represents a phish blocker action
 */
public enum Action
{
    PASS('P', "pass"), BLOCK('B', "block");

    public static char PASS_KEY = 'P';
    public static char BLOCK_KEY = 'B';

    private final char key;
    private final String name;

    /**
     * Constructor
     * 
     * @param key
     *        The action key
     * @param name
     *        The action name
     */
    private Action(char key, String name)
    {
        this.key = key;
        this.name = name;
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
     * Get the name
     * 
     * @return The name
     */
    public String getName()
    {
        return name;
    }

    /**
     * Get an action for a corresponding key
     * 
     * @param key
     *        The key to locate
     * @return The corresponding action
     */
    public static Action getInstance(char key)
    {
        Action[] values = values();
        for (int i = 0; i < values.length; i++) {
            if (values[i].getKey() == key) {
                return values[i];
            }
        }
        return null;
    }
}
