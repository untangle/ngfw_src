# MUTE - P2P filesharing - http://mute-net.sourceforge.net
# Pattern quality: marginal veryfast
# Protocol groups: p2p
#
# This pattern is lightly tested.  I don't know for sure that it will 
# match the actual file transfers.
#
#
# To get or provide more information about this protocol and/or pattern:
# http://www.protocolinfo.org/wiki/MUTE
# http://lists.sourceforge.net/lists/listinfo/l7-filter-developers

mute
^(Public|AES)Key: [0-9a-f]*\x0aEnd(Public|AES)Key\x0a$
