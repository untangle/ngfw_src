/**
 * $Id$
 */
#include <jni.h>
#include <stdlib.h>
#include <stdio.h>
#include <unistd.h>
#include <sys/socket.h>

#include <libmvutil.h>
#include <libvector.h>
#include <libnetcap.h>
#include <mvutil/debug.h>
#include <mvutil/mvpoll.h>
#include <mvutil/mailbox.h>
#include <mvutil/errlog.h>
#include <mvutil/debug.h>
#include <vector/event.h>
#include <vector/source.h>

#include "jvector.h"

#include "com_untangle_jvector_UDPSource.h"

/*
 * Class:     UDPSource
 * Method:    create
 * Signature: (I)I
 */
JNIEXPORT jlong JNICALL Java_com_untangle_jvector_UDPSource_create
  (JNIEnv *env, jobject _this, jlong pointer )
{
    jvector_source_t* src;
    mvpoll_key_t* key;
    mailbox_t* mb;
    
    if (( mb = (mailbox_t*)(uintptr_t)pointer) == NULL ) return (uintptr_t)errlogargs_null();
        
    if (( key = mailbox_get_mvpoll_src_key( mb )) == NULL ) {
        return (uintptr_t)errlog_null( ERR_CRITICAL, "mailbox_get_mvpoll_src_key\n" );
    }

    if (( src = jvector_source_create( _this, key )) == NULL ) {
        return (uintptr_t)errlog_null( ERR_CRITICAL, "jvector_source_create\n" );
    }
    
    return (uintptr_t)src;    
}

JNIEXPORT jint JNICALL Java_com_untangle_jvector_UDPSource_shutdown
(JNIEnv *env, jclass _this, jlong pointer, jlong mailbox_pointer )
{
    jvector_source_t* jv_src = (jvector_source_t*)(uintptr_t)pointer;
    
    if ( jv_src == NULL || jv_src->key == NULL ) return errlogargs();
    
    /* Nothing to do here */
    return 0;
}
