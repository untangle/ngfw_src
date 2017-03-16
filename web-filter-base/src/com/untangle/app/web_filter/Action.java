/*
 * $HeadURL$
 */
package com.untangle.app.web_filter;

/**
 * An action that was taken for given web visit (pass or block)
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

    public char getKey()
    {
        return key;
    }

    public String getName()
    {
        return name;
    }
    
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
