#
# Copyright (c) 2003 Metavize Inc.
# All rights reserved.
#
# This software is the confidential and proprietary information of
# Metavize Inc. ("Confidential Information").  You shall
# not disclose such Confidential Information.
#
# $Id: Makefile,v 1.8 2005/01/24 05:14:16 dmorris Exp $
#

sub_name=netcap

INSTALL_LIB=TRUE

include ../buildtools/Makefile.in

lib_file_name=lib$(sub_name).a

VPATH=$(src_path):$(local_lib_path)

## C Configuration
INC       += /usr/local/include/libxml2 /usr/include/libxml2
LIBS      += netcap xml2 mvutil ipq
DEFINES   += -DNETCAP_IPTABLES 

DEBUG_PKG  = $(NETCAP_PKG)

c_src=netcap_version.c netcap_traffic.c netcap_subscriptions.c netcap_rdr.c netcap_session.c \
      netcap_hook.c netcap_server.c netcap_init.c netcap_thread.c netcap_pkt.c netcap_queue.c \
      netcap_udp.c netcap_tcp.c netcap_tcp_srv_complete.c netcap_tcp_cli.c \
      netcap_icmp.c netcap_sesstable.c netcap_interface.c netcap_udp_session.c netcap_tcp_session.c \
      netcap_antisubscribe.c netcap_trie.c netcap_trie_item.c netcap_trie_level.c netcap_trie_root.c \
      netcap_trie_lru.c netcap_trie_base.c netcap_shield.c netcap_shield_cfg.c netcap_sched.c \
      netcap_load.c netcap_tuple.c netcap_icmp_msg.c netcap_ip.c 

objects=$(c_src:.c=.o)

install_includes=include/libnetcap.h

all: $(lib_file_name)

build: $(lib_file_name) build.default

$(lib_file_name): include/libnetcap.h $(src_path)/version.h $(objects)

## These all use the defaults defined in the parent directory.
clean: clean.default
	@$(RM) $(local_lib_path)/$(lib_file_name)

test: test.default

distclean: distclean.default

tags: tags.default

################################################## END OF NEW MAKEFILE
DESTDIR     = ${BUILD_ROOT}

PREFIX      = /usr/
INC_PREFIX  = ${PREFIX}include/
LIB_PREFIX  = ${PREFIX}lib/
MAN_PREFIX  = ${PREFIX}share/man/man3/
LIB         = lib/$(LIBNAME).a
MAN         = doc/$(LIBNAME).3
INCLUDE     = include/$(LIBNAME).h

install: libnetcap
	@if ! diff $(DESTDIR)$(LIB_PREFIX)$(LIBNAME).a $(LIB) &>/dev/null  ; then \
		echo -n "Installing $(LIB) ... " ;\
		$(ENSUREDIR) $(DESTDIR)$(PREFIX) 755 ;\
		$(ENSUREDIR) $(DESTDIR)$(LIB_PREFIX) 755 ;\
		$(ENSUREDIR) $(DESTDIR)$(INC_PREFIX) 755 ;\
		$(ENSUREDIR) $(DESTDIR)$(MAN_PREFIX) 755 ;\
		rm -f $(DESTDIR)$(LIB_PREFIX)$(LIBNAME).a ;\
		$(INSTALL_DATA) -m 644 $(LIB) $(DESTDIR)$(LIB_PREFIX) ;\
		$(INSTALL_DATA) -m 644 $(INCLUDE) $(DESTDIR)$(INC_PREFIX) ;\
		$(INSTALL_DATA) -m 644 $(MAN) $(DESTDIR)$(MAN_PREFIX) ;\
		echo "Done." ;\
	fi

## ???
binary:
	fakeroot debian/rules binary

pkg: clean
	fakeroot debian/rules binary

version:
	dch -i "auto build" ; \
	cat ./debian/changelog | head -1 | awk '{print $$2}' | sed -e 's/[\)\(]*//g' > ./VERSION

release: version pkg 
	@if [ "`hostname`" = "bebe" ] ; then \
		echo "libnetcap " `cat VERSION` " released." | mail -s release pkgs@metavize.com ; \
		cd ../pkgs/ ; sudo ./deb-add.sh ../libnetcap*deb &> /dev/null ; sudo ./deb-scan.sh &> /dev/null ; \
	else echo "Must be run on bebe"; \
	fi

## end of ???
