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
#include "libmvutil.h"
#include "debug.h"
#include "errlog.h"
#include "lock.h"
#include "hash.h"

#define TEST(func,lock,ret)  if ((func)((lock)) != ret) \
                                return errlog(ERR_WARNING,#func " should have returned " #ret "\n")
int verbose = 1;
int in_test = 0;



static int print_test (int num)
{
    if(verbose) {
        if (in_test) {
            printf("Test %i Success\n",num);
            in_test = 0;
            return 0;
        }
        else {
            printf("Test %i Start\n",num);
            in_test = 1;
            return 0;
        }
    }
    return 0;
}

static int test_without_readers ()
{
    lock_t lock;

    debug(10,"INIT\n");
    if (lock_init(&lock,LOCK_FLAG_NOTRACK_READERS) <0) {
        errlog(ERR_CRITICAL,"lock init failed\n");
        return -1;
    }
    
    print_test(1);
    TEST(lock_rdlock,&lock,0);
    TEST(lock_unlock,&lock,0);
    print_test(1);
    
    print_test(2);
    TEST(lock_wrlock,&lock,0);
    TEST(lock_unlock,&lock,0);
    print_test(2);

    print_test(3);
    TEST(lock_try_rdlock,&lock,0);
    TEST(lock_unlock,&lock,0);
    print_test(3);
    
    print_test(4);
    TEST(lock_try_wrlock,&lock,0);
    TEST(lock_unlock,&lock,0);
    print_test(4);

    print_test(5);
    TEST(lock_rdlock,&lock,0);
    TEST(lock_rdlock,&lock,0); /* locking twice is allowed */
    TEST(lock_unlock,&lock,0);
    TEST(lock_unlock,&lock,0);
    print_test(5);

/*     print_test(6); */
/*     TEST(lock_rdlock,&lock,0); */
/*     TEST(lock_unlock,&lock,0); */
/*     TEST(lock_unlock,&lock,0); /\* oh well - not tracking readers *\/ */
/*     print_test(6); */
    
    print_test(7);
    TEST(lock_rdlock,&lock,0);
    TEST(lock_try_wrlock,&lock,-1);
    TEST(lock_unlock,&lock,0);
    print_test(7);

    print_test(8);
    TEST(lock_wrlock,&lock,0);
    TEST(lock_try_rdlock,&lock,-2);
    TEST(lock_unlock,&lock,0);
    print_test(8);

    /* the Following would deadlock */
/*     print_test(9);  */
/*     TEST(lock_rdlock,&lock,0);  */
/*     TEST(lock_wrlock,&lock,-2);  */
/*     TEST(lock_unlock,&lock,0);  */
/*     print_test(9);  */

/*     print_test(10); */
/*     TEST(lock_wrlock,&lock,0); */
/*     TEST(lock_rdlock,&lock,-1); */
/*     TEST(lock_unlock,&lock,0); */
/*     print_test(10); */

    if (lock_destroy(&lock)<0)
        return errlog(ERR_CRITICAL,"lock_destroy failed\n");
            
    return 0;
}

static int test_with_readers ()
{
    lock_t lock;

    debug(10,"INIT\n");
    if (lock_init(&lock,0) <0) {
        errlog(ERR_CRITICAL,"lock init failed\n");
        return -1;
    }
    
    print_test(1);
    TEST(lock_rdlock,&lock,0);
    TEST(lock_unlock,&lock,0);
    print_test(1);
    
    print_test(2);
    TEST(lock_wrlock,&lock,0);
    TEST(lock_unlock,&lock,0);
    print_test(2);

    print_test(3);
    TEST(lock_try_rdlock,&lock,0);
    TEST(lock_unlock,&lock,0);
    print_test(3);
    
    print_test(4);
    TEST(lock_try_wrlock,&lock,0);
    TEST(lock_unlock,&lock,0);
    print_test(4);

    print_test(5);
    TEST(lock_rdlock,&lock,0);
    TEST(lock_rdlock,&lock,0); /* locking twice is allowed */
    TEST(lock_unlock,&lock,0);
    TEST(lock_unlock,&lock,0);
    print_test(5);

    print_test(6);
    TEST(lock_rdlock,&lock,0);
    TEST(lock_unlock,&lock,0);
    TEST(lock_unlock,&lock,-1);
    print_test(6);
    
    print_test(7);
    TEST(lock_rdlock,&lock,0);
    TEST(lock_try_wrlock,&lock,-2);
    TEST(lock_unlock,&lock,0);
    print_test(7);

    print_test(8);
    TEST(lock_wrlock,&lock,0);
    TEST(lock_try_rdlock,&lock,-2);
    TEST(lock_unlock,&lock,0);
    print_test(8);

    print_test(9); 
    TEST(lock_rdlock,&lock,0); 
    TEST(lock_wrlock,&lock,-2); 
    TEST(lock_unlock,&lock,0); 
    print_test(9); 

    print_test(10);
    TEST(lock_wrlock,&lock,0);
    TEST(lock_rdlock,&lock,-2);
    TEST(lock_unlock,&lock,0);
    print_test(10);

    if (lock_destroy(&lock)<0)
        return errlog(ERR_CRITICAL,"lock_destroy failed\n");
    
    return 0;
}


int main (int argc, char** argv)
{
    libmvutil_init();

    debug_set_mylevel(10);
    
    if (test_with_readers()<0)
        exit(-1);
    if (test_without_readers()<0)
        exit(-1);
    exit(0);
}
