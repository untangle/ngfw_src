#!/bin/sh
for i in debian/*.version ; do echo -n "-0" >| $i ; done
