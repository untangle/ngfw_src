#!/bin/sh

mkdir -p ./patterns

cd ./patterns
rm *
wget -l2 -nd -r --accept="*.pat" http://l7-filter.sourceforge.net/layer7-protocols/protocols/
wget -l2 -nd -r --accept="*.pat" http://l7-filter.sourceforge.net/layer7-protocols/malware/
wget -l2 -nd -r --accept="http-itunes.pat" http://l7-filter.sourceforge.net/layer7-protocols/extra/
wget -l2 -nd -r --accept="quicktime.pat" http://l7-filter.sourceforge.net/layer7-protocols/extra/
wget -l2 -nd -r --accept="pressplay.pat" http://l7-filter.sourceforge.net/layer7-protocols/extra/

# Now get rid  of patterns we know to be problematic...

# unneeded
rm unknown.pat
rm testing.pat
rm unset.pat
rm uucp.pat

# an untested hack:
rm citrix.pat

# duplicate of tls:
rm ssl.pat

# overmatches:
rm biff.pat
rm edonkey.pat
rm finger.pat
rm pcanywhere.pat
rm qq.pat
rm skypetoskype.pat
rm tsp.pat
rm whois.pat

# don't even work:
rm skypeout.pat
