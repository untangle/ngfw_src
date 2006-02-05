# Direct Connect - P2P filesharing - http://www.neo-modus.com
# Pattern quality: good veryfast
# Protocol groups: p2p
#
# Direct Connect "hubs" listen on port 411
# http://www.dcpp.net/wiki/
# I've verified that this pattern can be used to limit direct connect
# bandwidth using DC:PRO 0.2.3.149R11.
#
# To get or provide more information about this protocol and/or pattern:
# http://www.protocolinfo.org/wiki/Direct_Connect
# http://lists.sourceforge.net/lists/listinfo/l7-filter-developers

directconnect
# client-to-client handshake|client-to-hub login, hub speaking|client-to-hub login, client speaking
^(\$mynick |\$lock |\$key )
