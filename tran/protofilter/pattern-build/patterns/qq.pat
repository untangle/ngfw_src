# Tencent QQ Protocol - Chinese instant messenger protocol - http://www.qq.com
# Pattern quality: good veryfast
# Protocol groups: chat
#
# Over six million people use QQ in China, according to wsgtrsys.
# 
# This pattern has been tested and is believed to work well.
#
# To get or provide more information about this protocol and/or pattern:
# http://www.protocolinfo.org/wiki/QQ
# http://lists.sourceforge.net/lists/listinfo/l7-filter-developers
#
# QQ uses three (two?) methods to connect to server(s?).
# one is udp, and another is tcp
# udp protocol: the first byte is 02 and last byte is 03
# tcp protocol: the second byte is 02 and last byte is 03
# pattern written by www.routerclub.com wsgtrsys

qq
^.?\x02.+\x03$

