#!/bin/sh

psql -A -t -U postgres -c "SELECT tablename FROM pg_catalog.pg_tables WHERE schemaname = 'reports';" uvm | grep `date --date='yesterday' '+%Y_%m_%d'` | while read table ; do
                                                                                                                                                            echo "Vacuum analyzing $table..."
                                                                                                                                                            psql -U postgres -c "VACUUM ANALYZE reports.$table;" uvm
                                                                                                                                                        done
