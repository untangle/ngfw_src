#
# Copyright (c) 2003 Metavize Inc.
# All rights reserved.
#
# This software is the confidential and proprietary information of
# Metavize Inc. ("Confidential Information").  You shall
# not disclose such Confidential Information.
#
# $Id: Makefile,v 1.11 2005/01/24 05:14:16 dmorris Exp $
#


sub_name=vector

INSTALL_LIB=TRUE

include ../buildtools/Makefile.in

lib_file_name=lib$(sub_name).a

VPATH=$(src_path):$(local_lib_path)

## C Configuration
LIBS      += mvutil
DEFINES   += -DVECTOR_IPTABLES 

DEBUG_PKG  = $(VECTOR_PKG)

c_src=vector.c event.c relay.c fd_sink.c fd_source.c

objects=$(c_src:.c=.o)

## These are different since they must be installed into mvutil
vector_includes=$(patsubst %.c,$(src_path)/%.h,$(c_src)) $(patsubst %, $(src_path)/%,source.h sink.h)
vector_build_inc_dir=$(build_inc_path)/$(sub_name)

install_includes=include/libvector.h

all: $(lib_file_name)

build: $(lib_file_name) $(vector_includes) $(vector_build_inc_dir).$(dir_target) build.default
	@for i in $(vector_includes) ; do \
                echo "==> $$i => $(vector_build_inc_dir)" ; \
                $(INSTALL_DATA) -m $(inc_mode) $$i $(vector_build_inc_dir) ; \
        done

$(lib_file_name): $(install_includes) $(src_path)/version.h $(objects)

## These all use the defaults defined in the parent directory.
clean: clean.default
	@$(RM) $(local_lib_path)/$(lib_file_name)

test: test.default

distclean: distclean.default

tags: tags.default

################################################ END OF NEW MAKEFILE

PREFIX      = /usr/
BIN_PREFIX  = ${PREFIX}bin/
INC_PREFIX  = ${PREFIX}include/
INCS_PREFIX = ${PREFIX}include/vector/
LIB_PREFIX  = ${PREFIX}lib/

binary:
	fakeroot debian/rules binary

pkg: clean
	fakeroot debian/rules binary

version:
	dch -i "auto build" ; \
	cat ./debian/changelog | head -1 | awk '{print $$2}' | sed -e 's/[\)\(]*//g' > ./VERSION

release: version pkg 
	@if [ "`hostname`" = "bebe" ] ; then \
		echo "libvector " `cat VERSION` " released." | mail -s release pkgs@metavize.com ; \
		cd ../pkgs/ ; sudo ./deb-add.sh ../libvector*deb &> /dev/null ; sudo ./deb-scan.sh &> /dev/null ; \
	else echo "Must be run on bebe"; \
	fi