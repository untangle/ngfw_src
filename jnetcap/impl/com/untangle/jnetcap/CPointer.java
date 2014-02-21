/**
 * $Id$
 */
package com.untangle.jnetcap;

class CPointer 
{
    public static final long NULL = 0;
    protected long pointer = NULL;
    
    protected CPointer( long pointer ) 
    {
        if ( pointer == NULL ) throw new NullPointerException();
        this.pointer = pointer;
    }
    
    long value() 
    {
        if ( pointer == NULL ) throw new NullPointerException();
        return pointer;
    }

    void raze() 
    {
        pointer = NULL;
    }
}
