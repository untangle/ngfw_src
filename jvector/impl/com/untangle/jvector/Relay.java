/**
 * $Id$
 */
package com.untangle.jvector;

/**
 * A Relay is a source and sink pair
 */
public class Relay
{
    private Source src;
    private Sink   snk;

    private long relay_ptr;

    /**
     * relay_create
     * @return
     */
    private native long relay_create ();

    /**
     * relay_set_src
     * @param relay_ptr
     * @param src_ptr
     */
    private native void relay_set_src (long relay_ptr, long src_ptr);

    /**
     * relay_set_snk
     * @param relay_ptr
     * @param snk_ptr
     */
    private native void relay_set_snk (long relay_ptr, long snk_ptr);
    
    /**
     * Relay - make a relay with the specified source and sink
     * @param src
     * @param snk
     */
    public Relay (Source src, Sink snk)
    {
        this.src = src;
        this.snk = snk;
        this.relay_ptr = relay_create();
        relay_set_src(this.relay_ptr,src.src_ptr());
        relay_set_snk(this.relay_ptr,snk.snk_ptr());
    }

    /**
     * source - set the source
     * @param src
     */
    public void source(Source src)
    {
        this.src = src;
        relay_set_src(this.relay_ptr,this.src.src_ptr());
    }

    /**
     * source - get the source
     * @return source
     */
    public Source source()
    {
        return this.src;
    }

    /**
     * sink - set the sink
     * @param snk
     */
    public void sink(Sink snk)
    {
        this.snk = snk;
        relay_set_src(this.relay_ptr,this.snk.snk_ptr());
    }

    /**
     * sink - get the sink
     * @return sink
     */
    public Sink sink()
    {
        return this.snk;
    }

    /**
     * get_relay gets the relay pointer
     * @return pointer
     */
    public long get_relay()
    {
        return relay_ptr;
    }

    static 
    {
        Vector.load();
    }
}
