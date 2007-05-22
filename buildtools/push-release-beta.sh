#!/bin/sh

echo -e "\n\nSyncing...\n\n"

sudo rsync -rlpvz -e ssh /var/www/stable/ \
    --exclude 'echospam*' \
    --exclude 'echod*' \
    --exclude 'test-*' \
    --exclude 'fprot-*' \
    --exclude 'sophos-*' \
    --exclude 'virus-transform*' \
    --exclude 'kernel-dev*' \
    --exclude 'kernel-fake*' \
    --exclude 'dev-mv*' \
    --exclude 'kav-*' \
    --exclude 'Packages' \
    --exclude 'Packages.gz' \
    root@release.untangle.com:/var/www/beta

scp \
    ~/work/pkgs/scripts/override.testing.untangle \
    ~/work/pkgs/scripts/deb-scan.sh  \
    ~/work/pkgs/scripts/clean-packages.sh \
    root@release.untangle.com:~/

# Cleaning is bad.  Very very bad.  Clean dogfood first, but leave release-alpha full of
# packages.
#echo -e "\n\nCleaning...\n\n"
#ssh release.untangle.com -lroot "sh ~/clean-packages.sh /var/www.release/untangle 3 delete"

echo -e "\n\nBuilding Package List...\n\n"
ssh release.untangle.com -lroot "sh ~/deb-scan.sh /var/www/beta"

