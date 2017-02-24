FROM seb/jessie
MAINTAINER Sebastien Delafond <sdelafond@gmail.com>

RUN echo deb http://10.112.11.105/public/jessie nightly main non-free > /etc/apt/sources.list
RUN apt-get update
RUN apt-get install untangle-development-build
