#!/bin/bash

# Exit immediately if a command exits with a non-zero status
set -e

# Remove rule from FORWARD chain
echo "Removing rule from FORWARD chain..."
iptables -t filter -D FORWARD -m conntrack --ctstate NEW -j dynamic-block-list || {
    echo "Rule not found in FORWARD chain. Skipping..."
}

# Flush dynamic-block-list chain
echo "Flushing dynamic-block-list chain..."
iptables -t filter -F dynamic-block-list || true

# Destroy all IP sets
echo "Destroying specified IP sets..."
if [ -z "$1" ]; then
    echo "No IP sets provided for destruction. Skipping..."
else
    # Split the input argument into individual IP set names
    IFS=',' read -r -a ipset_list <<< "$1"

    # Process each IP set
    for ipset_name in "${ipset_list[@]}"; do
        if ipset list -n | grep -Fxq "$ipset_name"; then
            echo "Destroying IP set: $ipset_name..."
            ipset destroy "$ipset_name" || {
                echo "Failed to destroy IP set: $ipset_name."
            }
        else
            echo "IP set $ipset_name does not exist. Skipping..."
        fi
    done
fi

# Delete dynamic-block-list chain
echo "Deleting dynamic-block-list chain..."
iptables -t filter -X dynamic-block-list || {
    echo "dynamic-block-list chain does not exist or is already deleted."
}

echo "Filter chain removal complete!"