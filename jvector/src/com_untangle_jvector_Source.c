/**
 * $Id: com_untangle_jvector_Source.c 35567 2013-08-08 07:47:12Z dmorris $
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

#include "com_untangle_jvector_Source.h"

JNIEXPORT void JNICALL Java_com_untangle_jvector_Source_raze
    ( JNIEnv *env, jobject _this, jlong pointer )
{
    jvector_source_t* jv_src = (jvector_source_t*)(uintptr_t)pointer;
    
    if ( jv_src == NULL ) {
        errlogargs();
        return;
    }

    if ( jv_src->key != NULL ) 
        mvpoll_key_raze( jv_src->key );

    jv_src->key = NULL;

    if ( jv_src->this != NULL )
        (*env)->DeleteGlobalRef( env, jv_src->this );

    jv_src->this = NULL;

    free( jv_src );
}

