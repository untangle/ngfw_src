# Audiogalaxy - Peer to Peer filesharing
# Pattern quality: ok
#
# http://www.movspclr.co.uk/info/agprotocol.html
#
# This pattern is untested.
# Please post to l7-filter-developers@lists.sf.net as to whether it works 
# for you or not.  If you believe it could be improved please post your 
# suggestions to that list as well. You may subscribe to this list at 
# http://lists.sourceforge.net/lists/listinfo/l7-filter-developers

audiogalaxy
# (magic cookie that starts conversations)|(magic cookie that starts 
# 0.606W/0.608W client/server conversations and a string that should always
# appear in login messages)
^(\x45\x5f\xd0\xd5|\x45\x5f.*0.60(6|8)W)
