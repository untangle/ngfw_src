#!/bin/bash

# Exit immediately if a command exits with a non-zero status
set -e

echo "Creating parent set for all the blocking set"
ipset create dblsets list:set

echo "Creating dynamic-block-list chain..."
iptables -t filter -N dynamic-block-list

echo "Linking dynamic-block-list chain to FORWARD chain..."
iptables -t filter -A FORWARD -m conntrack --ctstate NEW -j dynamic-block-list

echo "Adding rule to log and drop parent ipset"
iptables -t filter -A dynamic-block-list -m set --match-set dblsets dst -j NFLOG --nflog-prefix "dynamic_block_list_blocked"
iptables -t filter -A dynamic-block-list -m set --match-set dblsets dst -j DROP

echo "Filter chain setup complete!"