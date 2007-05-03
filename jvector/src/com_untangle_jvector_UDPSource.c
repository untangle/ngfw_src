/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
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
JNIEXPORT jint JNICALL Java_com_untangle_jvector_UDPSource_create
  (JNIEnv *env, jobject _this, jint pointer )
{
    jvector_source_t* src;
    mvpoll_key_t* key;
    mailbox_t* mb;
    
    if (( mb = (mailbox_t*)pointer) == NULL ) return (jint)errlogargs_null();
        
    if (( key = mailbox_get_mvpoll_src_key( mb )) == NULL ) {
        return (jint)errlog_null( ERR_CRITICAL, "mailbox_get_mvpoll_src_key\n" );
    }

    if (( src = jvector_source_create( _this, key )) == NULL ) {
        return (jint)errlog_null( ERR_CRITICAL, "jvector_source_create\n" );
    }
    
    return (jint)src;    
}

JNIEXPORT jint JNICALL Java_com_untangle_jvector_UDPSource_shutdown
(JNIEnv *env, jclass _this, jint pointer, jint mailbox_pointer )
{
    jvector_source_t* jv_src = (jvector_source_t*)pointer;
    
    if ( jv_src == NULL || jv_src->key == NULL ) return errlogargs();
    
    /* Nothing to do here */
    return 0;
}
