/**
 * $Id$
 */
#define SERVER_PORT_NUM_U       5002    /* server port number for udp bind() */
  
#include <stdio.h>
#include <sys/socket.h>
#include <sys/uio.h>
#include <netinet/in.h>
#include <math.h>
#include <sys/time.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <unistd.h>
#include <stdlib.h>
#include <strings.h>
#define ERROR -1
#define OK 0
#define CONTROLLEN 1024
#define IP_NFMARK 22

char *DEV_INSIDE="eth0";
char *DEV_OUTSIDE="eth1";

int udpServer (void)
{
    struct sockaddr_in  serverAddr;     /* server's socket address */
    int                 sockAddrSize;   /* size of socket address structure */
    int                 sFd;            /* socket file descriptor */
    unsigned long       n;
    int                 nRead;
    int const           one = 1;
              struct msghdr msgh;
              struct cmsghdr *cmsg;
	      char msg_control[CONTROLLEN];
	      char base[64 * 1024];
	      struct iovec iovec = {base, 64 * 1024};
              unsigned int *nfmarkptr;
              unsigned int received_nfmark;
  
    /* set up the local address */
  
    sockAddrSize = sizeof (struct sockaddr_in);

    bzero ((char *) &serverAddr, sockAddrSize);
    serverAddr.sin_family = AF_INET;
    serverAddr.sin_port = htons (SERVER_PORT_NUM_U);
    serverAddr.sin_addr.s_addr = htonl (INADDR_ANY);
  
    /* create a UDP-based socket */
  
    if ((sFd = socket (AF_INET, SOCK_DGRAM, 0)) == ERROR) {
        perror ("socket");
        return (ERROR);
    }
  
    /* bind socket to local address */
  
    if (bind (sFd, (struct sockaddr *) &serverAddr, sockAddrSize) == ERROR) {
        perror ("bind");
        close (sFd);
        return (ERROR);
    }
	      if (setsockopt(sFd, SOL_IP, IP_NFMARK,
	                        &one, sizeof(one) + 1) < 0) {
		      perror("sendpacket: IP_NFMARK");
		      exit(1);
	      }
  
    n = 0;
  
    for (;;) {
    	msgh.msg_name = NULL;
    	msgh.msg_namelen = 0;
    	msgh.msg_iov = &iovec;
    	msgh.msg_iovlen = 1;
    	msgh.msg_control = msg_control;
    	msgh.msg_controllen = CONTROLLEN;
    	msgh.msg_flags = 0;

        if ((nRead = recvmsg (sFd, &msgh, 0)) < 0) {
            perror ("recvmsg");
            close (sFd);
            return (ERROR);
        }
              /* Receive auxiliary data in msgh */
              for (cmsg = CMSG_FIRSTHDR(&msgh);
                   cmsg != NULL;
                   cmsg = CMSG_NXTHDR(&msgh,cmsg)) {
                      if (cmsg->cmsg_level == SOL_IP
                        && cmsg->cmsg_type == IP_NFMARK) {
                              nfmarkptr = (unsigned int *) CMSG_DATA(cmsg);
                              received_nfmark = *nfmarkptr;
                              break;
                      }
              }
              if (cmsg == NULL) {
                      /*
                       * Error: IP_TTL not enabled or small buffer
                       * or I/O error.
                       */
		       printf("cmsg error\n");
              }
	      printf("mark: %08x\n", received_nfmark);
    }
} 

int main(int argc, char *argv[])
{
    printf("Starting %s server ...\n", argv[0]);
    udpServer();
    return 0;
}
