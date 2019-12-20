Ext.define('Ung.common.TableConfig.threatprevention', {
    singleton: true,

    fromTypes: {
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
    },

    /**
     * Extended report sessions table fields.
     */
    sessionsFields: [{
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
    }],

    // To do categories, do nested. Then eventreport selection can construct.
    // Make sure that with thee groupings, export still works.  And reports in general.

    /**
     * Extended report sessions table columns
     */
    sessionsColumns: [{
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
    }],

    /**
     * Extended report events table fields.
     */
    httpEventsFields: [{
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
    }],

    /**
     * Extended report http_events table columns
     */
    httpEventsColumns: [{
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
    }],

    initialized: false,
    initialize: function(tableConfig){
        if(this.initialized){
            return;
        }
        var me = this;

        // use TableConfig methods to alter fields and columns
        // field order doesn't matter.
        // column matter does; by default append but allow to insert if value is specified.
        // TableConfig.insertColumns(mylist,TableConfig.getColumn({dataIndex:'hurfdurf'}))

        Ext.Array.push(
            tableConfig.tableConfig.sessions.fields,
            me.sessionsFields
        );
        Ext.Array.push(
            tableConfig.tableConfig.sessions.columns,
            me.sessionsColumns
        );
        tableConfig.tableConfig.sessions.listeners = {
            select: Ung.common.TableConfig.threatprevention.getDetails
        };

        Ext.Array.push(
            tableConfig.tableConfig.http_events.fields,
            me.httpEventsFields
        );
        Ext.Array.push(
            tableConfig.tableConfig.http_events.columns,
            me.httpEventsColumns
        );
        tableConfig.tableConfig.http_events.listeners = {
            select: Ung.common.TableConfig.threatprevention.getDetails
        };

        Ext.Object.each( me.fromTypes, function(key, value){
            tableConfig.fromTypes[key] = value;
        });

        this.initialized = true;
    },

    /**
     * Detail property details API fields and renderers.
     * NOTE: Keys are flattened.
     */
    detailMaps: {
        getrepinfo: {
            name: 'Reputation'.t(),
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
                    name: 'Threat History'.t(),
                    renderer: function(value){
                        return Ext.String.format('{0} occurences'.t(), value);
                    }
                },
                reputation: {
                    name: 'Reputation'.t(),
                    renderer: Ung.common.Renderer.threatprevention.reputation
                }
            }
        },
        geturlhistory:{
            name: 'History'.t(),
            fields: {
                "current_categorization:categories": {
                    name: 'Current Categorization: Cateories'.t()
                },
               "current_categorization:categories:catid": {
                    name: 'Category'.t(),
                    renderer: function(value, metaData){
                        return Renderer.webCategory(value);
                    }
                },
                "current_categorization:categories:conf": {
                    name: 'Confidence'.t(),
                    renderer: function(value, metaData){
                        return value + '%';
                    }
                },
                "security_history": {
                    name: 'Security History'.t()
                },
                "security_history:categories": {
                    name: 'Categories'.t()
                },
                "security_history:categories:catid": {
                    name: 'Category'.t(),
                    renderer: function(value, metaData){
                        return Renderer.webCategory(value);
                    }
                },
                "security_history:categories:conf": {
                    name: 'Confidence'.t(),
                    renderer: function(value, metaData){
                        return value + '%';
                    }
                },
                "security_history:timestamp": {
                    name: 'Timestamp'.t(),
                    renderer: Renderer.timestamp
                },
                url: {
                    name: 'URL'
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
                    name: 'Reputation History'.t(),
                    renderer: function(value){
                        return Ext.String.format('{0} occurences'.t(), value);
                    }
                },
                history: {
                    name: 'History'.t()
                },
                "history:reputation":{
                    name: 'Reputation'.t(),
                    renderer: Ung.common.Renderer.threatprevention.reputation
                },
                "history:ts":{
                    name: 'Timestamp'.t(),
                    renderer: Renderer.timestamp
                }
            }
        },
        getthreathistory: {
            name: 'Threat History'.t(),
            fields: {
                "history:is_threat": {
                    name: 'Threat?'.t(),
                    renderer: function(value){
                        return value ? 'Yes'.t() : 'No'.t();
                    }
                },
                "history:threat_types": {
                    name: 'Threat Types'.t(),
                    renderer: function(value){
                        if(value.indexOf("org.json") > -1){
                            return 'None'.t();
                        }else{
                            return value;
                        }
                    }
                },
                "history:ts": {
                    name: 'Timestamp'.t(),
                    renderer: Renderer.timestamp
                },
                threat_count: {
                    name: 'Threat Count'.t(),
                    renderer: function(value){
                        return Ext.String.format('{0} occurences'.t(), value);
                    }
                },
                history:{
                    name: 'History'.t()
                }
            }
        },
        getipevidence: {
            name: 'Evidence'.t(),
            fields: {
                "ipint": {
                    name: 'IP Address'.t(),
                    renderer: function(value){
                        return ( (value>>>24) +'.' + (value>>16 & 255) +'.' + (value>>8 & 255) +'.' + (value & 255) );
                    }
                },
                "evidence": {
                    name: 'Evidence'.t()
                },
                "evidence:incidents": {
                    name: 'Incident'.t()
                },
                "evidence:is_threat": {
                    name: 'History: Threat?'.t(),
                    renderer: function(value){
                        return value ? 'Yes'.t() : 'No'.t();
                    }
                },
                "evidence:event_type": {
                    name: 'History: Event Type'.t(),
                    renderer: function(value){
                        if(value.indexOf("org.json") > -1){
                            return 'None'.t();
                        }else{
                            return value;
                        }
                    }
                },
                "evidence:convicted_time": {
                    name: 'History: Convinced Timestamp'.t(),
                    renderer: Renderer.timestamp
                },
                "evidence:incidents:start_time": {
                    name: 'History: Timestamp'.t(),
                    renderer: Renderer.timestamp
                },
                "evidence:incidents:event_type": {
                    name: 'History: Event Type'.t()
                },
                "evidence:incidents:event_desc": {
                    name: 'History: Event Description'.t()
                },
                "evidence:incidents:number_of_attempts": {
                    name: 'History: Attempt Count'.t()
                },
                "evidence:incidents:threat_type": {
                    name: 'History: Threat Type'.t()
                },
                "evidence:incidents:timespan": {
                    name: 'History: Time Span'.t()
                },
                "evidence:incidents:details:sources": {
                    name: 'History: Sources'.t()
                },
                "evidence:incidents:details:total_attacks": {
                    name: 'History: Attack Count'.t()
                },
                "evidence:incidents:details:events": {
                    name: 'History: Events'.t()
                },
                "evidence:incidents:details:exploits": {
                    name: 'History: Exploits'.t()
                },
                "evidence:incidents:details:hosted_urls": {
                    name: 'History: Hosted Urls'.t()
                },
            }
        }
    },

    sourceTypes: ['client', 'server'],
    /**
     * Build property records for reputation detail apis.
     *
     * @param {*} element Currently selected grid row
     * @param {*} record Current grid row record
     */
    getDetails: function(element, record){
        var me = this,
            v = me.getView(),
            vm = this.getViewModel(),
            clientIpAddress = record.get('c_client_addr'),
            serverIpAddress = record.get('s_server_addr'),
            policyId = record.get('policy_id'),
            uriAddress;

        /**
         * If host and uri are found (http_event), set the uriAddress.
         */
        uriAddress = record.get('host');
        if(uriAddress != undefined){
            uriAddress += record.get('uri');
        }

        /**
         * Get Threat Prevention app instance.
         */
        var policy = Ext.getStore('policies').findRecord('policyId', policyId);
        if(policy == null){
            return;
        }
        var app = Ext.Array.findBy(policy.get('instances').list, function (inst) {
            return inst.appName === "threat-prevention";
        });
        if(app == null){
            return;
        }

        /**
         * Determine list of ip addresses and url addresses to lookup.
         *
         * Ignore any client or server IP address that is in system local networks.
         */
        var localNetworks = Rpc.directData('rpc.reportsManager.getReportInfo', "threat-prevention", policyId, "localNetworks");
        var ipAddresses = [];
        var urlAddresses = [];
        Ung.common.TableConfig.threatprevention.sourceTypes.forEach( function(sourceType){
            // IP address reputation
            var reputation = record.get('threat_prevention_' +sourceType+'_reputation');
            if(sourceType == 'server' && reputation == undefined){
                // Try url reputation from http_events.
                reputation = record.get('threat_prevention_reputation');
            }
            if(reputation != null && reputation > 0){
                var clientIsRemote = false;
                localNetworks.forEach(function(network){
                    if( (sourceType == 'client') &&
                        (reputation > 0) &&
                        (false === Util.ipMatchesNetwork(clientIpAddress, network['maskedAddress'], network['netmaskString'] ))){
                        // Only consider if client IP address with a non-zero reputation, and not within this local network.
                        if(ipAddresses.indexOf(clientIpAddress) == -1){
                            // Only add to IP list if we haven't seen it already.
                            ipAddresses.push(clientIpAddress);
                        }
                        clientIsRemote = true;
                    }
                    if( (sourceType == 'server') &&
                        (reputation > 0) &&
                        (false === Util.ipMatchesNetwork(serverIpAddress, network['maskedAddress'], network['netmaskString'] ))){
                        // Only consider if server IP address with a non-zero reputation, and not within this local network.
                        if(clientIsRemote == true || uriAddress == undefined){
                            // Only consider if client is remote or we don't have a uri.
                            if(ipAddresses.indexOf(serverIpAddress) == -1){
                                // Only add to IP list if we haven't seen it already.
                                ipAddresses.push(serverIpAddress);
                            }
                        }else{
                            if(urlAddresses.indexOf(uriAddress != undefined ? uriAddress : serverIpAddress) == -1){
                                // Only add to uri list if we haven't seen it already.
                                urlAddresses.push(uriAddress != undefined ? uriAddress : serverIpAddress);
                            }
                        }
                    }
                });
            }
        });

        var app = Rpc.directData('rpc.appManager.app', app.id);

        var rpcSequence = [];
        /**
         * Detail queries for each IP address.
         */
        if(ipAddresses.length){
            ipAddresses.forEach(function(address){
                rpcSequence.push(Rpc.asyncPromise('rpc.reportsManager.getReportInfo', "threat-prevention", policyId, 'getIpHistory', [address]));
            });
        }
        /**
         * Detail queries for each url.
         */
        if(urlAddresses.length){
            urlAddresses.forEach(function(address){
                rpcSequence.push(Rpc.asyncPromise('rpc.reportsManager.getReportInfo', "threat-prevention", policyId, 'getUrlHistory', [address]));
            });
        }

        Ext.Deferred.sequence(rpcSequence, this)
        .then(function(results){
            if(Util.isDestroyed(v)){
                return;
            }
            var propertyRecord = [];

            var categories = [];
            var history = [];

            var category = null;
            results.forEach( function(result){
                result.forEach( function(answer){
                    // Determine directional label.
                    var answerAddress = 'url' in answer ? answer['url'] : answer['value'];
                    var addressType = answerAddress == clientIpAddress ? 'Client'.t() : 'Server'.t();

                    /**
                     * Walk detail maps for this answer.  Each call can make multiple API queries.
                     */
                    Ext.Object.each(
                        Ung.common.TableConfig.threatprevention.detailMaps,
                        function(detail, detailMap){
                            if(detail in answer['queries']){
                                category = Ext.String.format('Threat Prevention {0}: {1}'.t(), detailMap.name, addressType); 

                                /**
                                 * Flatten the object.  This makes it faster to do lookups.
                                 */
                                var flattened = Util.jsonFlatten(answer['queries'][detail]);
                                var flattenedKeys = Object.keys(flattened);
                                Ext.Object.each(
                                    flattened,
                                    function(key, value){
                                        var keyPath = [];
                                        var namePath = [];
                                        var keyIndex = 0;
                                        var useIndex= false;
                                        var lastKeyNameIndex = 0;
                                        var nameKey;
                                        key.split(':').forEach(function(key){
                                            if(isNaN(key) == false ){
                                                /**
                                                 * Found a numeric value indicating an index.  Determine
                                                 * the resposnse has other indexes for this path since
                                                 * flattened object will contain list indexes like 0 which are
                                                 * not very pleasing to see if we also don't see others in sequence.
                                                 */
                                                keyIndex = parseInt(key,10);
                                                var keyPathFlattened = keyPath.slice(lastKeyNameIndex).join(":");

                                                if('_useIndex' in detailMap &&
                                                    keyPathFlattened in detailMap['_useIndex']){
                                                    // We've seen this keypath already, so pull useIndex from cache.
                                                    useIndex = detailMap['_useIndex'][keyPathFlattened];
                                                }else{
                                                    // Determine if this keypath exists.  If so, use the index.
                                                    var keySubstring = keyPathFlattened + ":" + (keyIndex + 1); 
                                                    useIndex = flattenedKeys.filter( function(key){
                                                        if(key.indexOf(keySubstring) > -1){
                                                            return true;
                                                        }
                                                    }).length > 0 ? true : false;
                                                    if(!('_useIndex' in detailMap)){
                                                        detailMap['_useIndex'] = {};
                                                    }
                                                    detailMap['_useIndex'][keyPathFlattened] = useIndex;
                                                }

                                                /**
                                                 * Create label based on the current path and whether to use index value.
                                                 */
                                                nameKey = keyPath.join(':');
                                                namePath.push((nameKey in detailMap.fields && detailMap.fields[nameKey]['name'] ? detailMap.fields[nameKey]['name'] : nameKey) + ( useIndex ? ' ' + (keyIndex + 1): ''));
                                                lastKeyNameIndex = keyPath.length;
                                            }else{
                                                keyPath.push(key);
                                            }
                                        });
                                        key = keyPath.join(":");
                                        if(lastKeyNameIndex < keyPath.length ){
                                            namePath.push((key in detailMap.fields && detailMap.fields[key]['name'] ? detailMap.fields[key]['name'] : key));
                                        }

                                        /**
                                         * Add new property record for this object.
                                         */
                                        if(keyPath[keyPath.length-1].indexOf('javaClass') == -1){
                                            propertyRecord.push({
                                                name: namePath.length ? namePath.join(': ') : detail + '::' + key,
                                                value: key in detailMap.fields && detailMap.fields[key]['renderer'] ? detailMap.fields[key]['renderer'].call(this,value) : value,
                                                category: category
                                            });
                                        }
                                    }
                                );
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
});