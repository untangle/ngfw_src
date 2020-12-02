#!/bin/bash
IP=($1)

radwho |grep ${IP} | awk -F\  '{print $1}' | tail -1