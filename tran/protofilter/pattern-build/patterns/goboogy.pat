# GoBoogy - a Korean P2P protocol
# pattern quality: marginal notsofast
# Protocol groups: p2p
# 
# This pattern is untested and likely does not work in all cases!
#
# To get or provide more information about this protocol and/or pattern:
# http://www.protocolinfo.org/wiki/GoBoogy
# http://lists.sourceforge.net/lists/listinfo/l7-filter-developers
#
# By Adam Przybyla, modified by Matthew Strait.  Possibly lifted from 
# Josh Ballard (oofle.com).

goboogy
<peerplat>|^get /getfilebyhash\.cgi\?|^get /queue_register\.cgi\?|^get /getupdowninfo\.cgi\?
