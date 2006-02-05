# OpenFT - P2P filesharing (implemented in giFT library)
# Pattern quality: good fast
# Protocol groups: p2p
#
# To get or provide more information about this protocol and/or pattern:
# http://www.protocolinfo.org/wiki/OpenFT
# http://lists.sourceforge.net/lists/listinfo/l7-filter-developers

# Ben Efros <ben AT xgendev.com> says:
# "This pattern identifies openFT P2P transfers fine.  openFT is part of giFT
# and is a pretty large p2p network. I would describe this pattern as pretty 
# weak, but it works for the giFT-based clients I've used."

openft
x-openftalias: [-)(0-9a-z ~.]
