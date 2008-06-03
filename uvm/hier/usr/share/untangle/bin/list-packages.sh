#! /bin/bash

# prints a nicely formatted list of packages on the box

COLUMNS=250 dpkg -l | awk '/^ii/ { printf("%-50s %s\n", $2, $3) }'
