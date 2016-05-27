/**
 * $Id: Conntrack.java,v 1.00 2016/05/27 10:02:35 dmorris Exp $
 */
package com.untangle.jnetcap;

import java.net.InetAddress;

public class Conntrack
{
    protected CPointer pointer;

    /**
     * These constants must match the values in
     * /usr/include/libnetfilter_conntrack/libnetfilter_conntrack.h
     */
    private final static int FLAG_ORIG_IPV4_SRC = 0;			/* u32 bits */
    private final static int FLAG_ORIG_IPV4_DST = 1;			/* u32 bits */
    private final static int FLAG_REPL_IPV4_SRC = 2;			/* u32 bits */
    private final static int FLAG_REPL_IPV4_DST = 3;			/* u32 bits */
    private final static int FLAG_ORIG_IPV6_SRC = 4;			/* u128 bits */
    private final static int FLAG_ORIG_IPV6_DST = 5;			/* u128 bits */
    private final static int FLAG_REPL_IPV6_SRC = 6;			/* u128 bits */
    private final static int FLAG_REPL_IPV6_DST = 7;			/* u128 bits */
    private final static int FLAG_ORIG_PORT_SRC = 8;			/* u16 bits */
    private final static int FLAG_ORIG_PORT_DST = 9;			/* u16 bits */
    private final static int FLAG_REPL_PORT_SRC = 10;			/* u16 bits */
    private final static int FLAG_REPL_PORT_DST = 11;			/* u16 bits */
    private final static int FLAG_ICMP_TYPE = 12;			/* u8 bits */
    private final static int FLAG_ICMP_CODE = 13;				/* u8 bits */
    private final static int FLAG_ICMP_ID = 14;				/* u16 bits */
    private final static int FLAG_ORIG_L3PROTO = 15;			/* u8 bits */
    private final static int FLAG_REPL_L3PROTO = 16;			/* u8 bits */
    private final static int FLAG_ORIG_L4PROTO = 17;			/* u8 bits */
    private final static int FLAG_REPL_L4PROTO = 18;			/* u8 bits */
    private final static int FLAG_TCP_STATE = 19;				/* u8 bits */
    private final static int FLAG_SNAT_IPV4 = 20;			/* u32 bits */
    private final static int FLAG_DNAT_IPV4 = 21;				/* u32 bits */
    private final static int FLAG_SNAT_PORT = 22;				/* u16 bits */
    private final static int FLAG_DNAT_PORT = 23;				/* u16 bits */
    private final static int FLAG_TIMEOUT = 24;			/* u32 bits */
    private final static int FLAG_MARK = 25;				/* u32 bits */
    private final static int FLAG_ORIG_COUNTER_PACKETS = 26;		/* u64 bits */
    private final static int FLAG_REPL_COUNTER_PACKETS = 27;		/* u64 bits */
    private final static int FLAG_ORIG_COUNTER_BYTES = 28;		/* u64 bits */
    private final static int FLAG_REPL_COUNTER_BYTES = 29;		/* u64 bits */
    private final static int FLAG_USE = 30;				/* u32 bits */
    private final static int FLAG_ID = 31;				/* u32 bits */
    private final static int FLAG_STATUS = 32;			/* u32 bits  */
    private final static int FLAG_TCP_FLAGS_ORIG = 33;			/* u8 bits */
    private final static int FLAG_TCP_FLAGS_REPL = 34;			/* u8 bits */
    private final static int FLAG_TCP_MASK_ORIG = 35;			/* u8 bits */
    private final static int FLAG_TCP_MASK_REPL = 36;		/* u8 bits */
    private final static int FLAG_MASTER_IPV4_SRC = 37;			/* u32 bits */
    private final static int FLAG_MASTER_IPV4_DST = 38;			/* u32 bits */
    private final static int FLAG_MASTER_IPV6_SRC = 39;			/* u128 bits */
    private final static int FLAG_MASTER_IPV6_DST = 40;		/* u128 bits */
    private final static int FLAG_MASTER_PORT_SRC = 41;			/* u16 bits */
    private final static int FLAG_MASTER_PORT_DST = 42;			/* u16 bits */
    private final static int FLAG_MASTER_L3PROTO = 43;			/* u8 bits */
    private final static int FLAG_MASTER_L4PROTO = 44;		/* u8 bits */
    private final static int FLAG_SECMARK = 45;				/* u32 bits */
    private final static int FLAG_ORIG_NAT_SEQ_CORRECTION_POS = 46;	/* u32 bits */
    private final static int FLAG_ORIG_NAT_SEQ_OFFSET_BEFORE = 47;	/* u32 bits */
    private final static int FLAG_ORIG_NAT_SEQ_OFFSET_AFTER = 48;	/* u32 bits */
    private final static int FLAG_REPL_NAT_SEQ_CORRECTION_POS = 49;	/* u32 bits */
    private final static int FLAG_REPL_NAT_SEQ_OFFSET_BEFORE = 50;	/* u32 bits */
    private final static int FLAG_REPL_NAT_SEQ_OFFSET_AFTER = 51;		/* u32 bits */
    private final static int FLAG_SCTP_STATE = 52;			/* u8 bits */
    private final static int FLAG_SCTP_VTAG_ORIG = 53;			/* u32 bits */
    private final static int FLAG_SCTP_VTAG_REPL = 54;			/* u32 bits */
    private final static int FLAG_HELPER_NAME = 55;			/* string (30 bytes max) */
    private final static int FLAG_DCCP_STATE = 56;			/* u8 bits */
    private final static int FLAG_DCCP_ROLE = 57;				/* u8 bits */
    private final static int FLAG_DCCP_HANDSHAKE_SEQ = 58;		/* u64 bits */
    private final static int FLAG_TCP_WSCALE_ORIG = 59;			/* u8 bits */
    private final static int FLAG_TCP_WSCALE_REPL = 60;		/* u8 bits */
    private final static int FLAG_ZONE = 61;				/* u16 bits */
    private final static int FLAG_SECCTX = 62;				/* string */
    private final static int FLAG_TIMESTAMP_START = 63;			/* u64 bits; linux >= 2.6.38 */
    private final static int FLAG_TIMESTAMP_STOP = 64;		/* u64 bits; linux >= 2.6.38 */
    private final static int FLAG_HELPER_INFO = 65;			/* variable length */
    private final static int FLAG_CONNLABELS = 66;			/* variable length */
    private final static int FLAG_CONNLABELS_MASK = 67;			/* variable length */
    private final static int FLAG_MAX = 68;
    
    protected static native long   getLongValue  ( int id, long session );
    protected static native int    getIntValue   ( int id, long session );
    protected static native String toString( long session, boolean ifClient );
    
    protected Conntrack( CPointer pointer )
    {
        this.pointer = pointer;
    }
    
    public short getProtocol()
    {
        return (short)getIntValue( FLAG_ORIG_L4PROTO, pointer.value());
    }
}
