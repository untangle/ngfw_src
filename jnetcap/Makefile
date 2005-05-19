sub_name=jnetcap

INSTALL_LIB=TRUE

include ../buildtools/Makefile.in

lib_file_name=lib$(sub_name).a
jar_file_name=$(sub_name).jar

## Path to where the class files are stored.
java_class_path=output/$(sub_name)/com/metavize/$(sub_name)

## Path to where the jar files are stored.
java_jar_path=output/jar

VPATH=$(src_path):$(src_path)/com/metavize/$(sub_name):$(local_lib_path):$(java_class_path):$(java_jar_path)

## C Configuration
INC       += ./jni_includes/ $(java_include_base) $(java_include_base)/linux
LIBS      += netcap xml2 mvutil ipq
DEBUG_PKG  = $(JNETCAP_PKG)

javac_classpath=$(src_path)

class_base=com.metavize.$(sub_name)

c_src=jnetcap.c jinterface_set.c jsubscription.c jsession.c judp.c jtcp.c Shield.c
objects=$(c_src:.c=.o)

java_src=Subscription.java Range.java InterfaceSet.java Inet4AddressConverter.java  \
        Netcap.java NetcapSession.java NetcapUDPSession.java NetcapHook.java \
        UDPPacket.java NetcapTCPSession.java TCPEndpoints.java SubscriptionGenerator.java \
        CPointer.java IPTraffic.java SubscriptionManager.java Shield.java
classes=$(java_src:.java=.class)

all: build

build: $(lib_file_name) build.default

$(lib_file_name): $(jar_file_name) jnetcap.h $(objects)

jnetcap.h: jnetcap_src.sh Makefile
	@echo "==> $<"
	@$< $(class_base) > $(<:_src.sh=.h)

$(jar_file_name): $(classes)
ifndef ANT_INVOKED
	@echo "==> ant"
	@$(ANT)
endif

## These rules are just a placeholder for ant. if you want to compile a java file
## directly, use ant.
%.class: %.java ;

## These all use the defaults defined in the parent directory.
clean: ant.clean clean.default
	@$(RM) $(local_lib_path)/$(lib_file_name)
	@$(RM) ./jni_includes/*.h

test: test.default

ant.clean:   ant.clean.default
ant.build:   ant.build.default
ant.all:     ant.all.default
ant.install: ant.install.default

distclean: distclean.default

tags: tags.default
