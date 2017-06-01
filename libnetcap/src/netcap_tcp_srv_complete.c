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
#include <inttypes.h>
#include <netinet/ip.h>
#include <netinet/ip_icmp.h>
#define __FAVOR_BSD
#include <netinet/tcp.h>
#include <mvutil/errlog.h>
#include <mvutil/debug.h>
#include <mvutil/list.h>
#include <mvutil/hash.h>
#include <mvutil/unet.h>
#include <linux/netfilter_ipv4.h>
#include "libnetcap.h"
#include "netcap_hook.h"
#include "netcap_session.h"
#include "netcap_pkt.h"
#include "netcap_queue.h"
#include "netcap_globals.h"
#include "netcap_interface.h"
#include "netcap_sesstable.h"

/* How long to wait for TCP connection to complete */
#define TCP_SRV_COMPLETE_TIMEOUT_MSEC       ( 30 * 1000 )

static int  _netcap_tcp_setsockopt_srv ( int sock, int mark );

static int _srv_complete_connection( netcap_session_t* netcap_sess );
static int _srv_start_connection( netcap_session_t* netcap_sess, struct sockaddr_in* dst_addr );
static int _srv_wait_complete( int ep_fd, netcap_session_t* netcap_sess, struct sockaddr_in* dst_addr );

int  _netcap_tcp_callback_srv_complete ( netcap_session_t* netcap_sess, netcap_callback_action_t action )
{
    int ret = 0;

    switch( netcap_sess->srv_state ) {
    case CONN_STATE_INCOMPLETE:
        break;

    case CONN_STATE_COMPLETE:
        errlog( ERR_WARNING, "TCP: (%"PRIu64") SRV_COMPLETE %s connection already completed\n", 
                netcap_sess->session_id, netcap_session_srv_tuple_print( netcap_sess ));
        return 0;
        
    default:
        return errlog( ERR_WARNING, "TCP: (%"PRIu64") SRV_COMPLETE %s unknown state %d\n", 
                       netcap_sess->session_id, netcap_session_srv_tuple_print( netcap_sess ),
                       netcap_sess->srv_state );
    }
    
    if ( _srv_complete_connection( netcap_sess ) < 0 ) {
        ret = -1;
    } else {
        ret = 0;
    }

    return ret;
}

static int  _netcap_tcp_setsockopt_srv ( int sock, int mark )
{
    int one        = 1;
    int thirty     = 30;
    int sixhundo   = 600;
    int nine       = 9;
    
    struct ip_sendnfmark_opts nfmark = {
        .on = 1,
        .mark = MARK_BYPASS | mark
    };
    
    if (setsockopt(sock,SOL_IP,IP_TRANSPARENT,&one,sizeof(one))<0) 
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
    if (setsockopt(sock,SOL_SOCKET,SO_REUSEADDR,&one,sizeof(one)) < 0)
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
    int sock = -1;
    int numevents;
    struct epoll_event ev;
    struct epoll_event events[1];
    
    sock = netcap_sess->server_sock;
    
    debug( 8, "TCP: (%"PRIu64") Completing connection %i to %s\n", netcap_sess->session_id, sock,
           netcap_session_srv_endp_print( netcap_sess ));

    memset(&ev, 0, sizeof(ev));
    ev.events  = EPOLLOUT;
    ev.data.fd = sock;
    
    if ( epoll_ctl( ep_fd, EPOLL_CTL_ADD, sock, &ev ) < 0 ) {
        return perrlog( "epoll_ctl" );
    }
        
    if (( numevents = epoll_wait( ep_fd, events, 1, TCP_SRV_COMPLETE_TIMEOUT_MSEC )) < 0 ) {
        return perrlog( "epoll_wait" );
    } else if ( numevents == 0 ) {
        /* Connection timeout */
        netcap_sess->dead_tcp.exit_type = TCP_CLI_DEAD_DROP;
        return -1;
    } else {
        if ( events[0].data.fd == sock ) {
            /* Check if the connection was established */
            if ( connect( sock, (struct sockaddr*)dst_addr, sizeof(struct sockaddr_in)) < 0 ) {
                debug( 4, "TCP: (%"PRIu64") Server connection failed (errno: %i)\n", netcap_sess->session_id, errno );
                netcap_sess->dead_tcp.exit_type = TCP_CLI_DEAD_RESET;
                return -1;
            } else {
                debug( 10, "TCP: (%"PRIu64") Server connection complete\n", netcap_sess->session_id );
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
        
    debug( 8, "TCP: (%"PRIu64") Completing connection to %s:%i\n", netcap_sess->session_id, unet_next_inet_ntoa( dst_addr->sin_addr.s_addr ), ntohs( dst_addr->sin_port ));
    
    if (( newsocket = socket( AF_INET, SOCK_STREAM, IPPROTO_TCP )) < 0 ) return perrlog("socket");

    if ( _netcap_tcp_setsockopt_srv( newsocket, netcap_sess->initial_mark ) < 0 ) return perrlog("_netcap_tcp_setsockopt_srv");
    
    do {
        debug( 8,"TCP: (%"PRIu64") Binding %i to %s:%i\n", netcap_sess->session_id, newsocket, unet_next_inet_ntoa( src_addr.sin_addr.s_addr ), ntohs( src_addr.sin_port ));
        if ( bind( newsocket, (struct sockaddr*)&src_addr, sizeof(src_addr)) < 0 ) {
            ret = errlog( ERR_WARNING,"bind(%s:%i) failed: %s\n", unet_next_inet_ntoa( src_addr.sin_addr.s_addr ), ntohs( src_addr.sin_port ), errstr );
            break;
        }
        
        /**
         * set non-blocking
         */
        if ( unet_blocking_disable( newsocket ) < 0 ) {
            ret = errlog( ERR_CRITICAL, "unet_blocking_disable\n" );
            break;
        }
        
        debug( 6, "TCP: (%"PRIu64") Connect %i to %s:%i\n", netcap_sess->session_id, newsocket,
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


