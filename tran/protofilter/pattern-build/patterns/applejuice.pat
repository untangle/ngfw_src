# Apple Juice - P2P filesharing - http://www.applejuicenet.de
# Pattern quality: great veryfast
# Protocol groups: p2p
#
# This pattern has been tested with the Linux version (version
# 0,29,142,229).  It matches search reqests and file transfers.
#
# To get or provide more information about this protocol and/or pattern:
# http://www.protocolinfo.org/wiki/AppleJuice
# http://lists.sourceforge.net/lists/listinfo/l7-filter-developers

applejuice
# this pattern extracted from ipp2p, by Eicke Friedrich.
^ajprot\x0d\x0a
