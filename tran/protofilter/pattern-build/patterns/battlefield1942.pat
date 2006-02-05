# Battlefield 1942 - An EA game
# Pattern quality: ok veryfast
# Protocol groups: proprietary game
#
# Contributed by Myles Uyema <mylesuyema AT gmail.com>
#
# This pattern has only been tested by one person.

# To get or provide more information about this protocol and/or pattern:
# http://www.protocolinfo.org/wiki/Battlefield_1942
# http://lists.sourceforge.net/lists/listinfo/l7-filter-developers

# tested on two original EA battlefield 1942 servers
# matches the first two packets of joining a server
battlefield1942
^\x01\x11\x10\|\xf8\x02\x10\x40\x06
