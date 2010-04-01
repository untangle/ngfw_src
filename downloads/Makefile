include Makefile.in

OUTPUT_DIRECTORY=output

CP=cp
RM=rm
RM_DIR=$(RM) -rf
MK_DIR=mkdir -p

TAR=tar
UNZIP=unzip

###############################################################################
## THIS IS THE MASTER LIST, IN FOUR SECTIONS.
#* EDIT HERE WHEN ADDING, UPGRADING, OR REMOVING.
###############################################################################
tarballs_gz=apache-ant-1.6.5.tar.gz commons-beanutils-1.7.0.tar.gz \
	commons-digester-1.7.tar.gz cpptasks-1.0b3.tar.gz \
	hibernate-3.2.4.sp1.tar.gz hibernate-annotations-3.3.0.GA.tar.gz \
	apache-tomcat-5.5.26.tar.gz jfreechart-1.0.13.tar.gz \
	logging-log4j-1.2.14.tar.gz trove-1.0.2.tar.gz xdoclet-lib-1.2.3.tar.gz \
	bcel-5.2.tar.gz c3p0-0.9.1.2.tar.gz velocity-1.4.tar.gz \
	jcifs_1.2.9.tar.gz commons-fileupload-1.1.tar.gz commons-io-1.1.tar.gz \
	commons-httpclient-3.0.tar.gz AjaxTK-3.1.1_GA_394-src.tar.gz \
	commons-codec-1.3.tar.gz tightvnc-1.2.9_javabin.tar.gz \
	je-3.2.74.tar.gz xmlrpc-current-bin.tar.gz slf4j-1.4.3.tar.gz \
	python-jsonrpc-r19.tar.gz dnsjava-2.0.6.tar.gz \
	jradius-1.1.0-client.tar.gz

tarballs_bz2=ant-contrib-1.0b2.tar.bz2 jaf-1.0.2.tar.bz2 \
	junit3.8.1.tar.bz2 junit4.1.tar.bz2  \
	netbeans-3.5.tar.bz2 postgres-jdbc-7.4_215.tar.bz2 

zips=ant-pack200.zip javamail-1_3_3_01.zip alloylnf-1_4_4-1.zip \
	htmlparser1_6_20060319.zip xstream-distribution-1.3-bin.zip \
	jabsorb-1.2.2-src.zip yuicompressor-2.4.2.zip

jars=itext-1.3.jar hibernate-client.jar gettext-commons-0.9.1.jar \
	properJavaRDP-1.1.jar java-getopt-1.0.12.jar jnlp.jar

###############################################################################
###############################################################################

packages=$(patsubst %.zip,$(OUTPUT_DIRECTORY)/%,$(zips))
packages+=$(patsubst %.tar.gz,$(OUTPUT_DIRECTORY)/%,$(tarballs_gz))
packages+=$(patsubst %.tar.bz2,$(OUTPUT_DIRECTORY)/%,$(tarballs_bz2))
packages+=$(patsubst %.jar,$(OUTPUT_DIRECTORY)/%,$(jars))

$(OUTPUT_DIRECTORY)/jabsorb-1.2.2-src: $(OUTPUT_DIRECTORY)/apache-ant-1.6.5 jabsorb-1.2.2-src.patch
$(OUTPUT_DIRECTORY)/AjaxTK-3.1.1_GA_394-src: $(OUTPUT_DIRECTORY)/apache-ant-1.6.5

all: $(packages) ;

## This verifies that the output directory hasn't been modified, it
## should be done before a release.
verification: output.md5 verify
	@echo "==> Verifying output directory"
	@./verify

## Create a new md5sum, this should also be committed
md5sum:
	@echo "==> Generating a new md5sum file"
	@find ${OUTPUT_DIRECTORY} -type f -exec cat \{\} \; | md5sum | \
		sed 's/$$/ File contents/' > output.md5
	@find $(OUTPUT_DIRECTORY) | sort | md5sum | sed 's/$$/ Directory structure/' >> output.md5

$(OUTPUT_DIRECTORY)/%: %.tar.gz %.excl $(OUTPUT_DIRECTORY).$(dir_target)
	@echo "==> tar $< -> $@"
	@$(RM_DIR) $@
	@$(TAR) -C $(OUTPUT_DIRECTORY) -X $(<:.tar.gz=.excl) -zxf $<
	@if [ -x $(<:.tar.gz=.post) ]; then ./$(<:.tar.gz=.post) $(OUTPUT_DIRECTORY); fi
	@touch $@

$(OUTPUT_DIRECTORY)/%: %.tar.bz2 %.excl $(OUTPUT_DIRECTORY).$(dir_target)
	@echo "==> tar $< -> $@"
	@$(RM_DIR) $@
	@$(TAR) -C $(OUTPUT_DIRECTORY) -X $(<:.tar.bz2=.excl) -jxf $<
	@if [ -x $(<:.tar.bz2=.post) ]; then ./$(<:.tar.bz2=.post) $(OUTPUT_DIRECTORY); fi
	@touch $@

# Zip doesn't support exclude list files, just via arguments Assume
# zips made from directory itself (yuck) Also, unzip can exit with
# status 2 when everything is ok!
$(OUTPUT_DIRECTORY)/%: %.zip $(OUTPUT_DIRECTORY).$(dir_target)
	@echo "==> unzip $< -> $@"
	@$(RM_DIR) $@
	@-$(UNZIP) -d $@ $<
	@if [ -x $(<:.zip=.post) ]; then ./$(<:.zip=.post) $(OUTPUT_DIRECTORY); fi
	@touch $@

# Jars are simpler since there is no exclusion.
$(OUTPUT_DIRECTORY)/%: %.jar $(OUTPUT_DIRECTORY).$(dir_target)
	@echo "==> jar $< -> $@"
	@$(RM_DIR) $@
	@$(MK_DIR) $@
	@$(CP) $< $@
	@if [ -x $(<:.jar=.post) ]; then ./$(<:.jar=.post) $(OUTPUT_DIRECTORY); fi
	@touch $@

## Delete the output directory
distclean:
	rm -rf $(OUTPUT_DIRECTORY)

## Nothing to do
clean: ;
