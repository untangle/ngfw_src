FROM untangleinc/ngfw:base
LABEL maintainer="Sebastien Delafond <sdelafond@gmail.com>"

RUN apt-get update
RUN apt-get install --yes build-essential
RUN apt-get install --yes devscripts
RUN apt-get install --yes equivs
RUN apt-get install --yes untangle-development-build

ENV SRC=/opt/untangle/build
RUN mkdir -p ${SRC}
VOLUME ${SRC}

WORKDIR ${SRC}

ENTRYPOINT [ "rake" ]
