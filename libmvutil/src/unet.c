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

/* $Id$ */
#include "mvutil/unet.h"

#include <string.h>

#include <netinet/in.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <arpa/inet.h>
#include <unistd.h>
#include <string.h>
#include <fcntl.h>
#include <errno.h>
#include <poll.h>
#include "mvutil/errlog.h"
#include "mvutil/uthread.h"
#include "mvutil/debug.h"

#define __QUEUE_LENGTH 2048
#define START_PORT 9500

#define PORT_RANGE_ATTEMPTS 5
#define LISTEN_ATTEMPTS 5

#define ENABLE_BLOCKING 0xDABBA
#define NON_BLOCKING_FLAGS   O_NDELAY | O_NONBLOCK

static struct {
    pthread_key_t tls_key;
} _unet = {
    .tls_key = -1
};

typedef struct {
    int current;
    char buf_array[NTOA_BUF_COUNT][INET_ADDRSTRLEN];
} unet_tls_t;

typedef u_char  u8;
typedef u_short u16;
typedef u_long  u32;

static u_short next_port_tcp = START_PORT;
static u_short next_port_udp = START_PORT;


static void _close_socks( int count, int* fds );

static int _unet_startlisten (struct sockaddr_in* addr);

static u16 _unet_sum_calc ( u16 len, u8 src_addr[],u8 dest_addr[], u8 buff[], u8 proto );

static __inline__ int _unet_blocking_modify( int fd, int if_blocking )
{
    int flags;

    if ( fd < 0 ) return errlog( ERR_CRITICAL, "Invalid FD: %d\n", fd );
    
    if (( flags = fcntl( fd, F_GETFL )) < 0 ) return perrlog( "fcntl" );
    
    if ( if_blocking == ENABLE_BLOCKING ) flags &= ~( NON_BLOCKING_FLAGS );
    else                                  flags |= NON_BLOCKING_FLAGS;

    if ( fcntl( fd, F_SETFL, flags ) < 0 ) return perrlog( "fcntl" );

    return 0;
}

static unet_tls_t* _tls_get ( void );
static int         _tls_init( void* buf, size_t size );

int     unet_init        ( void )
{
    if ( pthread_key_create( &_unet.tls_key, uthread_tls_free ) < 0 ) {
        return perrlog( "pthread_key_create\n" );
    }
    
    return 0;
}

int     unet_startlisten (u_short listenport)
{
    struct sockaddr_in listen_addr;

    memset(&listen_addr,0,sizeof(listen_addr));
    listen_addr.sin_port = htons(listenport);
    listen_addr.sin_family = AF_INET;
    listen_addr.sin_addr.s_addr = htonl(INADDR_ANY); 

    return _unet_startlisten(&listen_addr);
}

int     unet_startlisten_addr(u_short listenport, struct in_addr* bind_addr )
{
    struct sockaddr_in listen_addr;

    memset(&listen_addr,0,sizeof(listen_addr));
    listen_addr.sin_port = htons(listenport);
    listen_addr.sin_family = AF_INET;
    listen_addr.sin_addr.s_addr = bind_addr->s_addr; 

    return _unet_startlisten(&listen_addr);
}

int     unet_startlisten_local (u_short listenport)
{
    struct sockaddr_in listen_addr;
    
    memset(&listen_addr,0,sizeof(listen_addr));
    listen_addr.sin_port = htons(listenport);
    listen_addr.sin_family = AF_INET;
    if (inet_aton("127.0.0.1", &listen_addr.sin_addr)<0) 
        return -1;

    return _unet_startlisten(&listen_addr);
}

int     unet_startlisten_udp (u_short listenport)
{
    int listen_sock=0;
  
    struct sockaddr_in listen_addr;

    memset(&listen_addr,0,sizeof(listen_addr));
    listen_addr.sin_port = htons(listenport);
    listen_addr.sin_family = AF_INET;
    listen_addr.sin_addr.s_addr = htonl(INADDR_ANY);

    if ((listen_sock = socket(AF_INET, SOCK_DGRAM, IPPROTO_UDP )) < 0)
        return -1;

    if (bind(listen_sock, (struct sockaddr*)&listen_addr, sizeof(listen_addr))<0) 
        return -1;
  
    return listen_sock;
}

/* Opens up a consecutive range of ports for a TCP connection, if unable to open up a consecutive
 * range after a 5 attempts, return an error */
int     unet_startlisten_on_portrange(  int count, u_short* base_port, int* socks, char* ip )
{
    int c;
    
    if ( count < 1 || base_port == NULL || socks == NULL )
        return errlogargs();

    struct in_addr bind_addr = {
        .s_addr = htonl( INADDR_ANY )
    };
    
    if ( ip != NULL ) {
        if ( inet_aton( ip, &bind_addr ) < 0 ) {
            return errlog( ERR_CRITICAL, "Unable to convert ip '%s' (inet_aton)", ip );
        }
    } 
    
    for ( c = 0 ; c < count ; c++ )
        socks[c] = -1;

    for ( c = 0 ; c < PORT_RANGE_ATTEMPTS; c++ ) {
        int d;
        u_short port;

        /* Get the first port */
        if ( unet_startlisten_on_anyport_tcp( base_port, &socks[0], &bind_addr )) {
            return errlog( ERR_CRITICAL, "unet_startlisten_on_anyport_tcp\n" );
        }
        
        for ( d = 1 ; d < count ; d++ ) {
            if ( unet_startlisten_on_anyport_tcp( &port, &socks[d], &bind_addr )) {
                _close_socks( d, socks );
                return errlog( ERR_CRITICAL, "unet_startlisten_on_anyport_tcp\n" );
            }
            
            if ( port == ( *base_port + d ))
                continue;

            _close_socks( d, socks );
            break;
        }
        if ( d == count ) return 0;
    }

    return errlog( ERR_CRITICAL, "Unable to open %d consecutive ports in %d attempts\n", count, c );
}

static void _close_socks( int count, int* socks )
{
    int c;

    for ( c = 0 ; c < count ; c++ ) {
        if ( socks[c] > 0 && close( socks[c] ))
            perrlog( "close" );
    }
}


int     unet_startlisten_on_anyport_tcp( u_short* port, int* fd, struct in_addr* bind_addr )
{
    *fd = -1;
    *port = next_port_tcp++;
    int attempts = LISTEN_ATTEMPTS;

    if (next_port_tcp < 2048 || next_port_tcp > 65000)
        next_port_tcp = START_PORT;
    
    while (*fd == -1) {

        if ( attempts -- < 0 ) {
            return errlog( ERR_CRITICAL, "Unable to open a port in %d attempts", LISTEN_ATTEMPTS );
        }

        if ((*fd = unet_startlisten_addr(*port, bind_addr ))<0) {

            if (errno == EINVAL  || errno == EADDRINUSE) {
                *port = *port + 1; 
                if (*port > 65000)
                    *port = START_PORT;
                *fd = -1;
                continue; 
            }
            else 
                return perrlog("unet_startlisten");
        }
        else break;
    }

    return 0;
}

int     unet_startlisten_on_anyport_udp (u_short* port, int* fd)
{
    *fd = -1;
    *port = next_port_udp++;

    if (next_port_udp < 2048 || next_port_udp > 65000)
        next_port_udp = START_PORT;
    
    while (*fd == -1) {
        
        if ((*fd = unet_startlisten_udp(*port))<0) {

            if (errno == EINVAL  || errno == EADDRINUSE) {
                *port = *port + 1; 
                if (*port>65000) *port = START_PORT;
                *fd = -1;
                continue; 
            } 
            else 
                return perrlog("unet_startlisten_udp");
        }
        else break;
    }

    return 0;
}

int     unet_accept (int listensocket)
{
    int tmplen = sizeof(struct sockaddr_in);
    int session_socket=0;
    struct sockaddr_in tmpaddr;

    session_socket = accept(listensocket, 
                            (struct sockaddr *)&tmpaddr,
                            (unsigned int*)&tmplen);

    return session_socket;
}

int     unet_open (in_addr_t* destaddr, u_short destport)
{
    int newsocket=-1;
    struct sockaddr_in out_addr;

    if ((newsocket = socket(AF_INET, SOCK_STREAM, IPPROTO_TCP))<0) 
        return -1;

    out_addr.sin_family = AF_INET;
    out_addr.sin_port   = htons(destport);
    memcpy(&out_addr.sin_addr,destaddr,sizeof(in_addr_t));

    if (connect(newsocket, (struct sockaddr*)&out_addr, sizeof(out_addr))<0) 
        return -1;

    return newsocket;
}

int     unet_readln (int fd, char * buf, int bufsize, int* retval)
{
    int position=0;
    char c;
    int ret=-1;
    
    if (fd < 0) {
        errno = EINVAL;
        return errlogargs();
    }

    while(position < bufsize - 1) {

        ret = read(fd,&c,1);
        
        if (ret <  0) {*retval = position; return ret;}
        if (ret == 0) {*retval = position; return ret;}

        buf[position] = c;
        position++;

        if (c == '\n') break;
    }

    buf[position] = '\0';

    *retval = position;
    return 0;
}

ssize_t unet_read_timeout (int fd, void* buf, size_t count, int millitime)
{
    struct pollfd fds[1];
    int n;
    
    fds[0].fd      = fd;
    fds[0].events  = POLLIN;
    fds[0].revents = 0;
    
    if ((n = poll(fds,1,millitime))<0) return -1;
    
    if (n == 0) return -2;
    
    if (fds[0].revents & POLLHUP)  return 0;
    if (fds[0].revents & POLLERR)  return -1;
    if (fds[0].revents & POLLNVAL) return -1;
    
    /* else POLLIN */
    return read(fd,buf,count);
}

ssize_t unet_read_loop (int fd, void* buf, size_t* count, int numloop)
{
    int num_read = 0;
    int n,i;

    if (fd < 0) {
        errno = EINVAL;
        return errlogargs();
    }
    
    for(num_read=0,i=0 ; num_read<*count && i<numloop ; i++) {
        if ((n=read(fd,buf+num_read,*count-num_read))<0) {
            *count = num_read;
            return perrlog("read");
        }
        if (n == 0) {
            *count = num_read;
            return 0;
        }
        num_read += n;
    }
        
    if (num_read<*count && i >= numloop) 
        errlog(ERR_WARNING,"loop expired\n");

    *count = num_read;
    return 0;
}

ssize_t unet_write_loop (int fd, void* buf, size_t* count, int numloop)
{
    int num_write = 0;
    int n,i;
    
    if (fd < 0) {
        errno = EINVAL;
        return errlogargs();
    }
        
    for(num_write=0,i=0 ; num_write<*count && i<numloop ; i++) {
        if ((n=write(fd,buf+num_write,*count-num_write))<0) {
            *count = num_write;
            return perrlog("write");
        }
        num_write += n;
    }
        
    if (num_write<*count && i >= numloop) 
        errlog(ERR_WARNING,"loop expired\n");
        
    *count = num_write;
    return 0;
}

int     unet_poll_dump (struct pollfd* fdset, int size)
{
    int i;
    for(i=0;i<size;i++) {
        if (fdset[i].revents)  {
            errlog(ERR_WARNING,
                   "fdset[%i] = 0x%08x  events: POLLIN:%i POLLPRI:%i POLLOUT:%i POLLERR:%i POLLHUP:%i POLLNVAL:%i \n",
                   i, fdset[i].revents,        fdset[i].revents & POLLIN,
                   fdset[i].revents & POLLPRI, fdset[i].revents & POLLOUT,
                   fdset[i].revents & POLLERR, fdset[i].revents & POLLHUP,
                   fdset[i].revents & POLLNVAL);
        }
    }

    return 0;
}

/**
 * Just reset the connection, but this doesn't close the fd
 */
int     unet_reset ( int fd )
{
    struct linger l = {1,0};
    
    if ( setsockopt( fd, SOL_SOCKET, SO_LINGER, &l, sizeof( struct linger )) < 0 ) {
        return perrlog("setsockopt");
    }

    return 0;
}

int     unet_reset_and_close (int fd)
{
    if ( unet_reset( fd )  < 0 ) 
        return errlog( ERR_CRITICAL, "unet_reset" );

    if (close(fd)<0)
        return perrlog("close");
    
    return 0;
}

/**
 * Close a file descriptor and set its value to negative one.
 */
int     unet_close( int* fd_ptr )
{
    if ( fd_ptr == NULL ) return errlogargs();

    int fd  = *fd_ptr;
    *fd_ptr = -1;
    
    if (( fd > 0 ) && ( close( fd ) < 0 )) return perrlog( "close" );
    
    return 0;
}

void    unet_reset_inet_ntoa( void )
{
    unet_tls_t* tls;

    if (( tls = _tls_get()) == NULL ) {
        errlog( ERR_CRITICAL, "_tls_get\n" );
        return;
    }

    tls->current = 0;
}

char*   unet_inet_ntoa (in_addr_t addr)
{
    unet_tls_t* tls;
    
    if (( tls = _tls_get()) == NULL ) return errlog_null( ERR_CRITICAL, "_tls_get\n" );
    
    tls->current = 0;
    return unet_next_inet_ntoa( addr );
}


char*   unet_next_inet_ntoa( in_addr_t addr )
{
    unet_tls_t* tls;
    
    if (( tls = _tls_get()) == NULL ) return errlog_null( ERR_CRITICAL, "_tls_get\n" );

    struct in_addr i;
    memset(&i, 0, sizeof(i));
    i.s_addr = addr;

    if ( tls->current >= NTOA_BUF_COUNT ) {
        debug( 10, "UNET: Cycled buffer\n" );
        tls->current = 0;
    }
    
    strncpy( tls->buf_array[tls->current], inet_ntoa( i ), INET_ADDRSTRLEN );
    
    /* Increment after using */
    return tls->buf_array[tls->current++];
}

static unet_tls_t* _tls_get( void )
{
    unet_tls_t* tls = NULL;

    if (( tls = uthread_tls_get( _unet.tls_key, sizeof( unet_tls_t ), _tls_init )) == NULL ) {
        return errlog_null( ERR_CRITICAL, "uthread_get_tls\n" );
    }
    
    return tls;
}

static int         _tls_init( void* buf, size_t size )
{
    unet_tls_t* tls = buf;

    if (( size != sizeof( unet_tls_t )) || ( tls == NULL )) return errlogargs();
    
    /* Initialize to zero */
    tls->current = 0;

    return 0;
}

u16     unet_in_cksum ( u16* addr, int len)
{
	int nleft = len;
	u_int16_t *w = addr;
	u_int32_t sum = 0;
	u_int16_t answer = 0;

	/*
	 * Our algorithm is simple, using a 32 bit accumulator (sum), we add
	 * sequential 16 bit words to it, and at the end, fold back all the
	 * carry bits from the top 16 bits into the lower 16 bits.
	 */
	while (nleft > 1)  {
		sum += *w++;
		nleft -= 2;
	}

	/* mop up an odd byte, if necessary */
	if (nleft == 1) {
		answer=0;
		*(u_char *)(&answer) = *(u_char *)w ;
		sum += answer;
	}

	/* add back carry outs from top 16 bits to low 16 bits */
	sum = (sum >> 16) + (sum & 0xffff);	/* add hi 16 to low 16 */
	sum += (sum >> 16);			/* add carry */
	answer = ~sum;				/* truncate to 16 bits */
	return(answer);
}

u16     unet_udp_sum_calc ( u16 len_udp, u8 src_addr[],u8 dest_addr[], u8 buff[] )
{
    return _unet_sum_calc( len_udp, src_addr, dest_addr, buff, IPPROTO_UDP );
}

u16     unet_tcp_sum_calc ( u16 len_tcp, u8 src_addr[],u8 dest_addr[], u8 buff[] )
{
    return _unet_sum_calc( len_tcp, src_addr, dest_addr, buff, IPPROTO_TCP );
}

static u16 _unet_sum_calc ( u16 len, u8 src_addr[],u8 dest_addr[], u8 buff[], u8 proto )
{
    u16 word16;
    u32 sum;	
    int i;
	
	sum=0;

    /* Handle the case where the length is odd */
	for ( i = 0 ; i < ( len & (~1)) ; i += 2 ) {
		word16 =((buff[i]<<8)&0xFF00)+(buff[i+1]&0xFF);
		sum = sum + word16;
	}
    
    /* If it is an odd length, pad properly */
    if ( len & 1 ) {
        word16 = ( buff[i] << 8) & 0xFF00;
        sum+= word16;
    }

	for (i=0;i<4;i=i+2){
		word16 =((src_addr[i]<<8)&0xFF00)+(src_addr[i+1]&0xFF);
		sum = sum + word16;	
	}
	for (i=0;i<4;i=i+2){
		word16 =((dest_addr[i]<<8)&0xFF00)+(dest_addr[i+1]&0xFF);
		sum = sum + word16; 	
	}
	sum = sum + proto + len;

    while (sum>>16)
		sum = (sum & 0xFFFF)+(sum >> 16);
		
	sum = ~sum;

    return htons((u16) sum);
}

static int _unet_startlisten (struct sockaddr_in* addr)
{
    int one         = 1;
    int listen_sock = 0;
    
    if ((listen_sock = socket(AF_INET, SOCK_STREAM, IPPROTO_TCP))<0) 
        return -1;

    setsockopt(listen_sock, SOL_SOCKET,SO_REUSEADDR,(char *)&one,sizeof(one));

    if (bind(listen_sock, (struct sockaddr*)addr, sizeof(struct sockaddr))<0) 
        return -1;
    
    if (listen(listen_sock, __QUEUE_LENGTH) < 0) 
        return -1;

    return listen_sock;
}

/**
 * Disable blocking on a filedescriptor
 */
int unet_blocking_disable( int fd )
{
    return _unet_blocking_modify( fd, ~ENABLE_BLOCKING );
}

/**
 * Enable blocking on a filedescriptor
 */
int unet_blocking_enable( int fd )
{
    return _unet_blocking_modify( fd, ENABLE_BLOCKING );
}

/**
 * Initialize a sockaddr structure
 */
int unet_sockaddr_in_init( struct sockaddr_in* sockaddr, in_addr_t host, u_short port )
{
    if ( sockaddr == NULL ) return errlogargs();
        
    sockaddr->sin_family = AF_INET;
    sockaddr->sin_port   = htons( port );
    memcpy( &sockaddr->sin_addr, &host, sizeof(in_addr_t));

    return 0;
}



