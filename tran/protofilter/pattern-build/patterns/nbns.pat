# NBNS - NetBIOS name service
# Pattern quality: good notsofast
# Protocol groups: networking
#
# This pattern has been tested and is believed to work well.
#
# To get or provide more information about this protocol and/or pattern:
# http://www.protocolinfo.org/wiki/NBNS
# http://lists.sourceforge.net/lists/listinfo/l7-filter-developers
#
# name query
# \x01\x10 means name query
#
# registration NB
# (\x10 or )\x10 means registration
#
# release NB (merged with registration)
# 0\x10 means release

nbns
\x01\x10\x01|\)\x10\x01\x01|0\x10\x01
