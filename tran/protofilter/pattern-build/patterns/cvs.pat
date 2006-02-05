# CVS - Concurrent Versions System
# Pattern quality: good veryfast
# Protocol groups: version_control
#
# To get or provide more information about this protocol and/or pattern:
# http://www.protocolinfo.org/wiki/CVS
# http://lists.sourceforge.net/lists/listinfo/l7-filter-developers

cvs

# Matches pserver login.  AUTH is for actually starting the protocol
# VERIFICATION is for authenticating without starting the protocols
# and GSSAPI is for using security services such as kerberos.
# http://www.loria.fr/~molli/cvs/doc/cvsclient_3.html

^BEGIN (AUTH|VERIFICATION|GSSAPI) REQUEST\x0a
