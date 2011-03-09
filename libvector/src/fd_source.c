/* $HeadURL$ */
/* @author: Dirk Morris <dmorris@untangle.com> */
#include <stdlib.h>
#include <unistd.h>
#include <sys/socket.h>
#include <mvutil/debug.h>
#include <mvutil/errlog.h>

#include <vector/fd_source.h>

#define FD_SOURCE_DEBUG 1
#define FD_SOURCE_DEBUG_LVL 9

source_t* fd_source_create ( int fd )
{
    fd_source_t* src = malloc(sizeof(fd_source_t));
    if (!src)
        return errlogmalloc_null();

    src->fd = fd;
    src->key = mvpoll_key_fd_create(fd);
    src->base.get_event_key = fd_source_get_event_key;
    src->base.get_event     = fd_source_get_event;
    src->base.shutdown      = fd_source_shutdown;
    src->base.raze       = fd_source_raze;

    return (source_t*)src;
}

event_t*  fd_source_get_event ( source_t* src )
{
    event_t* evt = event_create(0);
    int fd = ((fd_source_t*)src)->fd;
    int nbyt;
    char buf[1];
    
    nbyt = read(fd,&buf,1);

    if (nbyt < 0) {
#if FD_SOURCE_DEBUG
        debug(FD_SOURCE_DEBUG_LVL,"FD_SRC: error on fd %i: %s\n",fd,errstr);
#endif
        evt->type = EVENT_BASE_ERROR_SHUTDOWN;
    }
    else if (nbyt == 0) {
#if FD_SOURCE_DEBUG
        debug(FD_SOURCE_DEBUG_LVL,"FD_SRC: shutdown(RD) on fd %i\n",fd);
#endif
        evt->type = EVENT_BASE_SHUTDOWN;
    }
    else {
#if FD_SOURCE_DEBUG
        debug(FD_SOURCE_DEBUG_LVL,"FD_SRC: data on fd %i (%i bytes)\n",fd,nbyt);
#endif
        evt->type = EVENT_BASE_MAX;
    }

    return (event_t*)evt;
}

mvpoll_key_t* fd_source_get_event_key ( source_t* src )
{
    fd_source_t* fd_src = (fd_source_t*)src;

    if (!fd_src)
        return errlogargs_null();
    
    return fd_src->key;
}

int       fd_source_shutdown ( source_t* src )
{
    fd_source_t* fd_src = (fd_source_t*)src;

    if (!fd_src)
        return errlogargs();

    if (fd_src->fd < 0) {
        errlog(ERR_WARNING,"Multiple shutdown attempt\n");
        return 0;
    }

#if FD_SOURCE_DEBUG
    debug(FD_SOURCE_DEBUG_LVL,"FD_SRC: shutdown(%i,SHUT_RD)\n",fd_src->fd);
#endif
    
    mvpoll_key_raze(fd_src->key);

    if (shutdown(fd_src->fd,SHUT_RD)<0) {
        if (errno == ENOTSOCK) {
            if (close(fd_src->fd)<0)
                perrlog("close");
        }
        /* If it is not connected (ENOTCONN), Ignore it */
        else if (errno != ENOTCONN) 
            return perrlog("shutdown");
    }

    fd_src->fd = -1;
    fd_src->key = NULL;

    return 0;
}

void      fd_source_raze ( source_t* src )
{
    fd_source_t* fd_src = (fd_source_t*)src;

    if (!fd_src) {
        errlogargs();
        return;
    }

    if (fd_src->fd != -1) {
        errlog(ERR_WARNING,"Source not quenched (fd:%i)\n",fd_src->fd);
        fd_source_shutdown(src);
    }

    if (fd_src->key)
        mvpoll_key_raze(fd_src->key);

    free(src);

    return;
}
