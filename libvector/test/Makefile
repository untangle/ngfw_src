# $Id: Makefile,v 1.6 2005/01/24 04:11:53 dmorris Exp $
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
DEFINES       = -D_GNU_SOURCE -D_REENTRANT  
WARNINGS      = -Wall
OPTIMIZATIONS = -fomit-frame-pointer
INCLUDEDIR    = -I../include -I${BUILD_ROOT}/usr/include -I/usr/local/include/libxml2 -I/usr/include/libxml2 -I/opt/jrockit-j2sdk1.4.2_04/include -I/opt/jrockit-j2sdk1.4.2_04/include/linux
LDDIR         = -L../lib -L${BUILD_ROOT}/usr/lib 
LIBS          = -lvector -lmvutil -pthread 

DEFINES  +=  -DDEBUG_ON -DDEBUG_PKG=205
FLAGS    += -g -ggdb

CFLAGS      =  $(FLAGS) $(DEFINES) $(WARNINGS) $(OPTIMIZATIONS) $(INCLUDEDIR) $(LDDIR) 
LIB = $(LIBDIR)/libvector.a

BINS = test1

all:	$(BINS)

test1:  $(LIB) test1.c 
	${CC} $@.c ${UTIL_OBJS} ${LDLIBS} ${CFLAGS} ${LIBS} -o $@

$(LIB):
	make -C./../ build

clean:
	@${RM} -f `find . -name "*.class"`;  \
	${RM} -f `find . -name "*.o"`;  \
	${RM} -f `find . -name "*.so"`;  \
	${RM} -f `find . -name "*.core"`;  \
	${RM} -f core.*;  \
	${RM} -f ${BINS}

distclean:
	${RM} -f `find . -name "*~"`;  \
	${RM} -f `find . -name "#*"`;  \
	${RM} -f TAGS;

tags: 	clean
	find . -name "*.[hc]" -print | xargs etags -a
