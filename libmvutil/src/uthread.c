/**
 * $Id$
 */
#include "mvutil/uthread.h"

#include <pthread.h>
#include <stdlib.h>
#include <inttypes.h>
#include <sys/utsname.h>

#include "mvutil/errlog.h"

#define AMD64_STACK_SIZE 256*1024
#define I386_STACK_SIZE 96*1024
#define ARM_STACK_SIZE 256*1024
#define ARM64_STACK_SIZE 256*1024

uthread_attr_t uthread_attr;

pthread_attr_t small_detached_attr;

struct sched_param rr_high_priority;
struct sched_param rr_medium_priority;
struct sched_param rr_low_priority;

struct sched_param other_high_priority;
struct sched_param other_medium_priority;
struct sched_param other_low_priority;

int uthread_init (void)
{
    int c;
    unsigned int min, max;
    struct utsname utsn;

    if (pthread_attr_init(&small_detached_attr)<0)
        return perrlog("pthread_attr_init");
    if (pthread_attr_setdetachstate(&small_detached_attr,PTHREAD_CREATE_DETACHED)<0)
        return perrlog("pthread_attr_setdetachstate");
    if (uname(&utsn) < 0) {
        return perrlog("uname");
    }
    if ( strstr(utsn.release,"amd64") != NULL) {
        if ( pthread_attr_setstacksize( &small_detached_attr, AMD64_STACK_SIZE ) < 0 )
            return perrlog("pthread_attr_setstacksize");
    }
    else if ( strstr(utsn.release,"386") != NULL) {
        if ( pthread_attr_setstacksize( &small_detached_attr, I386_STACK_SIZE ) < 0 )
            return perrlog("pthread_attr_setstacksize");
    }
    else if ( strstr(utsn.release,"686") != NULL) {
        if ( pthread_attr_setstacksize( &small_detached_attr, I386_STACK_SIZE ) < 0 )
            return perrlog("pthread_attr_setstacksize");
    }
    else if ( strstr(utsn.machine,"x86_64") != NULL) {
        if ( pthread_attr_setstacksize( &small_detached_attr, AMD64_STACK_SIZE ) < 0 )
            return perrlog("pthread_attr_setstacksize");
    }
    else if ( strstr(utsn.machine,"686") != NULL) {
        if ( pthread_attr_setstacksize( &small_detached_attr, I386_STACK_SIZE ) < 0 )
            return perrlog("pthread_attr_setstacksize");
    }
    else if ( strstr(utsn.machine,"arm") != NULL) {
        if ( pthread_attr_setstacksize( &small_detached_attr, ARM_STACK_SIZE ) < 0 )
            return perrlog("pthread_attr_setstacksize");
    }
    else if ( strstr(utsn.machine,"aarch64") != NULL) {
        if ( pthread_attr_setstacksize( &small_detached_attr, ARM64_STACK_SIZE ) < 0 )
            return perrlog("pthread_attr_setstacksize");
    }
    else {
        errlog( ERR_WARNING, "Unknown architecture. Kernel release: \"%s\"\n", utsn.release );
        errlog( ERR_WARNING, "Using i386 stack size.\n" );
        if ( pthread_attr_setstacksize( &small_detached_attr, I386_STACK_SIZE ) < 0 )
            return perrlog("pthread_attr_setstacksize");
        
    }

    min = sched_get_priority_min(SCHED_RR);
    max = sched_get_priority_max(SCHED_RR);
    rr_medium_priority.sched_priority = (min+max)/2;
    rr_high_priority.sched_priority = rr_medium_priority.sched_priority + 1;
    rr_low_priority.sched_priority = rr_medium_priority.sched_priority - 1;

    /* According to the man page, Priority doesn't matter for SCHED_OTHER threads */
    min = sched_get_priority_min(SCHED_OTHER);
    max = sched_get_priority_max(SCHED_OTHER);
    other_medium_priority.sched_priority = (min+max)/2;
    other_high_priority.sched_priority = other_medium_priority.sched_priority + 1;
    other_low_priority.sched_priority  = other_medium_priority.sched_priority - 1;

    for ( c = 0 ; c < ( sizeof ( uthread_attr) / sizeof ( pthread_attr_t ) ) ; c++ ) {
        memcpy(&(((pthread_attr_t*)&uthread_attr)[c]), &small_detached_attr, sizeof ( pthread_attr_t ) );
    }

    if ( pthread_attr_setschedpolicy( &uthread_attr.rr.low, SCHED_RR ) < 0 ||
         pthread_attr_setschedpolicy( &uthread_attr.rr.medium, SCHED_RR ) < 0 ||
         pthread_attr_setschedpolicy( &uthread_attr.rr.high, SCHED_RR ) < 0 ||
         pthread_attr_setschedpolicy( &uthread_attr.other.low, SCHED_OTHER ) < 0 ||
         pthread_attr_setschedpolicy( &uthread_attr.other.medium, SCHED_OTHER ) < 0 ||
         pthread_attr_setschedpolicy( &uthread_attr.other.high, SCHED_OTHER ) < 0 ) {
        return perrlog( "pthread_attr_setschedpolicy" );
    }
    
    if ( pthread_attr_setschedparam ( &uthread_attr.rr.low, &rr_low_priority ) < 0 ||
         pthread_attr_setschedparam ( &uthread_attr.rr.medium, &rr_medium_priority ) < 0 ||
         pthread_attr_setschedparam ( &uthread_attr.rr.high, &rr_high_priority ) < 0 ||
         pthread_attr_setschedparam ( &uthread_attr.other.low, &other_low_priority ) < 0 ||
         pthread_attr_setschedparam ( &uthread_attr.other.medium, &other_medium_priority ) < 0 ||
         pthread_attr_setschedparam ( &uthread_attr.other.high, &other_high_priority ) < 0 )
    {
        return perrlog ( "pthread_attr_setschedpolicy" );
    }

    return 0;
}

void  uthread_tls_free( void* buf )
{
    if ( buf != NULL ) free( buf );
}

void* uthread_tls_get( pthread_key_t tls_key, size_t size, int(*init)(void *buf, size_t size ))
{
    void* buf;
    void* verify;
    
    if ( size < 0 )
        return errlogargs_null();
        
    
    if (( buf = pthread_getspecific( tls_key )) == NULL ) {
        /* Buffer is not set yet, allocate a new buffer */
        if (( buf = malloc( size )) == NULL ) {
            return errlogmalloc_null();
        }
        
        /* Set the data on the key */
        if ( pthread_setspecific( tls_key, buf ) != 0 ) {
            free( buf );
            return perrlog_null( "pthread_setspecific" );
        }
        
        /* Just a sanity check to make sure the correct value is returned */
        if (( verify = pthread_getspecific( tls_key )) != buf ) {
            free( buf );
            return errlog_null( ERR_CRITICAL, "pthread_getspecific returned different val: 0x%016"PRIxPTR"->0x%016"PRIxPTR"", (uintptr_t) buf, (uintptr_t) verify );
        }
        
        /* If necessary, call the initializer function, call this last so if the initializer
         * allocates more memory it doesn't have to be freed if one of the previous errors occured */
        if (( init != NULL ) && ( init( buf, size ) < 0 )) {
            free( buf );
            pthread_setspecific( tls_key, NULL );
            return errlog_null( ERR_CRITICAL, "init: size %i\n", (int) size );
        }

    }

    return buf;
}
