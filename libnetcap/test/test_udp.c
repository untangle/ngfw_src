/**
 * $Id: test_udp.c 35571 2013-08-08 18:37:27Z dmorris $
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
#include <signal.h>
#include <mvutil/debug.h>

sem_t exit_condition;


char* DEV_INSIDE="eth1";
char* DEV_OUTSIDE="eth0";


void handler (netcap_pkt_t* pkt, void* arg);
void cleanup(int sig) {sem_post(&exit_condition);}

int main()
{
    int i;
    int port1 = 7;
    int port2 = 8;
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
    if ( netcap_init( NETCAP_SHIELD_ENABLE ) < 0 ) {
        perror("netcap_init");
        exit(-1);
    }
    debug_set_level(NETCAP_DEBUG_PKG,10);

    netcap_udp_hook_register( handler );
    
    /**
     * add all the traffic we want to proxy to the list
     */
    if ((rule[0] = netcap_subscribe(flags,"rule2 conn",IPPROTO_UDP,NULL,NULL,  NULL,0,0,0,  NULL,NULL,port1,port1)) < 0) {
        fprintf(stderr,"error adding to traffic list \n");
        return -1;
    }
    numsubs++;
    if ((rule[1] = netcap_subscribe(flags,"rule2 conn",IPPROTO_UDP,NULL,NULL,  NULL,0,0,0,  NULL,NULL,port2,port2)) < 0) {
        fprintf(stderr,"error adding to traffic list \n");
        return -1;
    }
    numsubs++;

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

void  handler (netcap_pkt_t* pkt, void* arg)
{
    printf("Intercepted udp packet: ");
    printf("%s:%i -> ",inet_ntoa(pkt->src.host),pkt->src.port);
    printf("%s:%i "  ,inet_ntoa(pkt->dst.host),pkt->dst.port);
    printf("len: %i\n",pkt->data_len);

    if (netcap_udp_send(pkt->data,pkt->data_len,pkt)<0) 
        printf("send error %s\n",strerror(errno));
    
    return;
}







