/**
 * $Id: test1.c 35573 2013-08-08 19:43:35Z dmorris $
 */
#include <libvector.h>

#include <stdlib.h>
#include <stdio.h>
#include <errno.h>
#include <unistd.h>
#include <sys/time.h>
#include <time.h>
#include <libmvutil.h>
#include <mvutil/debug.h>
#include <string.h>
#include "../src/fd_source.h"
#include "../src/fd_sink.h"

#define NUMRELAY 200
#define NUMTRIPS 500

static vector_t vec;
static list_t  chain;
static int endpoint_rd;
static int endpoint_wr;


void* pthread_counter (void* arg)
{
    char buf[10];
    static struct timeval start;
    static struct timeval stop;
    int i;

    memset(buf,0,10);
    
    debug(10,"Send First Token\n");
    if (write(endpoint_wr,buf,1)<1) {
        perror("write"); exit(1);
    }
    if (read(endpoint_rd,buf,10)<1) {
        perror("write"); exit(1);
    }
    debug(10,"Got  First Token\n");

    debug(10,"----- Start Token Test -----\n");
    gettimeofday(&start,NULL);

    debug(10,"Token Trips (%50):");
    for (i=0;i<NUMTRIPS;i++) {
        if (i%50 == 0) debug_nodate(10,".");
        if (write(endpoint_wr,buf,1)<1) {
            perror("write"); exit(1);
        }
        if (read(endpoint_rd,buf,10)<1) {
            perror("write"); exit(1);
        }
    }
    debug_nodate(10,"\n");
    debug(10,"----- End   Token Test -----\n");

    gettimeofday(&stop,NULL);

    {
        long num_sec   = stop.tv_sec  - start.tv_sec;
        long num_micro = stop.tv_usec - start.tv_usec;
        float usecpertrip;

        num_micro += num_sec*1000000;
        usecpertrip = ((float)num_micro) / ((float)NUMTRIPS);

        debug(1,"TIME: %i relays per trip\n",NUMRELAY);
        debug(1,"TIME: %i trips in %i usec\n",NUMTRIPS,num_micro);
        debug(1,"TIME: %f usec/trip\n",usecpertrip);
    }
    return NULL;

}

int main()
{
    pthread_t id;
    int pipes[NUMRELAY][2];
    source_t* srcs[NUMRELAY];
    sink_t*   snks[NUMRELAY];
    relay_t*  relays[NUMRELAY];
    int i;

    if (libmvutil_init()<0) 
        perror("libmvutil_init");
    debug_set_mylevel(10);
    debug_set_level(UTIL_DEBUG_PKG,1);
    debug_set_level(VECTOR_DEBUG_PKG,1);
    
    if (list_init(&chain,0)<0)
        perror("list_init");
    
    for (i=0;i<NUMRELAY;i++) {
        if (pipe(pipes[i])<0) {
            perror("pipe"); exit(1);
        }
        debug(10,"Pipe %i: (%i,%i)\n",i,pipes[i][0],pipes[i][1]);
    }
            
    debug(10,"Creating src/snk: ",i);
    for (i=0;i<NUMRELAY;i++) {
        debug_nodate(10,".");

        if (i != 0)
            srcs[i] = fd_source_create(pipes[i][0]);
        else
            endpoint_rd = pipes[i][0];
        
        if (i != NUMRELAY - 1)
            snks[i] = fd_sink_create(pipes[i][1]);
        else
            endpoint_wr = pipes[i][1];
        
    }
    debug_nodate(10,": %i\n",NUMRELAY);

    debug(10,"Creating  relays: ",i);
    for (i=0;i<NUMRELAY-1;i++) {
        debug_nodate(10," (%i->%i)",((fd_source_t*)srcs[i+1])->fd,((fd_sink_t*)snks[i])->fd);
        relays[i] = relay_create();
        relay_set_src(relays[i],srcs[i+1]);
        relay_set_snk(relays[i],snks[i]);
    }
    debug_nodate(10,": %i\n",NUMRELAY);

    debug(10,"Adding    relays: ",i);
    for (i=0;i<NUMRELAY-1;i++) {
        debug_nodate(10,".");
        list_add_tail(&chain,relays[i]);
    }
    debug_nodate(10,": %i\n",NUMRELAY-1);

    if (vector_init(&vec,&chain)<0) {
        perror("vector_init"); exit(1);
    }

    if (pthread_create(&id,NULL,pthread_counter,NULL)<0)
        perror("pthread_create");

    debug(10,"Vectoring...\n");
    vector_set_timeout(&vec,1);
    vector(&vec);
    debug(10,"Done.\n");
    
    if (list_destroy(&chain)<0)
        perror("chain_destroy");

    for (i=0;i<NUMRELAY;i++) {
/*         relay_free(relays[i]); */
/*         snks[i]->raze(snks[i]); */
/*         srcs[i]->raze(srcs[i]); */
        if (vector_destroy(&vec)<0)
            perror("vector_destroy");
    }

    libmvutil_cleanup();
    
    return 0;
}
