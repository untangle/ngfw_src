alpine=${HOME}/work/alpine

javac=javac
javac_flags=-classpath $(alpine)/jvector/output/jar/jvector.jar:${HOME}/work/downloads/jakarta-log4j-1.2.8/dist/lib/log4j-1.2.8.jar:.

src=Test.java Test2.java Transform.java

all: ${src:.java=.class}

%.class: %.java Makefile
	$(javac) $(javac_flags) $<

clean:
	@rm -f *.class

