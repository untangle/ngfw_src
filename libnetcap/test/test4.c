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
#include <netinet/tcp.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <signal.h>
#include <fcntl.h>
#include <unistd.h>
#include <sys/stat.h>

#ifndef MAX
#define MAX(X , Y) ((X) > (Y) ? (X) : (Y))
#endif

#define DEBUG_STAT 1

#define BUFFER_SIZE 10000

sem_t exit_condition;

void handler (netcap_addrs_t * addrs);

void start_NxN_pipe(int * inputs, int * outputs, int num_pipes);

void start_premodule  (int client_sock_r, int server_sock_r, int C2S_to_module,   int S2C_to_module);
void start_postmodule (int client_sock_w, int server_sock_w, int C2S_from_module, int S2C_from_module);
void start_module     (int C2S_in_sock,   int S2C_in_sock,   int C2S_out_sock,    int S2C_out_sock);


void cleanup(int sig) {sem_post(&exit_condition);}

void * launch_postmodule(void * arg);
void * launch_premodule(void * arg);
void * launch_module(void * arg);


struct thread_args {
    netcap_addrs_t * addrs;
    int C2S_to_module[2];
    int C2S_from_module[2];
    int S2C_to_module[2];
    int S2C_from_module[2];
};
    
void sigpipe(int sig) {}

int main()
{
    int i;
    in_addr_t dest = inet_addr("192.168.0.0");
    u_int destmask = inet_addr("255.255.0.0");
    pthread_t id;
    int rule[3];
    int flags = NETCAP_FLAG_SUDO;

    signal(SIGPIPE,sigpipe);
    /**
     * init
     */
    printf("%s\n\n",netcap_version()); 
    sem_init(&exit_condition,0,0);
    signal(SIGINT,  cleanup);

    netcap_init();
    netcap_tcp_hook_register(handler);


    /**
     * donate a few threads
     */
    for(i=0;i<10;i++) pthread_create(&id,NULL,netcap_thread_donate,NULL);
    
    /**
     * add all the traffic we want to proxy to the list
     */
    if ((rule[0] = netcap_subscribe(flags,NULL,IPPROTO_TCP,NULL,NULL,  NULL,0,0,  &dest,destmask,0)) < 0) {
        fprintf(stderr,"error adding to traffic list \n");
        return -1;
    }
    
    /** 
     *  wait for exit signal and cleanup 
     */
    sem_wait(&exit_condition);
    sem_post(&exit_condition);

    printf("Removing subscriptions \n");
    netcap_unsubscribe_all();

    printf("cleaning up...\n\n"); fflush(stdout);
    netcap_tcp_hook_unregister(handler);
    
    printf("done\n"); fflush(stdout);
    exit(0);

    return 0;
}




void handler (netcap_addrs_t * addrs)
{
    pthread_t id;
    struct thread_args * args = malloc(sizeof(struct thread_args));
    
    if (!args) {
        fprintf(stderr,"malloc error: %s\n",strerror(errno));
        return;
    }
    
    printf("Starting Pipe :: Client: %s:%i (socket:%i) ->",
           inet_ntoa(addrs->client.sin_addr),
           addrs->client.sin_port, addrs->client.sock);
    printf(" Server %s:%i (socket:%i)  arg: %s\n",
           inet_ntoa(addrs->server.sin_addr),
           addrs->server.sin_port,
           addrs->server.sock, (char *)addrs->arg);

    args->addrs = addrs;

    if (pipe(args->C2S_to_module)<0)
        fprintf(stderr,"pipe error: %s\n",strerror(errno));
    if (pipe(args->C2S_from_module)<0)
        fprintf(stderr,"pipe error: %s\n",strerror(errno));
    if (pipe(args->S2C_to_module)<0)
        fprintf(stderr,"pipe error: %s\n",strerror(errno));
    if (pipe(args->S2C_from_module)<0)
        fprintf(stderr,"pipe error: %s\n",strerror(errno));

    if (fcntl(addrs->server.sock, F_SETFL, O_NDELAY) < 0) 
        fprintf(stderr,"fcntl error: %s\n",strerror(errno));
    if (fcntl(addrs->client.sock, F_SETFL, O_NDELAY) < 0) 
        fprintf(stderr,"fcntl error: %s\n",strerror(errno));
    if (fcntl(args->C2S_to_module[1], F_SETFL, O_NDELAY) < 0) 
        fprintf(stderr,"fcntl error: %s\n",strerror(errno));
    if (fcntl(args->S2C_to_module[1], F_SETFL, O_NDELAY) < 0) 
        fprintf(stderr,"fcntl error: %s\n",strerror(errno));
    if (fcntl(args->C2S_from_module[1], F_SETFL, O_NDELAY) < 0) 
        fprintf(stderr,"fcntl error: %s\n",strerror(errno));
    if (fcntl(args->S2C_from_module[1], F_SETFL, O_NDELAY) < 0) 
        fprintf(stderr,"fcntl error: %s\n",strerror(errno));

    pthread_create(&id,NULL,launch_module,args);
    pthread_create(&id,NULL,launch_premodule,args);
    pthread_create(&id,NULL,launch_postmodule,args);

    return;
}



void * launch_premodule(void * arg)
{
    struct thread_args * args = (struct thread_args *)arg;
    netcap_addrs_t * addrs = args->addrs;
    int inputs[2];
    int outputs[2];
    
    inputs[0]  = addrs->client.sock;
    outputs[0] = args->C2S_to_module[1];
    inputs[1]  = addrs->server.sock;
    outputs[0] = args->S2C_to_module[1];
    
    start_NxN_pipe(inputs,outputs,2);
    
    printf("Stopping Pipe :: Client: %s:%i (socket:%i) ->",
           inet_ntoa(addrs->client.sin_addr),
           addrs->client.sin_port,
           addrs->client.sock);
    printf(" Server %s:%i (socket:%i)   arg: %s\n",
           inet_ntoa(addrs->server.sin_addr),
           addrs->server.sin_port,
           addrs->server.sock,
           (char *)addrs->arg);

    close(addrs->client.sock);
    close(addrs->server.sock);
    close(args->S2C_to_module[1]);
    close(args->C2S_to_module[1]);

    netcap_addrs_free(addrs);
    free(args);

    return NULL;
}

void * launch_postmodule(void * arg)
{
    struct thread_args * args = (struct thread_args *)arg;
    netcap_addrs_t * addrs = args->addrs;
    int inputs[2];
    int outputs[2];
    
    inputs[0]  = args->C2S_from_module[0];
    outputs[0] = addrs->server.sock;
    inputs[1]  = args->S2C_from_module[0];
    outputs[1] = addrs->client.sock;
    
    start_NxN_pipe(inputs,outputs,2);

    close(inputs[0]);
    close(inputs[1]);

    return NULL;
}


void * launch_module(void * arg)
{
    struct thread_args * args = (struct thread_args *)arg;
    int inputs[2];
    int outputs[2];
    
    inputs[0]  = args->C2S_to_module[0];
    outputs[0] = args->C2S_from_module[1];
    inputs[1]  = args->S2C_to_module[0];
    outputs[1] = args->S2C_from_module[1];
    
    start_NxN_pipe(inputs,outputs,2);

    close(inputs[0]);
    close(inputs[1]);
    close(outputs[0]);
    close(outputs[1]);

    return NULL;
}

void start_NxN_pipe(int * inputs, int * outputs, int num_pipes)
{
    int nbyt=0,i=0,max=0,done=0;
    fd_set fdsr, fdse, fdsw;

    char *   input_buf[num_pipes];
    int      input_data_flag[num_pipes];
    int      input_data_len [num_pipes];
    char *   input_data[num_pipes];


    for(i=0;i<num_pipes;i++) {
        input_buf[i]       = malloc(BUFFER_SIZE);
        input_data_flag[i] = 0;
        input_data_len[i]  = 0;
        input_data[i]      = NULL;
        max = MAX(max,inputs[i]);
        max = MAX(max,outputs[i]);
        if (fcntl(outputs[i], F_SETFL, O_NDELAY) < 0) 
            fprintf(stderr,"fcntl error: %s\n",strerror(errno));
    }
    max++;
    
    while(1) {
        nbyt = 0;
        done = 0;
        
        FD_ZERO(&fdsr);
        FD_ZERO(&fdse);
        FD_ZERO(&fdsw);
        for(i=0;i<num_pipes;i++) {
            if (!input_data_flag[i]) {
                FD_SET(inputs[i],&fdsr);
                FD_SET(inputs[i],&fdse);
            }
            else {
                FD_SET(outputs[i],&fdsw);
                FD_SET(outputs[i],&fdse);
            }
        }
            
        if (select(max, &fdsr, &fdsw, &fdse, NULL) == -1) 
        {fprintf(stderr,"select failed: %s  - killing pipe\n", strerror(errno));return;}


        for(i=0;i<num_pipes;i++) {
            if (input_data_flag[i] && FD_ISSET(outputs[i],&fdsw)) {

                nbyt = write(outputs[i],input_data[i],input_data_len[i]);
            
                if (nbyt < 0) {
                    if (errno == EAGAIN) {
                        printf("would block\n");fflush(stdout);
                        done = 1;
                    }
                    else {
                        fprintf(stderr,"write error: %s - killing pipe \n",strerror(errno));
                        return;
                    }
                }
                else if (nbyt < input_data_len[i]) {
                    input_data_len[i] -= nbyt;
                    input_data[i]     += nbyt;
                    done = 1;
                }
                else if (nbyt == input_data_len[i]) {
                    input_data[i]      = NULL;
                    input_data_len[i]  = 0;
                    input_data_flag[i] = 0;
                    done = 1;
                }
                else fprintf(stderr,"unknown state- nbyt: %i  datalen: %i \n",nbyt,input_data_len[i]);
                
            }
        }
        if (done) continue;
        
        for(i=0;i<num_pipes;i++) {
            if (!input_data_flag[i] && FD_ISSET(inputs[i],&fdsr)) {

                nbyt = read(inputs[i],input_buf[i],BUFFER_SIZE);
            
                if (nbyt < 0) {
                    fprintf(stderr,"error reading: %s - killing pipe\n",strerror(errno));
                    return;
                }
                else if (nbyt == 0) return;
                else {
                    input_data_flag[i] = 1;
                    input_data[i]      = input_buf[i];
                    input_data_len[i]  = nbyt;
                    done = 1;
                }

            }
        }
        if (done) continue;

        fprintf(stderr,"unknown condition\n");
        for(i=0;i<num_pipes;i++) {
            if (FD_ISSET(outputs[i],&fdse)) 
                fprintf(stderr,"outputs[%i] socket exception - killing pipe\n",i);
            if (FD_ISSET(inputs[i],&fdse)) 
                fprintf(stderr,"inputs[%i] socket exception - killing pipe\n",i);
        }
        fprintf(stderr,"killing pipe\n");
        return;
    }/*while(1)*/
}





