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

sub_name=mvutil

INSTALL_LIB=TRUE

include ../buildtools/Makefile.in

lib_file_name=lib$(sub_name).a

VPATH=$(src_path):$(local_lib_path)

## C Configuration
DEBUG_PKG  = $(MVUTIL_PKG)

## All defaults

c_src=libmvutil.c errlog.c debug.c hash.c lock.c list.c mailbox.c utime.c unet.c \
      uthread.c usystem.c mvpoll.c mvsem.c

objects=$(c_src:.c=.o)

install_includes=$(src_path)/libmvutil.h

## These are different since they must be installed into mvutil
mvutil_includes=$(patsubst %.c,$(src_path)/%.h,$(c_src))
mvutil_build_inc_dir=$(build_inc_path)/$(sub_name)

all: $(lib_file_name)

## Do not install libmvutil.h into mvutil
build: $(lib_file_name) $(mvutil_build_inc_dir).$(dir_target) build.default
	@for i in $(mvutil_includes) ; do \
		echo "==> $$i => $(mvutil_build_inc_dir)" ; \
		$(INSTALL_DATA) -m $(inc_mode) $$i $(mvutil_build_inc_dir) ; \
	done

$(lib_file_name): $(src_path)/version.h $(objects)

## These all use the defaults defined in the parent directory.
clean: clean.default
	@$(RM) $(local_lib_path)/$(lib_file_name)

test: test.default

distclean: distclean.default

tags: tags.default

##################################################### OLD Makefile

PREFIX      = /usr/
BIN_PREFIX  = ${PREFIX}bin/
INC_PREFIX  = ${PREFIX}include/
INCS_PREFIX = ${PREFIX}include/mvutil/
LIB_PREFIX  = ${PREFIX}lib/

install: libmvutil 
	@if ! diff $(DESTDIR)$(LIB_PREFIX)$(LIBNAME).a $(LIB) &>/dev/null  ; then \
		echo -n "Installing $(LIB) ... " ;\
		$(ENSUREDIR) $(DESTDIR)$(PREFIX) 755 ;\
		$(ENSUREDIR) $(DESTDIR)$(LIB_PREFIX) 755 ;\
		$(ENSUREDIR) $(DESTDIR)$(INC_PREFIX) 755 ;\
		rm -f $(DESTDIR)$(LIB_PREFIX)$(LIBNAME).a ;\
		$(INSTALL_DATA) -m 644 $(MAIN_INC) $(DESTDIR)$(INC_PREFIX) ;\
		for i in $(UTIL_INCS) ; do $(INSTALL_DATA) -m 644 $$i $(DESTDIR)$(INCS_PREFIX) ; done ;\
		$(INSTALL_DATA) -m 644 $(LIB) $(DESTDIR)$(LIB_PREFIX) ;\
		echo "Done." ;\
	fi

#### ???? 

pkg: clean
	fakeroot debian/rules binary

version:
#	cat ./VERSION | perl -pe 's/(\d+)(\D*)$/($1+1).$2/e' > ./VERSION
	dch -i "auto build" ; \
	cat ./debian/changelog | head -1 | awk '{print $$2}' | sed -e 's/[\)\(]*//g' > ./VERSION

release: version pkg 
	@if [ "`hostname`" = "bebe" ] ; then \
		echo "libmvutil " `cat VERSION` " released." | mail -s release pkgs@metavize.com ; \
		cd ../pkgs/ ; sudo ./deb-add.sh /var/www/`whoami`/ ../libmvutil*deb &> /dev/null ; sudo ./deb-scan.sh /var/www/`whoami`/ &> /dev/null ; \
	else echo "Must be run on bebe"; \
	fi

#### END OF ??? 



