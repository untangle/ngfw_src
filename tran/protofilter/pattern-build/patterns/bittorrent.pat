# Bittorrent - http://sourceforge.net/projects/bittorrent/
# Pattern quality: great
#
# This pattern has been tested and is believed to work well.  If it does not
# work for you, or you believe it could be improved, please post to 
# l7-filter-developers@lists.sf.net .  This list may be subscribed to at
# http://lists.sourceforge.net/lists/listinfo/l7-filter-developers
bittorrent

# 0x13 is the length of "bittorrent protocol"
^\x13bittorrent protocol
