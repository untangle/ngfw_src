# Freenet - Anonymous information retrieval - http://freenetproject.org
# Pattern quality: marginal veryfast
# Protocol groups: p2p document_retrieval
#
# To get or provide more information about this protocol and/or pattern:
# http://www.protocolinfo.org/wiki/Freenet
# http://lists.sourceforge.net/lists/listinfo/l7-filter-developers

freenet
# Freenet is intentionally hard to identify...
# This is empirical, only tested on one computer!
^\x01[\x08\x09][\x03\x04]
