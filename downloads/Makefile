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
tarballs_gz=apache-ant-1.6.5.tar.gz \
	apache-tomcat-9.0.89-embed.tar.gz \
	logging-log4j-2.23.1.tar.gz \
	velocity-1.4.tar.gz \
	commons-fileupload-1.5-bin.tar.gz \
	commons-io-2.11.0.tar.gz \
	commons-lang3-3.9-bin.tar.gz \
	commons-logging-1.3.2-bin.tar.gz \
	commons-codec-1.17.0-bin.tar.gz \
	httpcomponents-client-5.3.1-bin.tar.gz \
	slf4j-2.0.9.tar.gz \
	Ext.ux.Exporter-1.0.tar.gz

tarballs_bz2=

zips=javamail-1_3_3_01.zip jabsorb-1.2.5.zip \
	jradius-client-1.0.0-release.zip apache-taglibs-standard-1.2.5.zip \
	selenium-java-3.141.59.zip geoip2-2.17.0-with-dependencies.zip

jars=gettext-commons-0.9.1.jar postgresql-9.4-1201.jdbc4.jar \
	dnsjava-3.5.0-20211020.jar sqlite-jdbc-3.15.1.jar \
	jakarta.activation-1.2.1.jar jta-1.1.jar \
	spotbugs-annotations-4.7.3.jar biz.aQute.bndlib-7.0.0.jar

###############################################################################
###############################################################################

packages=$(patsubst %.zip,$(OUTPUT_DIRECTORY)/%,$(zips))
packages+=$(patsubst %.tar.gz,$(OUTPUT_DIRECTORY)/%,$(tarballs_gz))
packages+=$(patsubst %.tar.bz2,$(OUTPUT_DIRECTORY)/%,$(tarballs_bz2))
packages+=$(patsubst %.jar,$(OUTPUT_DIRECTORY)/%,$(jars))

all: $(packages) ;
# remove files and directories with spaces in names
	@find ${OUTPUT_DIRECTORY} -type f -name '* *' -print0 | xargs -0 rm -f
	@find ${OUTPUT_DIRECTORY} -type d -name '* *' -print0 | xargs -0 rm -rf

$(OUTPUT_DIRECTORY)/%: %.tar.gz $(OUTPUT_DIRECTORY).$(dir_target)
	@echo "[untar gz] $< -> $@"
	@$(RM_DIR) $@
	@$(TAR) -C $(OUTPUT_DIRECTORY) -zxf $<
	@if [ -x $(<:.tar.gz=.post) ]; then echo "[post    ] ./$(<:.zip=.post) $(OUTPUT_DIRECTORY)" ; ./$(<:.tar.gz=.post) $(OUTPUT_DIRECTORY) > /dev/null 2>&1; fi
	@touch $@

$(OUTPUT_DIRECTORY)/%: %.tar.bz2 $(OUTPUT_DIRECTORY).$(dir_target)
	@echo "[untar bz] $< -> $@"
	@$(RM_DIR) $@
	@$(TAR) -C $(OUTPUT_DIRECTORY) -jxf $<
	@if [ -x $(<:.tar.bz2=.post) ]; then echo "[post    ] ./$(<:.zip=.post) $(OUTPUT_DIRECTORY)" ; ./$(<:.tar.bz2=.post) $(OUTPUT_DIRECTORY) > /dev/null 2>&1; fi
	@touch $@

# Zip doesn't support exclude list files, just via arguments Assume
# zips made from directory itself (yuck) Also, unzip can exit with
# status 2 when everything is ok!
$(OUTPUT_DIRECTORY)/%: %.zip $(OUTPUT_DIRECTORY).$(dir_target)
	@echo "[unzip   ] $< -> $@"
	@$(RM_DIR) $@
	@-$(UNZIP) -d $@ $< > /dev/null 2>&1
	@if [ -x $(<:.zip=.post) ]; then echo "[post    ] ./$(<:.zip=.post) $(OUTPUT_DIRECTORY)" ; ./$(<:.zip=.post) $(OUTPUT_DIRECTORY) > /dev/null 2>&1; fi
	@touch $@

# Jars are simpler since there is no exclusion.
$(OUTPUT_DIRECTORY)/%: %.jar $(OUTPUT_DIRECTORY).$(dir_target)
	@echo "[unjar   ] $< -> $@"
	@$(RM_DIR) $@
	@$(MK_DIR) $@
	@$(CP) $< $@
	@if [ -x $(<:.jar=.post) ]; then echo "[post    ] ./$(<:.zip=.post) $(OUTPUT_DIRECTORY)" ; ./$(<:.jar=.post) $(OUTPUT_DIRECTORY) > /dev/null 2>&1; fi
	@touch $@

## Delete the output directory
distclean:
	@echo "[rm -rf  ] $(OUTPUT_DIRECTORY)"
	@rm -rf $(OUTPUT_DIRECTORY)

## Nothing to do
clean: ;
