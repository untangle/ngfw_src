# Subspace - 2D asteroids-style space game - http://beginners.subspace.net
# Pattern quality: marginal veryfast
# Protocol groups: game
#
# To get or provide more information about this protocol and/or pattern:
# http://www.protocolinfo.org/wiki/Subspace
# http://lists.sourceforge.net/lists/listinfo/l7-filter-developers
#
# By Myles Uyema <mylesuyema AT gmail.com>
#
# This pattern matches the initial 2 packets of the client-server
# 'handshake' when joining a Zone.
#
# The first packet is an 8 byte UDP payload sent from client
# 0x00 0x01 0x?? 0x?? 0x?? 0x?? 0x11
# The next packet is a 12 byte UDP response from server
# 0x00 0x10 0x?? 0x?? 0x?? 0x?? 0x?? 0x?? 0x?? 0x?? 0x01 0x00
#
# l7-filter strips out the null bytes, leaving me with this pattern

subspace
^\x01....\x11\x10........\x01$

