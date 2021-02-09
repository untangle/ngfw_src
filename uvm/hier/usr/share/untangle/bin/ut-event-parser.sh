#!/bin/bash

# This script contains a helper functions which were moved out
# of UVM to accomplish additional security hardning.

/usr/bin/find $1 -name '*Event.class' | xargs grep -l 'logging.LogEvent' | sed -e 's|.*com/\\(.*\\)|com/\\1|' -e 's|/|.|g' -e 's/.class//'