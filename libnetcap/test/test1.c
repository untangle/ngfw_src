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

/* $Id$ */
#include <stdlib.h>
#include <stdio.h>
#include <libnetcap.h>

char* DEV_INSIDE="eth1";
char* DEV_OUTSIDE="eth0";

int main()
{
    printf("%s\n", netcap_version()); 
    return 0;
}



