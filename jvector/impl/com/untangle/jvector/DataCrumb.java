/**
 * $Id$
 */
package com.untangle.jvector;

/**
 * DataCrumb
 */
public class DataCrumb extends Crumb
{
    /* Data can point to a byte array that is larger than the actual amount of data read,
     * size contains the actual amount of data in the crumb */
    protected final byte[] data;
    protected int limit;
    protected int offset;

    /**
     * DataCrumb
     * @param data
     * @param offset
     * @param limit
     */
    public DataCrumb( byte[] data, int offset, int limit )
    {
        this.data = data;
        limit( limit );

        /** You have to set offset after limit */
        offset( offset );
    }

    /**
     * DataCrumb
     * @param data
     * @param limit
     */
    public DataCrumb( byte[] data, int limit )
    {
        this( data, 0, limit );
    }

    /**
     * DataCrumb
     * @param data
     */
    public DataCrumb( byte[] data )
    {
        this( data, data.length );
    }

    /**
     * DataCrumb
     * @param data
     */
    public DataCrumb( String data )
    {
        this( data.getBytes());
    }

    /**
     * type
     * @return
     */
    public int type()
    {
        return TYPE_DATA;
    }

    /**
     * data
     * @return
     */
    public byte[] data()
    {
        return data;
    }

    /**
     * limit
     * @return
     */
    public int limit()
    {
        return limit;
    }

    /**
     * offset
     * @return
     */
    public int offset()
    {
        return offset;
    }

    /**
     * Change the limit of the data crumb
     * @param limit
     */
    public void limit( int limit ) 
    {
        if ( limit > data.length ) {
            throw new IllegalArgumentException( "Limit is larger than the underlying byte array" );
        }
        this.limit = limit;
    }

    /**
     * Change the offset of the data crumb.
     * @param offset - The new offset of the data crumb
     */
    public void offset( int offset )
    {
        if ( offset > limit ) {
            throw new IllegalArgumentException( "Setting offset(" + offset + ") passed the end of the " + "data crumb(" + limit + ")" );
        }
        
        this.offset = offset;        
    }
    
    /** raze */
    public void raze()
    {
        /* Nothing to do here, C structure is freed automatically */
    }

    /**
     * advance
     * @param amount
     */
    protected void advance( int amount ) 
    {
        offset( offset + amount );
    }
    
}
