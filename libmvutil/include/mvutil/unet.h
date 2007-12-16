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
#ifndef __UNET_H
#define __UNET_H

#include <sys/types.h>
#include <sys/socket.h>
#include <arpa/inet.h>
#include <poll.h>

/**
 * opens a TCP socket for listening on listenport on the local machine only
 * this socket will not be remotely visible
 * returns the fd or -1 on error 
 */
int     unet_startlisten_local(u_short listenport);

/**
 * opens a TCP socket for listening on listenport
 * returns the fd or -1 on error 
 */
int     unet_startlisten      (u_short listenport);

int     unet_startlisten_addr (u_short listenport, struct in_addr* bind_addr );

/**
 * Opens up a range of consecutive TCP ports for listening.
 * count: The number of ports to open
 * port:  This will be set to the first port that is open in the range.
 * fds:   Array of filedescriptors, this should contain at least <code>count</code> items.
 * ip: IP to bind to or NULL to bind to 0.0.0.0.
 * If there is an error, or it is unable to open the range in a 
 * reasonable number of attempts, this returns -1.
 */
int     unet_startlisten_on_portrange(  int count, u_short* port, int* fds, char* ip );

/**
 * opens a UDP socket for listening on listenport
 * returns the fd or -1 on error 
 */
int     unet_startlisten_udp (u_short listenport);

/**
 * opens a TCP socket for listening on any port
 * sets port to port and insock to fd
 * returns 0 or -1 on error 
 */
int     unet_startlisten_on_anyport_tcp( u_short* port, int* fd, struct in_addr* bind_addr );

/**
 * opens a UDP socket for listening on any port
 * sets port to port and insock to fd
 * returns 0 or -1 on error 
 */
int     unet_startlisten_on_anyport_udp (u_short* port, int* fd);

/**
 * calls accept and listening fd
 * returns the fd or -1 on error 
 */
int     unet_accept (int listensocket);

/**
 * opens a TCP connection to the given address and port
 * returns the fd or -1 on error
 */
int     unet_open (in_addr_t* destaddr, u_short destport);

/**
 * reads a line from fd into buf
 * if fd closes returns 0 with partial read in buf
 * if an error occurs return -1 with partial read in buf
 * otherwise 0 is returned
 * the number of bytes read is put in retval
 * if fd is nonblocking it will return -1 with errno = EAGAIN if there was not a fullline
 */
int     unet_readln           (int fd, char * buf, int bufsize, int* retval);

/**
 * takes a timeout in milliseconds, 
 * returns -1 on error, -2 on timeout, 0 on EOF, or number of bytes read 
 */
ssize_t unet_read_timeout (int fd, void* buf, size_t count, int millitime);

/**
 * loops over a write until the write is complete, an error occurs, or numloop 
 * iterations occurs.
 * returns -1 on error (this can be after a partial write) and 0 otherwise
 * the number of written bytes is put in count
 */
ssize_t unet_write_loop   (int fd, void* buf, size_t* count, int numloop);

/**
 * loops over a read until the read is complete, an error occurs, or numloop 
 * iterations occurs.
 * returns -1 on error (this can be after a partial read) and 0 otherwise
 * the number of read bytes is put in count
 */
ssize_t unet_read_loop    (int fd, void* buf, size_t* count, int numloop);

/**
 * dumps the status of poll's fdset to stderr
 */
int     unet_poll_dump    (struct pollfd* fdset, int size);

/**
 * resets a tcp connection without closing the file descriptor.
 */
int     unet_reset( int sock );
 
/**
 * resets a tcp connection and closes the associated file descriptor.
 */
int     unet_reset_and_close (int sock);

/**
 * Close a file descriptor and set its value to negative one.
 */
int     unet_close( int* fd_ptr );

#define NTOA_BUF_COUNT 16
char*   unet_inet_ntoa (in_addr_t addr);

/**
 * Start an inet_ntoa loop.  Call unet_reset_inet_ntoa before calling unet_next_inet_ntoa.
 * statement, and then call unet_next_inet_ntoa up to NTOA_BUF_COUNT times..  This way you can
 * call inet_ntoa up to NTOA_BUF_COUNT times in the same print or debug statement without
 * getting the same buffer.  Do not combine unet_inet_ntoa and unet_next_inet_ntoa, since C
 * does not guarantee the order that arguments are evaluated.
 * 
 * eg. 
 * unet_reset_inet_ntoa();
 * printf( "%s -> %s", unet_next_inet_ntoa( cli_addr ), unet_next_inet_ntoa( srv_addr ));
 * will work properly.
 */
void    unet_reset_inet_ntoa( void );


char*   unet_next_inet_ntoa( in_addr_t addr );

/**
 * computes the checksum
 */
u_int16_t unet_in_cksum (u_int16_t *addr, int len);

/**
 * computes the TCP checksum
 */
u_int16_t unet_tcp_sum_calc ( u_int16_t len_tcp, u_int8_t src_addr[], u_int8_t dest_addr[], u_int8_t buff[] );

/**
 * computes the UDP checksum
 */
u_int16_t unet_udp_sum_calc ( u_int16_t len_udp, u_int8_t src_addr[], u_int8_t dest_addr[], u_int8_t buff[] );

/**
 * Disable blocking on a filedescriptor
 */
int unet_blocking_disable( int fd );

/**
 * Enable blocking on a filedescriptor
 */
int unet_blocking_enable( int fd );

/**
 * Initialize a sock addr structure
 * host: Address to configure in network byte order.
 * port: Port to configure in host byte order.
 */
int unet_sockaddr_in_init( struct sockaddr_in* sockaddr, in_addr_t host, u_short port );


#endif
