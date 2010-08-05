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
#ifndef __NETCAP_H_
#define __NETCAP_H_

#include <sys/types.h>
#include <netinet/in.h>

//#include <net/if.h>
#ifndef IF_NAMESIZE
#define IF_NAMESIZE       16
#endif
#include <pthread.h>
#include <semaphore.h>
#include <mvutil/lock.h>
#include <mvutil/mailbox.h>

#define NETCAP_DEBUG_PKG      201

#define IPPROTO_ALL  IPPROTO_MAX

#define NETCAP_MAX_IF_NAME_LEN  IF_NAMESIZE

#define NETCAP_MAX_INTERFACES   32 /* XXX */

#define NC_INTF_MAX NC_INTF_LOOPBACK

typedef enum {
    ACTION_NULL=0,
    CLI_COMPLETE,
    SRV_COMPLETE,
    CLI_RESET,
    CLI_DROP,
    CLI_ICMP,
    /* Forward whatever rejection the server sent to the client */
    CLI_FORWARD_REJECT,
    /* do whatever is necessary to treat this session as if it was antisubscribed,
     * this only works if the session is in SYN mode. */
    LIBERATE
} netcap_callback_action_t;

typedef enum {
    NC_INTF_ERR = 0,
    NC_INTF_0 = 1,
    NC_INTF_1,
    NC_INTF_2,
    NC_INTF_3,
    NC_INTF_4,
    NC_INTF_5,
    NC_INTF_6,
    NC_INTF_7,
    NC_INTF_8,
    NC_INTF_9,
    NC_INTF_10,
    NC_INTF_11,
    NC_INTF_12,
    NC_INTF_13,
    NC_INTF_14,
    NC_INTF_15,
    NC_INTF_LOOPBACK = 17,
    NC_INTF_UNK      = 18
} netcap_intf_t;

typedef enum {
    SRV_COMPLETE_NONLOCAL_BIND = 1
} netcap_callback_flag_t;

typedef enum {
    CONN_STATE_INCOMPLETE = 1,
    CONN_STATE_COMPLETE,
    CONN_STATE_NULL
} netcap_tcp_conn_state_t;

/* Different ways for the server to tell the client that the connection is dead */
enum {
    TCP_CLI_DEAD_DROP = 1,
    TCP_CLI_DEAD_RESET,
    TCP_CLI_DEAD_ICMP,
    TCP_CLI_DEAD_NULL
};

typedef enum
{
    NETCAP_QUEUE_CLIENT_PRE_NAT,
    NETCAP_QUEUE_CLIENT_POST_NAT,
    NETCAP_QUEUE_SERVER_PRE_NAT,
    NETCAP_QUEUE_SERVER_POST_NAT
} netcap_queue_type_t;

typedef struct
{
    char s[NETCAP_MAX_IF_NAME_LEN];
} netcap_intf_string_t;

/** XXXX
 * This should be changed to a sockaddr_in if possible.
 */
typedef struct netcap_endpoint_t {
    struct in_addr host;
    u_short        port;
} netcap_endpoint_t;

typedef struct netcap_endpoints {
    netcap_intf_t     intf;
    netcap_endpoint_t cli;
    netcap_endpoint_t srv;
} netcap_endpoints_t;

typedef struct netcap_ip_tuple
{
    u_int32_t src_address;
    /* these are stored in network byte order */
    u_int32_t src_protocol_id;

    u_int32_t dst_address;
    /* these are stored in network byte order */
    u_int32_t dst_protocol_id;
} netcap_ip_tuple;

typedef struct nat_info {
  netcap_ip_tuple original;
  netcap_ip_tuple reply;
} nat_info_t;

// random value to signal default
#define IS_MARKED_FORCE_FLAG 0x00001200

typedef struct netcap_pkt {
    /**
     * Protocol
     */
    int proto;

    /**
     * The interace that the packet came in on
     */
    netcap_intf_t     src_intf;
    
    /**
     * Source host and port of the machine that sent the packet.
     */
    netcap_endpoint_t src;

    /**
     * The interace that the packet came should go out on.
     */
    netcap_intf_t     dst_intf;

    /**
     * Destination host and port of the machine the packet should be sent to.
     */
    netcap_endpoint_t dst;
    
    /**
     * IP attributes
     */
    u_char ttl;
    u_char tos;

    /**
     * IP options
     */
    char* opts;
    int   opts_len;

    /**
     * The actual data from the packet
     * this points to a different place for different type of pkts
     */
    u_char* data;    
    int   data_len;

    /**
     * Indicator for whether or not the mark should be used for outgoing
     * packets.  This is only for UDP and is ignored for TCP.
     * 0 for not marked
     * non-zero for is marked.
     * IS_MARKED_FORCE_FLAG to override the default marking bits.
     */
    int is_marked;
    
    /**
     * netfilter mark
     */
    u_int nfmark;
    
    /**
     * QUEUE specific stuff
     */
    u_char* buffer; 

    u_int32_t packet_id;

    /**
     * TCP flags (if a tcp packet)
     */
    u_int8_t th_flags;
#  define TH_FIN	0x01
#  define TH_SYN	0x02
#  define TH_RST	0x04
#  define TH_PUSH	0x08
#  define TH_ACK	0x10
#  define TH_URG	0x20

    /**
     * free to be used by the application
     */
    void* app_data;

    /* Information about the packet before and after it was NATd */
    nat_info_t  nat_info;

    /* this indicates where the packet was queued */
    netcap_queue_type_t queue_type;
} netcap_pkt_t;

#define NETCAP_SESSION_REMOVE_SERVER_TUPLE 0x7BCD

typedef struct netcap_session {
    /**
     * this will be IPPROTO_TCP or IPPROTO_UDP \n
     */
    int protocol; 

    int syn_mode; /* 1 = syn_mode, 0 = opaque mode */

    nat_info_t  nat_info;

    /**
     * alive: Just for UDP!  Only modify if you have a lock on the session table
     */
    short alive;

    /* Indicates whether or not to remove the tuples associated with
     * traf_srv and traf_cli */
    short remove_tuples;

    /**
     * the session_id
     */
    u_int session_id;
    
    /* The mailbox for TCP sessions */
    mailbox_t tcp_mb;
    
    /**
     * the server udp packet mailbox
     * this is not freed in free, or init'd in create
     */
    mailbox_t srv_mb;

    /**
     * the client udp packet mailbox
     * this is not freed in free, or init'd in create
     */
    mailbox_t cli_mb;

    /** 
     * The icmp client packet mailbox.
     * This typically has a maximum size of, and is only used for queuing the last
     * packet in the case when an icmp error message must be returned.
     */
    mailbox_t icmp_cli_mb;

    /** 
     * The icmp server packet mailbox.
     * This typically has a maximum size of, and is only used for queuing the last
     * packet in the case when an icmp error message must be returned.
     */
    mailbox_t icmp_srv_mb;
    
    /* the server side traffic description */
    netcap_endpoints_t srv; 

    /* the client side traffic description */
    netcap_endpoints_t cli; 

    /* UDP Session */
    
    /* For UDP sessions, this is a byte that is initialized to the TTL of the first packet 
     * received in the session */
    u_char ttl;
    
    /* For UDP sessions, this is a byte that is initialized to the TOS of the first packet 
     * received in the session */
    u_char tos;

    /* TCP Session */

    /* How to handle TCP sessions that were never alive */
    struct {
        /* 0: Drop incoming packets *
         * 1: Reset incoming SYN packets *
         * 2: Send an ICMP packet back with the type and code that are specified  below */
        u_char exit_type;

        /* If exit_type is ICMP this is the type and code that should be returned for
         * subsequent packets */
        u_char type;
        u_char code;

        /**
         * 0 src is not used.
         * 1 src is used.
         */
        u_char use_src;
        
        /* If the type of ICMP exit is redirect, this is the address to redirect to in
         * network byte order */
        in_addr_t redirect;

        /* If the source address of the packet is not the server, then this is the address
         * where the error came from */
        in_addr_t src;
    } dead_tcp;
    
    /* Client information */
    int                client_sock;

    /* Server information */
    int                server_sock;

    /**
     * For ICMP echo session, this is the message id for the client side and server side.
     * These values are in host byte order
     */
    struct {
        u_short client_id;
        u_short server_id;
    } icmp;

    /**
     * the callback to change the state of client and server connections
     * in the case of SRV_UNFINI or CLI_UNFINI this can be used to complete the
     * connection
     */
    int  (*callback) ( struct netcap_session* netcap_sess, netcap_callback_action_t action,
                       netcap_callback_flag_t flags );

    /**
     * The state of this TCP session
     */
    netcap_tcp_conn_state_t cli_state;
    netcap_tcp_conn_state_t srv_state;

    /**
     * UDP information
     */
    /* Packet identifier of the first packet to come through */
    u_int32_t first_pkt_id;

    /* Data that is specific to an application */
    void *app_data;
} netcap_session_t;

typedef void (*netcap_tcp_hook_t)  (netcap_session_t* tcp_sess, void* arg);
typedef void (*netcap_udp_hook_t)  (netcap_session_t* netcap_sess, void* arg);
/* If session is set, this is a new session, and the pkt is already in the mailbox.
 * if pkt is set, this packet couldn't be associated with a session and should be handled
 * individually 
 */
typedef void (*netcap_icmp_hook_t) (netcap_session_t* netcap_sess, netcap_pkt_t* pkt, void* arg);


/**
 * Initialization, and global controls
 */
int   netcap_init( void );
int   netcap_cleanup (void);
const char* netcap_version (void);
void  netcap_debug_set_level   (int lev);

/** Update everything that must change when the address of the box changes */
int   netcap_update_address( void );

/**
 * Thread management
 */
void* netcap_thread_donate   (void* arg);
int   netcap_thread_undonate (int thread_id);

/**
 * Hook management
 */
int   netcap_tcp_hook_register   (netcap_tcp_hook_t hook);
int   netcap_tcp_hook_unregister ();
int   netcap_udp_hook_register   (netcap_udp_hook_t hook);
int   netcap_udp_hook_unregister ();
int   netcap_icmp_hook_register   (netcap_icmp_hook_t hook);
int   netcap_icmp_hook_unregister ();

/**
 * Packet Sending (XXX include pkt_create?)
 */
int   netcap_udp_send  (char* data, int data_len, netcap_pkt_t* pkt);
int   netcap_icmp_send (char *data, int data_len, netcap_pkt_t* pkt);

/**
 * Function to update an ICMP error packet so the host addresses and ports match the values inside of pkt.
 * data      - Buffer to work with.
 * data_len  - length of the current data inside of buffer
 * data_lim  - Total size of data. (This should always be greater than or equal to data_len).
 * icmp_type - Type of ICMP packet that is being sent.
 * icmp_code - Code for the ICMP packet.
 * icmp_pid  - identifier to use for packets wehre it can be modified (non-error packets) (-1 never modify)
 * icmp_mb   - Mailbox to retrieve the packet to respond to.
 */
int   netcap_icmp_update_pkt( char* data, int data_len, int data_lim,
                              int icmp_type, int icmp_code, int icmp_pid, mailbox_t* icmp_mb );

/**
 * Function to retrieve the source address of an unaltered data block from an ICMP packet.
 *   This function checks if the source of a packet is relevant and returns 1 if so or zero if it is
 *   not.
 *
 * Returns:
 * -1 : error. source unmodified
 *  0 : The source of the packet is irrelevant
 *  1 : source has been updated to contain the source address of the packet.
 */
int   netcap_icmp_get_source( char* data, int data_len, netcap_pkt_t* pkt, struct in_addr* source );

/**
 * Resource Freeing 
 */
void          netcap_pkt_free    (netcap_pkt_t* pkt);
void          netcap_pkt_destroy (netcap_pkt_t* pkt);
void          netcap_pkt_raze    (netcap_pkt_t* pkt);
int           netcap_pkt_action_raze ( netcap_pkt_t* pkt, int action );

/**
 * UDP and TCP session
 */
int netcap_session_raze(netcap_session_t* session);

/**
 * Interface management
 */

/* Convert a string representation of an interface to a netcap representation */
/* blocking on configuration */
int netcap_interface_string_to_intf ( char *intf_str, netcap_intf_t *intf );

/* Convert a netcap representation (eg 1) of an interface to a string representation ( eg eth0 ) */
/* blocking on configuration */
int netcap_interface_intf_to_string ( netcap_intf_t intf, char *intf_str, int str_len );

/**
 * netcap_interface_configure_intf:
 * Configure the mapping from mark indices to interfaces(blocking on configuration)
 * This must be configured at startup (after initialization but before processing any sessions).
 * @param intf_name_array - 
 *      An array of interface names.
 *        The first index should correspond to the interface with the mark 1, the second with the
 *        mark 2, etc.
 * @param num_intf - The number of interfaces in intf_name_array.
 */
int netcap_interface_configure_intf ( netcap_intf_t* intf_array, netcap_intf_string_t* intf_name_array, 
                                      int intf_count );

int netcap_interface_intf_verify    ( netcap_intf_t intf );

/* blocking on configuration */
int netcap_interface_update_address ( void );

/* blocking on configuration */
int netcap_interface_is_broadcast   ( in_addr_t addr, int index );
int netcap_interface_is_multicast   ( in_addr_t addr );

int netcap_interface_count          ( void );

/* Update the session information for a netcap session */
int netcap_interface_dst_intf       ( netcap_session_t* session );


/* Holder for interface data for a particular interface or one of its aliases */
typedef struct 
{
    struct in_addr address;
    struct in_addr netmask;
    struct in_addr broadcast;    
} netcap_intf_address_data_t;

/* Retrieve an array of interface data for an interface. 
 * @parameter interface_name: name of the interface to lookup.
 * @parameter data: data structure to fill in.
 * @parameter data_size: Size of data in bytes.
 * @return number of elements filled in data or -1 on error.
 */
int netcap_interface_get_data       ( char* name, netcap_intf_address_data_t* data, int data_size );

/* blocking on configuration.
 * Retrieve the netmask for an interface */
int netcap_interface_get_netmask    ( char* interface_name, struct in_addr* netmask );

/**
 * Query information about the redirect ports.
 */
int netcap_tcp_redirect_ports( int* port_low, int* port_high );

/**
 * Session table management
 */

/**
 * Get a session given its ID
 */
netcap_session_t* netcap_sesstable_get ( u_int id );
/**
 * Get the number of open sessions
 */
int               netcap_sesstable_numsessions ( void );
/**
 * get a list of all open sessions
 */
list_t*           netcap_sesstable_get_all_sessions ( void ); 
/**
 * Call the function kill_all_function on all of the sessions in the session table
 */
int               netcap_sesstable_kill_all_sessions ( void (*kill_all_function)(list_t *sessions) );
/**
 * merge two UDP or ICMP sessions into one
 * This function checks if there are two sessions in the session table for
 * the same session.  This can happen if a packet comes from both directions with the
 * exact opposite signature.
 * packet A: source-10.0.0.1:6000,dest-10.0.0.2:7000
 * packet B: source-10.0.0.2:7000,dest-10.0.0.1:6000
 * If A and B come in at the same time, then a session could be created for each packet, even
 * though the traffic should be tracked in the same session.
 * At some point in one of the sessions, the user calls merge which flags the other session
 * to die, and merges(packets/sessiontable) it into the calling session.
 */

int               netcap_sesstable_merge_udp_tuple ( netcap_session_t* netcap_sess, 
                                                     in_addr_t src, in_addr_t dst,
                                                     u_short sport, u_short dport, netcap_intf_t intf );

int               netcap_sesstable_merge_icmp_tuple ( netcap_session_t* netcap_sess, 
                                                      in_addr_t src, in_addr_t dst, netcap_intf_t intf,
                                                      int icmp_pid );

int  netcap_endpoints_copy          ( netcap_endpoints_t* dst, netcap_endpoints_t* src );
int  netcap_endpoints_bzero         ( netcap_endpoints_t* tuple );

/**
 * Toggle opaque mode
 */
int  netcap_tcp_syn_mode (int toggle);

/**
 * Gets the socket mark - used on packets sent to the server
 */
int  netcap_tcp_get_server_mark ( netcap_session_t* netcap_sess );

/**
 * Sets the socket mark - used on packets sent to the server
 */
int  netcap_tcp_set_server_mark ( netcap_session_t* netcap_sess , int nfmark );

/**
 * Gets the socked mark - used on packets sent to the client
 */
int  netcap_tcp_get_client_mark ( netcap_session_t* netcap_sess );

/**
 * Sets the socked mark - used on packets sent to the client
 */
int  netcap_tcp_set_client_mark ( netcap_session_t* netcap_sess , int nfmark );

/**
 * netcap_sched_donate: Donate a thread to the scheduler.
 */
void* netcap_sched_donate ( void* arg );


/**
 * Printing utilities
 * all return static buffers
 */
char* netcap_session_tuple_print     ( netcap_session_t* sess );

/**
 * Print the server(sess.srv.srv.*)/client(sess.cli.cli.*) side two tuple (host and port)
 */
char* netcap_session_srv_tuple_print ( netcap_session_t* sess );
char* netcap_session_cli_tuple_print ( netcap_session_t* sess );

/**
 * Print the server(sess.srv.*)/client(sess.cli.*) side endpoints
 */
char* netcap_session_srv_endp_print ( netcap_session_t* sess );
char* netcap_session_cli_endp_print ( netcap_session_t* sess );

char* netcap_session_fd_tuple_print  ( netcap_session_t* sess );

/* Set the verdict on a packet */
int  netcap_set_verdict      ( u_int32_t packet_id, int verdict, u_char* buffer, int len );



#endif
