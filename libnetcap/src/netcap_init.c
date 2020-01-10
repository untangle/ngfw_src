/**
 * $Id$
 */
#include "netcap_init.h"

#include <stdio.h>
#include <stdlib.h>
#include <netdb.h>
#include <unistd.h>
#include <pthread.h>
#include <errno.h>
#include <sys/utsname.h>

#include <mvutil/errlog.h>
#include <mvutil/debug.h>
#include <mvutil/uthread.h>

#include "netcap_globals.h"
#include "netcap_server.h"
#include "netcap_queue.h"
#include "netcap_hook.h"
#include "netcap_arp.h"
#include "netcap_tcp.h"
#include "netcap_udp.h"
#include "netcap_icmp.h"
#include "netcap_interface.h"
#include "netcap_session.h"
#include "netcap_sesstable.h"
#include "netcap_nfconntrack.h"
#include "netcap_virtual_interface.h"
#include "netcap_conntrack.h"

#define NETCAP_TUN_DEVICE_NAME "utun"


enum {
    STATUS_UNINITIALIZED,
    STATUS_INITIALIZED,
    STATUS_CLEANING,
    STATUS_CLEAN,
    STATUS_ERROR
};

static struct {
    int status;
    pthread_mutex_t mutex;
    pthread_key_t   tls_key;
} _init = {
    .status = STATUS_UNINITIALIZED,
    .mutex  = PTHREAD_RECURSIVE_MUTEX_INITIALIZER_NP
};

static int _netcap_init();
static int _tls_init   ( void* buf, size_t size );

static int ip_saddr = 22; 
static int ip_sendnfmark = 24;
static int is_new_kernel = 0;

int netcap_init()
{
    int ret;
        
    if ( pthread_mutex_lock( &_init.mutex ) < 0 ) {
        return perrlog( "pthread_mutex_lock" );
    }

    if (( ret = _netcap_init()) == 0 ) {
        _init.status = STATUS_INITIALIZED;
    } else {
        _init.status = STATUS_ERROR;
    }

    if ( pthread_mutex_unlock( &_init.mutex ) < 0 ) {
        return perrlog( "pthread_mutex_lock" );
    }

    return ret;
    
}

static int _netcap_init()
{
    struct utsname utsn;
    
    if ( pthread_key_create( &_init.tls_key, uthread_tls_free ) != 0 )
        return perrlog( "pthread_key_create\n" );

    if (uname(&utsn) < 0) {
        return perrlog("uname");
    }

    is_new_kernel = 0;
    if ( strstr(utsn.release,"2.6.26") != NULL) {
        return perrlog( "Unsupported kernel: 2.6.26\n" );
    }
    else if ( strstr(utsn.release,"2.6.32") != NULL ) {
        ip_saddr = 22;
        ip_sendnfmark = 24;
    }
    else if ( strstr(utsn.release,"3.0") != NULL ) {
        ip_saddr = 24;
        ip_sendnfmark = 25;
    }
    else if ( strstr(utsn.release,"3.2") != NULL ) {
        ip_saddr = 24;
        ip_sendnfmark = 25;
    }
    else if ( strstr(utsn.release,"3.10") != NULL ) {
        ip_saddr = 24;
        ip_sendnfmark = 25;
        is_new_kernel = 310;
    }
    else if ( strstr(utsn.release,"3.16") != NULL ) {
        ip_saddr = 24;
        ip_sendnfmark = 25;
        is_new_kernel = 316;
    }
    else if ( strstr(utsn.release,"3.18") != NULL ) {
        ip_saddr = 24;
        ip_sendnfmark = 25;
        is_new_kernel = 318;
    }
    else if ( strstr(utsn.release,"4.4.3") != NULL ) {
        ip_saddr = 24;
        ip_sendnfmark = 25;
        is_new_kernel = 443;
    }
    else if ( strstr(utsn.release,"4.9.28") != NULL ) {
        ip_saddr = 24;
        ip_sendnfmark = 25;
        is_new_kernel = 443;
    }
    else if ( strstr(utsn.release,"4.9.0-11") != NULL ) {
        ip_saddr = 24;
        ip_sendnfmark = 25;
        is_new_kernel = 443;
    }
    else {
        errlog( ERR_WARNING, "Unknown kernel: %s\n", utsn.release );
        errlog( ERR_WARNING, "Assuming 4.0.0\n" );
        /* unknown kernel */ 
        ip_saddr = 24;
        ip_sendnfmark = 25;
        is_new_kernel = 492;
    }
    
    if (netcap_sesstable_init()<0)
        return perrlog("netcap_sesstable_init");
    if (netcap_sessions_init()<0)
        return perrlog("netcap_sessions_init");
    if (netcap_hooks_init()<0) 
        return perrlog("netcap_hooks_init");
    if (netcap_queue_init()<0) 
        return perrlog("netcap_queue_init");
    if (netcap_arp_init()<0) 
        return perrlog("netcap_arp_init");
    if (netcap_tcp_init()<0) 
        return perrlog("netcap_tcp_init");
    if (netcap_udp_init()<0) 
        return perrlog("netcap_udp_init");
    if (netcap_icmp_init()<0) 
        return perrlog("netcap_icmp_init");    
    if (netcap_server_init()<0) 
        return perrlog("netcap_server_init");
    if (netcap_nfconntrack_init()<0)
        return errlog( ERR_CRITICAL, "netcap_nfconntrack_init\n" );
    if (netcap_conntrack_init( )<0)
        return errlog( ERR_CRITICAL, "netcap_conntrack_init\n" );
    if (netcap_virtual_interface_init( NETCAP_TUN_DEVICE_NAME ) < 0 )
        return errlog( ERR_CRITICAL, "netcap_virtual_interface_init\n" );
    
    debug(1,"NETCAP %s Initialized (kernel: %s)\n",netcap_version(), utsn.release);

    return 0;
}

int netcap_cleanup()
{
    debug(1,"NETCAP: Netcap Cleaning up...\n");

    int ret = 0;

    if ( pthread_mutex_lock( &_init.mutex ) < 0 )
        perrlog( "pthread_mutex_lock" );

    if ( _init.status == STATUS_CLEAN ) {
        ret = errlog( ERR_CRITICAL, "NETCAP: Already cleaned\n" );
    }
    
    _init.status = STATUS_CLEANING;

    if ( pthread_mutex_unlock( &_init.mutex ) < 0 )
        perrlog( "pthread_mutex_lock" );

    if ( ret < 0 ) {
        _init.status = STATUS_CLEAN;
        return errlog( ERR_CRITICAL, "NETCAP: Skipping second clean\n" );
    }

    if (netcap_server_shutdown()<0)
        perrlog("netcap_server_shutdown");
    if (netcap_nfconntrack_cleanup()<0)
        perrlog("netcap_nfconntrack_cleanup");
    if (netcap_conntrack_cleanup()<0)
        perrlog("netcap_conntrack_cleanup");
    if (netcap_queue_cleanup()<0)
        perrlog("netcap_queue_cleanup");
    if (netcap_tcp_cleanup()<0)
        perrlog("netcap_tcp_cleanup");
    if (netcap_udp_cleanup()<0)
        perrlog("netcap_udp_cleanup");
    if (netcap_hooks_cleanup()<0) 
        perrlog("netcap_hooks_cleanup");
    if (netcap_sessions_cleanup()<0)
        perrlog("netcap_sessions_cleanup");
    if (netcap_sesstable_cleanup()<0)
        perrlog("netcap_sesstable_cleanup");
    netcap_virtual_interface_destroy();
    
    debug(1,"NETCAP: Cleaned.\n");

    /* No need to lock since it has already moved out of the initialized state. */
    _init.status = STATUS_CLEAN;

    return 0;
}

int netcap_is_initialized()
{
    int ret;

    if ( pthread_mutex_lock( &_init.mutex ) < 0 )
        return perrlog( "pthread_mutex_lock" );

    if ( _init.status == STATUS_INITIALIZED || _init.status == STATUS_CLEANING ) {
        ret = 1;
    } else {
        ret = 0;
    }

    if ( pthread_mutex_unlock( &_init.mutex ) < 0 )
        return perrlog( "pthread_mutex_unlock" );

    return ret;
}

netcap_tls_t* netcap_tls_get( void )
{
    netcap_tls_t* tls = NULL;

    if (( tls = uthread_tls_get( _init.tls_key, sizeof( netcap_tls_t ), _tls_init )) == NULL ) {
        return errlog_null( ERR_CRITICAL, "uthread_get_tls\n" );
    }
    
    return tls;
}

int IP_SADDR_VALUE ( )
{
    return ip_saddr;
}

int IP_SENDNFMARK_VALUE ( )
{
    return ip_sendnfmark;
}

int IS_NEW_KERNEL ( )
{
    return is_new_kernel;
}

static int _tls_init( void* buf, size_t size )
{
    netcap_tls_t* tls = buf;

    if (( size != sizeof( netcap_tls_t )) || ( tls == NULL )) return errlogargs();
        
    if (( netcap_session_tls_init( &tls->session )) < 0 ) {
        return errlog( ERR_CRITICAL, "netcap_session_tls_init\n" );
    }

    return 0;
}
