/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */
#include "mvutil/usystem.h"

#include <stdlib.h>
#include <string.h>
#include <errno.h>
#include <sys/types.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/wait.h>
#include "mvutil/debug.h"
#include "mvutil/errlog.h"

#define MAX_ARGS 256

static char* _chop_str (char **args, char* str);


int  mvutil_system (char* str)
{
    int i;
    char* env[1];
    char* args[MAX_ARGS];

    int status;
    int tries;
    char* tmp;

    for (i=0;i<MAX_ARGS;i++)
        args[i] = NULL;
    env[0] = NULL;

    if (!(tmp = _chop_str(args,str)))
        return perrlog("_chop_str");

/*     debug(1,"LINE: "); */
/*     for (tmp = args[0], tries = 0; tmp ; tmp = args[++tries]) */
/*         debug_nodate(1,"\"%s\"",tmp); */
/*     debug_nodate(1,"\n",tmp); */
        
    /**
     * try 5 times,
     */
    for (tries = 0 ;  ; tries++) {

        if (fork() == 0) {
            execve(args[0], args, env);
        } else {
            if (wait(&status)<0) {
                perrlog("wait");
                break;  /* assume worked */
            }

            if (WIFEXITED(status) && !WEXITSTATUS(status)) 
                break;
            else {
                errlog(ERR_WARNING,"execv(%s) returned error\n",str);
                errlog(ERR_WARNING,"execv(%s) repeating...\n",str);
            }
                
                
        }  
                
        sleep(1);
                
        if (tries > 4) {
            errlog(ERR_CRITICAL,"execv(%s) failed\n",str);
            break;
        }
    }

    free(tmp);
    return 0;
}


static char* _chop_str (char **args, char* str)
{
    char* tmp;
    int i;
    char* tok;
    char* buf = strdup(str);
    
    tok = strtok_r(buf," \t\r\n",&tmp);

    for ( i=0 ; tok && i<MAX_ARGS ; i++ ) {
        args[i] = tok;
        tok = strtok_r(NULL," \t\r\n",&tmp);
    }

    args[i] = NULL;

    return buf;
}
