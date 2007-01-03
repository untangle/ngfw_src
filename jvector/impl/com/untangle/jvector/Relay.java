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

package com.untangle.jvector;

public class Relay
{
    private native int  relay_create ();
    private native void relay_free (int relay_ptr);
    private native void relay_set_src (int relay_ptr, int src_ptr);
    private native void relay_set_snk (int relay_ptr, int snk_ptr);

    private Source src;
    private Sink   snk;

    private int relay_ptr;
    
    public Relay (Source src, Sink snk)
    {
        this.src = src;
        this.snk = snk;
        this.relay_ptr = relay_create();
        relay_set_src(this.relay_ptr,src.src_ptr());
        relay_set_snk(this.relay_ptr,snk.snk_ptr());
    }

    public void source(Source src)
    {
        this.src = src;
        relay_set_src(this.relay_ptr,this.src.src_ptr());
    }

    public Source source()
    {
        return this.src;
    }

    public void sink(Sink snk)
    {
        this.snk = snk;
        relay_set_src(this.relay_ptr,this.snk.snk_ptr());
    }

    public Sink sink()
    {
        return this.snk;
    }

    public int get_relay()
    {
        return relay_ptr;
    }

    static 
    {
        Vector.load();
    }
}
