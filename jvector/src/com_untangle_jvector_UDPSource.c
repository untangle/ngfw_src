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
