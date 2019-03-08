FROM debian:stretch
LABEL maintainer="Sebastien Delafond <sdelafond@gmail.com>"

USER root
ENV DEBIAN_FRONTEND=noninteractive

RUN echo 'APT::Install-Recommends "false";' > /etc/apt/apt.conf.d/no-recommends && \
    echo 'APT::Install-Suggests "false";' >> /etc/apt/apt.conf.d/no-recommends

RUN apt-get update -q

RUN apt-get install -y gnupg
RUN apt-get install -y dirmngr

RUN echo "deb http://foo:foo@updates.untangle.com/public/stretch 14.1.1 main non-free" > /etc/apt/sources.list.d/14.0.0.list
RUN apt-key adv --keyserver keyserver.ubuntu.com --recv-keys 735A9E18E8F62EDF413592460B9D6AE3627BF103
