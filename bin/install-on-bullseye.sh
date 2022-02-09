#!/bin/sh

# see the following intranet page for more instructions and current
# limitations:
#
#   https://intranet.untangle.com/display/ngfw/Next+Generation+Firewall+Development

set -e

apt update

# required for apt-key
apt install -y gnupg2

# apt: remove official sources
rm -f /etc/apt/sources.list /etc/apt/sources.list.d/*

# apt: add Untangle key
apt-key adv --keyserver keyserver.ubuntu.com --recv-keys 0B9D6AE3627BF103

# apt: add bullseye/current source
echo 'deb http://package-server.untangle.int/public/bullseye current main non-free' > /etc/apt/sources.list.d/bullseye-current.list

# apt: add bullseye dev source, and set its pinning to always take
# precedence over packages coming from the master branch
echo 'deb [trusted=yes] http://package-server.untangle.int/dev/bullseye ngfw_src.bullseye main' > /etc/apt/sources.list.d/bullseye-dev.list
cat > /etc/apt/preferences.d/bullseye_dev.pref <<EOF
Package: *
Pin: release o=Untangle-dev
Pin-Priority: 1100
EOF

# update cache and install packages
apt update
DEBIAN_FRONTEND=noninteractive apt install -y untangle-gateway untangle-kiosk untangle-linux-config

# disable lxc-net bridge
perl -i -pe 's/(?<=^USE_LXC_BRIDGE=).*/false/' /etc/default/lxc-net

# notify reboot on the Untangle kernel
uname -a | grep -qi untangle  || echo "You need to reboot on the Untangle kernel now"
