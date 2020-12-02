#!/bin/bash

/bin/radwho | awk 'NR >1 {print $NF, $1 }' 2> /dev/null