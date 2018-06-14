/**
 * $Id
 */

package com.untangle.app.web_filter;

/**
 * An action that was taken for given web visit (pass or block)
 * 
 * @author mahotz
 * 
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
     * Get the action key
     * 
     * @return The action key
     */
    public char getKey()
    {
        return key;
    }

    /**
     * Get the action name
     * 
     * @return The action name
     */
    public String getName()
    {
        return name;
    }

    /**
     * Get the Action for the argumented key
     * 
     * @param key
     *        The lookup key
     * @return The corresponding action or null if not found
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
