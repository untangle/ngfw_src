/**
 * $Id: com_untangle_jvector_Sink.c 35567 2013-08-08 07:47:12Z dmorris $
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

