#
# Copyright (c) 2003 Metavize Inc.
# All rights reserved.
#
# This software is the confidential and proprietary information of
# Metavize Inc. ("Confidential Information").  You shall
# not disclose such Confidential Information.
#
# $Id: Makefile,v 1.5 2005/01/24 04:11:53 dmorris Exp $
#

ifeq ($(origin NOEFENCE), undefined)
  EFENCE:=-lefence
else
  EFENCE:=
endif

SRCDIR      = ../src/
LIBDIR      = ../lib/
UTILDIR     = ../util/

CC        := gcc
CFLAGS    += -g -ggdb -D_GNU_SOURCE -Wall -DDEBUG_ON -DDEBUG_PKG=1 -I../src/ -L../lib/ -lmvutil -pthread ${EFENCE}

BINS        = test_lock test_ht test_list test_mailbox test_mvpoll

TEST_SRCS = \
	${SRCDIR}/errlog.c    \
	${SRCDIR}/debug.c     \
	${SRCDIR}/unet.c  \
	${SRCDIR}/lock.c  \
	${SRCDIR}/list.c  \
	${SRCDIR}/hash.c  \
	${SRCDIR}/mailbox.c  \

TEST_OBJS = ${TEST_SRCS:.c=.o}
TEST_INCLUDES  +=  -I../src/util
TEST_LIBS      +=  -L/usr/local/lib -L/usr/local/ -pthread ${EFENCE}

all: init test_lock test_ht test_list test_mailbox test_mvpoll

init:
	@if ! [ -d tmp ] ; then mkdir tmp ; fi

test: all

run: run-lock-tests run-ht-tests run-list-tests run-mailbox-tests
	@echo "Done."

clean:
	@${RM}  `find . -type f -name "*.class"`; \
	${RM}  `find . -type f -name "*.o"`;     \
	${RM}  `find . -type f -name "*.core"`;  \
	${RM}  `find . -type f -name "core"`;    \
	${RM}  `find . -type f -name "*~"`;      \
	${RM}  `find . -type f -name "#*"`;      \
	${RM}  `find . -type f -name ".#*"`;     \
	${RM}  `find . -type f -name "TAGS"`;    \
	${RM} make ${BINS};                       
	@if [ -d tmp ] ; then rm -rf tmp ; fi


$(LIB):
	make -C./../ build

test_lock: test_lock.c $(LIBDIR)/libmvutil.a
	${CC} test_lock.c ${TEST_INCLUDES} ${TEST_LIBS} ${CFLAGS} ${LIBRARIES} -o test_lock

run-lock-tests: test_lock
	@./test_lock &> ./tmp/test_lock.out ;\
	if [ $$? -eq 0 ] ; \
	then echo "---- Test test_lock Passed";       \
	else echo "#### Test test_lock FAILED";       \
	fi

test_ht: test_ht.c $(LIBDIR)/libmvutil.a
	${CC} test_ht.c ${TEST_INCLUDES} ${TEST_LIBS} ${CFLAGS} ${LIBRARIES} -o test_ht

run-ht-tests: test_ht
	@./test_ht &> ./tmp/test_ht.out ;\
	if [ $$? -eq 0 ]; \
	then echo "---- Test test_ht Passed";       \
	else echo "#### Test test_ht FAILED";       \
	fi

test_list: test_list.c $(LIBDIR)/libmvutil.a
	${CC} test_list.c ${TEST_INCLUDES} ${TEST_LIBS} ${CFLAGS} ${LIBRARIES} -o test_list

run-list-tests: test_list
	@./test_list &> ./tmp/test_list.out ;\
	if [ $$? -eq 0 ]; \
	then echo "---- Test test_list Passed";       \
	else echo "#### Test test_list FAILED";       \
	fi

test_mailbox: test_mailbox.c $(LIBDIR)/libmvutil.a
	${CC} test_mailbox.c ${TEST_INCLUDES} ${TEST_LIBS} ${CFLAGS} ${LIBRARIES} -o test_mailbox

run-mailbox-tests: test_mailbox
	@./test_mailbox &> ./tmp/test_mailbox.out ;\
	if [ $$? -eq 0 ]; \
	then echo "---- Test test_mailbox Passed";       \
	else echo "#### Test test_mailbox FAILED";       \
	fi

test_mvpoll: test_mvpoll.c $(LIBDIR)/libmvutil.a
	${CC} test_mvpoll.c ${TEST_INCLUDES} ${TEST_LIBS} ${CFLAGS} ${LIBRARIES} -o test_mvpoll


