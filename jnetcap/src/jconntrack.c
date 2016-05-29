/*
 * $Id: jconntrack.c,v 1.00 2016/05/27 10:00:36 dmorris Exp $
 */

#include <stdio.h>
#include <stdlib.h>
#include <pthread.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <signal.h>

#include <libnetcap.h>
#include <libmvutil.h>
#include <mvutil/errlog.h>
#include <mvutil/debug.h>
#include <mvutil/uthread.h>
#include <mvutil/unet.h>
#include <jmvutil.h>

#include <jni.h>

#include "jnetcap.h"
#include "jconntrack.h"

#include JH_Conntrack

#define GET_CT_ITEM(elem, attr, x)                              \
        do { n->elem = nfct_get_attr_u##x(ct,(attr)); } while (0)

/*
 * Class:     com_untangle_jnetcap_Conntrack
 * Method:    getLongValue
 * Signature: (IJ)J
 */
JNIEXPORT jlong JNICALL JF_Conntrack( getLongValue )
  (JNIEnv *env, jclass _this, jint flag, jlong conntrack_ptr )
{
    struct nf_conntrack* conntrack;
    JLONG_TO_CONNTRACK( conntrack, conntrack_ptr );

    return nfct_get_attr_u64(conntrack,flag);
}

/*
 * Class:     com_untangle_jnetcap_Conntrack
 * Method:    getIntValue
 * Signature: (IJ)I
 */
JNIEXPORT jint JNICALL JF_Conntrack( getIntValue )
  (JNIEnv *env, jclass _this, jint flag, jlong conntrack_ptr )
{
    struct nf_conntrack* conntrack;
    JLONG_TO_CONNTRACK( conntrack, conntrack_ptr );

    return nfct_get_attr_u32(conntrack, flag);
}

/*
 * Class:     com_untangle_jnetcap_Conntrack
 * Method:    getShortValue
 * Signature: (IJ)S
 */
JNIEXPORT jshort JNICALL JF_Conntrack( getShortValue )
  (JNIEnv *env, jclass _this, jint flag, jlong conntrack_ptr )
{
    struct nf_conntrack* conntrack;
    JLONG_TO_CONNTRACK( conntrack, conntrack_ptr );

    return nfct_get_attr_u16(conntrack, flag);
}

/*
 * Class:     com_untangle_jnetcap_Conntrack
 * Method:    getByteValue
 * Signature: (IJ)B
 */
JNIEXPORT jbyte JNICALL JF_Conntrack( getByteValue )
  (JNIEnv *env, jclass _this, jint flag, jlong conntrack_ptr )
{
    struct nf_conntrack* conntrack;
    JLONG_TO_CONNTRACK( conntrack, conntrack_ptr );

    return nfct_get_attr_u8(conntrack, flag);
}

/*
 * Class:     com_untangle_jnetcap_Conntrack
 * Method:    getShortValueAsIntReversedByteOrder
 * Signature: (IJ)I
 */
JNIEXPORT jint JNICALL JF_Conntrack( getShortValueAsIntReversedByteOrder )
  (JNIEnv *env, jclass _this, jint flag, jlong conntrack_ptr )
{
    struct nf_conntrack* conntrack;
    JLONG_TO_CONNTRACK( conntrack, conntrack_ptr );

    return (jint)ntohs(nfct_get_attr_u16(conntrack, flag));
}

/*
 * Class:     com_untangle_jnetcap_Conntrack
 * Method:    toString
 * Signature: (J)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL JF_Conntrack( toString )
  (JNIEnv *env, jclass _this, jlong conntrack_ptr )
{
    struct nf_conntrack* conntrack;
    JLONG_TO_CONNTRACK_NULL( conntrack, conntrack_ptr );

    char buf[1024];
    bzero(buf, sizeof(buf));
    nfct_snprintf(buf, sizeof(buf), conntrack, NFCT_T_UNKNOWN, NFCT_O_DEFAULT, NFCT_OF_SHOW_LAYER3 | NFCT_OF_TIMESTAMP);

    return (*env)->NewStringUTF( env, buf );
}
