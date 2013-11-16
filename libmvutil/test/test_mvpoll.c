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
#include <stdlib.h>
#include <string.h>
#include <errno.h>
#include <unistd.h>
#include "libmvutil.h"
#include "mvpoll.h"
#include "mvsem.h"
#include "uthread.h"
#include "errlog.h"
#include "debug.h"


static mvpoll_id_t mvp;
static int fds[2];
static mvsem_t sem;

int   test_timeout()
{
    struct mvpoll_event mve[10];
      int n;
    
      if ((n = mvpoll_wait(mvp,mve,10,0))<0)
    return perrlog("mvpoll_wait");
      else if (n > 0)
    return errlog(ERR_CRITICAL,"Invalid event count: %i\n",n);

      return 0;
}

int   test_sem_basic()
{
    struct mvpoll_event mve[10];
    int n;

    debug(10,"TEST SEM\n");

    if (mvpoll_ctl(mvp,MVPOLL_CTL_ADD,(mvpoll_key_t*)&sem,MVPOLLIN|MVPOLLERR|MVPOLLHUP)<0)
        return perrlog("mvpoll_ctl");
    
    if ((n = mvpoll_wait(mvp,mve,10,0))<0)
        return perrlog("mvpoll_wait");
    else if (n != 0)
        return errlog(ERR_CRITICAL,"Invalid event count: %i\n",n);

    if (mvsem_post(&sem)<0)
        return perrlog("mvsem_post");

    if ((n = mvpoll_wait(mvp,mve,10,0))<0)
        return perrlog("mvpoll_wait");
    else if (n != 1)
        return errlog(ERR_CRITICAL,"Invalid event count: %i\n",n);

    if ((n = mvpoll_wait(mvp,mve,10,0))<0)
        return perrlog("mvpoll_wait");
    else if (n != 1)
        return errlog(ERR_CRITICAL,"Invalid event count: %i\n",n);

    if (mvsem_wait(&sem)<0)
        return perrlog("mvsem_wait");

    return 0;
}

int   test_fd_basic()
{
    struct mvpoll_event mve[10];
    mvpoll_key_t* fdkey;
    int n;

    debug(10,"TEST FD\n");

    if (!(fdkey = mvpoll_key_fd_create(fds[0])))
        return perrlog("mvpoll_key_fd_create");
    
    if (mvpoll_ctl(mvp,MVPOLL_CTL_ADD,fdkey,MVPOLLIN|MVPOLLERR|MVPOLLHUP)<0)
        return perrlog("mvpoll_ctl");
    
    if ((n = mvpoll_wait(mvp,mve,10,0))<0)
        return perrlog("mvpoll_wait");
    else if (n != 0)
        return errlog(ERR_CRITICAL,"Invalid event count: %i\n",n);

    if (write(fds[1],(char*)&fds,1)<1)
        return perrlog("write");

    if ((n = mvpoll_wait(mvp,mve,10,0))<0)
        return perrlog("mvpoll_wait");
    else if (n != 1)
        return errlog(ERR_CRITICAL,"Invalid event count: %i\n",n);

    if ((n = mvpoll_wait(mvp,mve,10,0))<0)
        return perrlog("mvpoll_wait");
    else if (n != 1)
        return errlog(ERR_CRITICAL,"Invalid event count: %i\n",n);

    {
        int foo;
        if (read(fds[0],&foo,sizeof(foo))<1)
            return perrlog("read");
    }

    if (mvpoll_ctl(mvp,MVPOLL_CTL_DEL,fdkey,0)<0)
        return perrlog("mvpoll_ctl");

    mvpoll_key_raze( fdkey );

    return 0;
}

int   test_both_basic()
{
    struct mvpoll_event mve[10];
    mvpoll_key_t* fdkey;
    int n;

    debug(10,"TEST BOTH\n");

    if (!(fdkey = mvpoll_key_fd_create(fds[0])))
        return perrlog("mvpoll_key_fd_create");
    if (mvpoll_ctl(mvp,MVPOLL_CTL_ADD,fdkey,MVPOLLIN|MVPOLLERR|MVPOLLHUP)<0)
        return perrlog("mvpoll_ctl");
    if (mvpoll_ctl(mvp,MVPOLL_CTL_ADD,(mvpoll_key_t*)&sem,MVPOLLIN|MVPOLLERR|MVPOLLHUP)<0)
        return perrlog("mvpoll_ctl");
    
    if ((n = mvpoll_wait(mvp,mve,10,0))<0)
        return perrlog("mvpoll_wait");
    else if (n != 0)
        return errlog(ERR_CRITICAL,"Invalid event count: %i\n",n);

    if (write(fds[1],(char*)&fds,1)<1)
        return perrlog("write");

    if ((n = mvpoll_wait(mvp,mve,10,0))<0)
        return perrlog("mvpoll_wait");
    else if (n != 1)
        return errlog(ERR_CRITICAL,"Invalid event count: %i\n",n);

    if (mvsem_post(&sem)<0)
        return perrlog("mvsem_post");

    if ((n = mvpoll_wait(mvp,mve,10,0))<0)
        return perrlog("mvpoll_wait");
    else if (n != 2)
        return errlog(ERR_CRITICAL,"Invalid event count: %i\n",n);

    if (mvsem_wait(&sem)<0)
        return perrlog("mvsem_wait");
    {
        int foo;
        if (read(fds[0],&foo,sizeof(foo))<1)
            return perrlog("read");
    }

    if (mvpoll_ctl(mvp,MVPOLL_CTL_DEL,fdkey,0)<0)
        return perrlog("mvpoll_ctl");
    mvpoll_key_raze(fdkey);

    return 0;
}

void* test_both_poster(void* foo)
{
    int i = 0;
     int flag = (int) foo;
    
     for (i=0;i<20;i++) {
    if (i%2==0) {
    debug(8,"post!\n");
             if (mvsem_post(&sem)<0)
    return perrlog_null("mvsem_post");
        }
         else {
    debug(8,"write!\n");
             if (write(fds[1],(char*)&fds,1)<1)
    return perrlog_null("write");
        }

         if (flag) {
    if (i%2==0)
    usleep(100000);
        }
         else {
    usleep(100000);
        }
    }

     return NULL;
}

int   test_loop()
{
    int n;
    struct mvpoll_event mve[10];
    int evcount = 0;
    
    while (evcount < 20) {
        int numevent;
        
        if ((n = mvpoll_wait(mvp,mve,10,-1))<0)
            return perrlog("mvpoll_wait");

        for (numevent = 0 ; numevent < n ; numevent++, evcount++) {
            mvpoll_event_t* event = &mve[numevent];

            if (event->key->type == MVSEM_MVPOLL_KEY_TYPE) {
                debug(8,"got event: SEM\n");
                if (mvsem_wait(&sem)<0)
                    return perrlog("mvsem_wait");
            }
            else if (event->key->type == mvpoll_key_type_fd) {
                int foo;
                
                debug(8,"got event: FD\n");

                if (read(fds[0],&foo,sizeof(foo))<1)
                    return perrlog("read");
            }
            else
                return errlog(ERR_CRITICAL,"Unknown event");
            
        }
    }

    return 0;
}

int   test_loops()
{
    pthread_t id;
    mvpoll_key_t* fdkey;
    
    debug(10,"TEST LOOP1\n");

    mvp = mvpoll_create(133);
    if (!(fdkey = mvpoll_key_fd_create(fds[0])))
        return perrlog("mvpoll_key_fd_create");
    if (mvpoll_ctl(mvp,MVPOLL_CTL_ADD,fdkey,MVPOLLIN|MVPOLLERR|MVPOLLHUP)<0)
        return perrlog("mvpoll_ctl");
    if (mvpoll_ctl(mvp,MVPOLL_CTL_ADD,(mvpoll_key_t*)&sem,MVPOLLIN|MVPOLLERR|MVPOLLHUP)<0)
        return perrlog("mvpoll_ctl");

    if (pthread_create(&id,&small_detached_attr,test_both_poster,NULL)<0)
        return perrlog("pthread_create");
    if (test_loop()<0) return -1;

    mvpoll_raze(mvp);
    mvpoll_key_raze(fdkey);

    debug(10,"TEST LOOP2\n");

    mvp = mvpoll_create(133);
    if (!(fdkey = mvpoll_key_fd_create(fds[0])))
        return perrlog("mvpoll_key_fd_create");
    if (mvpoll_ctl(mvp,MVPOLL_CTL_ADD,fdkey,MVPOLLIN|MVPOLLERR|MVPOLLHUP)<0)
        return perrlog("mvpoll_ctl");
    if (mvpoll_ctl(mvp,MVPOLL_CTL_ADD,(mvpoll_key_t*)&sem,MVPOLLIN|MVPOLLERR|MVPOLLHUP)<0)
        return perrlog("mvpoll_ctl");

    if (pthread_create(&id,&small_detached_attr,test_both_poster,(void*)1)<0)
        return perrlog("pthread_create");
    if (test_loop()<0) return -1;

    mvpoll_raze(mvp);
    mvpoll_key_raze(fdkey);

    return 0;
}

int main ()
{
    libmvutil_init();

    debug_set_mylevel(10);
    debug_set_level(255,10); /* mvutil */

    if (pipe(fds)<0)
        return perrlog("pipe");
    if (mvsem_init(&sem,0,0)<0)
        return perrlog("mvsem_init");

    mvp = mvpoll_create(133);
    if (test_timeout()<0) return -1;
    mvpoll_raze(mvp);

    mvp = mvpoll_create(133);
    if (test_sem_basic()<0) return -1;
    mvpoll_raze(mvp);

    mvp = mvpoll_create(133);
    if (test_fd_basic()<0) return -1;
    mvpoll_raze(mvp);

    mvp = mvpoll_create(133);
    if (test_both_basic()<0) return -1;
    mvpoll_raze(mvp);

    if (test_loops()<0) return -1;

    if (close(fds[0])<0)
        return perrlog("close");
    if (close(fds[1])<0)
        return perrlog("close");
    if (mvsem_destroy(&sem)<0)
        return perrlog("mvsem_destroy");

    return 0;
}
