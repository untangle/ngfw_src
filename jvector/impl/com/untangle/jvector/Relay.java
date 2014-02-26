/**
 * $Id$
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
