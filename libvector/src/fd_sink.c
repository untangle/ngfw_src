/**
 * $Id: fd_sink.c 35573 2013-08-08 19:43:35Z dmorris $
 */
#include <libnetcap.h>

#include <stdlib.h>
#include <unistd.h>
#include <sys/socket.h>
#include <mvutil/errlog.h>
#include <mvutil/debug.h>
#include <mvutil/unet.h>

#include <vector/fd_sink.h>
#include <vector/event.h>

#define FD_SINK_DEBUG 1
#define FD_SINK_DEBUG_LVL 9

sink_t* fd_sink_create ( int fd )
{
    fd_sink_t* snk = malloc(sizeof(fd_sink_t));
    if (!snk)
        return errlogmalloc_null();

    snk->fd = fd;
    snk->key = mvpoll_key_fd_create(fd);
    snk->base.get_event_key = fd_sink_get_event_key;
    snk->base.send_event    = fd_sink_send_event;
    snk->base.shutdown      = fd_sink_shutdown;
    snk->base.raze          = fd_sink_raze;
    
    return (sink_t*)snk;
}

event_action_t  fd_sink_send_event ( sink_t* snk, event_t* event )
{
    fd_sink_t* fd_snk = (fd_sink_t*) snk;

    if (!fd_snk || !event) 
        return errlogargs();

    if (fd_snk->fd < 0) {
        errlog(ERR_WARNING,"Closed sink, Dropping Event.\n");
        return EVENT_ACTION_DEQUEUE;
    }
            
    switch (event->type) {

    case EVENT_BASE_SHUTDOWN:
        return EVENT_ACTION_SHUTDOWN;

    case EVENT_BASE_ERROR_SHUTDOWN:
        return EVENT_ACTION_SHUTDOWN;

    case EVENT_BASE_MAX: {
        char foo = '.';
        int nbyt = write(fd_snk->fd,&foo,1);

#if FD_SINK_DEBUG
        debug(FD_SINK_DEBUG_LVL,"FD_SNK: wrote(%i) - %i bytes\n",fd_snk->fd,nbyt);
#endif        
        if (nbyt < 0) {
            if (errno == EAGAIN) 
                errlog(ERR_WARNING,"FD_SNK: write would block\n");
            else 
                errlog(ERR_WARNING,"FD_SNK: write: %s\n",errstr); 
            return EVENT_ACTION_SHUTDOWN; 
        }

        return EVENT_ACTION_DEQUEUE;
    }
    default:
        errlog(ERR_CRITICAL,"This sink does not handle this event type: %i\n",event->type);
        return EVENT_ACTION_ERROR;
    }
}

mvpoll_key_t*   fd_sink_get_event_key ( sink_t* snk )
{
    fd_sink_t* fd_snk = (fd_sink_t*)snk;
    if (!fd_snk)  return errlogargs_null();

    return fd_snk->key;
}

int       fd_sink_shutdown ( struct sink* snk )
{
    fd_sink_t* fd_snk = (fd_sink_t*)snk;
    if (!fd_snk)
        return errlogargs();

    if (fd_snk->fd < 0) {
        errlog(ERR_WARNING,"Multiple shutdown attempt\n");
        return 0;
    }

    mvpoll_key_raze(fd_snk->key);

    if (shutdown(fd_snk->fd,SHUT_WR)<0) {
         /* If it is not connected (ENOTCONN), Ignore it */
        if (errno == ENOTSOCK) {
            if (close(fd_snk->fd)<0)
                perrlog("close");
        }
        else if (errno != ENOTCONN)
            return perrlog("shutdown");

    }
   
#if FD_SINK_DEBUG
    debug(FD_SINK_DEBUG_LVL,"FD_SNK: shutdown(%i,SHUT_WR)\n",fd_snk->fd);
#endif

    fd_snk->fd = -1;
    fd_snk->key = NULL;
    
    return 0;
}

void      fd_sink_raze ( struct sink* snk )
{
    fd_sink_t* fd_snk = (fd_sink_t*)snk;

    if (!fd_snk) {
        errlogargs();
        return;
    }

    if (fd_snk->fd != -1) {
        errlog(ERR_WARNING,"Sink not shutdownd (fd:%i)\n",fd_snk->fd);
        fd_sink_shutdown(snk);
    }

    if (fd_snk->key)
        mvpoll_key_raze(fd_snk->key);

    free(snk);

    return;
}


