/* $HeadURL: svn://chef/work/src/libnetcap/src/netcap_sched.c $ */

#include <unistd.h>
#include <sys/time.h>
#include <stdlib.h>
#include <pthread.h>

#include <mvutil/debug.h>
#include <mvutil/errlog.h>
#include <mvutil/list.h>
#include <mvutil/mailbox.h>
#include <mvutil/utime.h>

#include "netcap_sched.h"

/* Number of seconds to wait before just exiting the scheduler */
#define _EXIT_TIMEOUT_SEC 4

/* Return 1 when ts1 is before ts2 (both arguments should be pointers)*/
static __inline int _ts_before ( struct timespec* ts1, struct timespec* ts2 ) 
{
    if ( ts1->tv_sec == ts2->tv_sec ) {
        if ( ts1->tv_nsec > ts2->tv_nsec ) return 0;        
    } else if ( ts1->tv_sec > ts2->tv_sec ) return 0;
    
    return 1;
}

/* Calcuate the value of x useconds in the future */
static __inline int _ts_future ( struct timespec* ts, int usec ) 
{
    struct timeval tv;

    if ( gettimeofday ( &tv, NULL ) < 0 ) return perrlog ( "gettimeofday");
    
    ts->tv_sec  = tv.tv_sec + USEC_TO_SEC( usec );
    ts->tv_nsec = USEC_TO_NSEC( tv.tv_usec + ( usec % U_SEC ));

    if ( ts->tv_nsec >= N_SEC ) {
        ts->tv_sec += NSEC_TO_SEC( ts->tv_nsec );
        ts->tv_nsec = ts->tv_nsec % N_SEC;
    } else if ( ts->tv_nsec < 0 ) {
        /* Just in case? */
        ts->tv_nsec = 0;
    }

    return 0;
}

/* At least 1 millisecond */
#define _SCHED_TIME_MIN  1000

/* At most half an hour ( a whole hour will cause a buffere overflow ) */
#define _SCHED_TIME_MAX  SEC_TO_USEC(30*60)

/* 1 Quarter of the max */
#define _SCHED_TIME_WAIT ( _SCHED_TIME_MAX >> 2 )

#define _SCHED_EXIT 0xDEADBEEF

/* Anything within this time window is considered now, this is in USECs */ 
#define _SCHED_NOW  2000
    
typedef enum
{
    _FUNC,
    _FUNC_Z,
    _CLEANUP,
    _CLEANUP_Z
} _msg_type_t;

typedef union 
{
    netcap_sched_func_t*   func;
    netcap_sched_func_z_t* func_z;
} _sched_func_t;

typedef struct 
{
    _msg_type_t type;
    struct timespec ts;
    void* arg;
    _sched_func_t func;
} _sched_msg_t, _sched_event_t;

typedef struct
{
    int       initialized;
    mailbox_t mailbox;
    /* Queue of events to occur */
    list_t queue;
    sem_t     sem_exit;
    pthread_mutex_t mutex;
} _sched_t;

static _sched_t _sched =
{
    .initialized = 0,
    .mutex = PTHREAD_MUTEX_INITIALIZER
};

static int           _sched_mailbox_put ( _msg_type_t type, int usec, _sched_func_t func, void* arg );

/* This sets timespec to the amount of time to wait until the next event */
static int           _sched_next_event  ( struct timespec* ts );

/* _sched_run_events: run all of the events that are ready */
static int           _sched_run_events ( int if_exit );

/* Insert an event into the event queue */
static int           _sched_add_event   ( _sched_msg_t* msg, int* if_alive );

static _sched_msg_t* _sched_msg_malloc  ( void );
static int           _sched_msg_init    ( _sched_msg_t* msg, _msg_type_t type, int usec,
                                          _sched_func_t func, void* arg );

static _sched_msg_t* _sched_msg_create  ( _msg_type_t type, int usec, _sched_func_t func, void* arg );

static int           _sched_msg_free    ( _sched_msg_t* msg );
//static int           _sched_msg_destroy ( _sched_msg_t* msg );
//static int           _sched_msg_raze    ( _sched_msg_t* msg );

static int           _wait_exit         ( void );

int   netcap_sched_init      ( void )
{
    /* Initialize the mailbox and the list */
    if ( mailbox_init ( &_sched.mailbox ) < 0 ) return errlog ( ERR_CRITICAL, "mailbox_init\n" );
    
    /* Initialize the queue */
    if ( list_init ( &_sched.queue, LIST_FLAG_FREE_VAL ) < 0 ) return errlog ( ERR_CRITICAL, "list_init\n" );
    
    if ( sem_init ( &_sched.sem_exit, 0, 0 ) < 0 ) return perrlog ( "sem_init" );

    _sched.initialized = 1;

    return 0;
}

int   netcap_sched_event     ( netcap_sched_func_t* func, void* arg, int usec )
{
    if ( func == NULL ) return errlogargs();

    if (( usec < _SCHED_TIME_MIN ) || ( usec > _SCHED_TIME_MAX )) {
        return errlog ( ERR_CRITICAL, "usec is too long or short %d\n", usec );
    }

    if ( _sched_mailbox_put ( _FUNC, usec, (_sched_func_t)func, arg ) < 0 ) {
        return errlog ( ERR_CRITICAL, "_sched_mailbox_put\n");
    }

    return 0;
}

int   netcap_sched_event_z   ( netcap_sched_func_z_t* func, int usec )
{
    if ( func == NULL ) return errlogargs();

    if (( usec < _SCHED_TIME_MIN ) || ( usec > _SCHED_TIME_MAX )) {
        return errlog ( ERR_CRITICAL, "usec is too long or short %d\n", usec );
    }

    if ( _sched_mailbox_put ( _FUNC_Z, usec, (_sched_func_t)func, NULL ) < 0 ) {
        return errlog ( ERR_CRITICAL, "_sched_mailbox_put\n");
    }

    return 0;

}

int   netcap_sched_cleanup   ( netcap_sched_func_t* func, void* arg )
{
    if ( _sched_mailbox_put ( _CLEANUP, 0, (_sched_func_t)func, arg ) < 0 ) {
        return errlog ( ERR_CRITICAL, "_sched_mailbox_put\n");
    }

    _wait_exit ();    

    if ( sem_destroy ( &_sched.sem_exit ) < 0 ) return perrlog ( "sem_destroy" );

    return 0;    
}

int   netcap_sched_cleanup_z ( netcap_sched_func_z_t* func )
{
    if ( _sched_mailbox_put ( _CLEANUP_Z, 0, (_sched_func_t)func, NULL ) < 0 ) {
        return errlog ( ERR_CRITICAL, "_sched_mailbox_put\n");
    }

    _wait_exit ();

    if ( sem_destroy ( &_sched.sem_exit ) < 0 ) return perrlog ( "sem_destroy" );
    
    return 0;    
}

void* netcap_sched_donate    ( void* arg )
{
    _sched_msg_t* msg;
    struct timespec ts;
    int alive = 1;


    debug( 4, "NETCAP: Starting scheduler\n" );

    /* XXX Should you return on an error??? */
    /* XXX Should there be one last read from the mailbox before exiting */    
    while ( alive )
    {
        /* Get the timeout until the next event */
        if ( _sched_next_event ( &ts ) < 0 ) {
            errlog ( ERR_CRITICAL, "_sched_next_event\n" );

            sleep ( 1 ); /* To avoid spinning */
            continue;
        }
        
        /* Check the mailbox */
        msg = mailbox_ntimed_get ( &_sched.mailbox, &ts );

        _sched_run_events ( ~_SCHED_EXIT );
        
        if ( msg != NULL ) {
            /* Get all messages off of the queue */
            do {
                /* Add the message to the queue */
                if ( _sched_add_event ( msg, &alive ) < 0 ) errlog ( ERR_CRITICAL, "_sched_add_event\n" );
            } while (( msg = mailbox_try_get ( &_sched.mailbox )) != NULL );
        }
    }

    debug( 4, "NETCAP: Exiting scheduler\n" );

    _sched.initialized = 0;

    _sched_run_events ( _SCHED_EXIT );
    
    sem_post ( &_sched.sem_exit );

    if ( list_destroy ( &_sched.queue ) < 0 ) errlog ( ERR_CRITICAL, "list_destroy\n" );
    
    if ( mailbox_destroy ( &_sched.mailbox ) < 0 ) errlog ( ERR_CRITICAL, "mailbox_destroy\n" );

    return NULL;
}

static int           _sched_mailbox_put ( _msg_type_t type, int usec, _sched_func_t func, void* arg )
{
    _sched_msg_t* msg;
    int initialized;

    if ( pthread_mutex_lock ( &_sched.mutex ) < 0 ) return perrlog ( "pthread_mutex_lock" );
    initialized = _sched.initialized;
    if ( pthread_mutex_unlock ( &_sched.mutex ) < 0 ) return perrlog ( "pthread_mutex_unlock" );

    if ( !initialized ) return errlog ( ERR_CRITICAL, "Scheduler is not initialized\n" );
    
    if ( ( msg = _sched_msg_create ( type, usec, func, arg )) == NULL ) {
        return errlog ( ERR_CRITICAL, "_sched_msg_create\n" );
    }
    
    if ( mailbox_put ( &_sched.mailbox, msg ) < 0 ) {
        return errlog ( ERR_CRITICAL, "mailbox_put\n");
    }
    
    return 0;
}

/* This sets timespec to the amount of time to wait until the next event */
static int           _sched_next_event  ( struct timespec* ts )
{
    _sched_event_t* event;

    if ( list_length ( &_sched.queue ) == 0 ) {
        if ( _ts_future ( ts, _SCHED_TIME_WAIT ) < 0 ) return errlog ( ERR_CRITICAL, "_ts_future\n" );
    } else {
        if ( ( event = list_head_val ( &_sched.queue ) ) == NULL ) {
            return errlog ( ERR_CRITICAL, "list_head_val\n");
        }
        
        ts->tv_sec  = event->ts.tv_sec;
        ts->tv_nsec = event->ts.tv_nsec;
    }

    return 0;
}

/* _sched_run_events: run all of the events that are ready */
static int           _sched_run_events  ( int if_exit )
{
    _sched_event_t* event;
    list_node_t* node;
    list_node_t* next;
    struct timespec now;
    bzero(&now, sizeof(now));
    
    if ( list_length ( &_sched.queue ) == 0 ) return 0;

    if (( node = list_head ( &_sched.queue )) == NULL) return errlog ( ERR_CRITICAL, "list_head\n" );
    
    _ts_future ( &now, _SCHED_NOW );

    do {
        if (( event = list_node_val ( node )) == NULL ) return errlog ( ERR_CRITICAL, "list_node_val\n" );

        if ( _ts_before ( &event->ts, &now ) || ( if_exit == _SCHED_EXIT ) ) {
            if ( event->func.func == NULL ) {
                if ( event->type == _FUNC || event->type == _FUNC_Z ) {
                    errlog ( ERR_CRITICAL, "event->func == NULL\n" );
                }
            } else {
                switch ( event->type ) {
                case _CLEANUP_Z:
                case _FUNC_Z: event->func.func_z(); break;
                    
                case _CLEANUP:
                case _FUNC: event->func.func ( event->arg ); break;
                    
                default: errlog ( ERR_CRITICAL, "event->type is invalid (%d)\n", event->type );
                }
            }
        } else {
            /* Done executing */
            break;
        }
        
        next = list_node_next ( node );
        if ( list_remove ( &_sched.queue, node ) < 0 ) return errlog ( ERR_CRITICAL, "list_remove\n" );
        node = next;
    } while ( node != NULL );
    
    return 0;
}

/* Insert an event into the event queue */
static int           _sched_add_event   ( _sched_msg_t* msg, int* if_alive )
{
    list_node_t* node;
    _sched_event_t* event;
    int c;

    if (( msg->type == _CLEANUP ) || ( msg->type == _CLEANUP_Z )) {
        /* Lock the mutex while clearing the initialized flag */
        if ( pthread_mutex_lock ( &_sched.mutex ) < 0 ) return perrlog ( "pthread_mutex_lock" );
        _sched.initialized = 0;
        if ( pthread_mutex_unlock ( &_sched.mutex ) < 0 ) return perrlog ( "pthread_mutex_unlock" );

        *if_alive = 0;
        
        if ( list_add_tail ( &_sched.queue, msg ) < 0 ) return errlog ( ERR_CRITICAL, "list_add_tail\n" );
        return 0;
    }

    if ( ( c = list_length ( &_sched.queue ) ) == 0 ) {
        if ( list_add_head ( &_sched.queue, msg ) < 0 ) return errlog ( ERR_CRITICAL, "list_add_head\n" );
        return 0;
    }

    c++;     /* give the loop a maximum bound */
    
    if (( node = list_head ( &_sched.queue )) == NULL ) return errlog ( ERR_CRITICAL, "list_head\n");
        
    while ( c--  >= 0 ) {
        if ( node == NULL ) {
            if ( list_add_tail ( &_sched.queue, msg ) < 0 ) return errlog ( ERR_CRITICAL, "list_add_tail" );
            return 0;
        }
        
        if (( event = list_node_val ( node )) == NULL ) return errlog ( ERR_CRITICAL, "list_node_val\n" );
        
        if ( _ts_before ( &msg->ts, &event->ts )) {
            if ( list_add_before ( &_sched.queue, node, msg ) < 0 ) {
                return errlog ( ERR_CRITICAL, "list_add_before\n" );
            }
            return 0;
        }
        node = list_node_next ( node );
    }

    return errlog ( ERR_CRITICAL, "Failed to add item\n");
}


static _sched_msg_t* _sched_msg_malloc  ( void )
{
    _sched_msg_t* msg;
    
    if ( ( msg = calloc ( sizeof(_sched_msg_t), 1 ) ) == NULL ) return errlogmalloc_null();

    return msg;
}

static int           _sched_msg_init    ( _sched_msg_t* msg, _msg_type_t type, int usec,
                                          _sched_func_t func, void* arg )
{
    if ( msg == NULL ) return errlogargs();

    if ( _ts_future ( &msg->ts, usec ) < 0 ) return errlog ( ERR_CRITICAL, "_ts_future\n" );

    switch ( type ) {
    case _FUNC:
    case _FUNC_Z:
    case _CLEANUP:
    case _CLEANUP_Z:
        msg->type = type;
        break;
    default:
        return errlog ( ERR_CRITICAL, "Invalid type: %d\n", type );
    }

    msg->func = func;
    msg->arg  = arg;

    return 0;
}

static _sched_msg_t* _sched_msg_create  ( _msg_type_t type, int usec, _sched_func_t func, void* arg )
{
    _sched_msg_t* msg;
    
    if ( ( msg = _sched_msg_malloc () ) == NULL ) return errlog_null ( ERR_CRITICAL, "_sched_msg_malloc\n" );

    if ( _sched_msg_init ( msg, type, usec, func, arg ) < 0 ) {
        _sched_msg_free ( msg );
        return errlog_null ( ERR_CRITICAL, "_sched_msg_init\n" );
    }

    return msg;
}


static int           _sched_msg_free    ( _sched_msg_t* msg )
{
    if ( msg == NULL ) return errlogargs();
    free ( msg );
    return 0;
}

/* static int           _sched_msg_destroy ( _sched_msg_t* msg ) */
/* { */
/*     if ( msg == NULL ) return errlogargs(); */
/*     return 0; */
/* } */

/* static int           _sched_msg_raze    ( _sched_msg_t* msg ) */
/* { */
/*     if ( msg == NULL ) return errlogargs(); */
    
/*     if ( _sched_msg_destroy ( msg ) < 0 ) errlog ( ERR_CRITICAL, "_sched_msg_destroy\n" ); */

/*     if ( _sched_msg_free ( msg ) < 0 ) errlog ( ERR_CRITICAL, "_sched_msg_free\n" ); */
    
/*     return 0; */
/* } */

static int           _wait_exit         ( void )
{
    struct timespec ts;
    struct timeval  tv;
    
    if ( gettimeofday ( &tv, NULL ) < 0 ) return perrlog ( "gettimeofday" );

    /* Wait a few seconds for cleanup to occur */
    ts.tv_sec  = tv.tv_sec + _EXIT_TIMEOUT_SEC;
    ts.tv_nsec = 0;

    /* Wait until there is a timeout or all of the events are completed */
    if ( sem_timedwait( &_sched.sem_exit, &ts ) != 0 ) {
        if ( errno == ETIMEDOUT ) return errlog( ERR_CRITICAL, "Scheduler exit timeout\n" );
        else  return perrlog("sem_timedwait");
    }
    
    return 0;
}


