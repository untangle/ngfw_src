/**
 * $Id$
 */
package com.untangle.jnetcap;

/**
 * CPointer represents a C pointer in java
 */
class CPointer 
{
    public static final long NULL = 0;
    protected long pointer = NULL;
    
    /**
     * CPointer create a pointer
     * @param pointer
     */
    protected CPointer( long pointer ) 
    {
        if ( pointer == NULL )
            throw new NullPointerException();
        this.pointer = pointer;
    }
    
    /**
     * value
     * @return the pointer
     */
    long value() 
    {
        if ( pointer == NULL )
            throw new NullPointerException();
        return pointer;
    }

    /**
     * raze
     * sets the pointer to NULL
     */
    void raze() 
    {
        pointer = NULL;
    }
}
