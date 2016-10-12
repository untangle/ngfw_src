/**
 * $Id: netcap_arp.c,v 1.00 2015/03/10 14:06:42 dmorris Exp $
 */
#include "netcap_arp.h"

#include <stdlib.h>
#include <sys/ioctl.h>
#include <sys/socket.h>
#include <mvutil/errlog.h>
#include <mvutil/debug.h>

int  netcap_arp_init ( void )
{
    return 0;
}

/**
 * Lookups up the MAC address for the provided IP in the ARP table
 * The result (if found) is copied into the mac array
 * 0 is return if the result was found.
 * non-zero result if the result was not found.
 *
 * We read /proc/net/arp instead of using ioctl/SIOCGARP because the latter
 * requires that we specify an interface. We don't really care about
 * interface so we just read the file instead.
 */
int netcap_arp_lookup ( const char* ip, char* mac, int maclength )
{
    FILE* file = fopen("/proc/net/arp", "r"); /* should check the result */
    char line[256];
    char* saveptr;
    int i;
    char* tok;
    int linecount = 0;
    
    while (fgets(line, sizeof(line), file)) {
        linecount++;
        if ( linecount == 1 )
            continue;

        for ( i=0, tok=strtok_r(line," \t",&saveptr); tok ; i++, tok=strtok_r(NULL," \t",&saveptr) ) {
            //errlog( ERR_WARNING, "TOKEN[%i]: %s\n", i, tok); 
            if ( i == 0 ) {
                if ( strcmp( tok, ip) != 0 )
                    break;
            }
            if ( i == 3 ) {
                strncpy( mac, tok, maclength );
                if ( fclose(file) < 0 )
                    perrlog( "fclose");
                return 0;
            }
        }
    }

    if ( fclose(file) < 0 )
        perrlog( "fclose");
    return -1;
}

