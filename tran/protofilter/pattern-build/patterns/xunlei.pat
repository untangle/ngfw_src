# Xunlei - Chinese P2P filesharing - http://xunlei.com
# Pattern quality: good veryfast
# Protocol groups: p2p
#
# This has been tested by three people.  It definitely catches some
# streams. 
#
# To get or provide more information about this protocol and/or pattern:
# http://www.protocolinfo.org/wiki/Xunlei
# http://lists.sourceforge.net/lists/listinfo/l7-filter-developers
#
# Written by wsgtrsys of www.routerclub.com.  Improved by VeNoMouS.
# Improved more by wsgtrsys and platinum of bbs.chinaunix.net.

xunlei
^[\x28\x29]...?.?.?(reg|get|query)

