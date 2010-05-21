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
#include <libmvutil.h>
#include <libvector.h>
#include <mvutil/debug.h>
#include <mvutil/mvpoll.h>
#include <mvutil/errlog.h>
#include <mvutil/debug.h>
#include <jmvutil.h>

#include <vector/event.h>
#include <vector/source.h>

#include "jvector.h"

#include "com_untangle_jvector_Sink.h"

JNIEXPORT void JNICALL Java_com_untangle_jvector_Sink_raze
    ( JNIEnv *env, jobject _this, jlong pointer )
{
    jvector_sink_t* jv_snk = (jvector_sink_t*)(uintptr_t)pointer;
    
    if ( jv_snk == NULL ) {
        errlogargs();
        return;
    }

    if ( jv_snk->key != NULL ) 
        mvpoll_key_raze( jv_snk->key );

    jv_snk->key = NULL;

    if ( jv_snk->this != NULL )
        (*env)->DeleteGlobalRef( env, jv_snk->this );

    jv_snk->this = NULL;

    free( jv_snk );
}

