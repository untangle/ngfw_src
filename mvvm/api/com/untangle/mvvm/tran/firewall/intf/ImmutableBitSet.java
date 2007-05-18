/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.untangle.mvvm.tran.firewall.intf;

import java.io.Serializable;

import java.util.BitSet;

/**
 * A BitSet that is not modifiable.
 *
 * @author <a href="mailto:rbscott@untangle.com">Robert Scott</a>
 * @version 1.0
 */
final class ImmutableBitSet implements Serializable
{
    private final BitSet bitSet;
    
    ImmutableBitSet( BitSet bitSet )
    {
        /* Create a copy of bitset */
        this.bitSet = new BitSet( bitSet.size());

        /* Or in the present values */
        this.bitSet.or( bitSet );
    }
    
    /**
     * Get the value from the bitset 
     *
     * @param value The bit to test.
     * @return True if <param>value</param> is set.
     */
    boolean get( byte value )
    {
        return bitSet.get((int)value );
    }

    public int hashCode()
    {
        return bitSet.hashCode();
    }

    public boolean equals( Object o )
    {
        if ( o == null ) return false;
        
        if (!( o instanceof ImmutableBitSet )) return false;
        
        return bitSet.equals( ((ImmutableBitSet)o).bitSet );
    }

    public String toString()
    {
        return this.bitSet.toString();
    }
}
