#!/bin/bash

# Exit immediately if a command exits with a non-zero status
set -e

# Remove the DROP rule from the dynamic-block-list chain
echo "Removing DROP rule from dynamic-block-list chain..."
iptables -t filter -D dynamic-block-list -m set --match-set dblsets dst -j DROP ||

# Remove the NFLOG rule from the dynamic-block-list chain
echo "Removing NFLOG rule from dynamic-block-list chain..."
iptables -t filter -D dynamic-block-list -m set --match-set dblsets dst -j NFLOG --nflog-prefix "dynamic_block_list_blocked" ||

# Remove rule from FORWARD chain
echo "Removing rule from FORWARD chain..."
iptables -t filter -D FORWARD -m conntrack --ctstate NEW -j dynamic-block-list || {
    echo "Rule not found in FORWARD chain. Skipping..."
}

# Flush dynamic-block-list chain
echo "Flushing dynamic-block-list chain..."
iptables -t filter -F dynamic-block-list || true

# Destroy active blocklists IP sets
echo "Destroying specified IP sets..."

# Capture the members from the ipset list
ipset_members=$(sudo ipset list dblsets | awk '/Members:/ {flag=1; next} flag && NF {print $1} /done/ {flag=0}')

# Destroy parent set
ipset destroy dblsets ||

# Check if members exist
if [ -z "$ipset_members" ]; then
    echo "No members found in the ipset set dblsets."
    exit 1
fi

# Loop through each member and delete it from the ipset set
echo "Removing entries from the ipset set dblsets..."
for entry in $ipset_members; do
    echo "Removing member: $entry"
    sudo ipset destroy "$entry" ||{
                echo "Failed to destroy IP set: $entry."
            }
done


# Delete dynamic-block-list chain
echo "Deleting dynamic-block-list chain..."
iptables -t filter -X dynamic-block-list || {
    echo "dynamic-block-list chain does not exist or is already deleted."
}

# Delete dynamic blocklist config files and dbl-crons file
echo "Removing dynamic blocklist config files and cron job..."
rm -rf /etc/cron.d/dbl-crons
if [ $? -eq 0 ]; then
    echo "Successfully removed cron file"
else
    echo "Failed to remove cron file"
fi

rm -rf /etc/config/blocklists
if [ $? -eq 0 ]; then
    echo "Successfully removed blocklist config folder"
else
    echo "Failed to remove blocklist config folder"
fi


echo "Filter chain removal complete!"