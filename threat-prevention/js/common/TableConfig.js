Ext.define('Ung.common.TableConfig.threatprevention', {
    singleton: true,

    initialized: false,
    initialize: function(tableConfig){
        if(this.initialized){
            return;
        }
        var me = this;

        /**
         * Set From types
         */
        tableConfig.setFromType({
            threat_reputation: {
                type: 'RANGE',
                rangeValues: [
                    [1,20],
                    [21,40],
                    [41,60],
                    [61,80],
                    [81,100]
                ]
            },
            threat_category: {
                type: 'BITMASK',
                length: 31
            }
        });

        /**
         * Extended report sessions table fields.
         */
        tableConfig.setTableField("sessions", [{
            name: 'threat_prevention_blocked'
        }, {
            name: 'threat_prevention_flagged'
        }, {
            name: 'threat_prevention_rule_id'
        }, {
            name: 'threat_prevention_client_reputation',
            fromType: 'threat_reputation'
        }, {
            name: 'threat_prevention_client_categories',
            fromType: 'threat_category'
        }, {
            name: 'threat_prevention_server_reputation',
            fromType: 'threat_reputation'
        }, {
            name: 'threat_prevention_server_categories',
            fromType: 'threat_category'
        }]);
        /**
         * Extended report sessions table columns
         */
        tableConfig.setTableColumn("sessions", [{
            header: 'Blocked'.t() + ' (Threat Prevention)',
            width: Renderer.booleanWidth,
            sortable: true,
            dataIndex: 'threat_prevention_blocked',
            filter: Renderer.booleanFilter
        }, {
            header: 'Flagged'.t() + ' (Threat Prevention)',
            width: Renderer.booleanWidth,
            sortable: true,
            dataIndex: 'threat_prevention_flagged',
            filter: Renderer.booleanFilter
        }, {
            header: 'Rule Id'.t() + ' (Threat Prevention)',
            width: Renderer.idWidth,
            sortable: true,
            flex:1,
            dataIndex: 'threat_prevention_rule_id',
            filter: Renderer.numericFilter,
            renderer: Ung.common.Renderer.threatprevention.ruleId
        }, {
            header: 'Client Reputation'.t() + ' (Threat Prevention)',
            width: Renderer.idWidth,
            sortable: true,
            flex:1,
            dataIndex: 'threat_prevention_client_reputation',
            renderer: Ung.common.Renderer.threatprevention.reputation,
            filter: Renderer.numericFilter
        }, {
            header: 'Client Categories'.t() + ' (Threat Prevention)',
            width: Renderer.idWidth,
            sortable: true,
            flex:1,
            dataIndex: 'threat_prevention_client_categories',
            renderer: Ung.common.Renderer.threatprevention.category,
            filter: Renderer.numericFilter
        }, {
            header: 'Server Reputation'.t() + ' (Threat Prevention)',
            width: Renderer.idWidth,
            sortable: true,
            flex:1,
            dataIndex: 'threat_prevention_server_reputation',
            renderer: Ung.common.Renderer.threatprevention.reputation,
            filter: Renderer.numericFilter,
        }, {
            header: 'Server Categories'.t() + ' (Threat Prevention)',
            width: Renderer.idWidth,
            sortable: true,
            flex:1,
            dataIndex: 'threat_prevention_server_categories',
            renderer: Ung.common.Renderer.threatprevention.category,
            filter: Renderer.numericFilter,
        }]);
        /**
         * Add get detail listener
         */
        tableConfig.setTableListener("sessions", {
            select: Ung.common.TableConfig.threatprevention.getIpDetails
        });

        /**
        * Extended report http_events table fields.
        */
        tableConfig.setTableField("http_events", [{
            name: 'threat_prevention_blocked'
        }, {
            name: 'threat_prevention_flagged'
        }, {
            name: 'threat_prevention_rule_id'
        }, {
            name: 'threat_prevention_reputation',
            fromType: 'threat_reputation'
        }, {
            name: 'threat_prevention_categories',
            fromType: 'threat_category'
        }]);
        /**
         * Extended report http_events table columns
         */
        tableConfig.setTableColumn("http_events",[{
            header: 'Blocked'.t() + ' (Threat Prevention)',
            width: Renderer.booleanWidth,
            sortable: true,
            dataIndex: 'threat_prevention_blocked',
            filter: Renderer.booleanFilter
        }, {
            header: 'Flagged'.t() + ' (Threat Prevention)',
            width: Renderer.booleanWidth,
            sortable: true,
            dataIndex: 'threat_prevention_flagged',
            filter: Renderer.booleanFilter
        }, {
            header: 'Rule Id'.t() + ' (Threat Prevention)',
            width: Renderer.idWidth,
            sortable: true,
            flex:1,
            dataIndex: 'threat_prevention_rule_id',
            filter: Renderer.numericFilter,
            renderer: Ung.common.Renderer.threatprevention.ruleId
        }, {
            header: 'Reputation'.t() + ' (Threat Prevention)',
            width: Renderer.idWidth,
            sortable: true,
            flex:1,
            dataIndex: 'threat_prevention_reputation',
            renderer: Ung.common.Renderer.threatprevention.reputation,
            filter: Renderer.numericFilter
        }, {
            header: 'Categories'.t() + ' (Threat Prevention)',
            width: Renderer.idWidth,
            sortable: true,
            flex:1,
            dataIndex: 'threat_prevention_categories',
            renderer: Ung.common.Renderer.threatprevention.category,
            filter: Renderer.numericFilter
        }]);
        /**
         * Add get detail listener
         */
        tableConfig.setTableListener("http_events", {
            select: Ung.common.TableConfig.threatprevention.getUrlDetails
        });

        this.initialized = true;
    },

    /**
     * Detail property details API fields and renderers.
     * NOTE: Keys are flattened.
     */
    detailMaps: {
        getrepinfo: {
            name: 'Reputation History'.t(),
            fields: {
                age: {
                    name: 'Age'.t(),
                    renderer: function(value){
                        return Ext.String.format('{0} months'.t(), value);
                    }
                },
                country: {
                    name: 'Country'.t()
                },
                popularity: {
                    name: 'Popularity'.t(),
                    renderer: Ung.common.Renderer.threatprevention.ipPopularity
                },
                threathistory: {
                    name: 'Most Recent Threat History'.t(),
                    renderer: function(value){
                        return Ext.String.format('{0} occurrences'.t(), value > Ung.common.TableConfig.threatprevention.maxKeyIndex ? Ung.common.TableConfig.threatprevention.maxKeyIndex : value);
                    }
                },
                reputation: {
                    name: 'Reputation'.t(),
                    renderer: Ung.common.Renderer.threatprevention.reputation
                }
            }
        },
        geturlhistory:{
            name: 'Category History'.t(),
            fields:{
                current_categorization: {
                    name: 'Current Categorization'.t(),
                    fields:{
                        url: {
                            name: 'URL'
                        },
                        categories:{
                            name: 'Cateories'.t(),
                            fields: {
                                catid: {
                                    name: 'Category'.t(),
                                    renderer: function(value, metaData){
                                        return Renderer.webCategory(value);
                                    }
                                },
                                conf: {
                                    name: 'Confidence'.t(),
                                    renderer: function(value, metaData){
                                        return value + '%';
                                    }
                                }
                            }
                        }
                    }
                },
                security_history: {
                    name: 'Security History'.t(),
                    fields:{
                        categories: {
                            name: 'Categories'.t(),
                            fields: {
                                catid: {
                                    name: 'Category'.t(),
                                    renderer: function(value, metaData){
                                        return Renderer.webCategory(value);
                                    }
                                },
                                conf: {
                                    name: 'Confidence'.t(),
                                    renderer: function(value, metaData){
                                        return value + '%';
                                    }
                                }
                            }
                        },
                        timestamp: {
                            name: 'Timestamp'.t(),
                            renderer: Renderer.timestamp
                        }
                    }
                },
                url: {
                    name: 'URL'.t()
                }
            }
        },
        getrephistory: {
            name: 'Reputation History'.t(),
            fields: {
                max_reputation: {
                    name: 'Maximum Reputation'.t(),
                    renderer: Ung.common.Renderer.threatprevention.reputation
                },
                min_reputation: {
                    name: 'Minimum Reputation'.t(),
                    renderer: Ung.common.Renderer.threatprevention.reputation
                },
                avg_reputation: {
                    name: 'Average Reputation'.t(),
                    renderer: Ung.common.Renderer.threatprevention.reputation
                },
                history_count: {
                    name: 'Most Recent Reputation History'.t(),
                    renderer: function(value){
                        return Ext.String.format('{0} occurrences'.t(), value > Ung.common.TableConfig.threatprevention.maxKeyIndex ? Ung.common.TableConfig.threatprevention.maxKeyIndex : value);
                    }
                },
                history: {
                    name: 'History'.t(),
                    fields: {
                        reputation:{
                            name: 'Reputation'.t(),
                            renderer: Ung.common.Renderer.threatprevention.reputation
                        },
                        ts:{
                            name: 'Timestamp'.t(),
                            renderer: Renderer.timestamp
                        }
                    }
                },
            }
        },
        getthreathistory: {
            name: 'Category History'.t(),
            fields: {
                history: {
                    name: 'History'.t(),
                    fields: {
                        is_threat: {
                            name: 'Threat?'.t(),
                            renderer: function(value){
                                return value ? 'Yes'.t() : 'No'.t();
                            }
                        },
                        threat_types: {
                            name: 'Threat Categories'.t(),
                            renderer: function(value){
                                if(value.indexOf("org.json") > -1){
                                    return 'None'.t();
                                }else{
                                    return value;
                                }
                            }
                        },
                        ts: {
                            name: 'Timestamp'.t(),
                            renderer: Renderer.timestamp
                        }
                    }
                },
                threat_count: {
                    name: 'Threat Count'.t()
                }
            }
        },
        getipevidence: {
            name: 'Evidence History'.t(),
            fields: {
                ipint: {
                    name: 'IP Address'.t(),
                    renderer: function(value){
                        return ( (value>>>24) +'.' + (value>>16 & 255) +'.' + (value>>8 & 255) +'.' + (value & 255) );
                    }
                },
                evidence: {
                    name: 'Evidence'.t(),
                    fields:{
                        is_threat: {
                            name: 'Threat?'.t(),
                            renderer: function(value){
                                return value ? 'Yes'.t() : 'No'.t();
                            }
                        },
                        event_type: {
                            name: 'Event Type'.t(),
                            renderer: function(value){
                                if(value.indexOf("org.json") > -1){
                                    return 'None'.t();
                                }else{
                                    return value;
                                }
                            }
                        },
                        convicted_time: {
                            name: 'Convinced Timestamp'.t(),
                            renderer: Renderer.timestamp
                        },
                        incidents: {
                            name: 'Incidents'.t(),
                            fields: {
                                start_time: {
                                    name: 'Timestamp'.t(),
                                    renderer: Renderer.timestamp
                                },
                                event_desc: {
                                    name: 'Event Description'.t()
                                },
                                event_type: {
                                    name: 'Event Type'.t()
                                },
                                number_of_attempts: {
                                    name: 'History: Attempt Count'.t()
                                },
                                threat_type: {
                                    name: 'History: Threat Type'.t()
                                },
                                timespan: {
                                    name: 'History: Time Span'.t(),
                                    renderer: Renderer.timespan
                                },
                                details: {
                                    name: 'Details'.t(),
                                    fields: {
                                        sources: {
                                            name: 'History: Sources'.t()
                                        },
                                        total_attacks: {
                                            name: 'History: Attack Count'.t()
                                        },
                                        events: {
                                            name: 'History: Events'.t()
                                        },
                                        exploits: {
                                            name: 'History: Exploits'.t()
                                        },
                                        hosted_urls: {
                                            name: 'History: Hosted Urls'.t()
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    },

    /**
     * Build url-address based property records for reputation detail apis.
     *
     * @param {*} element Currently selected grid row
     * @param {*} record Current grid row record
     */
    getUrlDetails: function( element, record){
        var me = this,
            v = me.getView(),
            vm = this.getViewModel(),
            policyId = record.get('policy_id'),
            uriAddress = record.get('host'),
            reputation = record.get('threat_prevention_reputation');

        if(reputation == null || reputation == 0){
            return;
        }

        if(uriAddress != undefined){
            uriAddress += record.get('uri');
        }

        Ext.Deferred.sequence([Rpc.asyncPromise('rpc.reportsManager.getReportInfo', "threat-prevention", policyId, 'getUrlHistory', [uriAddress])], this)
        .then(function(results){
            if(Util.isDestroyed(v)){
                return;
            }
            var propertyRecord = [];
            var propertyCategory = null;
            results.forEach( function(result){
                result.forEach( function(answer){
                    /**
                     * Walk detail maps for this answer.  Each call can make multiple API queries.
                     */
                    Ext.Object.each(
                        Ung.common.TableConfig.threatprevention.detailMaps,
                        function(detail, detailMap){
                            if(detail in answer['queries']){
                                propertyCategory = Ext.String.format('Threat Prevention: {0}: {1}'.t(), detailMap.name, 'Server'.t());
                                Ung.common.TableConfig.threatprevention.toPropertyRecord(propertyRecord, propertyCategory, detailMap['fields'], answer['queries'][detail]);
                            }
                        }
                    );
                });
            });
            v.up().down('unpropertygrid').getStore().loadData(propertyRecord, true);
        }, function(ex) {
            console.log(ex);
        });

    },

    /**
     * Build IP-address based property records for reputation detail apis.
     *
     * @param {*} element Currently selected grid row
     * @param {*} record Current grid row record
     */
    getIpDetails: function( element, record){
        var me = this,
            v = me.getView(),
            vm = this.getViewModel(),
            clientIpAddress = record.get('c_client_addr'),
            serverIpAddress = record.get('s_server_addr'),
            policyId = record.get('policy_id'),
            clientReputation = record.get('threat_prevention_client_reputation'),
            serverReputation = record.get('threat_prevention_server_reputation'),
            ipAddresses = [];

        if( clientReputation != null && clientReputation > 0 ){
            ipAddresses.push(clientIpAddress);
        }
        if( serverReputation != null && serverReputation > 0){
            ipAddresses.push(serverIpAddress);
        }

        if(ipAddresses.length == 0){
            return;
        }

        Ext.Deferred.sequence([Rpc.asyncPromise('rpc.reportsManager.getReportInfo', "threat-prevention", policyId, 'getIpHistory', ipAddresses)], this)
         .then(function(results){
            if(Util.isDestroyed(v)){
                return;
            }
            var propertyRecord = [];
            var propertyCategory = null;
            results.forEach( function(result){
                result.forEach( function(answer){
                    /**
                     * Walk detail maps for this answer.  Each call can make multiple API queries.
                     */
                    Ext.Object.each(
                        Ung.common.TableConfig.threatprevention.detailMaps,
                        function(detail, detailMap){
                            if(detail in answer['queries']){
                                var ipAddress = "ip" in answer ? answer["ip"] : answer["value"];
                                propertyCategory = Ext.String.format('Threat Prevention: {0}: {1}'.t(), detailMap.name, ipAddress == serverIpAddress ? 'Server'.t() : 'Client'.t());
                                Ung.common.TableConfig.threatprevention.toPropertyRecord(propertyRecord, propertyCategory, detailMap['fields'], answer['queries'][detail]);
                            }
                        }
                    );
                });
            });
            v.up().down('unpropertygrid').getStore().loadData(propertyRecord, true);
        }, function(ex) {
            console.log(ex);
        });
    },

    /*
     * Convert multi-level json object into a single-level key-pair flattened json object.
     */
    maxKeyIndex: 10,
    toPropertyRecord: function(propertyRecord, propertyCategory, fields, obj, fieldPath, namePath, currentIndex) {
        fieldPath = fieldPath || [];
        namePath = namePath || [];

        if(currentIndex != undefined &&
            (currentIndex + 1) > Ung.common.TableConfig.threatprevention.maxKeyIndex){
            return propertyRecord;
        }

        if (typeof (obj) === 'object' && obj !== null) {
            Ext.Object.each(obj, function(key, value){
                if(Array.isArray(obj)){
                    var newName = namePath[namePath.length - 1];
                    var keyIndex = parseInt(key,10);
                    if(obj.length > 1){
                        newName += ' ' + ( keyIndex + 1 );
                    }
                    Ung.common.TableConfig.threatprevention.toPropertyRecord(propertyRecord, propertyCategory, fields, value, fieldPath, namePath.slice(0,namePath.length-2).concat(newName), keyIndex);
                }else{
                    Ung.common.TableConfig.threatprevention.toPropertyRecord(propertyRecord, propertyCategory, fields[key] && 'fields' in fields[key] ? fields[key]['fields'] : fields, value, fieldPath.concat(key), namePath.concat(fields[key] && 'name' in fields[key] ? fields[key]['name'] : key));
                }
            });
        } else {
            var field = fieldPath[fieldPath.length - 1];
            var addProperty = true;
            if(field == 'javaClass'){
                addProperty = false;
            }
            if(addProperty == true){
                // append to property record array
                propertyRecord.push({
                    category: propertyCategory,
                    name: namePath.join(': '),
                    value: field in fields && fields[field]['renderer'] ? fields[field]['renderer'].call(this,obj) : obj
                });
            }
        }

        return propertyRecord;
    }
});