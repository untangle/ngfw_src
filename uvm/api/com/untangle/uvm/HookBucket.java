/**
 * $Id: HookBucket.java,v 1.00 2018/06/28 09:41:00 mahotz Exp $
 */

package com.untangle.uvm;

/**
 * A generic object that can be passed to hook callback functions allowing data
 * to be passed in and results to be returned. This was originally created to
 * pass a username to multiple captive portal hook callback functions to
 * determine if that user is currently logged in to any of them. Rather than
 * modifying our hook callback implementation to deal with return values, we
 * pass this object, the contents of which can be modified by the callback
 * functions, and then evaluated when all have returned.
 */
public class HookBucket
{
    private String stringHolder;
    private int numberHolder;

    /**
     * Constructor sets simple defaults for the string and number
     */
    public HookBucket()
    {
        stringHolder = "";
        numberHolder = 0;
    }

    /**
     * Constructor sets the argumented defaults for the string and number
     * 
     * @param stringValue
     *        The string value
     * @param numberValue
     *        The number value
     */
    public HookBucket(String stringValue, int numberValue)
    {
        stringHolder = stringValue;
        numberHolder = numberValue;
    }

    /**
     * Get the string
     * 
     * @return The string
     */
    public String getString()
    {
        return stringHolder;
    }

    /**
     * Set the string
     * 
     * @param stringValue
     *        The new value
     */
    public void setString(String stringValue)
    {
        stringHolder = stringValue;
    }

    /**
     * Get the number
     * 
     * @return The number
     */
    public int getNumber()
    {
        return numberHolder;
    }

    /**
     * Set the number
     * 
     * @param numberValue
     *        The new value
     */
    public void setNumber(int numberValue)
    {
        numberHolder = numberValue;
    }

    /**
     * Increment the number
     * 
     * @return The number
     */
    public int incrementNumber()
    {
        numberHolder++;
        return (numberHolder);
    }

    /**
     * Decrement the number
     * 
     * @return The number
     */
    public int decrementNumber()
    {
        numberHolder--;
        return (numberHolder);
    }
}
