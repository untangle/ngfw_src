sub_name=jvector

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
LIBS      += vector mvutil
DEBUG_PKG  = $(JVECTOR_PKG)

class_base=com.metavize.$(sub_name)

c_src=SocketQueue.c OutgoingSocketQueue.c IncomingSocketQueue.c \
      Relay.c Vector.c TCPSink.c TCPSource.c UDPSource.c UDPSink.c \
      Source.c Sink.c

objects=$(c_src:.c=.o) jvector.o jerror.o

java_src=SocketQueue.java IncomingSocketQueue.java SocketQueueImpl.java \
	 OutgoingSocketQueue.java Relay.java \
	 Vector.java TCPSink.java TCPSource.java \
         DataCrumb.java Crumb.java ShutdownCrumb.java ResetCrumb.java \
	 SocketQueueListener.java Sink.java Source.java UDPSource.java UDPSink.java \
	 UDPPacketCrumb.java UDPPacketDesc.java

classes=$(java_src:.java=.class)

all: build 

build: $(lib_file_name) build.default

$(lib_file_name): $(jar_file_name) jni_header.h $(objects)

jni_header.h: jni_header_src.sh Makefile
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
	@$(RM) ./src/jni_header.h

test: all test.default

ant.clean:   ant.clean.default
ant.build:   ant.build.default
ant.all:     ant.all.default
ant.install: ant.install.default

distclean: distclean.default

tags: tags.default
