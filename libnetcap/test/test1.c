/**
 * $Id: test1.c 35571 2013-08-08 18:37:27Z dmorris $
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



