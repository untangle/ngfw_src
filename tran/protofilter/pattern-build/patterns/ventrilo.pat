# Ventrilo - VoIP - http://ventrilo.com
# Pattern quality: good veryfast
# Protocol groups: proprietary voip
#
# I have tested this with Ventrilo client 2.3.0 on Windows talking to
# Ventrilo server 2.3.1 (the public version) on Linux.  I've done this
# both within a LAN and over the Internet.  In one test, I tried
# monkeying around with the server settings to see if I could break the
# pattern, and I couldn't.  However, you can't change the port number in
# the public server. 
#
# It has also been tested by one other person in an unknown configuration.
#
# To get or provide more information about this protocol and/or pattern:
# http://www.protocolinfo.org/wiki/Ventrilo
# http://lists.sourceforge.net/lists/listinfo/l7-filter-developers

ventrilo
^..?v\$\xcf

