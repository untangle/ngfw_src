#!/bin/sh

/usr/bin/find . -type f -a ! -path "*/dist/*" -a ! -path '*/downloads/*' -a ! -path '*/staging/*' | gtags -i -f -
