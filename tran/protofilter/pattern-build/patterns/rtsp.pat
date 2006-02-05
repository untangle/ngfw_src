# RTSP - Real Time Streaming Protocol - http://www.rtsp.org - RFC 2326
# Pattern quality: good fast
# Protocol groups: ietf_proposed_standard streaming_video
#
# usually runs on port 554
#
# To take full advantage of this pattern, please see the RTSP connection 
# tracking patch to the Linux kernel referenced at the above site.
#
# This pattern has been tested and is believed to work well.
#
# To get or provide more information about this protocol and/or pattern:
# http://www.protocolinfo.org/wiki/RTSP
# http://lists.sourceforge.net/lists/listinfo/l7-filter-developers

rtsp
rtsp/1.0 200 ok
