/*
 * Copyright (c) 2003 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 *  $Id: Shield.c,v 1.1 2005/01/31 01:15:12 rbscott Exp $
 */

#include <jni.h>

#include <libnetcap.h>
#include <mvutil/libmvutil.h>
#include <mvutil/errlog.h>
#include <mvutil/debug.h>

#include "jnetcap.h"
#include JH_Shield

#include "jerror.h"

/*
 * Class:     com_metavize_jnetcap_Shield
 * Method:    config
 * Signature: (Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL JF_Shield( config )
    ( JNIEnv* env, jobject _this, jstring file_name )
{
    const char* file_str;
    
    if (( file_str = (*env)->GetStringUTFChars( env, file_name, NULL )) == NULL ) {
        return jnetcap_error_void( JNETCAP_ERROR_STT, ERR_CRITICAL, "(*env)->GetStringUTFChars\n" );
    };

    debug( 5, "JNETCAP: Loading shield configuration file: %s\n", file_str );
    
    do {
        struct stat file_stat;

        if ( stat( file_str, &file_stat ) < 0 ) {
            jnetcap_error( JNETCAP_ERROR_STT, ERR_CRITICAL, "stat: %s", errstr );
            break;
        }

        if ( access( file_str, R_OK ) == 0 && S_ISREG( file_stat.st_mode )) {
            int fd;
            char buf[4096];
            int msg_len;

            /* Open and read the configuration file */
            if (( fd = open( file_str, O_RDONLY )) < 0 ) {
                jnetcap_error( JNETCAP_ERROR_STT, ERR_CRITICAL, "open: %s", errstr );
                break;
            }
            
            if (( msg_len = read( fd, buf, sizeof( buf ))) < 0 ) {
                jnetcap_error( JNETCAP_ERROR_STT, ERR_CRITICAL, "read: %s", errstr );                
                if ( close( fd ) < 0 )
                    perrlog( "close" );
                break;
            }
            
            /* Don't stop if there is an error, the data has already been read */
            if ( close( fd ) < 0 ) 
                perrlog( "close" );
            
            fd = -1;
            
            if ( msg_len == sizeof ( buf )) {
                jnetcap_error( JNETCAP_ERROR_STT, ERR_CRITICAL, "Invalid shield configuration(size>=%d)\n", 
                               sizeof ( buf ));
                continue;
            }
                        
            /* Load the shield configuration */
            if ( msg_len != 0 && netcap_shield_cfg_load ( buf, msg_len ) < 0 ) {
                jnetcap_error( JNETCAP_ERROR_STT, ERR_CRITICAL, "netcap_shield_load_configuration\n" );
                break;
            }
            
            debug( 5, "JNETCAP: Successfully loaded shield configuration\n" );
        } else {
            jnetcap_error( JNETCAP_ERROR_ARGS, ERR_CRITICAL, "Unable to access file: '%s'", file_str );
        }
    } while ( 0 );

    (*env)->ReleaseStringUTFChars( env, file_name, file_str );
}

/*
 * Class:     com_metavize_jnetcap_Shield
 * Method:    addChunk
 * Signature: (JII)V
 */
JNIEXPORT void JNICALL JF_Shield( addChunk )
  (JNIEnv *env, jobject _this, jlong ip, jshort protocol, jint num_bytes )
{
    /* Could throw an error, but shield errors are currently ignored. */
    if ( netcap_shield_rep_add_chunk((in_addr_t)ip, protocol, (u_short)num_bytes ) < 0 )
        errlog( ERR_WARNING, "netcap_shield_rep_add_chunk\n" );  
}
