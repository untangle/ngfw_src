# Quake 1 - A popular computer game.
# Pattern quality: marginal veryfast
# Protocol groups: proprietary game
#
# This pattern is untested and unconfirmed.
#
# To get or provide more information about this protocol and/or pattern:
# http://www.protocolinfo.org/wiki/Quake
# http://lists.sourceforge.net/lists/listinfo/l7-filter-developers

# Info taken from http://www.gamers.org/dEngine/quake/QDP/qnp.html,
# which says that it "is incomplete, inaccurate and only applies to
# versions 0.91, 0.92, 1.00 and 1.01 of QUAKE"

quake1
# Connection request: 80 00 00 0c 01 51 55 41 4b 45 00 03
# \x80 = control packet.
# \x0c = packet length
# \x01 = CCREQ_CONNECT
# \x03 = protocol version (3 == 0.91, 0.92, 1.00, 1.01)
^\x80\x0c\x01quake\x03
