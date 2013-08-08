/**
 * $Id$
 */
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



