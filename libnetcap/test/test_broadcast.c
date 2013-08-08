/**
 * $Id$
 */
#include <stdlib.h>
#include <stdio.h>
#include <libnetcap.h>
#include <unistd.h>
#include <errno.h>
#include <string.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <mvutil/debug.h>

char* DEV_INSIDE="eth1";
char* DEV_OUTSIDE="eth0";


int main(int argc, char *argv[])
{
    int ret;
    netcap_pkt_t *prop;
    
    /**
     * init
     */
    printf("%s\n\n",netcap_version()); 
    netcap_init();
    debug_set_level(NETCAP_DEBUG_PKG,10);
    prop = netcap_pkt_create();
    if (!prop) {
	    exit(-1);
    }
#define DATA "hello, world"
    prop->dst_addr.s_addr = argc < 2 ? htonl(0x0a000020) : htonl(0x0a0000ff);
    prop->dst_port = 2048;
    if (argc < 2) {
        prop->outdev[0] = 0;
    } else {
        strcpy(prop->outdev, argv[1]);
    }
    ret = netcap_udp_send(DATA, strlen(DATA), prop);
    netcap_pkt_free(prop);
    return 0;
}







