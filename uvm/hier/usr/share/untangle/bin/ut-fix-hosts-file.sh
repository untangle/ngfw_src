#! /bin/sh

HOSTS="/etc/hosts"

perl -i -pe 's/127\.0\.0\.1\s+localhost.*\n//' $HOSTS
echo "127.0.0.1 localhost" >> $HOSTS

exit 0
