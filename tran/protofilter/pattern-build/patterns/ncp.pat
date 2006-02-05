# NCP - Novell Core Protocol
# Pattern quality: good veryfast
# Protocol groups: networking
#
# This pattern has been tested and is believed to work well.
#
# To get or provide more information about this protocol and/or pattern:
# http://www.protocolinfo.org/wiki/NCP
# http://lists.sourceforge.net/lists/listinfo/l7-filter-developers

# ncp request
# dmdt means Request
#  *any length
#
#  *any reply buffer size
# "" means service request
#  | \x17\x17 means create a service connection
#  | uu means destroy service connection

# ncp reply
# tncp means reply
# 33 means service reply

ncp
^(dmdt.*\x01.*(""|\x11\x11|uu)|tncp.*33)
