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
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <signal.h>
#include <mvutil/debug.h>

sem_t            exit_condition;

void  handler (netcap_session_t* netcap_sess, void* arg);
void* newthread(void * arg);
void  _start_pipe(int session_sock, int out_sock);
void  cleanup(int sig) {sem_post(&exit_condition);}

char* DEV_INSIDE="eth1";
char* DEV_OUTSIDE="eth0";

int main()
{
    int i;
    int port1 = 21;
    int port2 = 22;
    in_addr_t local = inet_addr("10.0.0.0");
    in_addr_t localmask = inet_addr("0.0.0.0");
    pthread_t id;
    int rule[3];
    int flags = NETCAP_FLAG_SUDO;
    netcap_intfset_t intfset;
    
    netcap_intfset_clear( &intfset );

    /**
     * init
     */
    printf("Netcap Version: %s\n",netcap_version()); 
    sem_init(&exit_condition,0,0);
    signal(SIGINT,  cleanup);
    netcap_init( ~NETCAP_SHIELD_ENABLE );
    netcap_debug_set_level(10);
    netcap_tcp_hook_register(handler);
    
    /**
     * add all the traffic we want to proxy to the list
     */
    if ((rule[0] = netcap_subscribe(flags,"rule1 conn",IPPROTO_TCP,intfset,intfset,  NULL,0,0,0,  &local,&localmask,0,0)) < 0) {
        fprintf(stderr,"error adding to traffic list \n");
        return -1;
    }
    if ((rule[1] = netcap_subscribe(flags,"rule2 conn",IPPROTO_TCP,intfset,intfset,NULL,0,0,0,  NULL,NULL,port1,port1)) < 0) {
        fprintf(stderr,"error adding to traffic list \n");
        return -1;
    }

    /**
     * create and start the proxy 
     */
    for(i=0;i<2;i++)
        pthread_create(&id,NULL,netcap_thread_donate,NULL);

    sleep(1);
    if ((rule[2] = netcap_subscribe(flags,"rule3 conn",IPPROTO_TCP,intfset,intfset,NULL,0,0,0,  NULL,NULL,port2,port2)) < 0) {
        fprintf(stderr,"error adding to traffic list \n");
        return -1;
    }
    
    /** 
     *  wait for exit signal and cleanup 
     */
    sem_wait(&exit_condition);

    netcap_tcp_hook_unregister(handler);

    for(i= 0;i<3;i++) 
        if (netcap_unsubscribe(rule[i]) < 0) 
            fprintf(stderr,"error unsubscribing \n");

    netcap_cleanup();
    
    printf("done\n"); fflush(stdout);
    exit(0);

    return 0;
}

void  handler (netcap_session_t* netcap_sess, void* arg)
{
    /**
     * we must return this thread 
     * in order to get more connections 
     * so spawn a new one to handle this connection
     */
#if 1
   pthread_t id;
   printf("Entered Hook\n");
   netcap_sess->app_data = arg;
   pthread_create(&id,NULL,newthread,netcap_sess);
#else
   printf("Entered Hook\n");
    if (close(netcap_sess->client.sock)<0) 
        fprintf(stderr,"close failed: %s\n",strerror(errno));
    if (close(netcap_sess->server_sock)<0) 
        fprintf(stderr,"close failed: %s\n",strerror(errno));
    netcap_tcp_session_raze(1,netcap_sess);
#endif
    
    return;
}

void* newthread(void * arg)
{
    netcap_session_t* netcap_sess = (netcap_session_t *) arg;

    printf("Starting Pipe :: Client: %s:%i ->",
           inet_ntoa( netcap_sess->cli.cli.host ),
           netcap_sess->cli.cli.port);
    printf(" Server %s:%i    arg: %s\n",
           inet_ntoa( netcap_sess->cli.srv.host ),
           netcap_sess->cli.srv.port,(char*)netcap_sess->app_data);
    
    _start_pipe(netcap_sess->client_sock,netcap_sess->server_sock);
    
    printf("Stopping Pipe :: Client: %s:%i ->",
           inet_ntoa( netcap_sess->cli.cli.host ),
           netcap_sess->cli.cli.port);
    printf(" Server %s:%i    arg: %s\n",
           inet_ntoa( netcap_sess->cli.srv.host ),
           netcap_sess->cli.srv.port,(char*)netcap_sess->app_data);

    netcap_session_raze( netcap_sess );

    return NULL;
}

void  _start_pipe(int client_sock, int server_sock)
{
    int nbyt=0;
    fd_set fdsr, fdse;
    unsigned char buf[5000];
    int num = 0;
    while (1) {
        nbyt = 0;
        num  = 0;
        FD_ZERO(&fdsr);
        FD_ZERO(&fdse);
        FD_SET(client_sock,&fdsr);
        FD_SET(client_sock,&fdse);
        FD_SET(server_sock,&fdsr);
        FD_SET(server_sock,&fdse);

        if (select(400, &fdsr, NULL, &fdse, NULL) < 0) 
        {fprintf(stderr,"select failed: %s  - killing pipe\n", strerror(errno)); return;}
    
        if (FD_ISSET(client_sock,&fdsr) || FD_ISSET(client_sock,&fdse)) {
            if ((nbyt = read(client_sock,buf,4096)) < 0)
            {fprintf(stderr,"client_sock read error: %s - killing pipe\n",strerror(errno));return;}

            if (nbyt == 0) 
                return;

            printf("read  %i bytes\n",nbyt);
            while(num<nbyt) {
                int i;
                i = write(server_sock,buf+num,nbyt-num);
                if (i < 0)
                {fprintf(stderr,"server_sock write error: %s - killing pipe\n",strerror(errno));return;}
                printf("wrote %i bytes\n",i);
                num+=i;
            }
        } 
    
        else if (FD_ISSET(server_sock,&fdsr) || FD_ISSET(server_sock,&fdse)) {
            if ((nbyt = read(server_sock,buf,4096)) < 0)
            {fprintf(stderr,"client_sock read error: %s - killing pipe\n",strerror(errno));return;}

            if (nbyt == 0) 
                return;

            printf("read  %i bytes\n",nbyt);
            while(num<nbyt) {
                int i;
                i = write(client_sock,buf+num,nbyt-num);
                if (i < 0)
                {fprintf(stderr,"server_sock write error: %s - killing pipe\n",strerror(errno));return;}
                printf("wrote %i bytes\n",i);
                num+=i;
            }
        } 
    }
}






