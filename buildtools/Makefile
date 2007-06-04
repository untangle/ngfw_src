DISTRIBUTION ?= $(USER)
BUILDTOOLS_DIR = $(shell dirname $(MAKEFILE_LIST))

checkroot:
	@if [ "$$UID" = "0" ]; then \
	  echo "You can't be root to build packages"; \
	  exit 1; \
	fi

clean: checkroot
	svn revert debian/changelog
	fakeroot debian/rules clean

version: checkroot
	svn revert debian/changelog
	sh $(BUILDTOOLS_DIR)/incVersion.sh $(DISTRIBUTION)
	# so we can use that later to find out what to upload if needs be
	dpkg-parsechangelog | awk '/Version: / { print $$2 }' >| debian/version

source: checkroot
	tar cz --exclude="*stamp*" \
		--exclude=".svn" \
		--exclude="debian" \
		--exclude="todo" \
		--exclude="staging" \
		--exclude="dist" \
		-f ../`dpkg-parsechangelog | awk '/Source: / { print $$2 }'`_`perl -npe 's/-.*//' debian/version`.orig.tar.gz ../`basename $$(pwd)`

pkg: checkroot
	# FIXME: sign packages when we move to apt 0.6
	# FIXME: don't clean before building !!!
	/usr/bin/debuild -i -us -uc
	svn revert debian/changelog

release: checkroot
	# FIXME: come on now, let's be real !!!
	if [ `basename $$(pwd)` != "alpine" ] ; then dput -c $(BUILDTOOLS_DIR)/dput.cf mephisto ../`basename $$(pwd)`_`cat debian/version`*.changes ; else dput -c $(BUILDTOOLS_DIR)/dput.cf mephisto ../mvvm_`cat debian/version`*.changes ; fi

.PHONY: checkroot clean version source pkg release
