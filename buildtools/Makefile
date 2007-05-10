DISTRIBUTION ?= `cat RELEASE_CODENAME`

checkroot:
	@if [ "$$UID" = "0" ]; then \
	  echo "You can't be root to build packages"; \
	  exit 1; \
	fi

clean: checkroot
	fakeroot debian/rules clean

version: checkroot
	buildtools/incVersion.sh $(DISTRIBUTION)
	# so we can use that later to find out what to upload if needs be
	dpkg-parsechangelog | awk '/Version: / { print $$2 }' >| debian/version

source: checkroot
	tar cz --exclude="*stamp*" \
		--exclude=".svn" \
		--exclude="debian" \
		--exclude="todo" \
		--exclude="staging" \
		--exclude="dist" \
		-f ../mvvm_`perl -npe 's/-.*//' debian/version`.orig.tar.gz .

pkg: checkroot
	# FIXME: sign packages when we move to apt 0.6
	/usr/bin/debuild -i -us -uc
	svn revert debian/changelog

release: checkroot
	dput -c buildtools/dput.cf mephisto ../mvvm_*`cat debian/version`*.changes

.PHONY: clean checkroot version source pkg release