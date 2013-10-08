/**
 * $Id: test_trie.c 35571 2013-08-08 18:37:27Z dmorris $
 */
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <arpa/inet.h>

#include <mvutil/libmvutil.h>
#include <mvutil/errlog.h>
#include <mvutil/debug.h>

#include "netcap_trie.h"
#include "netcap_interface.h"
#include "netcap_sched.h"

/* This is needed by netcap_interface */
netcap_intf_t DEV_INSIDE,DEV_OUTSIDE;

static netcap_trie_t _trie;

static struct test_vector { 
    in_addr_t ip;
    int mem;
} _ip_vectors[] = {
    { 0x00000000, 3},
    { 0x00000001, 3},
    { 0x00000100, 4},
    { 0x00000101, 4},
    { 0x00010000, 6},
    { 0x00010100, 7},
    { 0x01000000, 10},
    { 0x01010000, 12},
    { 0xFFFFFFFF, 0 }
};

/* Apply function */
static int increment_val  ( netcap_trie_item_t* item, void *arg, in_addr_t ip );
static int increment_data ( netcap_trie_item_t* item, void* arg, in_addr_t ip );
static int check_data     ( netcap_trie_item_t* item );

/* Test functions */
/* XXX Should be test_case then size */
static int test_trie_set    ( int size, int test_case );
static int test_trie_new    ( int size, int test_case );

static int test_empty_trie  (int test_case, int size );
static int test_depth       (int test_case );

static int test_lru         ( int test_case );
static int test_lru_threads ( int test_case );


static int _main ( int argc, char **argv);

static void debug_start_test (int test_case);
static void debug_end_test   (int test_case, int err);

static int parse_args ( int argc, char **argv );

typedef enum 
{
    _LRU_FRONT_TAIL,
    _LRU_FRONT_HEAD,
    _LRU_FRONT_MID
} test_lru_front_t; 

static int test_lru_front         ( netcap_trie_t* trie, test_lru_front_t type, int item_count );
static int test_lru_head          ( netcap_trie_t* trie, netcap_trie_item_t* item, int expected );
static int test_lru_update_length ( netcap_trie_t* trie, int high_water, int low_water, int expected );

static int verify_trie_size       ( int index, netcap_trie_t* trie, int item_count, int mem );

#define LRU_VECTOR_SIZE     16
#define LRU_DEF_NUM_THREADS 8
#define LRU_DEF_TEST_TIME   15

#define LRU_MAX_NUM_THREADS 64
#define LRU_MIN_TEST_TIME   3

static struct {
    in_addr_t ips[LRU_VECTOR_SIZE];
    int size;
    int alive;
    int num_threads;
    int time;
    int set_total;
    int get_total;
    int trash_total;
    int empty_total;
    pthread_mutex_t mutex; /* Grab before modifying set_total, get_total */
    netcap_trie_t trie;
} _lru = {
    { 0x00000000, 0x00000080, 0x00008000, 0x00008080,
      0x00800000, 0x00800080, 0x00808000, 0x00808080,
      0x08000000, 0x08000080, 0x08008000, 0x08008080,
      0x08800000, 0x08800080, 0x08808000, 0x08808080
    },
    LRU_VECTOR_SIZE,
    1,
    LRU_DEF_NUM_THREADS,
    LRU_DEF_TEST_TIME,
    0,0,0,0,
    PTHREAD_MUTEX_INITIALIZER
};

#define _lru_vectors_size ((sizeof(_lru_vectors)) / (sizeof(in_addr_t)))


/* These are threads for the LRU test */
static void* thread_set             ( void* arg );
static void* thread_get             ( void* arg );
static void* thread_take_off_lru    ( void* arg );
/* -RBS static void* thread_empty_trash     ( void* arg ); */

int main ( int argc, char **argv )
{
    int ret;

    /* Initialize the mvvm */
    if ( libmvutil_init() < 0 ) {
        perror( "libmvutil_init" );
        exit(1);
    }

    netcap_debug_set_level(10);
    netcap_sched_init();

    ret = _main (argc, argv);
    
    /* Cleanup the library */
    libmvutil_cleanup();
    
    return ret;
}

int _main ( int argc, char **argv)
{
    int ret;
    int flags;
    pthread_t thread_sched;
    
    if ( parse_args ( argc, argv ) < 0 ) {
        printf( "usage: %s [<num_threads> <time>]\n", argv[0] );
        printf( "\tnum_threads: defaults to %d\n", LRU_DEF_NUM_THREADS );
        printf( "\ttime: defaults to %d seconds\n", LRU_DEF_TEST_TIME );
        return -1;
    }

    if ( pthread_create ( &thread_sched, NULL, netcap_sched_donate, NULL ) < 0 ) {
        return perrlog("pthread_create");
    }

    flags = ( ~NC_TRIE_COPY & ~NC_TRIE_FREE  & ~NC_TRIE_INHERIT & ~NC_TRIE_LRU );


    /* Initialize the trie, do not free items on exit, do not copy items on update */
    if ( netcap_trie_init ( &_trie, flags, NULL, 0, NULL, NULL, NULL, 0, 0 ) < 0 ) {
        return errlog(ERR_CRITICAL,"netcap_trie_init");
    }
    
    /* This value is dependent on the size of netcap_trie_level_t */
    ret = test_empty_trie (1, sizeof(netcap_trie_level_t));

    ret -= test_trie_set (sizeof(netcap_trie_level_t),2);
    
    ret -= test_trie_new (sizeof(netcap_trie_level_t),4);
    ret -= test_depth    ( 5 );

    ret -= test_lru ( 8 );

    ret -= test_lru_threads ( 12 );

    netcap_trie_destroy( &_trie );
    
    if ( netcap_sched_cleanup_z ( NULL ) < 0 ) ret -= errlog ( ERR_CRITICAL, "netcap_sched_cleanup_z\n" );
    
    if ( pthread_join ( thread_sched, NULL ) < 0 ) ret -= perrlog ( "pthread_join" );
    
    return ret;
}

static int increment_val ( netcap_trie_item_t* item, void* arg, in_addr_t ip )
{
    if ( item == NULL ) return errlogargs();
    
    item->data += (int)arg;
    
    return 0;
}

static int increment_data ( netcap_trie_item_t* item, void* arg, in_addr_t ip )
{
    if ( item == NULL ) return errlogargs();

    *((int*)item->data) += (int)arg;
    
    return 0;
}

static int check_data ( netcap_trie_item_t* item )
{
    static __thread int seed = 0;
    int num;

    if ( seed == 0 ) seed = *((int*)item->data);

    num = rand_r ( &seed );
    
    /* 1/4 chance of not being deletable */
    if ( num < ( RAND_MAX >> 2 )) return !NC_TRIE_IS_DELETABLE;
    
    return NC_TRIE_IS_DELETABLE;
}


static int test_trie_set ( int size, int test_case ) 
{
    int c;
    int response;
    in_addr_t ip;
    int mem;
    int vector_size;
    int err;
    int val;
    netcap_trie_item_t* item;
    
    err = 0;
    
    debug_start_test(test_case);

    for ( c = 0 ; _ip_vectors[c].mem != 0 ; c++ ) {
        ip = htonl ( _ip_vectors[c].ip );

        mem = _ip_vectors[c].mem * size;
        val = rand();

        /* Insert the item */        
        if ( (item = netcap_trie_set ( &_trie, ip, (void*)val)) == NULL ) {
            return errlog(ERR_CRITICAL,"netcap_trie_set\n");
        }

        response = (int)netcap_trie_item_data ( item );

        /* Verify the item matches */
        if ( response != val ) {
            errlog(ERR_WARNING,"%d: Incorrect value: (expected,actual) (%d,%d)\n",
                   c, c+1,response);
            err--;
        }
        
        /* Attempt to retrieve the item */
        if ( (item = netcap_trie_get (&_trie,ip)) == NULL ) {
            return errlog(ERR_CRITICAL,"netcap_trie_get\n");
        }
        response = (int)netcap_trie_item_data ( item );
 
        /* Verify the item matches */
        if ( response != val ) {
            errlog(ERR_WARNING,"%d: Incorrect value: (expected,actual) (%d,%d)\n",
                   c, val,response);
            err--;
        }

        val = rand();

        /* Insert the item */        
        if ( (item = netcap_trie_set ( &_trie, ip, (void*)val)) == NULL ) {
            return errlog(ERR_CRITICAL,"netcap_trie_set\n");
        }

        response = (int)netcap_trie_item_data ( item );

        /* Verify the item matches */
        if ( response != val ) {
            errlog(ERR_WARNING,"%d: Incorrect value: (expected,actual) (%d,%d)\n",
                   c, val,response);
            err--;
        }
        
        /* Attempt to retrieve the item */
        if ( (item = netcap_trie_get (&_trie,ip)) == NULL ) {
            return errlog(ERR_CRITICAL,"netcap_trie_get\n");
        }

        response = (int)netcap_trie_item_data ( item );
        
        /* Verify the item matches */
        if ( response != val ) {
            errlog(ERR_WARNING,"%d: Incorrect value: (expected,actual) (%d,%d)\n",
                   c, c+1,response);
            err--;
        }

        err -= verify_trie_size ( c, &_trie, c+1, mem);
    }

    debug_end_test(test_case,err);

    /* Only move onto the next test case if everything passes */
    if ( err ) return err;

    test_case++;
    
    debug_start_test(test_case);

    vector_size = c;
    
    /* Run in reverse removing items */
    for ( c = vector_size ; c-- ; ) {
        ip = htonl ( _ip_vectors[c].ip );
        mem = (c == 0 ) ? 0 : _ip_vectors[c-1].mem * size;

        /* Remove the item */
        if ( netcap_trie_delete ( &_trie, ip ) < 0 ) {
            return errlog(ERR_CRITICAL,"netcap_trie_delete(%d)\n", c);
        }

        /* Attempt to retrieve the item */
        if ( (item = netcap_trie_get (&_trie,ip)) != 0 ) {
            return errlog(ERR_CRITICAL,"netcap_trie_get\n");
        }
        
        err -= verify_trie_size ( c, &_trie, c, mem);
    }
    
    debug_end_test(test_case,err);

    return err;
}

static int test_empty_trie ( int test_case, int size )
{
    int err = 0;
    netcap_trie_t trie;
    int flags;

    debug_start_test(test_case);

    flags = ( ~NC_TRIE_COPY & ~NC_TRIE_FREE  & ~NC_TRIE_INHERIT & ~NC_TRIE_LRU );

    /* Create an empty trie */
    if ( netcap_trie_init ( &trie, flags , NULL, 0, NULL, NULL, NULL, 0, 0 ) < 0 ) {
        return errlog(ERR_CRITICAL,"netcap_trie_init\n");
    }

    err -= verify_trie_size(1, &trie, 0, 0);

    /* Destroy the empty trie */
    netcap_trie_destroy ( &trie );

    debug_end_test(test_case, err);

    return err;
}

static int test_trie_new ( int size, int test_case )
{
    int* response;
    in_addr_t ip;
    int c, mem, err, val;
    netcap_trie_t trie;
    int flags;
    netcap_trie_item_t* item;
    
    err= 0;
    
    debug_start_test(test_case);

    flags = ( NC_TRIE_COPY | NC_TRIE_FREE ) & ( ~NC_TRIE_INHERIT & ~NC_TRIE_LRU );

    /* Create an empty trie */
    if ( netcap_trie_init ( &trie,flags , NULL, sizeof(val), NULL, NULL, NULL, 0, 0 ) < 0 ) {
        return errlog(ERR_CRITICAL,"netcap_trie_init\n");
    }

    for ( c = 0 ; _ip_vectors[c].mem != 0 ; c++ ) {
        ip = htonl ( _ip_vectors[c].ip );

        mem = _ip_vectors[c].mem * size;
        val = rand();

        /* Insert a new item */
        if ((item = netcap_trie_set(&trie,ip,&val)) == 0 ) {
            return errlog(ERR_CRITICAL,"netcap_trie_set\n");
        }
        
        response = netcap_trie_item_data ( item );
        
        /* Verify the item is a copy */
        if ( response == &val ) {
            errlog(ERR_WARNING,"%d: netcap_trie_new did not copy result\n", c);
            err--;
        }

        /* Verify that the contents are correct */
        if ( *response != val ) {
            errlog(ERR_WARNING,"%d: netcap_trie_new did not copy data (%d,%d))\n",
                   c,val, *response);
            err--;
        }
        
        /* Make sure you retrieve the same value */
        val = (int)response;
        
        /* Attempt to retrieve the item */
        if ( ( item = netcap_trie_get ( &trie, ip )) == 0 ) {
            return errlog(ERR_CRITICAL,"netcap_trie_get\n");
        }

        response = netcap_trie_item_data ( item );        

        /* Verify the item matches */
        if ( response != (int*)val ) {
            errlog(ERR_WARNING,"%d: Incorrect value: (expected,actual) (%d,%d)\n",
                   c, c+1,response);
            err--;
        }

        err -= verify_trie_size ( c, &trie, c+1, mem);
    }

    netcap_trie_destroy ( &trie );

    debug_end_test(test_case,err);

    return err;
}

static int test_depth       ( int index )
{
    int c,d, val[4];
    netcap_trie_t trie;
    int base;
    netcap_trie_item_t* item;
    int data;
    int err = 0;
    int flags;

    debug_start_test(index);

    base = rand();
    
    flags = ( NC_TRIE_INHERIT ) & ( ~NC_TRIE_FREE & ~NC_TRIE_COPY & ~NC_TRIE_LRU );
    
    if ( netcap_trie_init ( &trie,flags, (void*)base-1, 0, NULL, NULL, NULL, 0, 0 ) < 0 ) {
        return errlog(ERR_CRITICAL,"netcap_trie_init\n");
    }
    
    /* Set, and then get a value at each depth */
    for ( c = 0 ; c <= NC_TRIE_DEPTH_TOTAL ; c++ ) {
        val[c] = base + rand();
        
        if ( netcap_trie_set_depth ( &trie, 0, (void*)(val[c]), c) < 0 ) {
            return errlog(ERR_CRITICAL,"netcap_trie_set_depth\n");
        }

        for ( d = 0 ; d <= NC_TRIE_DEPTH_TOTAL ; d++ ) {
            item = netcap_trie_get_depth ( &trie, 0, d );

            if ( d <= c) {
                data = (item == NULL ) ? 0 : (int)netcap_trie_item_data ( item );

                if ( data != val[d] ) {
                    err--;
                    errlog(ERR_CRITICAL, "set_depth(%d) did not work properly: %d:%d\n", d, val[d], data);
                }
            } else if ( item != NULL ) {
                errlog(ERR_CRITICAL,"set_depth set a node improperly\n");
            }
        }

        base = rand();

        if ( netcap_trie_apply_close ( &trie, 0, increment_val, (void*)base ) == NULL ) {
            errlog(ERR_CRITICAL,"netcap_trie_apply_close\n");
        }
        
        for ( d = 0 ; d <= c ; d++ ) val[d] += base;
        
        if ( ( item = netcap_trie_get_close ( &trie, 0 ) ) == NULL ) {
            return errlog(ERR_CRITICAL,"netcap_trie_get_close\n");
        }

        if ( item->depth != c ) { 
            err -= errlog(ERR_CRITICAL, "netcap_trie_get_close: item->depth != c (%d,%d)\n", item->depth, c);
        }

        data = (int)netcap_trie_item_data ( item );
        
        if ( data != val[c]) {
            err--;
            errlog(ERR_CRITICAL, "netcap_trie_apply_close did not work properly: %d:%d\n", val[c], data);
        }
    }

    debug_end_test(index, err);
    if ( err ) {
        netcap_trie_destroy(&trie);
        return err;
    }
    
    index++;
    debug_start_test(index);

    for ( c = 0 ; c <= NC_TRIE_DEPTH_TOTAL ; c++ ) {
        base = rand();
        if ( netcap_trie_apply_depth ( &trie, 0, increment_val, (void*)base, c) == NULL ) {
            errlog(ERR_CRITICAL,"netcap_trie_apply_depth\n");
        }
        
        for ( d = 0 ; d <= NC_TRIE_DEPTH_TOTAL ; d++ ) {
            if ( d <= c) val[d] += base;

            item = netcap_trie_get_depth ( &trie, 0, d );
            data = (int)netcap_trie_item_data ( item );
            
            if ( data != val[d]) {
                err--;
                errlog(ERR_CRITICAL, "netcap_trie_apply_depth did not work properly: %d:%d\n", val[d], data);
            }            
        }
    }

    debug_end_test(index, err);
    if ( err ) {
        netcap_trie_destroy(&trie);
        return err;
    }
    
    index++;
    debug_start_test(index);

    /* Remove items */
    for ( c = 0 ; c < NC_TRIE_DEPTH_TOTAL ; c++) {
        if ( netcap_trie_delete_depth ( &trie, 0, c ) < 0 ) {
            netcap_trie_destroy(&trie);
            return errlog(ERR_CRITICAL,"netcap_trie_delete_depth\n");
        }

        item = netcap_trie_get_depth ( & trie, 0, c );
        
        if ( ( item != NULL ) && ( netcap_trie_item_data ( item ) != NULL ) ) {
            err--;
            errlog(ERR_CRITICAL,"netcap_trie_delete_depth did not work: %d\n",c);
        }
    }
    
    netcap_trie_destroy ( &trie );
    
    debug_end_test ( index, err);

    return err;
}

static int test_lru        ( int test_case )
{
    int c, err=0, ret, item_count;
    int flags;
    in_addr_t ip;
    netcap_trie_item_t* item;
    netcap_trie_t trie;
    int val;

    netcap_debug_set_level(0);

    debug_start_test ( test_case );
    
    flags = ( NC_TRIE_LRU ) & ( ~NC_TRIE_FREE & ~NC_TRIE_COPY & ~NC_TRIE_INHERIT );
    if ( netcap_trie_init ( &trie, flags, NULL, 0, NULL, NULL, NULL, 0, 0 ) < 0 ) {
        return errlog(ERR_CRITICAL, "netcap_trie_init\n");
    }

    /* Verify that items are being placed onto the LRU properly */
    for ( c = 0 ; _ip_vectors[c].mem != 0 ; c++ ) {
        ip = htonl ( _ip_vectors[c].ip );
        val = rand();
        
        if ( ( item = netcap_trie_set ( &trie, ip, (void*)val)) == NULL ) {
            return errlog(ERR_CRITICAL,"netcap_trie_set\n");
        }        
        
        if ( (ret = test_lru_head ( &trie, item, c+1 )) < 0 ) return -1; err -= ret;

    }
    item_count = c;
    
    debug_end_test ( test_case++, err );
    
    if ( err != 0 ) {
        netcap_trie_destroy ( &trie );
        return err;
    }
    
    debug_start_test ( test_case );

    /* Verify that you can place items onto the front of the LRU */
    /* Move the tail to the front */
    if ( ( ret = test_lru_front ( &trie, _LRU_FRONT_TAIL, item_count )) < 0 ) return -1; err -= ret;

    /* Move the head to the front */
    if ( ( ret = test_lru_front ( &trie, _LRU_FRONT_HEAD, item_count )) < 0 ) return -1; err -= ret;

    /* Move the second to last item to the front */
    if ( ( ret = test_lru_front ( &trie, _LRU_FRONT_MID, item_count )) < 0 ) return -1; err -= ret;

    debug_end_test ( test_case++, err );

    if ( err != 0 ) {
        netcap_trie_destroy ( &trie );
        return err;
    }

    debug_start_test ( test_case );

    /* Verify that items are getting taken out of the LRU properly */
    /* Test 1: Should remove 0 items from the LRU */
    if ( ( ret = test_lru_update_length ( &trie, c, c, c )) < 0 ) return -1; err -= ret;

    /* Test 2: Should remove 0 items from the LRU */
    if ( ( ret = test_lru_update_length ( &trie, c+1, 0, c )) < 0 ) return -1; err -= ret;

    /* Test 3: Should remove 1 item from the LRU */
    if ( ( ret = test_lru_update_length ( &trie, c-1, c-1, c-1 )) < 0 ) return -1; err -= ret;
    
    c--;

    /* Test 4: Should remove 2 items from the LRU */
    if ( ( ret = test_lru_update_length ( &trie, c-1, c-2, c-2)) < 0 ) return -1; err -= ret;

    c-=2;
    
    /* Test 5: Should remove all of the items from the LRU */
    if ( ( ret = test_lru_update_length ( &trie, c-1, 0, 0)) < 0 ) return -1; err -= ret;
    
    c = 0;

    debug_end_test ( test_case++, err );
    
    if ( err ) {
        netcap_trie_destroy ( &trie );
        return err;
    }

    debug_start_test ( test_case );

/* -RBS */
/*     /\* Remove all of the items from the trash *\/ */
/*     /\* This test must be verified with valgrind *\/ */
/*     if ( netcap_trie_lru_empty ( &trie ) < 0 ) return errlog(ERR_CRITICAL,"netcap_trie_lru_empty\n"); */

    debug_end_test ( test_case, err );
    
    netcap_trie_destroy ( &trie );

    return err;
}

/* Test the LRU in a multithreaded environment *
 * 1. x threads that just set one of 16 unique ips randomly.
 * 2. 1 thread for taking stuff off of the LRU.
 * 3. 1 thread for garbage collection.
 */
static int test_lru_threads       ( int test_case )
{
    int err = 0;
    int ret = 0;
    int c;
    int flags;
    time_t duration;
    pthread_t threads[(_lru.num_threads << 1)+1];
    c = 0;
    
    debug_start_test ( test_case );
    
    /* Initialize the LRU */
    flags = ( NC_TRIE_LRU | NC_TRIE_FREE | NC_TRIE_COPY | NC_TRIE_INHERIT );

    /* The item gets copied */
    if ( netcap_trie_init ( &_lru.trie, flags, &c, sizeof(c), NULL, NULL, check_data, 8, 4 ) < 0 ) {
        return errlog(ERR_CRITICAL, "netcap_trie_init\n");
    }

    /* Startup some threads */
    do {     
        if ( pthread_create ( &threads[c++], NULL, thread_take_off_lru, NULL ) < 0 ) {
            err = perrlog("pthread_create");
            break;
        }

        if ( err < 0 ) break;

        duration = time(NULL);

        for ( ; c < ( _lru.num_threads << 1 ) + 1 ; c+=2 ) {
            if ( pthread_create( &threads[c], NULL, thread_set, (void*)rand() ) < 0 ) { 
                err = perrlog("pthread_create"); 
                break; 
            }

            if ( pthread_create( &threads[c+1], NULL, thread_get, (void*)rand() ) < 0 ) { 
                err = perrlog("pthread_create"); 
                break; 
            }
        }

        
        if ( err < 0 ) break;
        
        printf ("Startup took %d seconds\n", (int)(time(NULL) - duration) );
        
        /* Add a mailbox to exit early */
        sleep ( _lru.time );
    } while ( 0 );

    _lru.alive = 0;
        
    /* Wait for all of the threads to join */
    while ( c-- > 0 ) {
        if ( c == 1 ) duration = time ( NULL ) - duration;
        if ( pthread_join ( threads[c], (void**)&ret) < 0 ) err -= perrlog ("pthread_join");
        err += ret;
    }

    printf( "Get %d items in %d seconds, speed: %f gets/second\n", _lru.get_total, (int)duration, 
            (0.0 + _lru.get_total) / (0.0 + duration) );
    
    printf( "Set %d items in %d seconds, speed: %f sets/second\n", _lru.set_total, (int)duration, 
            (0.0 + _lru.set_total) / (0.0 + duration) );
    
    printf( "thread_take_off_lru: executed %d times\n", _lru.trash_total );
    
    printf( "thread_empty_trash: executed %d times\n", _lru.empty_total );
    
    printf( "trie->root: %d\n", *((int*)netcap_trie_data ( &_lru.trie )) );

    pthread_mutex_destroy ( &_lru.mutex );

    netcap_trie_destroy ( &_lru.trie );

    debug_end_test ( test_case, err );
    return err;
}

static void* thread_set             ( void* arg )
{
    int seed;
    int c;
    int num;
    int count = 0;
    netcap_trie_item_t* item;
    in_addr_t ip;
    
    seed = (int)arg;
    
    /* Initialize the random number generator */
    rand_r ( &seed );

    sleep ( 1 );

    while ( _lru.alive ) {
        /* Random number between 0 and number of ips */
        num = rand_r ( &seed );

        c=(int)( (_lru.size) * (num/(RAND_MAX+1.0)));
        
        ip = htonl ( _lru.ips[c] );

        if ( ( item = netcap_trie_apply ( &_lru.trie, ip, increment_data, (void*)1 ) ) == NULL ) {
            _lru.alive = 0;
            return (void*)errlog( ERR_CRITICAL, "netcap_trie_set\n" );
        }
        
        /* Attempt to move the ip to the front of the LRU  (50% probability) */
        if ( rand_r( &seed ) < rand_r( &seed )) {
            if ( netcap_trie_lru_front ( &_lru.trie, (netcap_trie_element_t)item) < 0 ) {
                errlog( ERR_CRITICAL, "netcap_trie_lru_front\n");
            }
        }

        count++;
    }

    if ( pthread_mutex_lock ( &_lru.mutex ) < 0 ) return (void*)perrlog("pthread_mutex_lock");
    printf("Created: %d nodes\n", count);
    _lru.set_total += count;
    if ( pthread_mutex_unlock ( &_lru.mutex ) < 0 ) return (void*)perrlog("pthread_mutex_unlock");    

    return NULL;
}

static void* thread_get             ( void* arg )
{
    int seed;
    int num;
    int c;
    int count = 0;
    netcap_trie_item_t* item;

    seed = (int)arg;

    rand_r ( &seed );

    sleep ( 1 );

    while ( _lru.alive ) {
        num = rand_r ( &seed );
        
        c = (int) ((_lru.size)*(num/(RAND_MAX+1.0)));
        
        count++;

        if ( ( item = netcap_trie_get ( &_lru.trie, htonl ( _lru.ips[c] ))) == NULL ) continue;

        if (  rand_r ( &seed ) < rand_r( &seed ) ) {
            if ( netcap_trie_lru_front ( &_lru.trie, (netcap_trie_element_t)item) < 0 ) {
                errlog( ERR_CRITICAL, "netcap_trie_lru_front\n");
            }
        }
    }

    if ( pthread_mutex_lock ( &_lru.mutex ) < 0 ) return (void*)perrlog("pthread_mutex_lock");
    printf("Read:    %d nodes\n", count);
    _lru.get_total += count;
    if ( pthread_mutex_unlock ( &_lru.mutex ) < 0 ) return (void*)perrlog("pthread_mutex_unlock");    

    return NULL;
}

static void* thread_take_off_lru    ( void* arg )
{
    int count = 0;

    while ( _lru.alive ) {
        sleep ( 1 );

        if ( netcap_trie_lru_update ( &_lru.trie ) < 0 ) {
            _lru.alive = 0;
            return (void*)errlog( ERR_CRITICAL, "netcap_trie_lru_update\n" );
        }
        count++;
    }
    
    _lru.trash_total = count;
        
    return NULL;
}

/* static void* thread_empty_trash     ( void* arg ) */
/* { */
/*     int count = 0; */
/*     while ( _lru.alive ) { */
/*         if ( netcap_trie_lru_empty ( &_lru.trie ) < 0 ) { */
/*             _lru.alive = 0; */
/*             return (void*)errlog( ERR_CRITICAL, "netcap_trie_lru_empty\n" ); */
/*         } */

/*         count++; */
/*     } */
    
/*     _lru.empty_total = count; */

/*     return NULL; */
/* } */

static int test_lru_front         ( netcap_trie_t* trie, test_lru_front_t type, int item_count )
{
    list_node_t* node;
    netcap_trie_item_t* item;
    int ret;

    switch ( type ) {
    case _LRU_FRONT_TAIL:
        if (( node = list_tail ( &trie->lru_list )) == NULL ) return errlog(ERR_CRITICAL,"list_tail\n");
        break;

    case _LRU_FRONT_HEAD:
        if (( node = list_head ( &trie->lru_list )) == NULL ) return errlog(ERR_CRITICAL,"list_head\n");
        break;

    case _LRU_FRONT_MID:
        if (( node = list_tail ( &trie->lru_list )) == NULL ) return errlog(ERR_CRITICAL,"list_tail\n");
        if (( node = list_node_prev ( node )) == NULL ) return errlog(ERR_CRITICAL,"list_node_prev\n");
        break;

    default:
        return errlog(ERR_CRITICAL,"Invalid type\n");
    }

    if (( item = list_node_val ( node )) == NULL ) return errlog(ERR_CRITICAL,"list_node_val\n");

    if ( netcap_trie_lru_front ( trie, (netcap_trie_element_t)item ) < 0 ) {
        return errlog(ERR_CRITICAL,"netcap_trie_lru_front\n");
    }

    if ((ret = test_lru_head ( trie, item, item_count )) < 0 ) return -1;

    return ret;
}

static int test_lru_head          ( netcap_trie_t* trie, netcap_trie_item_t* item, int expected )
{
    list_node_t* node;
    int err=0;

    /* Verify that the item is now on the LRU */
    node = list_head ( &trie->lru_list );
    
    if ( node != item->lru_node ) err -= errlog(ERR_CRITICAL,"test_lru: node != item->lru_node\n");
    
    if ( node == NULL || list_node_val( node ) != item ) {
        err -= errlog(ERR_CRITICAL,"test_lru: list_node_val(node != item\n");
    }
    
    if ( list_length ( &trie->lru_list ) != expected ) {
        err -= errlog(ERR_CRITICAL, "test_lru: list_list ( &trie.lru_list ) != %d\n", expected);
    }

    return err;
}
static int test_lru_update_length ( netcap_trie_t* trie, int high_water, int low_water, int expected )
{
    int length;
    trie->lru_low_water  = low_water;
    trie->lru_high_water = high_water;
    
    if ( netcap_trie_lru_update ( trie ) < 0 ) return errlog ( ERR_CRITICAL, "netcap_trie_lru_update\n");
    
    if ( ( length = list_length ( &trie->lru_list )) != expected ) {
        errlog(ERR_CRITICAL, "test_lru: list_length ( &trie.lru_list (%d)) != %d\n", length, expected );
        return 1;
    }
    
    return 0;
}

static int verify_trie_size ( int index, netcap_trie_t* trie, int item_count, 
                              int mem )
{
    int err = 0;

    /* Verify that the size is correct */
    if ( trie->mem != mem ) {
        errlog(ERR_WARNING,"%d:Incorrect size: (expected,actual) (%d,%d)\n",
               index, mem, trie->mem );
        err--;
    }
    
    /* Verify that the number of items is correct */
    if ( trie->item_count != item_count ) {
        errlog(ERR_WARNING,"%d:Incorrect number of items: (expected,actual) (%d,%d)\n",
               index, item_count, trie->item_count);
        err--;
    }

    return err;
}


static void debug_start_test (int test_case)
{
    printf("Test case %d: Start\n", test_case );
    
}

static void debug_end_test   (int test_case, int err)
{
    printf("Test case %d: %s\n", test_case, ( err == 0 ) ? "Passed" : "Failed" );
}


static int parse_args ( int argc, char **argv )
{
    if ( argc == 3 ) {
        if ( (_lru.num_threads = strtol ( argv[1], NULL, 10 )) < 0 ) {
            printf("Invalid number of threads: '%s'\n", argv[1] ); return -1;
        }

        if ( _lru.num_threads > LRU_MAX_NUM_THREADS ) {
            printf("The number of threads must be less than %d\n", LRU_MAX_NUM_THREADS ); return -1;
        }

        if ( _lru.num_threads < 1 ) {
            printf("The number of threads must be greater than 1\n"); return -1;
            
        }

        if ( (_lru.time = strtol ( argv[2], NULL, 10 )) < 0 ) {
            printf("Invalid amount of time: '%s'\n", argv[2] ); return -1;
        }

        if ( _lru.time < LRU_MIN_TEST_TIME ) {            
            printf("The test duration must be greater than %d\n", LRU_MIN_TEST_TIME  ); return -1;
        }

    } else if ( argc != 1 ) {
        return -1;
    }

    return 0;
}

