/**
 * $Id$
 */
package com.untangle.uvm.util;

/**
 * I found myself creating many little classes (structs), to associate
 * two objects together.  This is a generic version of that
 * approach, useful for returns/arguments.
 * <br><br>
 * Example of use:
 * <code>
 * ArrayList&lt;Pair&lt;String,InetAddress>> myList = new ArrayList&lt;Pair&lt;String,InetAddress>>();<br>
 * myList.put(new Pair&lt;String, InetAddress>("localhost", InetAddress.getByName("localhost"));
 * </code>
 */
@SuppressWarnings("serial")
public class Pair<A,B> implements java.io.Serializable
{
    public final A a;
    public final B b;

    /**
     * Iniitalize instnace of Pair with second value null.
     * @param  a A object to create.
     * @return   intance of Pair.
     */
    public Pair(A a)
    {
        this.a = a;
        this.b = null;
    }

    /**
     * Iniitalize instnace of Pair..
     * @param  a A object to create.
     * @param  b B object to create.
     * @return   intance of Pair.
     */
    public Pair(A a, B b)
    {
        this.a = a;
        this.b = b;
    }
}
