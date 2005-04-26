/*
 * Copyright (c) 2003 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */
#ifndef __NETCAP_SUBSCRIPTIONS_H_
#define __NETCAP_SUBSCRIPTIONS_H_

#include <libnetcap.h>

#include "netcap_traffic.h"
#include "netcap_rdr.h"

// The chain containing all of the antisubscribe rules
#define ANTISUBSCRIBE_CHAIN "antisub"

// The chain containing all of the interface marking rules
#define INTERFACE_CHAIN     "markintf"


/**
 * A user's subscription
 */
typedef struct netcap_sub {

    /**
     * the subscription ID
     */
    int sub_id;

    /**
     * the traffic description 
     * NOTE:
     * this is not destroyed in _subscription_destroy
     * traffic_destroy must be done seperately
     * traffic_init must also be done seperately
     */
    netcap_traffic_t traf;

    /**
     * the redirect of this subscription
     * NOTE:
     * this is not destroyed in _subscription_destroy
     * rdr_destroy it must be done seperately
     * rdr_init must also be done seperately
     */
    rdr_t rdr;

    /**
     * user supplied argument
     */
    void* arg;

} netcap_sub_t;

/* typedef subscription_t netcap_sub_t; */

int    netcap_redirect_tables_init (void);
int    netcap_subscriptions_init (void);
int    netcap_redirect_tables_cleanup (void);
int    netcap_subscriptions_cleanup (void);

netcap_sub_t* netcap_subscription_malloc (void);
int           netcap_subscription_init (netcap_sub_t* sub, void* arg);
netcap_sub_t* netcap_subscription_create (void* arg);
int           netcap_subscription_destroy (netcap_sub_t* sub);
int           netcap_subscription_raze (netcap_sub_t* sub);
int           netcap_subscription_free (netcap_sub_t* sub);

netcap_sub_t* netcap_subscription_get ( int sub_id );


#define subscription_malloc(...)     netcap_subscription_malloc(__VA_ARGS__)
#define subscription_init(...)       netcap_subscription_init(__VA_ARGS__)
#define subscription_create(...)     netcap_subscription_create(__VA_ARGS__)


#define subscription_free(...)       netcap_subscription_free(__VA_ARGS__)
#define subscription_destroy(...)    netcap_subscription_destroy(__VA_ARGS__)
#define subscription_raze(...)       netcap_subscription_raze(__VA_ARGS__)

#define subscriptions_init(...)      netcap_subscriptions_init(__VA_ARGS__)
#define subscriptions_cleanup(...)   netcap_subscriptions_cleanup(__VA_ARGS__)

#endif

