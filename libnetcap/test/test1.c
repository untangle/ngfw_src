/* $Id: test1.c,v 1.1 2004/11/09 19:40:00 dmorris Exp $ */
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



