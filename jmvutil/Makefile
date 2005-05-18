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

sub_name=jmvutil

INSTALL_LIB=TRUE

include ../buildtools/Makefile.in

lib_file_name=lib$(sub_name).a

VPATH=$(src_path):$(local_lib_path)

## C Configuration
INC       +=  $(java_include_base) $(java_include_base)/linux
LIBS      +=  mvutil
DEFINES   += 

DEBUG_PKG  = $(JMVUTIL_PKG)

c_src=jerror.c jmvutil.c

objects=$(c_src:.c=.o)

install_includes=include/libnetcap.h

## These are different since they must be installed into mvutil
jmvutil_includes=$(patsubst %.c,$(src_path)/%.h,$(c_src))
jmvutil_build_inc_dir=$(build_inc_path)/$(sub_name)

all: $(lib_file_name)

build: $(lib_file_name) $(jmvutil_build_inc_dir).$(dir_target) build.default
	@for i in $(jmvutil_includes) ; do \
		echo "==> $$i => $(jmvutil_build_inc_dir)" ; \
		$(INSTALL_DATA) -m $(inc_mode) $$i $(jmvutil_build_inc_dir) ; \
	done

$(lib_file_name): $(src_path)/version.h $(objects)

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
