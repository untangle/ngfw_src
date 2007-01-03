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

#include <stdlib.h>
#include <stdio.h>
#include <libxml/xmlmemory.h>
#include <libxml/parser.h>

#include <mvutil/debug.h>
#include <mvutil/errlog.h>
#include <mvutil/list.h>
#include <mvutil/utime.h>

#include "netcap_shield.h"
#include "netcap_shield_cfg.h"

/* Shield configuration defaults */
/* Need to tune these values appropriately */

/* XXX What are reasonable defaults */
/* Default limits: lax, tight, closed */
/* XXX CPU Loads are really high (ignored) until we have a way getting a load average with 
 * a shorter interval */
#define _CPU_LOAD_LIMITS        40.0, 60.0, 80.0
#define _ACTIVE_SESSION_LIMITS  512, 1024, 1536
#define _REQUEST_LOAD_LIMITS    60, 75, 85
#define _SESSION_LOAD_LIMITS    50, 60, 75
#define _TCP_CHK_LOAD_LIMITS    5000, 10000, 14000
#define _UDP_CHK_LOAD_LIMITS    3000, 6000, 10000
#define _ICMP_CHK_LOAD_LIMITS   3000, 6000, 10000
#define _EVIL_LOAD_LIMITS       800, 1600, 2000

/* Use a scale from 0 to 100 */
#define _SHIELD_REP_MAX 100

#define _REQUEST_LOAD_MULT     (((_SHIELD_REP_MAX+0.0)/30)  * (.3))
#define _SESSION_LOAD_MULT     (((_SHIELD_REP_MAX+0.0)/30)  * (.2))
#define _TCP_CHK_LOAD_MULT     (((_SHIELD_REP_MAX+0.0)/100) * (.4))
#define _UDP_CHK_LOAD_MULT     (((_SHIELD_REP_MAX+0.0)/100) * (.4))
#define _EVIL_LOAD_MULT        (((_SHIELD_REP_MAX+0.0)/75)  * (.1))
#define _ACT_SESSION_MULT      (((_SHIELD_REP_MAX+0.0)/100) * (.1))

/* This is not actually a multiplier, it is just a rate limit in seconds per IP */
#define _ICMP_CHK_LOAD_MULT    40.0

/* For debugging, if the shield exceeds this threshold a lot of
 * information is printed about the reputation being analyzed */
#define _REPUTATION_DEBUG_THRESHOLD 160.0

static int _verifyCfg            ( nc_shield_cfg_t* cfg );
static int _verifyFence          ( nc_shield_fence_t* fence );
static int _parseCfg             ( nc_shield_cfg_t* cfg, xmlDoc* doc, xmlNode* xml_cfg );
static int _parseLimit           ( nc_shield_limit_t* limit, xmlDoc* doc, xmlNode* shield_cfg );
static int _parseMult            ( nc_shield_cfg_t* cfg, xmlDoc* doc, xmlNode* shield_cfg );
static int _parseLru             ( nc_shield_cfg_t* cfg, xmlDoc* doc, xmlNode* lruNode );
static int _parseFences          ( nc_shield_cfg_t* cfg, xmlDoc* doc, xmlNode* fencesNode );
static int _parseFence           ( nc_shield_fence_t* fence, xmlDoc* doc, xmlNode* fenceNode );
static int _parseFencePost       ( nc_shield_post_t* fence, xmlDoc* doc, xmlNode* fenceNode );
static int _parsePrint           ( nc_shield_cfg_t* cfg, xmlDoc* doc, xmlNode* printNode );

/* This verifies that the double is positive */
static int _parseAttributeDouble  ( double* val, xmlDoc* doc, xmlNode* node, char* name );
static int _parseAttributePercent ( double* percent, xmlDoc* doc, xmlNode* node, char* name );
static int _parseAttributeInt     ( int* val, xmlDoc* doc, xmlNode* node, char* name );

static int _update_buf ( int count, char **buf, size_t* buf_len )
{
    if ( count > *buf_len ) return errlog ( ERR_CRITICAL, "Shield: Buffer is too short to get config\n" );
    
    *buf_len -= count;
    *buf     += count;
    
    return 0;
}

static int _str_append ( const char* val, char** buf, size_t* buf_len )
{
    int count;

    if (( count = snprintf ( *buf, *buf_len, "%s", val )) < 0 ) return perrlog ( "snprintf" );

    return _update_buf ( count, buf, buf_len );
}

static int _limit_get ( const char* name, nc_shield_limit_t* limit, char** buf, size_t* buf_len )
{
    int count;
    if (( count = snprintf ( *buf, *buf_len, "<%s lax='%lg' tight='%lg' closed='%lg'/>\n",
                             name, limit->lax, limit->tight, limit->closed )) < 0 ) {
        return perrlog ( "snprintf" );
    }
    return _update_buf ( count, buf, buf_len );
}

static int _fence_get ( const char* name, nc_shield_fence_t* fence, char** buf, size_t* buf_len )
{
    int count;
    if (( count = snprintf ( *buf, *buf_len, 
                             "<%s>\n<limited prob='%lg' post='%lg'/>\n"
                             "<closed prob='%lg' post='%lg'/>\n</%s>\n",
                             name, fence->limited.prob, fence->limited.post, 
                             fence->closed.prob, fence->closed.post, name )) < 0 ) {
        return perrlog ( "snprintf" );
    }
    
    return _update_buf ( count, buf, buf_len );
}

int nc_shield_cfg_def  ( nc_shield_cfg_t* cfg )
{
    static nc_shield_cfg_t default_cfg = { /* Shield default configuration */
        .limit = {
            .cpu_load      = { _CPU_LOAD_LIMITS }, 
            .sessions      = { _ACTIVE_SESSION_LIMITS }, 
            .request_load  = { _REQUEST_LOAD_LIMITS }, 
            .session_load  = { _SESSION_LOAD_LIMITS }, 
            .tcp_chk_load  = { _TCP_CHK_LOAD_LIMITS }, 
            .udp_chk_load  = { _UDP_CHK_LOAD_LIMITS }, 
            .icmp_chk_load = { _ICMP_CHK_LOAD_LIMITS }, 
            .evil_load     = { _EVIL_LOAD_LIMITS }
        },
        .mult = {
            .request_load  = _REQUEST_LOAD_MULT,
            .session_load  = _SESSION_LOAD_MULT,
            .tcp_chk_load  = _TCP_CHK_LOAD_MULT,
            .udp_chk_load  = _UDP_CHK_LOAD_MULT,
            .icmp_chk_load = _ICMP_CHK_LOAD_MULT,
            .evil_load     = _EVIL_LOAD_MULT,
            .active_sess   = _ACT_SESSION_MULT
        },
        .lru = {
            .low_water    = 512,
            .high_water   = 1024,
            .sieve_size   = 8,
            .ip_rate      = .016, /* 1/60  */
        },
        .fence = {
            .relaxed = {
                .inheritance = .1,
                .limited = { .prob = 0.70, .post = _SHIELD_REP_MAX * 0.65 },
                .closed  = { .prob = 0.85, .post = _SHIELD_REP_MAX * 0.90 },
                .error   = { .prob = 0.95, .post = _SHIELD_REP_MAX * 1.00 }
            },
            .lax = {
                .inheritance = .4,
                .limited = { .prob = 0.75, .post = _SHIELD_REP_MAX * 0.50 },
                .closed  = { .prob = 0.80, .post = _SHIELD_REP_MAX * 0.80 },
                .error   = { .prob = 0.95, .post = _SHIELD_REP_MAX * 1.00 }
            },
            .tight = {
                .inheritance = .6,
                .limited = { .prob = 0.70, .post = _SHIELD_REP_MAX * 0.15 },
                .closed  = { .prob = 0.90, .post = _SHIELD_REP_MAX * 0.60 },
                .error   = { .prob = 0.95, .post = _SHIELD_REP_MAX * 0.70 }
            }, 
            .closed = {
                .inheritance = .9,
                .limited = { .prob = 0.90, .post = _SHIELD_REP_MAX * 0.05 },
                .closed  = { .prob = 0.95, .post = _SHIELD_REP_MAX * 0.20 },
                .error   = { .prob = 0.95, .post = _SHIELD_REP_MAX * 0.40 }
            }
        },
        .print_rate   = 0.25,
        .rep_threshold = _REPUTATION_DEBUG_THRESHOLD
    };
    
    if ( cfg == NULL ) return errlogargs();

    memcpy ( cfg, &default_cfg, sizeof ( nc_shield_cfg_t ));

    return 0;
}


int nc_shield_cfg_load ( nc_shield_cfg_t* cfg, char* buf, int buf_len )
{
    xmlDoc*  doc = NULL;
    xmlNode* cur = NULL;
    int ret = 0;
    char xmlstrbuf[buf_len+1];
    nc_shield_cfg_t new_cfg;

    if ( cfg == NULL || buf == NULL || buf_len < 0 ) return errlogargs();

    memcpy( &new_cfg, cfg, sizeof ( nc_shield_cfg_t ));
    memcpy( xmlstrbuf, buf, buf_len );
    
    xmlstrbuf[ buf_len ] = 0x00;
    
    if (( doc = xmlParseMemory( xmlstrbuf, buf_len )) == NULL ) {
        return errlog ( ERR_WARNING, "Shield: Document parsing failed\n" );
    }

    do {
        if (( cur = xmlDocGetRootElement ( doc )) == NULL ) {
            ret = errlog ( ERR_WARNING, "Shield: Empty document\n" );
            break;
        }
    
        if ( xmlStrcmp ( cur->name, (const xmlChar*)"shield-config" )) {
            ret = errlog ( ERR_WARNING, "Shield: Document of the wrong type, '%s'\n'", cur->name );
            break;
        }
        
        ret = _parseCfg ( &new_cfg, doc, cur );
    } while ( 0 );

    if ( doc != NULL ) xmlFreeDoc ( doc );

    if ( ret == 0 ) {
        /* Verify the config is okay */
        if ( _verifyCfg ( &new_cfg ) < 0 ) ret = -1;
        
        memcpy( cfg, &new_cfg, sizeof ( nc_shield_cfg_t ));
        debug ( 10, "NETCAP: Shield: Sucessfully loaded new configuration\n" );
    } else {
        errlog ( ERR_WARNING, "Shield: Unable to load configuration\n" );
    }

    return ret;
}

int nc_shield_cfg_get ( nc_shield_cfg_t* cfg, char* buf, int buf_len )
{
    int count;
    size_t buf_rem = buf_len;

    if ( buf == NULL || buf_len < 0 ) return errlogargs();
    
    if (( _str_append ( "<shield-cfg>\n", &buf, &buf_rem ) < 0 ) ||
        ( _limit_get ( "cpu-limit",     &cfg->limit.cpu_load,      &buf, &buf_rem ) < 0 ) ||
        ( _limit_get ( "sess-limit",    &cfg->limit.sessions,      &buf, &buf_rem ) < 0 ) ||
        ( _limit_get ( "request-load",  &cfg->limit.request_load,  &buf, &buf_rem ) < 0 ) ||
        ( _limit_get ( "session-load",  &cfg->limit.session_load,  &buf, &buf_rem ) < 0 ) ||
        ( _limit_get ( "tcp-chk-load",  &cfg->limit.tcp_chk_load,  &buf, &buf_rem ) < 0 ) ||
        ( _limit_get ( "udp-chk-load",  &cfg->limit.udp_chk_load,  &buf, &buf_rem ) < 0 ) ||
        ( _limit_get ( "icmp-chk-load", &cfg->limit.icmp_chk_load, &buf, &buf_rem ) < 0 ) ||
        ( _limit_get ( "evil-load",     &cfg->limit.evil_load,     &buf, &buf_rem ) < 0 ) ||
        ( _str_append ( "<fence>\n", &buf, &buf_rem ) < 0 ) ||
        ( _fence_get ( "relaxed", &cfg->fence.relaxed, &buf, &buf_rem ) < 0 ) ||
        ( _fence_get ( "lax",     &cfg->fence.lax,     &buf, &buf_rem ) < 0 ) ||
        ( _fence_get ( "tight",   &cfg->fence.tight,   &buf, &buf_rem ) < 0 ) ||
        ( _fence_get ( "closed",  &cfg->fence.closed,  &buf, &buf_rem ) < 0 ) ||
        ( _str_append ( "</fence>\n", &buf, &buf_rem ) < 0 ))
    {
        return errlog ( ERR_CRITICAL, "Shield: String manipulation\n" );
    }

    count = snprintf ( buf, buf_len, 
                       "<mult request-load='%lg' session-load='%lg' tcp-chk-load='%lg' udp-chk-load='%lg' "
                       " icmp-chk-load='%lg' evil-load='%lg' active-sess='%lg'/>\n<lru  low-water='%d' "
                       " high-water='%d'/>\n"
                       "<print rate='%lg' reputation-threshold='%lg'/>\n"
                       "</shield-cfg>\n",
                       cfg->mult.request_load, cfg->mult.session_load, cfg->mult.tcp_chk_load, 
                       cfg->mult.udp_chk_load, cfg->mult.icmp_chk_load, cfg->mult.evil_load, 
                       cfg->mult.active_sess, cfg->lru.low_water, cfg->lru.high_water,
                       cfg->print_rate, cfg->rep_threshold );                       

    if ( count < 0 ) return perrlog ( "snprintf" );

    if ( count > buf_len ) return errlog ( ERR_CRITICAL, "Shield: Buffer is too short to get config\n" );

    buf += count;
    buf_rem -= count;

    /* Return the length of the configuration */
    return buf_len - buf_rem;
}

static int _verifyCfg            ( nc_shield_cfg_t* cfg )
{
    if ( cfg->lru.low_water > cfg->lru.high_water ) {
        return errlog ( ERR_WARNING, "Shield: low water (%d) < high water (%d)\n", 
                        cfg->lru.low_water, cfg->lru.high_water );
    }

    if (( _verifyFence ( &cfg->fence.relaxed ) < 0 ) ||
        ( _verifyFence ( &cfg->fence.lax     ) < 0 ) ||
        ( _verifyFence ( &cfg->fence.tight   ) < 0 ) ||
        ( _verifyFence ( &cfg->fence.closed  ) < 0 )) {
        return errlog ( ERR_WARNING, "Shield: Invalid fence configuration\n" );
    }
     
    return 0;
}

static int _verifyFence          ( nc_shield_fence_t* fence )
{
    if ( fence->limited.prob < 0 || fence->limited.prob > 1 ) {
        return errlog ( ERR_WARNING, "Shield: Fence limited probability must be between 0 and 1\n" );
    }

    if ( fence->closed.prob < 0 || fence->closed.prob > 1 ) {
        return errlog ( ERR_WARNING, "Shield: Fence closed probability must be between 0 and 1\n" );
    }

    if ( fence->closed.post < fence->limited.post ) {
        return errlog ( ERR_WARNING, "Shield: Fence closed post must be greater than limited post\n" );
    }
   
    return 0;
}


static int _parseCfg             ( nc_shield_cfg_t* cfg, xmlDoc* doc, xmlNode* xml_cfg )
{
    xmlNode* cur = NULL;
    
    for ( cur = xml_cfg->xmlChildrenNode ; cur != NULL ; cur = cur->next ) {
        if ((!xmlStrcmp(cur->name, (xmlChar*)"text"))) {
            continue;
        } else if ((!xmlStrcmp(cur->name, (xmlChar*)"comment"))) {
            continue;
        } if (( !xmlStrcmp ( cur->name, (xmlChar*)"cpu-limit" ))) {
            if ( _parseLimit ( &cfg->limit.cpu_load, doc, cur ) < 0 ) return -1;
        } else if (( !xmlStrcmp ( cur->name, (xmlChar*)"sess-limit" ))) {
            if ( _parseLimit ( &cfg->limit.sessions, doc, cur ) < 0 ) return -1;
        } else if (( !xmlStrcmp ( cur->name, (xmlChar*)"request-load" ))) {
            if ( _parseLimit ( &cfg->limit.request_load, doc, cur ) < 0 ) return -1;
        } else if (( !xmlStrcmp ( cur->name, (xmlChar*)"session-load" ))) {
            if ( _parseLimit ( &cfg->limit.session_load, doc, cur ) < 0 ) return -1;
        } else if (( !xmlStrcmp ( cur->name, (xmlChar*)"tcp-chk-load" ))) {
            if ( _parseLimit ( &cfg->limit.tcp_chk_load, doc, cur ) < 0 ) return -1;
        } else if (( !xmlStrcmp ( cur->name, (xmlChar*)"udp-chk-load" ))) {
            if ( _parseLimit ( &cfg->limit.udp_chk_load, doc, cur ) < 0 ) return -1;
        } else if (( !xmlStrcmp ( cur->name, (xmlChar*)"icmp-chk-load" ))) {
            if ( _parseLimit ( &cfg->limit.icmp_chk_load, doc, cur ) < 0 ) return -1;
        } else if (( !xmlStrcmp ( cur->name, (xmlChar*)"evil-load" ))) {
            if ( _parseLimit ( &cfg->limit.evil_load, doc, cur ) < 0 ) return -1;
        } else if (( !xmlStrcmp ( cur->name, (xmlChar*)"mult" ))) {
            if ( _parseMult ( cfg, doc, cur ) < 0 ) return -1;
        } else if (( !xmlStrcmp ( cur->name, (xmlChar*)"lru" ))) {
            if ( _parseLru ( cfg, doc, cur ) < 0 ) return -1;
        } else if (( !xmlStrcmp ( cur->name, (xmlChar*)"fence" ))) {
            if ( _parseFences ( cfg, doc, cur ) < 0 ) return -1;
        } else if (( !xmlStrcmp ( cur->name, (xmlChar*)"print" ))) {
            if ( _parsePrint ( cfg, doc, cur ) < 0 ) return -1;
        } else if (( !xmlStrcmp ( cur->name, (xmlChar*)"udp-multiplier" ))) {
            errlog ( ERR_WARNING, "Shield: UDP multiplier is no longer supported\n" );
        } else {
            errlog ( ERR_WARNING, "Shield: Unknown configuration type: '%s'\n", cur->name );
        }
    }

    return 0;
}

static int _parseLimit           ( nc_shield_limit_t* limit, xmlDoc* doc, xmlNode* limitNode )
{
    if ( _parseAttributeDouble ( &limit->lax,    doc, limitNode, "lax" ) < 0 ) return -1;
    if ( _parseAttributeDouble ( &limit->tight,  doc, limitNode, "tight" ) < 0 ) return -1;
    if ( _parseAttributeDouble ( &limit->closed, doc, limitNode, "closed" ) < 0 ) return -1;
    return 0;
}

static int _parseMult            ( nc_shield_cfg_t* cfg, xmlDoc* doc, xmlNode* multNode )
{
    if ( _parseAttributeDouble ( &cfg->mult.request_load,  doc, multNode, "request-load" ) < 0 ) return -1;
    if ( _parseAttributeDouble ( &cfg->mult.session_load,  doc, multNode, "session-load" ) < 0 ) return -1;
    if ( _parseAttributeDouble ( &cfg->mult.tcp_chk_load,  doc, multNode, "tcp-chk-load" ) < 0 ) return -1;
    if ( _parseAttributeDouble ( &cfg->mult.udp_chk_load,  doc, multNode, "udp-chk-load" ) < 0 ) return -1;
    if ( _parseAttributeDouble ( &cfg->mult.icmp_chk_load, doc, multNode, "icmp-chk-load" ) < 0 ) return -1;
    if ( _parseAttributeDouble ( &cfg->mult.evil_load,     doc, multNode, "evil-load" ) < 0 ) return -1;
    if ( _parseAttributeDouble ( &cfg->mult.active_sess,   doc, multNode, "active-sess" ) < 0 ) return -1;

    return 0;
}

static int _parseLru             ( nc_shield_cfg_t* cfg, xmlDoc* doc, xmlNode* lruNode )
{
    if ( _parseAttributeInt ( &cfg->lru.low_water, doc, lruNode, "low-water" ) < 0 ) return -1;
    if ( _parseAttributeInt ( &cfg->lru.high_water, doc, lruNode, "high-water" ) < 0 ) return -1;
    if ( _parseAttributeInt ( &cfg->lru.sieve_size, doc, lruNode, "sieve-size" ) < 0 ) return -1;
    if ( _parseAttributeDouble ( &cfg->lru.ip_rate, doc, lruNode, "ip-rate" ) < 0 ) return -1;
    
    return 0;
}

static int _parseFences          ( nc_shield_cfg_t* cfg, xmlDoc* doc, xmlNode* fencesNode )
{
    xmlNode* cur = NULL;
    
    for ( cur = fencesNode->xmlChildrenNode ; cur != NULL ; cur = cur->next ) {
        if ((!xmlStrcmp(cur->name, (xmlChar*)"text"))) {
            continue;
        } else if ((!xmlStrcmp(cur->name, (xmlChar*)"comment"))) {
            continue;
        } else if (( !xmlStrcmp ( cur->name, (xmlChar*)"relaxed" ))) {
            if ( _parseFence ( &cfg->fence.relaxed, doc, cur ) < 0 ) return -1;
        } else if (( !xmlStrcmp ( cur->name, (xmlChar*)"lax" ))) {
            if ( _parseFence ( &cfg->fence.lax, doc, cur ) < 0 ) return -1;
        } else if (( !xmlStrcmp ( cur->name, (xmlChar*)"tight" ))) {
            if ( _parseFence ( &cfg->fence.tight, doc, cur ) < 0 ) return -1;
        } else if (( !xmlStrcmp ( cur->name, (xmlChar*)"closed" ))) {
            if ( _parseFence ( &cfg->fence.closed, doc, cur ) < 0 ) return -1;
        } else {
            errlog ( ERR_WARNING, "Shield: Unknown fence type: '%s'\n", cur->name );
        }
    }

    return 0;
}

static int _parseFence           ( nc_shield_fence_t* fence, xmlDoc* doc, xmlNode* fenceNode )
{
    xmlNode* cur = NULL;
    
    if ( _parseAttributeDouble ( &fence->inheritance, doc, fenceNode, "inheritance" ) < 0 ) return -1;
    
    for ( cur = fenceNode->xmlChildrenNode ; cur != NULL ; cur = cur->next ) {
        if ((!xmlStrcmp(cur->name, (xmlChar*)"text"))) {
            continue;
        } else if ((!xmlStrcmp(cur->name, (xmlChar*)"comment"))) {
            continue;
        } else if (( !xmlStrcmp ( cur->name, (xmlChar*)"limited" ))) {
            if ( _parseFencePost ( &fence->limited, doc, cur ) < 0 ) return -1;
        } else if (( !xmlStrcmp ( cur->name, (xmlChar*)"closed" ))) {
            if ( _parseFencePost ( &fence->closed, doc, cur ) < 0 ) return -1;
        } else if (( !xmlStrcmp ( cur->name, (xmlChar*)"error" ))) {
            if ( _parseFencePost ( &fence->error, doc, cur ) < 0 ) return -1;
        } else {
            errlog ( ERR_WARNING, "Shield: Unknown fence post type: '%s'\n", cur->name );
        }
    }

    return 0;
}

static int _parseFencePost       ( nc_shield_post_t* post, xmlDoc* doc, xmlNode* postNode )
{
    if ( _parseAttributeDouble ( &post->post, doc, postNode, "post" ) < 0 ) return -1;
    if ( _parseAttributePercent ( &post->prob, doc, postNode, "prob" ) < 0 ) return -1;
   
    return 0;
}

static int _parsePrint           ( nc_shield_cfg_t* cfg, xmlDoc* doc, xmlNode* printNode )
{
    if ( _parseAttributeDouble( &cfg->print_rate, doc, printNode, "rate" ) < 0 ) return -1;
    if ( _parseAttributeDouble( &cfg->rep_threshold, doc, printNode, "reputation-threshold" ) < 0 ) return -1;

    /* Calculate the print delay, this is the inverse of the print rate multiplied by 100,000 */
    if ( cfg->print_rate != 0 ) {
        cfg->print_delay = (((double)U_SEC) / cfg->print_rate );
    } else {
        errlog ( ERR_WARNING, "Shield: zero print_rate.\n" );
        cfg->print_delay = U_SEC;
    }

    return 0;
}


static int _parseAttributeDouble ( double* val, xmlDoc* doc, xmlNode* node, char* name )
{
    char *endptr;
    xmlChar* xret = xmlGetProp( node, (xmlChar*)name );
    int ret = 0;
    
    /* A missing attribute is not a problem */
    if ( xret == NULL ) {
        debug ( 10, "Shield: Configuration is missing attribute '%s'\n", name );
        return 0;
    }
    
    *val = strtod ( (char*) xret, &endptr );
    
    if ( endptr == NULL || endptr[0] != '\0' ) {
        ret = errlog ( ERR_WARNING, "Shield: Unable to parse attribute: '%s', value '%s'\n", name, xret );
    }

    if ( *val < 0 ) {
        ret = errlog ( ERR_WARNING, "Shield: Negative double attribute: '%s', value '%lg'\n", name, *val );
    }
    
    xmlFree( xret );

    return ret;

}

static int _parseAttributePercent ( double* percent, xmlDoc* doc, xmlNode* node, char* name )
{
    if ( _parseAttributeDouble ( percent, doc, node, name ) < 0 ) return -1;

    if ( *percent < 0 || *percent > 100 ) {
        return errlog ( ERR_WARNING, "Shield: Invalid percentage: '%lg'\n", *percent );
    }
    
    *percent = *percent / 100;
    
    return 0;
}

static int _parseAttributeInt    ( int* val, xmlDoc* doc, xmlNode* node, char* name )
{
    char *endptr;
    xmlChar* xret = xmlGetProp( node, (xmlChar*)name );
    int ret = 0;
    
    /* A missing attribute is not a problem */
    if ( xret == NULL ) {
        debug ( 10, "Shield: Configuration is missing attribute '%s'\n", name );
        return 0;
    }
    
    *val = (int)strtol ( (char*) xret, &endptr, 10 );
    
    if ( endptr == NULL || endptr[0] != '\0' ) {
        ret = errlog ( ERR_WARNING, "Shield: Unable to parse attribute: '%s', value '%s'\n", name, xret );
    }

    if ( *val < 0 ) {
        ret = errlog ( ERR_WARNING, "Shield: Negative int attribute: '%s', value '%d'\n", name, *val );
    }

    xmlFree( xret );

    return ret;

}

