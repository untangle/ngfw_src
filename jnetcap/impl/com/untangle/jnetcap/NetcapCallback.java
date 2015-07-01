/**
 * $Id$
 */
package com.untangle.jnetcap;

public interface NetcapCallback
{
    /* This is the callback that will be called for the UDP/TCP hooks */
    public void event( long sessionId );

    /* This is the callback for conntrack events */
    public void event(int type, long mark, long conntrack_id, long session_id,
                       int l3_proto, int l4_proto,
                       long c_client_addr, long c_server_addr,
                       int  c_client_port, int c_server_port,
                       long s_client_addr, long s_server_addr,
                       int  s_client_port, int s_server_port,
                       int c2s_packets, int c2s_bytes,
                       int s2c_packets, int s2c_bytes,
                       long timestamp_start, long timestamp_stop );
}
