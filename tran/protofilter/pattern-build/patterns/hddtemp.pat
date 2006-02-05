# hddtemp - Hard drive temperature reporting
# Pattern quality: great veryfast
# Protocol groups: monitoring
# 
# Usually runs on port 7634
#
# You're a silly person if you use this pattern.
#
# This pattern has been tested and is believed to work well.
#
# To get or provide more information about this protocol and/or pattern:
# http://www.protocolinfo.org/wiki/HDDtemp
# http://lists.sourceforge.net/lists/listinfo/l7-filter-developers

hddtemp
^\|/dev/[a-z][a-z][a-z]\|[0-9a-z]*\|[0-9][0-9]\|[cfk]\|
