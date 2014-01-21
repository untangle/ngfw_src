/**
 * $Id$
 */
#include <stdlib.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <signal.h>
#include <unistd.h>
#include <inttypes.h>
#include <mvutil/hash.h>
#include <mvutil/errlog.h>
#include <mvutil/debug.h>
#include <mvutil/unet.h>

#include "libnetcap.h"
#include "netcap_sesstable.h"
#include "netcap_session.h"

#define _SESS_TABLE_SIZE    1337 
#define _INITIALIZED        0xC0ADDED

#define _verify_initialized() if ( _initialized != _INITIALIZED ) \
                                return errlog ( ERR_CRITICAL, "Unitialized\n" );

#define _verify_initialized_null() if ( _initialized != _INITIALIZED ) \
                                     return errlog_null ( ERR_CRITICAL, "Unitialized\n" );

typedef struct session_tuple {
    u_short proto;
    in_addr_t shost;
    in_addr_t dhost;
    u_short sport;
    u_short dport;
} session_tuple_t;

static u_char _tuple_equ_func (const void* input,const void* input2);
static u_long _tuple_hash_func (const void* input);
static session_tuple_t* _tuple_create (u_short proto, in_addr_t shost, in_addr_t dhost, u_short sport, u_short dport );
static int _netcap_sesstable_remove (netcap_session_t* netcap_sess);
static int _netcap_sesstable_remove_tuple (u_short proto, in_addr_t shost, in_addr_t dhost, u_short sport, u_short dport );

static int    _initialized = 0;
static ht_t   _sess_id_table;
static ht_t   _sess_tuple_table;
lock_t  netcap_sesstable_lock;

void netcap_sesstable_rdlock() {
    SESSTABLE_RDLOCK();
}

void netcap_sesstable_wrlock() {
    SESSTABLE_WRLOCK();
}

void netcap_sesstable_unlock() {
    SESSTABLE_UNLOCK();
}

int        netcap_sesstable_init ()
{
    if (ht_init(&_sess_id_table, _SESS_TABLE_SIZE, int_hash_func, int_equ_func, HASH_FLAG_KEEP_LIST)<0)
        return errlog(ERR_CRITICAL,"ht_create failed\n");

    if (ht_init(&_sess_tuple_table, _SESS_TABLE_SIZE, _tuple_hash_func, _tuple_equ_func, HASH_FLAG_FREE_KEY)<0)
        return errlog(ERR_CRITICAL,"ht_create failed\n");

    if (lock_init(&netcap_sesstable_lock,0)<0)
        return errlog(ERR_CRITICAL,"lock_init failed\n");

    _initialized = _INITIALIZED;

    return 0;
}

int        netcap_sesstable_cleanup ()
{
    int rm_count;

    _verify_initialized();

    SESSTABLE_WRLOCK();
    if (ht_destroy(&_sess_tuple_table)<0)
        perrlog("ht_destroy");
    if ((rm_count = ht_destroy(&_sess_id_table))<0)
        perrlog("ht_destroy");
    SESSTABLE_UNLOCK();

    if (lock_destroy(&netcap_sesstable_lock)<0)
        errlog(ERR_CRITICAL,"lock_destroy failed\n");

    if ( rm_count > 0 ) {
        errlog( ERR_WARNING, "%i entries left in session id table\n", rm_count);
    }

    return 0;
}

netcap_session_t* netcap_sesstable_get ( u_int64_t id )
{
    return netcap_nc_sesstable_get ( NC_SESSTABLE_LOCK, id );
}

netcap_session_t* netcap_nc_sesstable_get (int if_lock, u_int64_t id)
{
    netcap_session_t* session;

    _verify_initialized_null();

    if ( if_lock ) SESSTABLE_RDLOCK();

#if __WORDSIZE == 32
    session = (netcap_session_t*)ht_lookup(&_sess_id_table,(void*)(u_int32_t)id);
#else
    session = (netcap_session_t*)ht_lookup(&_sess_id_table,(void*)id);
#endif
    if ( if_lock ) SESSTABLE_UNLOCK();
    
    return session;
}

netcap_session_t* netcap_sesstable_get_tuple ( int proto, in_addr_t src, in_addr_t dst, u_short sport, u_short dport)
{
    return netcap_nc_sesstable_get_tuple(NC_SESSTABLE_LOCK,proto,src,dst,sport,dport);
}

netcap_session_t* netcap_nc_sesstable_get_tuple ( int if_lock, int proto, in_addr_t src, in_addr_t dst, u_short sport, u_short dport )
{
    session_tuple_t st = { .proto = proto, .shost = src, .dhost = dst, .sport = sport, .dport = dport };
    netcap_session_t* session;

    _verify_initialized_null();

    if ( if_lock ) SESSTABLE_RDLOCK();

    session = ht_lookup(&_sess_tuple_table,(void*)&st);
    
    if ( if_lock ) SESSTABLE_UNLOCK();

    //debug(4,"SESSTAB: %s :: (%i,%s:%i -> ","Getting tuple", proto, unet_next_inet_ntoa(src), sport);
    //debug_nodate(4,"%s:%i) = 0x%08x\n",  unet_next_inet_ntoa(dst), dport, session );
    
    return session;
}

int        netcap_sesstable_numsessions ( void )
{
    return netcap_nc_sesstable_numsessions(NC_SESSTABLE_LOCK);
}

int        netcap_nc_sesstable_numsessions (int if_lock )
{
    int ret;

    _verify_initialized();

    if ( if_lock ) SESSTABLE_RDLOCK();

    ret = ht_num_entries(&_sess_id_table);
    
    if ( if_lock) SESSTABLE_UNLOCK();

    return ret;
}

list_t*    netcap_sesstable_get_all_sessions ( void )
{
    return netcap_nc_sesstable_get_all_sessions (NC_SESSTABLE_LOCK);
}

list_t*    netcap_nc_sesstable_get_all_sessions ( int if_lock )
{
    list_t* sessions;

    _verify_initialized_null();

    if ( if_lock ) SESSTABLE_RDLOCK();

    sessions = ht_get_content_list ( &_sess_id_table);

    if ( if_lock) SESSTABLE_UNLOCK();

    return sessions;
}

int        netcap_sesstable_add ( netcap_session_t* netcap_sess )
{
    return netcap_nc_sesstable_add(NC_SESSTABLE_LOCK,netcap_sess);
}

int        netcap_nc_sesstable_add ( int if_lock, netcap_session_t* netcap_sess )
{
    if ( !netcap_sess ) return errlogargs();

    _verify_initialized();

    debug(4, "SESSTAB: Inserting session id %"PRIu64" = 0x%016"PRIxPTR"\n", netcap_sess->session_id, (uintptr_t) netcap_sess);

    if ( if_lock) SESSTABLE_WRLOCK();

#if __WORDSIZE == 32
    if ( ht_add( &_sess_id_table, (void*)(u_int32_t)netcap_sess->session_id, (void*)netcap_sess ) < 0 ) {
        if ( if_lock ) SESSTABLE_UNLOCK();
        return perrlog( "hash_add" );
    }
#else
    if ( ht_add( &_sess_id_table, (void*)netcap_sess->session_id, (void*)netcap_sess ) < 0 ) {
        if ( if_lock ) SESSTABLE_UNLOCK();
        return perrlog( "hash_add" );
    }
#endif

    if ( if_lock) SESSTABLE_UNLOCK();

    return 0;
}

int        netcap_sesstable_add_tuple ( netcap_session_t* netcap_sess, int protocol, in_addr_t src, in_addr_t dst, u_short sport, u_short dport )
{
    return netcap_nc_sesstable_add_tuple( NC_SESSTABLE_LOCK, netcap_sess, protocol,src,dst, sport, dport);
}

int        netcap_nc_sesstable_add_tuple ( int if_lock, netcap_session_t* sess, int protocol, in_addr_t src, in_addr_t dst, u_short sport, u_short dport )
{
    session_tuple_t* st;
    
    if (!sess) {
       return errlogargs();
    }

    st = _tuple_create(protocol,src,dst,sport,dport);
    
    if ( !st ) {
        return perrlog("_tuple_create");
    }
    

    if ( if_lock) SESSTABLE_WRLOCK();

    if ( ht_add( &_sess_tuple_table,(void*)st,(void*)sess)<0) {
        if  (if_lock) SESSTABLE_UNLOCK();
        free(st);
        return perrlog("hash_add");
    }

    if ( if_lock) SESSTABLE_UNLOCK();

    //debug(4,"SESSTAB: %s :: (%i,%s:%i -> ","Inserting tuple", protocol, unet_next_inet_ntoa(src), sport);
    //debug_nodate(4,"%s:%i) = 0x%08x\n",  unet_next_inet_ntoa(dst), dport, sess );
    
    return 0;
}

int        netcap_sesstable_remove_tuple (int if_lock, int proto, in_addr_t shost, in_addr_t dhost, u_short sport, u_short dport )
{
    //debug(4,"SESSTAB: %s :: (%i,%s:%i -> ","Removing tuple", proto, unet_next_inet_ntoa(shost),sport);
    //debug_nodate(4,"%s:%i)\n",unet_next_inet_ntoa(dhost), dport);

    if ( if_lock) SESSTABLE_WRLOCK();

    if (_netcap_sesstable_remove_tuple(proto, shost, dhost, sport, dport)) {
        if ( if_lock) SESSTABLE_UNLOCK();
        return perrlog("_netcap_sesstable_remove_tuple");
    }

    if ( if_lock) SESSTABLE_UNLOCK();

    return 0;
}

int        netcap_sesstable_remove ( int if_lock, netcap_session_t* netcap_sess )
{
    int ret;

    if (!netcap_sess) return errlogargs();

    debug(4, "SESSTAB: Removing session id: %"PRIu64"\n", netcap_sess->session_id);

    if  ( if_lock) SESSTABLE_WRLOCK();
    
    ret = _netcap_sesstable_remove(netcap_sess);

    if ( if_lock) SESSTABLE_UNLOCK();
    
    if ( ret < 0 ) {
        ret = errlog(ERR_CRITICAL, "Session missing from session table\n");
    }

   return ret;
}

int        netcap_sesstable_remove_session ( int if_lock, netcap_session_t* netcap_sess )
{
    netcap_endpoints_t* endpoints;

    if ( netcap_sess == NULL ) {
        return errlogargs();
    }

    if  ( if_lock ) SESSTABLE_WRLOCK();

    /* Try to remove the session id, ignore errors */
    _netcap_sesstable_remove(netcap_sess);

    /* Remove the forward tuple */
    endpoints = &netcap_sess->cli;

    if ( _netcap_sesstable_remove_tuple( netcap_sess->protocol,
                                         endpoints->cli.host.s_addr, endpoints->srv.host.s_addr,
                                         endpoints->cli.port, endpoints->srv.port ) < 0 ) {
        errlog( ERR_WARNING, "Failed to remove tuple (%d,%s:%i -> %s:%i)\n",
                netcap_sess->protocol, 
                unet_next_inet_ntoa( endpoints->cli.host.s_addr ), endpoints->cli.port,
                unet_next_inet_ntoa( endpoints->srv.host.s_addr ), endpoints->srv.port );

        /**
         * If its UDP try removing the reverse tuple
         * This should never happen, but it is just a safety mechanism
         **/
        if ( netcap_sess->protocol == IPPROTO_UDP ) {
            if ( _netcap_sesstable_remove_tuple( netcap_sess->protocol,
                                                 endpoints->srv.host.s_addr, endpoints->cli.host.s_addr,
                                                 endpoints->srv.port, endpoints->cli.port ) < 0 ) 
                errlog(ERR_WARNING,"Failed to remove reverse tuple (%d,%s:%i -> %s:%i)\n",
                       netcap_sess->protocol, 
                       unet_next_inet_ntoa( endpoints->srv.host.s_addr ), endpoints->srv.port,
                       unet_next_inet_ntoa( endpoints->cli.host.s_addr ), endpoints->cli.port );
        }
    }
        
    if  ( if_lock ) SESSTABLE_UNLOCK();
        
    return 0;
}

int        netcap_sesstable_kill_all_sessions ( void (*kill_all_function)(list_t *sessions) )
{
    list_t *sessions;
    int count;
    
    if ( kill_all_function == NULL ) return errlogargs();

    _verify_initialized();
    
    SESSTABLE_RDLOCK();

    count = ht_num_entries(&_sess_id_table);
    
    if ( count < 0 ) {
        SESSTABLE_UNLOCK();
        return perrlog("ht_num_entries");
    }

    if ( count == 0 ) {
        SESSTABLE_UNLOCK();
        return 0;
    }

    sessions = ht_get_content_list ( &_sess_id_table);
    
    if ( sessions == NULL ) {
        SESSTABLE_UNLOCK();
        return perrlog("ht_get_content_list");        
    }

    kill_all_function(sessions);
    
    SESSTABLE_UNLOCK();
    
    list_destroy(sessions);
    list_free(sessions);
    
    return count;
}

static int _netcap_sesstable_remove ( netcap_session_t* netcap_sess )
{
    /* Static/private function, no error checking necessary */
#if __WORDSIZE == 32
    if (ht_remove(&_sess_id_table,(void*)(u_int32_t)netcap_sess->session_id)<0) {
        perrlog("ht_remove");
        return -1;
    }
#else
    if (ht_remove(&_sess_id_table,(void*)netcap_sess->session_id)<0) {
        perrlog("ht_remove");
        return -1;
    }
#endif
    
    return 0;
}

static int _netcap_sesstable_remove_tuple ( u_short proto, in_addr_t shost, in_addr_t dhost, u_short sport, u_short dport ) 
{    
    /* Static/private function, no error checking necessary */
    session_tuple_t st = {proto,shost,dhost,sport,dport};

    if ( ht_remove( &_sess_tuple_table, (void*)&st ) < 0 ) {
        return errlog( ERR_WARNING, "ht_remove (%d,%s:%i -> %s:%i)\n", proto, 
                       unet_next_inet_ntoa( shost ), sport,
                       unet_next_inet_ntoa( dhost ), dport );
    }

    return 0;
}

static session_tuple_t* _tuple_create ( u_short proto, in_addr_t shost, in_addr_t dhost, u_short sport, u_short dport )
{
    session_tuple_t* st;

    if (( st = malloc( sizeof( session_tuple_t ))) == NULL ) return errlogmalloc_null();

    st->proto = proto;
    st->shost = shost;
    st->dhost = dhost;
    st->sport = sport;
    st->dport = dport;

    return st;
}

static u_long  _tuple_hash_func ( const void* input )
{
    u_long hash;
    session_tuple_t* st = (session_tuple_t*) input;

    /* XXX weak, but prime hash table size */
    hash  = 17;
    hash  = ( 37 * hash ) + (u_long)st->proto;
    hash  = ( 37 * hash ) + (u_long)st->shost;
    hash  = ( 37 * hash ) + (u_long)st->dhost;
    hash  = ( 37 * hash ) + (u_long)st->sport;
    hash  = ( 37 * hash ) + (u_long)st->dport;

/*     hash  = (u_int)(st->shost << 4 | ((st->shost>>28) & 0x0F)); */
/*     hash ^= (u_int)(st->dhost << 8 | ((st->dhost>>24) & 0xFF)); */
/*     hash ^= (u_int)(st->sport << 2 ) * ( st->proto << 4); */
/*     hash ^= (u_int)(st->dport << 12 ) * ( st->proto << 8 ); */
    
    return hash;
}

static u_char _tuple_equ_func ( const void* input, const void* input2 )
{
    session_tuple_t* st1 = (session_tuple_t*) input;
    session_tuple_t* st2 = (session_tuple_t*) input2;
    
    if (st1->proto != st2->proto) return 0;
    if (st1->dport != st2->dport) return 0;
    if (st1->sport != st2->sport) return 0;
    if ((u_int)st1->dhost != (u_int)st2->dhost) return 0;
    if ((u_int)st1->shost != (u_int)st2->shost) return 0;

    return 1;
}
