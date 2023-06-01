#!/bin/bash

[ -f /tmp/$(basename -s .sh $0).debug ] && export DEBUG=1 || DEBUG=0

if [ $DEBUG -eq 1 ]; then
    export
fi

SCRIPT_ARGS=("$@")
ILLEGAL_VALUES=(";" "&&" "|" ">")

# check_illegal_arguments
#
# Look at script argument and environment variables for illegal characters
# that can present security issues and if found, exit script immediately
#
# All end-user accessible scripts should call this!
check_illegal_arguments(){
    local illegal_found=0

    for argument in "${SCRIPT_ARGS[@]}"; do
        for illegal in "${ILLEGAL_VALUES[@]}"; do
            if [[ "$argument" = *$illegal* ]] ; then
                illegal_found=1
            fi
        done
    done
    while IFS='=' read -r -d '' key value; do
        for illegal in "${ILLEGAL_VALUES[@]}"; do
            if [[ "$value" = *$illegal* ]] ; then
                illegal_found=1
            fi
        done
    done < <(env -0)

    if [ $illegal_found -eq 1 ] ; then
        echo "unable to run"
        if [ $DEBUG -eq 1 ]; then
            echo "found illegal strings in arguments"
        fi
        exit
    fi
}

# NO_CHECK_ARGUMENTS should NEVER be set for scripts that will accept any input from an end user!
if [ -z $NO_CHECK_ARGUMENTS ] ; then
    check_illegal_arguments
fi
