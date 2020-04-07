Ext.define('Ung.common.TableConfig.threatprevention', {
    singleton: true,
    initialized: false,

    /**
     * extra map with fields, columns tables
     * used to inject threat prevention info
     * into sessions and http_events tables
     */
    map: {
        fields: {
            threat_prevention_blocked: {
                col: { text: 'Blocked'.t() + ' (Threat Prevention)', filter: Rndr.filters.boolean, width: Rndr.colW.boolean, renderer: Rndr.boolean },
                fld: { type: 'boolean' }
            },
            threat_prevention_client_categories: {
                col: { text: 'Client Categories'.t() + ' (Threat Prevention)', width: 150 },
                fld: { type: 'integer', convert: Ung.common.Converter.threatprevention.category }
            },
            threat_prevention_client_reputation: {
                col: { text: 'Client Reputation'.t() + ' (Threat Prevention)', width: 150 },
                fld: { type: 'integer', convert: Ung.common.Converter.threatprevention.reputation }
            },
            threat_prevention_flagged: {
                col: { text: 'Flagged'.t() + ' (Threat Prevention)', filter: Rndr.filters.boolean, width: Rndr.colW.boolean, renderer: Rndr.boolean },
                fld: { type: 'boolean' }
            },
            threat_prevention_reason: {
                col: { text: 'Reason'.t() + ' (Threat Prevention)', width: 100 },
                fld: { type: 'string' }
            },
            threat_prevention_rule_id: { // should use converter instead
                col: { text: 'Rule Id'.t() + ' (Threat Prevention)', width: 120 },
                fld: { type: 'integer', convert: Ung.common.Converter.threatprevention.ruleId }
            },
            threat_prevention_server_categories: {
                col: { text: 'Server Categories'.t() + ' (Threat Prevention)', width: 150 },
                fld: { type: 'integer', convert: Ung.common.Converter.threatprevention.category }
            },
            threat_prevention_server_reputation: {
                col: { text: 'Server Reputation'.t() + ' (Threat Prevention)', width: 150 },
                fld: { type: 'integer', convert: Ung.common.Converter.threatprevention.reputation }
            }
        },

        tables: {
            sessions: [
                'threat_prevention_blocked',
                'threat_prevention_flagged',
                'threat_prevention_rule_id',
                'threat_prevention_client_reputation',
                'threat_prevention_client_categories',
                'threat_prevention_server_reputation',
                'threat_prevention_server_categories'
            ],
            http_events: [
                'threat_prevention_blocked',
                'threat_prevention_flagged',
                'threat_prevention_rule_id',
                'threat_prevention_client_reputation',
                'threat_prevention_client_categories',
                'threat_prevention_server_reputation',
                'threat_prevention_server_categories'
            ]
        }
    },

    initialize: function() {
        var me = this, _map = me.map;

        /**
         * Set From types
         */
        TableConfig.setFromType({
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
         * Add threat prevention fields and tables configuration
         * to the main Map
         */
        Ext.apply(Map.fields, this.map.fields);
        Ext.Object.each(this.map.tables, function (table, fields) {
            Ext.Array.push(Map.tables[table], fields);
        });

        Map.listeners['sessions'] = {
            select: Ung.common.TableConfig.threatprevention.getIpDetails
        };
        Map.listeners['http_events'] = {
            select: Ung.common.TableConfig.threatprevention.getHttpDetails
        };


    },

    /**
     * Detail property details API fields and renderers.
     * NOTE: Keys are flattened.
     */
    detailMaps: {
        getrepinfo: {
            name: 'Domain Reputation History'.t(),
            fields: {
                age: {
                    name: 'Age'.t(),
                    renderer: Ung.common.Renderer.threatprevention.age
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
                    renderer: Ung.common.Renderer.threatprevention.recentOccurrences
                },
                reputation: {
                    name: 'Reputation (Parent Domain)'.t(),
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
    getHttpDetails: function(record, cb) {
        var uriAddress = record.get('host'),
            reputation = record.get('threat_prevention_server_reputation'),
            clientIpAddress = record.get('c_client_addr'),
            clientReputation = record.get('threat_prevention_client_reputation'),
            ipAddresses = [];

        if (!reputation && !clientReputation) { return; }

        if (uriAddress != undefined) {
            uriAddress += record.get('uri');
        }

        if(reputation){
            Ext.Deferred.sequence([
                Rpc.asyncPromise(
                    'rpc.reportsManager.getReportInfo',
                    'threat-prevention',
                    -1,
                    'getUrlHistory',
                    [uriAddress])
            ], this)
            .then(function(results) {
                var propertyRecord = [];
                var propertyCategory = null;
                results.forEach( function(result){
                    if(result != null){
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
                    }
                });
                cb(propertyRecord);
            }, function(ex) {
                cb();
                console.log(ex);
            });
        }

        if(clientReputation){
            ipAddresses.push(clientIpAddress);
            Ext.Deferred.sequence([
                Rpc.asyncPromise(
                    'rpc.reportsManager.getReportInfo',
                    'threat-prevention',
                    -1,
                    'getIpHistory',
                    ipAddresses)
            ], this)
             .then(function(results){
                var propertyRecord = [];
                var propertyCategory = null;
                results.forEach( function(result){
                    if(result != null){
                        result.forEach( function(answer){
                            /**
                             * Walk detail maps for this answer.  Each call can make multiple API queries.
                             */
                            Ext.Object.each(
                                Ung.common.TableConfig.threatprevention.detailMaps,
                                function(detail, detailMap){
                                    if(detail in answer['queries']){
                                        var ipAddress = "ip" in answer ? answer["ip"] : answer["value"];
                                        propertyCategory = Ext.String.format('Threat Prevention: {0}: {1}'.t(), detailMap.name, 'Client'.t());
                                        Ung.common.TableConfig.threatprevention.toPropertyRecord(propertyRecord, propertyCategory, detailMap['fields'], answer['queries'][detail]);
                                    }
                                }
                            );
                        });
                    }
                });
                cb(propertyRecord);
            }, function(ex) {
                cb();
                console.log(ex);
            });
        }
    },

    /**
     * Build IP-address based property records for reputation detail apis.
     * @param {*} record Current grid row record
     */
    getIpDetails: function(record, cb) {
        var clientIpAddress = record.get('c_client_addr'),
            serverIpAddress = record.get('s_server_addr'),
            clientReputation = record.get('threat_prevention_client_reputation'),
            serverReputation = record.get('threat_prevention_server_reputation'),
            ipAddresses = [];

        if (clientReputation != null && clientReputation > 0) {
            Ext.Deferred.sequence([
                Rpc.asyncPromise(
                    'rpc.reportsManager.getReportInfo',
                    'threat-prevention',
                    -1,
                    'getIpHistory',
                    [clientIpAddress])
            ], this)
            .then(function(results){
                var propertyRecord = [];
                var propertyCategory = null;
                results.forEach( function(result){
                    if(result != null){
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
                    }
                });
                cb(propertyRecord);
            }, function(ex) {
                cb();
                console.log(ex);
            });
        }
        if (serverReputation != null && serverReputation > 0) {
            Ext.Deferred.sequence([
                Rpc.asyncPromise(
                    'rpc.reportsManager.getReportInfo',
                    'threat-prevention',
                    -1,
                    'getUrlHistory',
                    [serverIpAddress])
            ], this)
            .then(function(results) {
                var propertyRecord = [];
                var propertyCategory = null;
                results.forEach( function(result){
                    if(result != null){
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
                    }
                });
                cb(propertyRecord);
            }, function(ex) {
                cb();
                console.log(ex);
            });
        }
    },

    /*
     * Convert multi-level json object into a single-level key-pair flattened json object.
     */
    maxKeyIndex: 9,
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
                    Ung.common.TableConfig.threatprevention.toPropertyRecord(propertyRecord, propertyCategory, fields, value, fieldPath, namePath.slice(0,namePath.length-1).concat(newName), keyIndex);
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
