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

final class ImmutableBitSet implements Serializable
{
    private final BitSet bitSet;
    
    ImmutableBitSet( BitSet bitSet )
    {
        this.bitSet = bitSet;
    }
    
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
}
