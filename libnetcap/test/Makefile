# $Id: Makefile,v 1.4 2005/01/24 04:11:53 dmorris Exp $
# 

BUILD_ROOT ?= $(shell echo ~/build)
DESTDIR     = ${BUILD_ROOT}

CP=/bin/cp
RM=/bin/rm
RMDIR=/bin/rmdir
MKDIR=/bin/mkdir
CC=gcc
LIBDIR=../lib

FLAGS         =
DEFINES       = -D_GNU_SOURCE -D_REENTRANT -DNETCAP_IPTABLES 
WARNINGS      = -Wall
OPTIMIZATIONS = -fomit-frame-pointer
INCLUDEDIR    = -I../include -I${BUILD_ROOT}/usr/include -I/usr/local/include/libxml2 -I/usr/include/libxml2
LDDIR         = -L../lib -L${BUILD_ROOT}/usr/lib 
LIBS          = -lnetcap -pthread -lmvutil -lipq -lm -lxml2 #-lefence 

DEFINES  +=  -DDEBUG_ON -DDEBUG_PKG=1
FLAGS    += -g -ggdb 

CFLAGS      =  $(FLAGS) $(DEFINES) $(WARNINGS) $(OPTIMIZATIONS) $(INCLUDEDIR) $(LDDIR) $(LIBS)

LIB = $(LIBDIR)/libnetcap.a

BINS = test_trie test1 test2 test3 test4 test_udp test_udp_mark

all:	$(BINS)

run: all

test1: test1.c $(LIB)
	${CC} $@.c ${UTIL_OBJS} ${LDLIBS} ${CFLAGS} -o $@

test2: test2.c $(LIB)
	${CC} $@.c ${UTIL_OBJS} ${LDLIBS} ${CFLAGS} -o $@

test3: test3.c $(LIB)
	${CC} $@.c ${UTIL_OBJS} ${LDLIBS} ${CFLAGS} -o $@

test4: test3.c $(LIB)
	${CC} test3.c ${UTIL_OBJS} ${LDLIBS} -lcrypto -DWITHRC4 ${CFLAGS} -o $@

test_udp: test_udp.c $(LIB)
	${CC} test_udp.c ${UTIL_OBJS} ${LDLIBS} ${CFLAGS} -o $@

test_udp_mark: test_udp_mark.c $(LIB)
	${CC} test_udp_mark.c ${UTIL_OBJS} ${LDLIBS} ${CFLAGS} -o $@

test_broadcast: test_broadcast.c $(LIB)
	${CC} test_broadcast.c ${UTIL_OBJS} ${LDLIBS} ${CFLAGS} -o $@

test_queue: test_queue.c $(LIB)
	${CC} test_queue.c ${UTIL_OBJS} ${LDLIBS} ${CFLAGS} -o $@

test_trie: test_trie.c $(LIB)
	${CC} $@.c -I../src/ ${UTIL_OBJS} ${LDLIBS} ${CFLAGS} -o $@

test_shield_cfg: test_shield_cfg.c $(LIB)
	${CC} $@.c -I../src/ ${UTIL_OBJS} ${LDLIBS} ${CFLAGS} -o $@

$(LIB):
	make -C./../ build

clean:
	@${RM} -f `find . -name "*.class"`;  \
	${RM} -f `find . -name "*.o"`;  \
	${RM} -f `find . -name "*.core"`;  \
	${RM} -f core;  \
	${RM} -f TAGS;  \
	${RM} -f ${BINS}


distclean:
	${RM} -f `find . -name "*~"`;  \
	${RM} -f `find . -name "#*"`;  

tags: 	clean
	find . -name "*.[hc]" -print | xargs etags -a
