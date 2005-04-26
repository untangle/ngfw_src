/*
 * Copyright (c) 2003 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 *  $Id$
 */

#include <jni.h>

#include <stdio.h>
#include <sys/socket.h>
#include <netinet/in.h>

#include <libnetcap.h>
#include <mvutil/errlog.h>
#include <mvutil/debug.h>

#include "jnetcap.h"

#include JH_InterfaceSet

/*
 * Class:     com_metavize_jnetcap_InterfaceSet
 * Method:    clear
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL JF_InterfaceSet( clear )
  (JNIEnv *env, jclass _this )
{
    netcap_intfset_t tmp;

    if ( netcap_intfset_clear( &tmp ) < 0 ) return errlog( ERR_CRITICAL, "netcap_intfset_clear\n" );

    return (int)tmp;
}

/*
 * Class:     com_metavize_jnetcap_InterfaceSet
 * Method:    add
 * Signature: (II)I
 */
JNIEXPORT jint JNICALL JF_InterfaceSet( add )
  (JNIEnv *env, jclass _this, jint data, jstring interface_name )
{
    const jbyte *str;
    netcap_intf_t intf = -1;
    int ret = 0;
    
    if (( str = (*env)->GetStringUTFChars( env, interface_name, NULL )) == NULL ) {
        return errlogmalloc();
    };
    
    do {
        if ( netcap_interface_string_to_intf ((char*)str, &intf ) < 0 ) {
            ret = errlog ( ERR_CRITICAL, "netcap_interface_string_to_intf\n" );
        }
    } while ( 0 );
    
    (*env)->ReleaseStringUTFChars( env, interface_name, str );

    if ( ret < 0 ) return ret;
    
    if ( netcap_intfset_add (( netcap_intfset_t*)&data, intf ) < 0 ) {
        return errlog ( ERR_CRITICAL, "netcap_intfset_add\n" );
    }

    return data;
}
