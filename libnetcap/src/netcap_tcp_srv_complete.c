/**
 * $Id$
 */
#include "netcap_tcp.h"

#include <stdlib.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <sys/epoll.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <unistd.h>
#include <string.h>
#include <errno.h>
#include <limits.h>
#include <netinet/ip.h>
#include <netinet/ip_icmp.h>
#define __FAVOR_BSD
#include <netinet/tcp.h>
#include <mvutil/errlog.h>
#include <mvutil/debug.h>
#include <mvutil/list.h>
#include <mvutil/hash.h>
#include <mvutil/unet.h>
#include <mvutil/mailbox.h>
#include <linux/netfilter_ipv4.h>
#include "libnetcap.h"
#include "netcap_hook.h"
#include "netcap_session.h"
#include "netcap_pkt.h"
#include "netcap_queue.h"
#include "netcap_globals.h"
#include "netcap_interface.h"
#include "netcap_sesstable.h"
#include "netcap_icmp.h"
#include "netcap_nfconntrack.h"
/* When completing to the server, this is essentially one plus number of ICMP messages to receive
 * before giving up.  On the last attempt, incoming ICMP messages are ignored */
#define TCP_SRV_COMPLETE_ATTEMPTS 3

/* Delay of the first connection attempt */
#define TCP_SRV_COMPLETE_TIMEOUT_MSEC       ( 30 * 1000 )

/* Delay after receiving at least one ICMP message before giving up. */
#define TCP_SRV_COMPLETE_IGN_TIMEOUT_MSEC   ( 2 * 1000 )

static int  _netcap_tcp_setsockopt_srv ( int sock );

static int _srv_complete_connection( netcap_session_t* netcap_sess );
static int _srv_start_connection( netcap_session_t* netcap_sess, struct sockaddr_in* dst_addr );
static int _srv_wait_complete( int ep_fd, netcap_session_t* netcap_sess, struct sockaddr_in* dst_addr );

static int _icmp_mailbox_init    ( netcap_session_t* netcap_sess );
static int _icmp_mailbox_destroy ( netcap_session_t* netcap_sess );

int  _netcap_tcp_callback_srv_complete ( netcap_session_t* netcap_sess, netcap_callback_action_t action )
{
    int ret = 0;

    switch( netcap_sess->srv_state ) {
    case CONN_STATE_INCOMPLETE:
        break;

    case CONN_STATE_COMPLETE:
        errlog( ERR_WARNING, "TCP: (%10u) SRV_COMPLETE %s connection already completed\n", 
                netcap_sess->session_id, netcap_session_srv_tuple_print( netcap_sess ));
        return 0;
        
    default:
        return errlog( ERR_WARNING, "TCP: (%10u) SRV_COMPLETE %s unknown state %d\n", 
                       netcap_sess->session_id, netcap_session_srv_tuple_print( netcap_sess ),
                       netcap_sess->srv_state );
    }
    
    ret = 0;

    /* Grab the session table lock */
    SESSTABLE_WRLOCK();
    ret = _icmp_mailbox_init( netcap_sess );
    SESSTABLE_UNLOCK();
    
    if ( ret < 0 ) return errlog( ERR_CRITICAL, "_icmp_mailbox_init\n" );    

    if ( _srv_complete_connection( netcap_sess ) < 0 ) {
        ret = -1;
    } else {
        ret = 0;
    }
    
    SESSTABLE_WRLOCK();
    if ( _icmp_mailbox_destroy( netcap_sess ) < 0 ) errlog( ERR_CRITICAL, "_destroy_icmp_mailbox\n" );
    SESSTABLE_UNLOCK();



    return ret;
}

static int  _netcap_tcp_setsockopt_srv ( int sock )
{
    int one        = 1;
    int thirty     = 30;
    int sixhundo   = 600;
    int nine       = 9;
    
    struct ip_sendnfmark_opts nfmark = {
        .on = 1,
        .mark = MARK_BYPASS
    };
    
    if ( IP_TRANSPARENT_VALUE() != 0 ) {
        if (setsockopt(sock, SOL_IP, IP_TRANSPARENT_VALUE(), &one, sizeof(one) )<0) 
            perrlog("setsockopt");
    }
    if (setsockopt(sock,SOL_IP,IP_NONLOCAL_VALUE(),&one,sizeof(one))<0) 
        perrlog("setsockopt");
    if (setsockopt(sock,SOL_TCP,TCP_NODELAY,&one,sizeof(one))<0) 
        perrlog("setsockopt");
    if (setsockopt(sock,SOL_TCP,TCP_LINGER2,&thirty,sizeof(thirty))<0) 
        perrlog("setsockopt");
    if (setsockopt(sock,SOL_SOCKET,SO_KEEPALIVE,&one,sizeof(one))<0)
        perrlog("setsockopt");
    if (setsockopt(sock,SOL_TCP,TCP_KEEPIDLE,&sixhundo, sizeof(sixhundo)) < 0 )
        perrlog("setsockopt");
    if (setsockopt(sock,SOL_TCP,TCP_KEEPINTVL,&thirty,sizeof(thirty))<0) 
        perrlog("setsockopt");
    if (setsockopt(sock,SOL_TCP,TCP_KEEPCNT,&nine,sizeof(nine)) < 0 )
        perrlog("setsockopt");

    if (setsockopt(sock,SOL_IP,IP_SENDNFMARK_VALUE(),&nfmark,sizeof(nfmark))<0)
        return perrlog( "setsockopt" );

    return 0;
}

static int _srv_complete_connection( netcap_session_t* netcap_sess )
{
    struct sockaddr_in dst_addr;
    int ret = 0;
    int ep_fd;

    netcap_sess->dead_tcp.exit_type = TCP_CLI_DEAD_NULL;
    if ( _srv_start_connection( netcap_sess, &dst_addr ) < 0 ) {
        /* Some codes like net unreachable may be returned immediately */
        if ( netcap_sess->dead_tcp.exit_type == TCP_CLI_DEAD_NULL ) {
            netcap_sess->dead_tcp.exit_type = TCP_CLI_DEAD_RESET;
            errlog( ERR_WARNING, "_srv_start_connection\n" );
        }

        return -1;
    }
    
    if (( ep_fd = epoll_create( 2 )) < 0 ) {
        return errlog( ERR_CRITICAL, "ep_fd" );
    }

    do {
        ret = _srv_wait_complete( ep_fd, netcap_sess, &dst_addr );
    } while ( 0 );

    if ( close( ep_fd ) < 0 ) {
        perrlog( "close" );
    }

    return ret;   
}

static int _srv_wait_complete( int ep_fd, netcap_session_t* netcap_sess, struct sockaddr_in* dst_addr )
{
    int mb_fd = -1, sock = -1;
    int c, numevents;
    netcap_pkt_t* pkt = NULL;
    struct icmp* icmp_hdr = NULL;
    struct epoll_event ev;
    struct epoll_event events[1];
    int timeout;  /* Timeout in milliseconds */
    
    sock = netcap_sess->server_sock;
    
    if (( mb_fd = mailbox_get_pollable_event( &netcap_sess->srv_mb )) < 0 ) {
        return errlog( ERR_CRITICAL, "mailbox_get_pollable_event\n" );
    }
    
    debug( 8, "TCP: (%10u) Completing connection %i to %s\n", netcap_sess->session_id, sock,
           netcap_session_srv_endp_print( netcap_sess ));

    bzero(&ev, sizeof(ev));      /* Cleanup valgrind */
    ev.events  = EPOLLOUT;
    ev.data.fd = sock;
    
    if ( epoll_ctl( ep_fd, EPOLL_CTL_ADD, sock, &ev ) < 0 ) {
        return perrlog( "epoll_ctl" );
    }

    ev.events  = EPOLLIN;
    ev.data.fd = mb_fd;
    
    if ( epoll_ctl( ep_fd, EPOLL_CTL_ADD, mb_fd, &ev ) < 0 ) {
        return perrlog( "epoll_ctl" );
    }
    
    /* Number of attempts to complete the connection */
    for ( c = 0 ; c < TCP_SRV_COMPLETE_ATTEMPTS ; c++ ) {
        if ( c == 0 ) {
            timeout = TCP_SRV_COMPLETE_TIMEOUT_MSEC;
        } else {
            /* This is the timeout for when ICMP messages may come but the connection
             * !might! complete anyway(many implementations ignore ICMP messages).
             */
            timeout = TCP_SRV_COMPLETE_IGN_TIMEOUT_MSEC;
        }
        
        /* Look for ICMP messages on the first few attempts, but on the last attempt
         * just try to connect. */
        if ( c >= TCP_SRV_COMPLETE_ATTEMPTS - 1 )  {
            ev.events  = 0;
            ev.data.fd = mb_fd;
            
            if ( epoll_ctl( ep_fd, EPOLL_CTL_DEL, mb_fd, &ev ) < 0 ) {
                return perrlog( "epoll_ctl" );
            }
        }
        
        bzero( events, sizeof( events ));
        
        if (( numevents = epoll_wait( ep_fd, events, 1, timeout )) < 0 ) {
            return perrlog( "epoll_wait" );
        } else if ( numevents == 0 ) {
            if ( c == 0 ) {
                /* Connection timeout only if this is the first iteration. 
                   (Otherwise, could have been ICMP). */
                netcap_sess->dead_tcp.exit_type = TCP_CLI_DEAD_DROP;
            }
            return -1;
        } else {
            if ( events[0].data.fd == mb_fd ) {
                /* Attempt to read out the ICMP packet */
                /* A packet should definitely have been available */
                if (( pkt = mailbox_try_get( &netcap_sess->srv_mb )) == NULL ) {
                    return errlog( ERR_CRITICAL, "mailbox_try_get\n" );
                }
                
                if (( pkt->data == NULL ) || ( pkt->data_len < ICMP_ADVLENMIN ) || 
                    ( pkt->proto != IPPROTO_ICMP )) {
                    netcap_pkt_raze( pkt );
                    pkt = NULL;
                    return errlog( ERR_CRITICAL, "Invalid ICMP packet\n" );
                }
                
                icmp_hdr = (struct icmp*)pkt->data;
                
                if (( netcap_icmp_verify_type_and_code( icmp_hdr->icmp_type, icmp_hdr->icmp_code ) < 0 ) || 
                    ICMP_INFOTYPE( icmp_hdr->icmp_type )) {
                    /* Invalid ICMP type or code, just drop the packet */
                    netcap_sess->dead_tcp.exit_type = TCP_CLI_DEAD_DROP;
                    netcap_pkt_raze( pkt );
                    icmp_hdr = NULL;
                    pkt = NULL;
                    continue;
                }

                /* XX May want to do some more validation here */
                if ( netcap_sess->dead_tcp.exit_type == TCP_CLI_DEAD_ICMP ) {
                    if (( netcap_sess->dead_tcp.type != icmp_hdr->icmp_type ) ||
                        ( netcap_sess->dead_tcp.code != icmp_hdr->icmp_code )) {
                        debug( 8, "TCP: (%10u) ICMP modification (%d/%d) -> (%d/%d)\n",
                               netcap_sess->dead_tcp.type, netcap_sess->dead_tcp.code,
                               icmp_hdr->icmp_type, icmp_hdr->icmp_code );
                    }
                }

                /* Check to see if this packet is from a different source address */
                if ( icmp_hdr->icmp_ip.ip_dst.s_addr != pkt->src.host.s_addr ) {
                    netcap_sess->dead_tcp.use_src = 1;
                    netcap_sess->dead_tcp.src     = pkt->src.host.s_addr;
                } else {
                    netcap_sess->dead_tcp.use_src = 0;
                    netcap_sess->dead_tcp.src     = (in_addr_t)0;
                }
                
                netcap_sess->dead_tcp.exit_type = TCP_CLI_DEAD_ICMP;
                netcap_sess->dead_tcp.type = icmp_hdr->icmp_type;
                netcap_sess->dead_tcp.code = icmp_hdr->icmp_code;

                debug( 10, "TCP: (%10u) ICMP message type %d code %d\n", netcap_sess->session_id, 
                       netcap_sess->dead_tcp.type, netcap_sess->dead_tcp.code );
                
                if ( icmp_hdr->icmp_type == ICMP_REDIRECT ) {
                    debug( 10, "TCP: (%10u) ICMP message redirect: %s\n", netcap_sess->session_id, 
                           unet_next_inet_ntoa( icmp_hdr->icmp_gwaddr.s_addr ));
                    
                    netcap_sess->dead_tcp.redirect = icmp_hdr->icmp_gwaddr.s_addr;
                }
                
                netcap_pkt_raze( pkt );
                icmp_hdr = NULL;
                pkt = NULL;
            } else if ( events[0].data.fd == sock ) {
                /* Check if the connection was established */
                if ( connect( sock, (struct sockaddr*)dst_addr, sizeof(struct sockaddr_in)) < 0 ) {
                    debug( 4, "TCP: (%10u) Server connection failed '%s'\n", netcap_sess->session_id, strerror( errno ));
                    netcap_sess->dead_tcp.exit_type = TCP_CLI_DEAD_RESET;
                    return -1;
                } else {
                    debug( 10, "TCP: (%10u) Server connection complete\n", netcap_sess->session_id );
                    /* Reenable blocking io */
                    if ( unet_blocking_enable( sock ) < 0 ) {
                        errlog( ERR_CRITICAL, "unet_blocking_enable\n" );
                    }
                    return 0;
                }
            } else {
                errlog( ERR_CRITICAL, "Unknown event: %d", events[0].data.fd );
            }
        }
    }
        
    return errlog( ERR_CRITICAL, "invalid server complete\n" );
}

static int _srv_start_connection( netcap_session_t* netcap_sess, struct sockaddr_in* dst_addr )
{
    int newsocket;
    struct sockaddr_in src_addr;
    int ret = 0;
    netcap_endpoint_t* src;
    netcap_endpoint_t* dst;
        
    src = &netcap_sess->srv.cli;
    dst = &netcap_sess->srv.srv;
    
    if (( unet_sockaddr_in_init( &src_addr, src->host.s_addr, src->port ) < 0 ) ||
        ( unet_sockaddr_in_init(  dst_addr, dst->host.s_addr, dst->port ) < 0 )) {
        return errlog( ERR_CRITICAL, "unet_sockaddr_in_init\n" );
    }
        
    debug( 8, "TCP: (%10u) Completing connection to %s\n", netcap_sess->session_id,
           netcap_session_srv_endp_print( netcap_sess ));
    
    if (( newsocket = socket( AF_INET, SOCK_STREAM, IPPROTO_TCP )) < 0 ) return perrlog("socket");

    if ( _netcap_tcp_setsockopt_srv( newsocket ) < 0 ) return perrlog("_netcap_tcp_setsockopt_srv");
    
    do {
        debug( 8,"TCP: (%10u) Binding %i to %s:%i\n", netcap_sess->session_id, newsocket, unet_next_inet_ntoa( src_addr.sin_addr.s_addr ), ntohs( src_addr.sin_port ));
        if ( bind( newsocket, (struct sockaddr*)&src_addr, sizeof(src_addr)) < 0 ) {
            ret = errlog( ERR_WARNING,"bind(%s) failed: %s\n",unet_next_inet_ntoa( src_addr.sin_addr.s_addr ), errstr );
            break;
        }
        
        /**
         * set non-blocking
         */
        if ( unet_blocking_disable( newsocket ) < 0 ) {
            ret = errlog( ERR_CRITICAL, "unet_blocking_disable\n" );
            break;
        }
        
        debug( 6, "TCP: (%10u) Connect %i to %s:%i\n", netcap_sess->session_id, newsocket,
               unet_next_inet_ntoa( dst_addr->sin_addr.s_addr ), ntohs( dst_addr->sin_port ));
    
        if ( connect( newsocket, (struct sockaddr*)dst_addr, sizeof(struct sockaddr_in)) < 0 ) {
            if ( errno == EHOSTUNREACH ) {
                errlog( ERR_WARNING,"connect(%s) failed: %s\n", 
                        unet_next_inet_ntoa( dst_addr->sin_addr.s_addr ), errstr );

                ret = -1;
                netcap_sess->dead_tcp.exit_type = TCP_CLI_DEAD_ICMP;
                netcap_sess->dead_tcp.type      = ICMP_DEST_UNREACH;
                netcap_sess->dead_tcp.code      = ICMP_HOST_UNREACH;
            } else if ( errno == ENETUNREACH ) {
                errlog( ERR_WARNING, "connect to (%s) failed: %s\n",
                        unet_next_inet_ntoa( dst_addr->sin_addr.s_addr ), errstr );
                
                ret = -1;
                netcap_sess->dead_tcp.exit_type = TCP_CLI_DEAD_ICMP;
                netcap_sess->dead_tcp.type      = ICMP_DEST_UNREACH;
                netcap_sess->dead_tcp.code      = ICMP_NET_UNREACH;
            } else if ( errno == EINPROGRESS ) {
                /* nothing to do here */
            } else {
                ret = errlog( ERR_CRITICAL, "connect: %s : (%s:%d -> %s:%d)\n", errstr,
                              unet_next_inet_ntoa( src_addr.sin_addr.s_addr ), ntohs( src_addr.sin_port ),
                              unet_next_inet_ntoa( dst_addr->sin_addr.s_addr ), ntohs( dst_addr->sin_port ));
            }
        }
    } while ( 0 );
    
    if ( ret < 0 ) { 
        if ( close( newsocket ) < 0 ) perrlog( "close" );
    } else {
        netcap_sess->server_sock = newsocket;
    }
    
    return ret;
}

static int _icmp_mailbox_init    ( netcap_session_t* netcap_sess )
{
    netcap_session_t* current_sess;
    
    netcap_endpoint_t* src;
    netcap_endpoint_t* dst;
    
    /* Insert the reverse session (matching an incoming packet from the server side ) */
    dst = &netcap_sess->srv.cli;
    src = &netcap_sess->srv.srv;

    /* Lookup the tuple */
    // First check to see if the session already exists.
    current_sess = netcap_nc_sesstable_get_tuple ( !NC_SESSTABLE_LOCK, IPPROTO_TCP,
                                                   src->host.s_addr, dst->host.s_addr,
                                                   src->port, dst->port, 0 );
    
    if ( current_sess == NULL ) {
        debug( 10, "TCP: (%10u) Creating server mailbox\n", netcap_sess->session_id );
                 
        /* Create the mailbox */
        if ( mailbox_init( &netcap_sess->srv_mb ) < 0 ) return errlog ( ERR_CRITICAL, "mailbox_init\n" );

        /* Insert the tuple */
        if ( netcap_nc_sesstable_add_tuple( !NC_SESSTABLE_LOCK, netcap_sess, IPPROTO_TCP,
                                            src->host.s_addr, dst->host.s_addr,
                                            src->port, dst->port, 0 ) < 0 ) {
            if ( mailbox_destroy( &netcap_sess->srv_mb ) < 0 ) errlog( ERR_CRITICAL, "mailbox_destroy\n" );
            return errlog( ERR_CRITICAL, "netcap_nc_sesstable_add_tuple\n" );
        }
    } else {
        return errlog( ERR_WARNING, "TCP: (%10u) Server TCP session already exists %10u\n", 
                       netcap_sess->session_id, current_sess->session_id );
    }

    return 0;
}

static int _icmp_mailbox_destroy ( netcap_session_t* netcap_sess )
{
    netcap_endpoint_t* src;
    netcap_endpoint_t* dst;
    
    netcap_pkt_t* pkt = NULL;
    int c;

    debug( 10, "TCP: (%10u) Removing tuples and destroying server mailbox\n", netcap_sess->session_id );
    
    /* Remove the reverse session (matching an incoming packet from the server side ) */
    dst = &netcap_sess->srv.cli;
    src = &netcap_sess->srv.srv;

    /* Remove the tuple */
    if ( netcap_sesstable_remove_tuple( !NC_SESSTABLE_LOCK, IPPROTO_TCP,
                                           src->host.s_addr, dst->host.s_addr,
                                           src->port, dst->port, 0 ) < 0 ) {
        errlog( ERR_WARNING, "netcap_nc_sesstable_remove_tuple\n" );
    }
    
    /* Empty the mailbox */
    for ( c = 0 ; ( pkt = (netcap_pkt_t*)mailbox_try_get( &netcap_sess->srv_mb )) != NULL ; c++ ) {
        netcap_pkt_raze( pkt );
    }
    
    debug( 10, "TCP: (%10u) Deleted %d packets from mailbox\n", netcap_sess->session_id, c );
    
    /* Destroy the mailbox */
    if ( mailbox_destroy( &netcap_sess->srv_mb ) < 0 ) {
        errlog( ERR_WARNING, "mailbox_destroy\n" );
    }
    
    return 0;
}

