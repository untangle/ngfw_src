#!/bin/sh

mkdir -p ./patterns

cd ./patterns
http://l7-filter.sourceforge.net/layer7-protocols/protocols/
wget -l2 -nd -r --accept="*.pat" http://l7-filter.sourceforge.net/layer7-protocols/protocols/
wget -l2 -nd -r --accept="*.pat" http://l7-filter.sourceforge.net/layer7-protocols/malware/

