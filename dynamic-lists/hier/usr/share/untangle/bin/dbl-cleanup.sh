#!/bin/bash

# Exit immediately if a command exits with a non-zero status
set -e

echo "Creating dynamic-block-list chain..."
iptables -t filter -N dynamic-block-list

echo "Linking dynamic-block-list chain to FORWARD chain..."
iptables -t filter -A FORWARD -m conntrack --ctstate NEW -j dynamic-block-list

echo "Filter chain setup complete!"
