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

/* $Id$ */
#include <stdlib.h>
#include <stdio.h>
#include <libnetcap.h>
#include <unistd.h>
#include <errno.h>
#include <string.h>
#include <ctype.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <signal.h>
#include <netinet/ip_icmp.h>
#include <netinet/ip.h>
#include <mvutil/debug.h>
#include <linux/netfilter.h>

char* DEV_INSIDE="eth1";
char* DEV_OUTSIDE="eth0";

  
void dump(void *ptr, int n)
{
	int i, j, k;
	char *p = (char *) ptr, *q;
	for (i = 0; i < n;) {
		q = &p[i];
		k = ((n - i) > 16) ? 16 : (n - i);
		for (j = 0; j < k; j++) {
			printf("%02x ", q[j] & 0xff);
		}
		for (j = 0; j < (16 - k); j++) {
			printf("   ");
		}
		printf("  ");
		for (j = 0; j < k; j++) {
			printf("%c", isprint(q[j]) ? q[j] : '.');
		}
		printf("\n");
		i += k;
	}
}

sem_t exit_condition;

void syn_handler  (netcap_pkt_t* pkt, void* arg);
void cleanup(int sig) {sem_post(&exit_condition);}

int main()
{
    int i;
    pthread_t id;
    int rule[3];
    int flags = NETCAP_FLAG_SUDO;
    int numsubs = 0;
    
    /**
     * init
     */
    printf("%s\n\n",netcap_version()); 
    sem_init(&exit_condition,0,0);
    signal(SIGINT,  cleanup);
    if (netcap_init()<0) {
        perror("netcap_init");
        exit(-1);
    }
    debug_set_level(NETCAP_DEBUG_PKG,10);
    netcap_syn_hook_register(syn_handler);
    
    /**
     * add all the traffic we want to proxy to the list
     */
#if 0
    if ((rule[0] = netcap_subscribe(flags,"rule1 queue",IPPROTO_ICMP,NULL,NULL,  NULL,0,0,0,  NULL,NULL,0,0)) < 0) {
        fprintf(stderr,"error adding to traffic list \n");
        return -1;
    }
    numsubs++;
#endif
/* #if 1 */
/*     if ((rule[1] = netcap_subscribe(flags,"rule2 queue",IPPROTO_SYN,NULL,NULL,  NULL,0,0,0,  NULL,NULL,0,0)) < 0) { */
/*         fprintf(stderr,"error adding to traffic list \n"); */
/*         return -1; */
/*     } */
/*     numsubs++; */
/* #endif */

    /**
     * create and start the proxy 
     */
    for(i=0;i<2;i++)
        pthread_create(&id,NULL,netcap_thread_donate,NULL);

    /** 
     *  wait for exit signal and cleanup 
     */
    sem_wait(&exit_condition);

    for(i=0;i<numsubs;i++) 
        if (netcap_unsubscribe(rule[i]) < 0) 
            fprintf(stderr,"error unsubscribing \n");

    netcap_cleanup();
    
    printf("done\n"); fflush(stdout);
    exit(0);
    return 0;
}

void  syn_handler (netcap_pkt_t* pkt, void* arg)
{
    printf("Intercepted SYN packet: ");
    printf("(%s -> ",inet_ntoa(pkt->src_addr));
    printf("%s)\n"  , inet_ntoa(pkt->dst_addr));

    if (netcap_set_verdict (pkt->packet_id, NULL, 0, NF_ACCEPT)) 
        printf("set_verdict error %s\n",strerror(errno));
    netcap_pkt_free (pkt);

    return;
}
