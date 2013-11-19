/**
 * $Id$
 */
package com.untangle.node.router;


class RouterConstants
{
    /* TCP Port range for nat */
    static final int TCP_NAT_PORT_START = 10000;
    static final int TCP_NAT_PORT_END   = 60000;

    /* UDP Port range for nat */
    static final int UDP_NAT_PORT_START = 10000;
    static final int UDP_NAT_PORT_END   = 60000;

    /* ICMP PID range for nat */
    static final int ICMP_PID_START     = 1;
    static final int ICMP_PID_END       = 60000;

    /* Port the server receives data on, probably not the best place for this constant */
    static final int FTP_SERVER_PORT    = 21;
}
