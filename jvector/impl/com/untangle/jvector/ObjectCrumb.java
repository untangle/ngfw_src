/**
 * $Id: ObjectCrumb.java,v 1.00 2014/07/14 12:03:21 dmorris Exp $
 */
package com.untangle.jvector;

/**
 * ObjectCrumb is a crumb that embeds an arbitrary object
 */
public class ObjectCrumb extends Crumb
{
    protected final Object obj;

    /**
     * ObjectCrumb makes an ObjectCrumb with the specified object
     * @param obj
     */
    public ObjectCrumb( Object obj )
    {
        this.obj = obj;
    }

    /**
     * type - get the type of crumb
     * @return the type
     */
    public int    type()
    {
        return TYPE_OBJECT;
    }

    /**
     * raze - noop
     */
    public void raze()
    {
        /* Nothing to do here */
    }

    /**
     * getObject gets the embedded object
     * @return the object
     */
    public Object getObject()
    {
        return this.obj;
    }
}
