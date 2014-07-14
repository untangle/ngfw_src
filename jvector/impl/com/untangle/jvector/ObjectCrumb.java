/**
 * $Id: ObjectCrumb.java,v 1.00 2014/07/14 12:03:21 dmorris Exp $
 */
package com.untangle.jvector;

public class ObjectCrumb extends Crumb
{
    protected final Object obj;

    public ObjectCrumb( Object obj )
    {
        this.obj = obj;
    }

    public int    type()
    {
        return TYPE_OBJECT;
    }

    public void raze()
    {
        /* Nothing to do here */
    }

    public Object getObject()
    {
        return this.obj;
    }
}
