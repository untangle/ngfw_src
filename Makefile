include buildtools/Makefile.in

## The following rules are only valid for the base makefile, make sure to define sub_name before
## including this makefile

CLIBS=libmvutil libnetcap jnetcap libvector jvector

lib_name       = alpine
lib_file_name  = lib$(lib_name).so
packages      ?= "all"



%: %.pre_base %.post_base ;

all: chk build

## This limits the rules that are caught by the wildcard above
distclean.pre_base clean.pre_base test.pre_base:
	@echo $(build_dist)
	@echo "==> ant $(@:.pre_base=)"
	@$(ANT) $(@:.pre_base=)
	@for i in $(CLIBS) ; do \
		make ANT_INVOKED=TRUE -C ./$$i $(@:.pre_base=); \
		if [ "$$?" != "0" ] ; then exit ; fi \
	done

build.pre_base: 
	@echo "==> ant"
	@$(ANT)
	@for i in $(CLIBS) ; do \
		make ANT_INVOKED=TRUE -C ./$$i $(@:.pre_base=); \
		if [ "$$?" != "0" ] ; then exit ; fi \
	done

build.post_base: $(lib_file_name) ;

tags.pre_base:
	@for i in $(CLIBS) ; do \
		make ANT_INVOKED=TRUE -C ./$$i $(@:.pre_base=); \
		if [ "$$?" != "0" ] ; then exit ; fi \
	done

tags.post_base:
	@echo "==> TAGS: Making tags for alpine"
	@find . -name "*.[hc]" -print | xargs etags -a

clean.post_base:
	@rm -f $(lib_file_name)
	@sudo rm -rf $(build_dist)/usr/share/metavize
	@fakeroot debian/rules clean


## This is the catch all for all of the rules that do not execute anything after calling the
## sub rule
%.post_base: ;

chk:
	@echo "alpine_root  = "$(alpine_root)
	@echo "build_dist   = "$(build_dist)
	@echo "build_prefix = "$(build_prefix)
	@echo

alpine_libs=jnetcap netcap vector jvector mvutil
lib_deps = $(patsubst %,$(build_lib_path)/lib%.a,$(alpine_libs))

$(lib_file_name): LIBS = xml2
$(lib_file_name): $(lib_deps)
	@echo "==> gcc ($(alpine_libs)) -> $@"
	@$(CC) $(CFLAGS) $(LIBS_FLAGS) -Wl,--whole-archive  $(lib_deps) -lipq -Wl,--no-whole-archive \
		-shared -o $@
	@$(STRIPCMD) $@
	@echo "==> cp $@ -> $(build_lib_path)/$@"
	@cp $@ $(build_lib_path)/$@ # FIXME

pkgs: chk
	@echo "==> pkgs"
	@rm -f ./*stamp
	@fakeroot debian/rules binary

release: pkgs
	@buildtools/incVersion.sh "$(packages)"
	@buildtools/release.sh    "$(packages)"

src: clean
	@echo "==> src"
	rm -rf src
	find $(CLIBS) mvvm tran -regex ".*/output" -prune , -type d -name "com" | while read i ; do cp -rl $$i ./src/ ; done &>/dev/null || true
	find $(CLIBS) mvvm tran -type d -name "resources" | while read i ; do cp -rl $$i ./src/ ; done &>/dev/null || true
	find $(CLIBS) mvvm tran -type d -name "hier" | while read i ; do cp -rl $$i ./src/ ; done  &>/dev/null || true
	find ./src/ -type d -name "CVS" | xargs rm -rf 

