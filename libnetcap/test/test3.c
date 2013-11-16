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
#include <netinet/tcp.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <signal.h>
#include <fcntl.h>
#include <unistd.h>
#include <sys/stat.h>
#include <sys/poll.h>
#ifdef WITHRC4
#include <openssl/rc4.h>
#endif
#include <mvutil/debug.h>

#define BUFFER_SIZE 10000

char* DEV_INSIDE="eth1";
char* DEV_OUTSIDE="eth0";

typedef int (*hook_func_t) (int pipe_no, char * data, int datalen);

struct thread_args {
    netcap_session_t* netcap_sess;
    int C2S_to_module[2];
    int C2S_from_module[2];
    int S2C_to_module[2];
    int S2C_from_module[2];
};



void handler           (netcap_session_t* netcap_sess, void* arg);
void start_NxN_pipe    (int * inputs, int * outputs, int num_pipes, hook_func_t func);
void start_premodule   (int client_sock_r, int server_sock_r, int C2S_to_module,   int S2C_to_module);
void start_postmodule  (int client_sock_w, int server_sock_w, int C2S_from_module, int S2C_from_module);
void start_module      (int C2S_in_sock,   int S2C_in_sock,   int C2S_out_sock,    int S2C_out_sock);
void* launch_postmodule(void * arg);
void* launch_premodule (void * arg);
void* launch_module    (void * arg);
void  cleanup(int sig);
void  sigpipe(int sig);
#ifdef WITHRC4
int  rc4_hook(int pipe_no, char * data, int datalen) ;
#endif


sem_t exit_condition;
#ifdef WITHRC4
RC4_KEY key;
#endif


    

int main()
{
    int i;
    in_addr_t dest     = inet_addr("10.0.0.0");
    in_addr_t destmask = inet_addr("0.0.0.0");
    pthread_t id;
    int rule[3];
    int flags = NETCAP_FLAG_SUDO;
#ifdef WITHRC4
    char key_seed[16];
#endif
    
    netcap_intfset_t intfset;
    
    netcap_intfset_clear( &intfset );
    
    /**
     * init
     */
    printf("%s\n\n",netcap_version()); 
    signal(SIGPIPE,sigpipe);
    sem_init(&exit_condition,0,0);
    signal(SIGINT,  cleanup);

    netcap_init( ~NETCAP_SHIELD_ENABLE );
    debug_set_level(NETCAP_DEBUG_PKG,10);
    netcap_tcp_hook_register(handler);

#ifdef WITHRC4
    for(i=0;i<16;i++) key_seed[i] = i;
    RC4_set_key(&key,16,key_seed);
#endif

    /**
     * donate a few threads
     */
    for(i=0;i<10;i++) pthread_create(&id,NULL,netcap_thread_donate,NULL);
    
    /**
     * add all the traffic we want to proxy to the list
     */
    if ((rule[0] = netcap_subscribe(flags,NULL,IPPROTO_TCP, intfset, intfset,  NULL,0,0,0,  &dest,&destmask,0,0)) < 0) {
        fprintf(stderr,"error subscribing to traffic\n");
        return -1;
    }
    
    /** 
     *  wait for exit signal and cleanup 
     */
    sem_wait(&exit_condition);

    netcap_cleanup();

    exit(0);

    return 0;
}


void  handler (netcap_session_t * netcap_sess, void* arg)
{
    pthread_t id;
    struct thread_args * args = malloc(sizeof(struct thread_args));
    
    if (!args) {
        fprintf(stderr,"malloc error: %s\n",strerror(errno));
        return;
    }

    printf("Starting Pipe :: Client: %s:%i ->",
           inet_ntoa( netcap_sess->cli.cli.host ),
           netcap_sess->cli.cli.port);
    printf(" Server %s:%i    arg: %s\n",
           inet_ntoa( netcap_sess->cli.srv.host ),
           netcap_sess->cli.srv.port,(char*)netcap_sess->app_data);

    args->netcap_sess = netcap_sess;

    if (pipe(args->C2S_to_module)<0)
        fprintf(stderr,"pipe error: %s\n",strerror(errno));
    if (pipe(args->C2S_from_module)<0)
        fprintf(stderr,"pipe error: %s\n",strerror(errno));
    if (pipe(args->S2C_to_module)<0)
        fprintf(stderr,"pipe error: %s\n",strerror(errno));
    if (pipe(args->S2C_from_module)<0)
        fprintf(stderr,"pipe error: %s\n",strerror(errno));

    if (fcntl(netcap_sess->server_sock, F_SETFL, O_NDELAY) < 0) 
        fprintf(stderr,"fcntl error: %s\n",strerror(errno));
    if (fcntl(netcap_sess->client_sock, F_SETFL, O_NDELAY) < 0) 
        fprintf(stderr,"fcntl error: %s\n",strerror(errno));
    if (fcntl(args->C2S_to_module[1], F_SETFL, O_NDELAY) < 0) 
        fprintf(stderr,"fcntl error: %s\n",strerror(errno));
    if (fcntl(args->S2C_to_module[1], F_SETFL, O_NDELAY) < 0) 
        fprintf(stderr,"fcntl error: %s\n",strerror(errno));
    if (fcntl(args->C2S_from_module[1], F_SETFL, O_NDELAY) < 0) 
        fprintf(stderr,"fcntl error: %s\n",strerror(errno));
    if (fcntl(args->S2C_from_module[1], F_SETFL, O_NDELAY) < 0) 
        fprintf(stderr,"fcntl error: %s\n",strerror(errno));

    netcap_sess->app_data = arg;

    pthread_create(&id,NULL,launch_module,args);
    pthread_create(&id,NULL,launch_premodule,args);
    pthread_create(&id,NULL,launch_postmodule,args);

    return;
}

void* launch_premodule(void * arg)
{
    struct thread_args * args = (struct thread_args *)arg;
    netcap_session_t* netcap_sess = args->netcap_sess;
    int inputs[2];
    int outputs[2];
    
    inputs[0]  = netcap_sess->client_sock;
    outputs[0] = args->C2S_to_module[1];
    inputs[1]  = netcap_sess->server_sock;
    outputs[0] = args->S2C_to_module[1];
    
    start_NxN_pipe(inputs,outputs,2,NULL);
    
    printf("Stopping Pipe :: Client: %s:%i ->",
           inet_ntoa( netcap_sess->cli.cli.host ),
           netcap_sess->cli.cli.port);
    printf(" Server %s:%i    arg: %s\n",
           inet_ntoa( netcap_sess->cli.srv.host ),
           netcap_sess->cli.srv.port,(char*)netcap_sess->app_data);

    close(netcap_sess->client_sock);
    close(netcap_sess->server_sock);
    close(args->S2C_to_module[1]);
    close(args->C2S_to_module[1]);

    netcap_session_raze( netcap_sess );
    free(args);

    return NULL;
}

void* launch_postmodule(void * arg)
{
    struct thread_args * args = (struct thread_args *)arg;
    netcap_session_t * netcap_sess = args->netcap_sess;
    int inputs[2];
    int outputs[2];
    
    inputs[0]  = args->C2S_from_module[0];
    outputs[0] = netcap_sess->server_sock;
    inputs[1]  = args->S2C_from_module[0];
    outputs[1] = netcap_sess->client_sock;
    
    start_NxN_pipe(inputs,outputs,2,NULL);

    close(inputs[0]);
    close(inputs[1]);

    return NULL;
}

void* launch_module(void * arg)
{
    struct thread_args * args = (struct thread_args *)arg;
    int inputs[2];
    int outputs[2];
    
    inputs[0]  = args->C2S_to_module[0];
    outputs[0] = args->C2S_from_module[1];
    inputs[1]  = args->S2C_to_module[0];
    outputs[1] = args->S2C_from_module[1];
    
#ifdef WITHRC4
    start_NxN_pipe(inputs,outputs,2,rc4_hook);
#else
    start_NxN_pipe(inputs,outputs,2,NULL);
#endif
    
    close(inputs[0]);
    close(inputs[1]);
    close(outputs[0]);
    close(outputs[1]);

    return NULL;
}

void  start_NxN_pipe(int * inputs, int * outputs, int num_pipes, hook_func_t func)
{
    int nbyt=0,i=0,num;

    struct pollfd fdset[num_pipes];
    char *   input_buf[num_pipes];
    int      input_data_flag[num_pipes];
    int      input_data_len [num_pipes];
    char *   input_data[num_pipes];

    for(i=0;i<num_pipes;i++) {
        input_buf[i]       = malloc(BUFFER_SIZE);
        input_data_flag[i] = 0;
        input_data_len[i]  = 0;
        input_data[i]      = NULL;
        if (fcntl(outputs[i], F_SETFL, O_NDELAY) < 0) 
            fprintf(stderr,"fcntl error: %s\n",strerror(errno));
    }
    
    while(1) {
        nbyt = 0;

        /** 
         * register to watch all the events
         * read if we have no data from that pipe
         * write if we have data from that pipe
         */
        for(i=0;i<num_pipes;i++) {
            if (!input_data_flag[i]) {
                fdset[i].fd      = inputs[i];
                fdset[i].events  = POLLIN | POLLPRI | POLLERR | POLLNVAL | POLLHUP;
                fdset[i].revents = 0;
            } 
            else {
                fdset[i].fd      = outputs[i];
                fdset[i].events  = POLLOUT | POLLERR | POLLNVAL | POLLHUP;
                fdset[i].revents = 0;
            }
        }
           
        /**
         * wait for events
         */
        if ((num = poll(fdset,num_pipes,-1)) == -1) 
        {fprintf(stderr,"poll failed: %s  - killing pipe\n", strerror(errno));return;}

        /**
         * handle write events
         */
        for(i=0;i<num_pipes;i++) {
            if (input_data_flag[i] && (fdset[i].revents & POLLOUT)) {

                nbyt = write(outputs[i],input_data[i],input_data_len[i]);
            
                if (nbyt < 0) {
                    if (errno == EAGAIN) {
                        printf("would block\n");fflush(stdout);
                        num--;
                    }
                    else {
                        fprintf(stderr,"write error: %s - killing pipe \n",strerror(errno));
                        return;
                    }
                }
                else if (nbyt < input_data_len[i]) {
                    input_data_len[i] -= nbyt;
                    input_data[i]     += nbyt;
                    num--;
                }
                else if (nbyt == input_data_len[i]) {
                    input_data[i]      = NULL;
                    input_data_len[i]  = 0;
                    input_data_flag[i] = 0;
                    num--;
                }
                else fprintf(stderr,"unknown state- nbyt: %i  datalen: %i \n",nbyt,input_data_len[i]);
                
            }
        }
        if (!num) continue;
        
        /**
         * handle read events
         */
        for(i=0;i<num_pipes;i++) {
            if (!input_data_flag[i] && (fdset[i].revents & POLLIN)) {

                nbyt = read(inputs[i],input_buf[i],BUFFER_SIZE);
            
                if (nbyt < 0) {
                    fprintf(stderr,"error reading: %s - killing pipe\n",strerror(errno));
                    return;
                }
                else if (nbyt == 0) return;
                else {
                    if (func) {
                        func(i,input_buf[i],nbyt);
                    }
                    input_data_flag[i] = 1;
                    input_data[i]      = input_buf[i];
                    input_data_len[i]  = nbyt;
                    num--;
                }

            }
        }
        if (!num) continue;

        /**
         * handle HUP events
         */
        for(i=0;i<num_pipes;i++) 
            if (fdset[i].revents & POLLHUP) return;

        /**
         * handle other events
         */
        fprintf(stderr,"unknown condition\n");
        for(i=0;i<num_pipes;i++) {
            if (fdset[i].revents) {
                fprintf(stderr,"fdset[%i] = 0x%08x events: POLLIN:%i POLLPRI:%i POLLOUT:%i POLLERR:%i POLLHUP:%i POSSNVAL:%i \n",
                        i, fdset[i].revents, fdset[i].revents & POLLIN,
                        fdset[i].revents & POLLPRI, fdset[i].revents & POLLOUT,
                        fdset[i].revents & POLLERR, fdset[i].revents & POLLHUP,
                        fdset[i].revents & POLLNVAL);
            }
        }
        fprintf(stderr,"killing pipe\n");
        return;
    }/*while(1)*/
}

#ifdef WITHRC4
int rc4_hook(int pipe_no, char * data, int datalen) 
{
    char buf[datalen];

    RC4(&key,datalen,data,buf);
    memcpy(data,buf,datalen);

    return 0;
}
#endif

void  sigpipe(int sig) {}

void  cleanup(int sig) {sem_post(&exit_condition);}

