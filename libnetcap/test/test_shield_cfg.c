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

#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <arpa/inet.h>
#include <libxml/xmlmemory.h>
#include <libxml/parser.h>

#include <mvutil/libmvutil.h>
#include <mvutil/errlog.h>
#include <mvutil/debug.h>

#include "netcap_shield_cfg.h"
#include "netcap_interface.h"

/* This is needed by netcap_interface */
netcap_intf_t DEV_INSIDE,DEV_OUTSIDE;

int main ( int argc, char **argv )
{
    int ret = 0;
    char buf[2000];
    
    nc_shield_cfg_t shield_cfg;
    nc_shield_cfg_t expected_cfg = { 
        .limit {
            .cpu_load     { 40.0, 60.0, 120.0 }, 
            .sessions     { 512, 1024, 1536 },
            .request_load { 60.0, 75.0, 85.0 },
            .session_load { 50.0, 60.0, 75.0 },
            .tcp_chk_load { 200.0, 300.0, 400.0 },
            .udp_chk_load { 100.0, 200.0, 300.0 },
            .evil_load    { 400.0, 800.0, 1200.0 }
        },
        .mult {
            .request_load 0.09, .session_load 0.06, .tcp_chk_load 0.3, .udp_chk_load 0.1, .evil_load 0.012,
            .active_sess 1.0
        },
        .lru {
            .low_water 20,
            .high_water 25
        },
        .fence {
            .relaxed {
                .limited { .prob 0.1, .post 1 },
                .closed  { .prob 0.2, .post 2 }
            },
            .lax {
                .limited { .prob 0.3, .post 3 },
                .closed  { .prob 0.4, .post 4 }
            },
            .tight {
                .limited { .prob 0.5, .post 5 },
                .closed  { .prob 0.6, .post 6 }
            }, 
            .closed {
                .limited { .prob 0.7, .post 7 },
                .closed  { .prob 0.8, .post 8 }
            }
        },
    };

    const char test_config[] = "\
<?xml version='1.0'?>\n\
<shield-config>\n\
  <cpu-limit lax='40.0' tight='60.0' closed='120.0'/>\n\
  <sess-limit lax='512' tight='1024' closed='1536'/>\n\
  <request-load lax='60.0' tight='75.0' closed='85.0'/>\n\
  <session-load lax='50.0' tight='60.0' closed='75.0'/>\n\
  <tcp-chk-load lax='200.0' tight='300.0' closed='400.0'/>\n\
  <udp-chk-load lax='100.0' tight='200.0' closed='300.0'/>\n\
  <evil-load lax='400.0' tight='800.0' closed='1200.0'/>\n\
  <mult request-load='0.09' session-load='0.06' tcp-chk-load='0.3' udp-chk-load='0.1' evil-load='0.012' \
        active-sess='1.0'/>\n\
  <lru  low-water='20' high-water='25'/>\n\
  <fence>\n\
    <relaxed>\n\
      <limited prob='10' post='1'/>\n\
      <closed  prob='20' post='2'/>\n\
    </relaxed>\n\
    <lax>\n\
      <limited prob='30' post='3'/>\n\
      <closed  prob='40' post='4'/>\n\
    </lax>\n\
    <tight>\n\
      <limited prob='50' post='5'/>\n\
      <closed  prob='60' post='6'/>\n\
    </tight>\n\
    <closed>\n\
      <limited prob='70' post='7'/>\n\
      <closed  prob='80' post='8'/>\n\
    </closed>\n\
  </fence>\n\
</shield-config>\n";

    /* Initialize the mvvm */
    if ( libmvutil_init() < 0 ) {
        perror( "libmvutil_init" );
        exit(1);
    }

    xmlInitParser();

    netcap_debug_set_level(10);

    printf ( "test config: \n\n%s\n\n", test_config );
    memset ( &shield_cfg, 0, sizeof ( shield_cfg ));

    do {
        if ( nc_shield_cfg_get ( &expected_cfg, buf, sizeof ( buf )) < 0 ) {
            ret -= errlog ( ERR_CRITICAL, "nc_shield_cfg_get\n" );
        }
        printf ( "Expected configuration:\n\n%s\n\n", buf );
        /* Parse the configuration */
        if ( nc_shield_cfg_load ( &shield_cfg, (char*)test_config, sizeof( test_config )) < 0 ) {
            ret -= errlog ( ERR_CRITICAL, "nc_shield_cfg_load\n" );
            break;
        };

        if ( memcmp ( &shield_cfg, &expected_cfg, sizeof ( shield_cfg ) ) != 0 ) {
            ret -= errlog ( ERR_CRITICAL, "Parsing failed\n" );
        }

        if ( nc_shield_cfg_get ( &shield_cfg, buf, sizeof ( buf )) < 0 ) {
            ret -= errlog ( ERR_CRITICAL, "nc_shield_cfg_get\n" );
        }
        
        printf ( "New configuration:\n\n%s\n\n", buf );
    } while ( 0 );

    xmlCleanupParser();

    /* Cleanup the library */
    libmvutil_cleanup();
    
    return ret;
}
