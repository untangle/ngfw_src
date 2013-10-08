/**
 * $Id: netcap_globals.h 35400 2013-07-23 16:31:40Z dmorris $
 */
#ifndef __NETCAP_GLOBALS_
#define __NETCAP_GLOBALS_

/**
 * maximum queue packet size
 */
#define UDP_MAX_MESG_SIZE   65536
#define QUEUE_MAX_MESG_SIZE 65536

/**
 * max control message size
 * WARNING: fragile
 */
#define MAX_CONTROL_MSG 200

/**
 * max fd in the epoll server
 */
#define EPOLL_MAX_EVENT 4096

/**
 * Maximum number of messages inside of a mailbox for UDP/ICMP
 */
#define MAX_MB_SIZE    16

/**
 * Returns IP_TRANSPARENT constant (varies by kernel version)
 * Returns 0 if the kernel does not support IP_TRANSPARENT
 */
int IP_TRANSPARENT_VALUE ( );

/**
 * Returns IP_SADDR constant (varies by kernel version)
 */
int IP_SADDR_VALUE ( );

/**
 * Returns IP_RECVNFMARK constant (varies by kernel version)
 */
int IP_RECVNFMARK_VALUE ( );

/**
 * Returns IP_SENDNFMARK constant (varies by kernel version)
 */
int IP_SENDNFMARK_VALUE ( );

/**
 * Returns IP_FIRSTNFMARK constant (varies by kernel version)
 */
int IP_FIRSTNFMARK_VALUE ( );

struct ip_sendnfmark_opts {
    u_int32_t on;
    u_int32_t mark;
};

/* Bits for the Netfilter marks */
#define MARK_BYPASS   0x01000000

#ifndef SOL_UDP /* missing from early kernels */
#define SOL_UDP 17
#endif
#ifndef UDP_RECVDPORT
#define UDP_RECVDPORT 2
#endif
#ifndef UDP_RECVDHDR
#define UDP_RECVDHDR 2
#endif
#ifndef UDP_SPORT
#define UDP_SPORT 1 
#endif


#endif
