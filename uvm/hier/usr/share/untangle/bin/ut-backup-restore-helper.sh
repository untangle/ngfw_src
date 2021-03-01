#!/bin/bash -x

# This script contains a number of helper functions which were moved out
# of UVM to accomplish additional security hardning.

restore()
{
   shift
   nohup $DIR/ut-restore.sh -i $1 -v -m $2 >/var/log/uvm/restore.log 2>&1 &
}

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
$1 "$@"