#!/bin/sh

COLUMNS=200 dpkg -l "untangle*" | awk '/^ii/ {print $3}' | sort -u | tail -1
