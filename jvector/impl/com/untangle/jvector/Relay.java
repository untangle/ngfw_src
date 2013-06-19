/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.untangle.jvector;

public class Relay
{
    private native long  relay_create ();
    @SuppressWarnings("unused")
    private native void relay_free (long relay_ptr);
    private native void relay_set_src (long relay_ptr, long src_ptr);
    private native void relay_set_snk (long relay_ptr, long snk_ptr);

    private Source src;
    private Sink   snk;

    private long relay_ptr;
    
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

    public long get_relay()
    {
        return relay_ptr;
    }

    static 
    {
        Vector.load();
    }
}
