# GnucleusLAN - LAN-only P2P filesharing
# Pattern quality: good fast
# Protocol groups: p2p
#
# This pattern has been tested and is believed to work well.
#
# To get or provide more information about this protocol and/or pattern:
# http://www.protocolinfo.org/wiki/GnucleusLAN
# http://lists.sourceforge.net/lists/listinfo/l7-filter-developers

gnucleuslan
gnuclear connect/[\x09-\x0d -~]*user-agent: gnucleus [\x09-\x0d -~]*lan:
