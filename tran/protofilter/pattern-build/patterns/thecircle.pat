# The Circle - P2P application - http://thecircle.org.au
# Pattern quality: ok veryfast
# Protocol groups: open_source p2p
#
# To get or provide more information about this protocol and/or pattern:
# http://www.protocolinfo.org/wiki/The_Circle
# http://lists.sourceforge.net/lists/listinfo/l7-filter-developers

# This is tested with The Circle 0.41c on Linux.
# It likely misses some stuff.  Notably, I wasn't able to test it on any 
# large downloads, because no one is sharing anything!

thecircle
^t\x03ni.?[\x01-\x06]?t[\x01-\x05]s[\x0a\x0b](glob|who are you$|query data)
