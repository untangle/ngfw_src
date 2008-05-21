#! /bin/bash

COLUMNS=250 dpkg -l | awk '/^ii/ { printf("%-50s %s\n", $2, $3) }'
