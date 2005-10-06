#!/bin/sh

mkdir -p ./patterns

cd ./patterns
rm *
wget -l2 -nd -r --accept="*.pat" http://l7-filter.sourceforge.net/layer7-protocols/protocols/
wget -l2 -nd -r --accept="*.pat" http://l7-filter.sourceforge.net/layer7-protocols/malware/

# Now get rid  of patterns we know to be problematic...

# a hack:
rm citrix.pat

# duplicate of tls:
rm ssl.pat

# overmatches:
rm finger.pat
rm whois.pat

# don't even work:
rm skypetoskype.pat
rm skypeout.pat
