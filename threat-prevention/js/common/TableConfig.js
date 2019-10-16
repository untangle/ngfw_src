Ext.define('Ung.common.TableConfig.threatprevention', {
    singleton: true,

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

    httpFields: [{
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


    // To do categories, do nested. Then eventreport selection can construct.
    // Make sure that with thee groupings, export still works.  And reports in general.
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

    httpColumns: [{
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
        Ext.Array.push(
            tableConfig.tableConfig.session_minutes.fields,
            me.sessionsFields
        );

        Ext.Array.push(
            tableConfig.tableConfig.session_minutes.columns,
            me.sessionsColumns
        );

        Ext.Array.push(
            tableConfig.tableConfig.http_events.fields,
            me.httpFields
        );
        Ext.Array.push(
            tableConfig.tableConfig.http_events.columns,
            me.httpColumns
        );


        tableConfig.tableConfig.sessions.listeners = {
            select: Ung.common.TableConfig.threatprevention.onSelectDetails
        };

        this.initialized = true;
    },

    // columnRenderer: function(vaue, meta){
    //     //meta.innerCls = 'fa fa-info-circle';
    //     return value;
    // },

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
                current_categorization: {
                    name: 'Current Categories',
                    renderer: function(value){
                        var categories = [];
                        value['categories'].forEach(function(cat){
                                categories.push(Renderer.webCategory(cat['catid']) + ' : ' + cat['conf'] + '%');
                        });
                        return categories.join('<br>');
                    }
                },
                security_history: {
                    name: 'Security'.t(),
                    renderer: function(value){
                        var history = [];
                        value.forEach( function(shistory){
                            var categories = [];
                            shistory['categories'].forEach(function(cat){
                                categories.push(Renderer.webCategory(cat['catid']) + ' : ' + cat['conf'] + '%');
                            });
                            history.push(Renderer.timestamp(shistory['timestamp']) + ': ' + categories.join('<br>'));
                        });
                        return history.join('<br>');
                    }
                },
                url: {
                    name: 'IP Address'
                }
            }
        },
        getrephistory: {
            name: 'Reputation'.t(),
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
                history:{
                    name: 'Reputation History'.t(),
                    renderer: function(value){
                        var history = [];
                        value.forEach( function(shistory){
                            history.push(Renderer.timestamp(shistory['ts']) + ': ' + Ung.common.Renderer.threatprevention.reputation(shistory['reputation']));
                        });
                        return history.join('<br>\n');
                    }
                }
            }
        },
        getthreathistory: {
            name: 'Reputation'.t(),
            fields: {
                threat_count: {
                    name: 'Threat Count'.t(),
                    renderer: function(value){
                        return Ext.String.format('{0} occurences'.t(), value);
                    }
                },
                history:{
                    name: 'Threat History'.t(),
                    renderer: function(value){
                        var history = [];
                        value.forEach( function(shistory){
                            history.push(Renderer.timestamp(shistory['ts']) + ': ' + shistory['status'] + ': '  );
                        });
                        return history.join('<br>\n');
                    }
                },
                is_threat: {
                    name: 'Is Threat?',
                    renderer: function(value){
                        return value ? 'Yes'.t() : 'No'.t();
                    }
                }
            }
        },
        getipevidence: {
            name: 'History'.t(),
            fields: {

            }
        }
    },

    sourceTypes: ['client', 'server'],
    onSelectDetails: function(element, record){
        // console.log(arguments);
        var me = this,
            v = me.getView(),
            vm = this.getViewModel(),
            clientAddress = record.get('c_client_addr'),
            serverAddress = record.get('s_server_addr'),
            policyId = record.get('policy_id');

        var policy = Ext.getStore('policies').findRecord('policyId', policyId);
        if(policy == null){
            return;
        }
        var appInstance = Ext.Array.findBy(policy.get('instances').list, function (inst) {
            return inst.appName === "threat-prevention";
        });
        if(appInstance == null){
            return;
        }

        var localNetworks = Rpc.directData('rpc.reportsManager.getReportInfo', "threat-prevention", policyId, "localNetworks");
        var ipAddresses = [];
        var urlAddresses = [];
        Ung.common.TableConfig.threatprevention.sourceTypes.forEach( function(sourceType){
            var reputation = record.get('threat_prevention_' +sourceType+'_reputation');
            if(reputation != null && reputation > 0){
                localNetworks.forEach(function(network){
                    var clientIsRemote = false;
                    if( (sourceType == 'client') &&
                        (record.get('threat_prevention_client_reputation') > 0) &&
                        (false === Util.ipMatchesNetwork(clientAddress, network['maskedAddress'], network['netmaskString'] ))){
                        ipAddresses.push(clientAddress);
                        clientIsRemote = true;
                    }
                    if( (sourceType == 'server') &&
                        (record.get('threat_prevention_server_reputation') > 0) &&
                        (false === Util.ipMatchesNetwork(serverAddress, network['maskedAddress'], network['netmaskString'] ))){
                        if(clientIsRemote == true){
                            ipAddresses.push(serverAddress);
                        }else{
                            urlAddresses.push(serverAddress);
                        }
                    }
                });
            }
        });

        // // TEST FOR IP
        // ipAddresses = urlAddresses;
        // urlAddresses = [];

        urlAddresses.push();
        // console.log(ipAddresses);
        // console.log(urlAddresses);

        var app = Rpc.directData('rpc.appManager.app', appInstance.id);

        var rpcSequence = [];
        if(ipAddresses.length){
            ipAddresses.forEach(function(address){
                rpcSequence.push(Rpc.asyncPromise('rpc.reportsManager.getReportInfo', "threat-prevention", policyId, 'getIpHistory', [address]));
            });
        }
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
            // console.log("result=");
            // console.log(results);

            var propertyRecord = [];

            var categories = [];
            var history = [];

            var category = null;
            results.forEach( function(result){
                result.forEach( function(answer){
                    var answerAddress = 'url' in answer ? answer['url'] : answer['value'];
                    var addressType = answerAddress == clientAddress ? 'Client'.t() : 'Server'.t();

                    // console.log('answer=');
                    // console.log(answer);
                    Ext.Object.each(
                        Ung.common.TableConfig.threatprevention.detailMaps,
                        function(detail, detailMap){
                            if(detail in answer['queries']){
                                // console.log('detail=' + detail);
                                category = Ext.String.format('Threat Prevention {0}: {1}'.t(), detailMap.name, addressType); 
                                // console.log('use detailMap');
                                // console.log(detailMap);
                                Ext.Object.each( answer['queries'][detail], function(key, value){
                                    propertyRecord.push({
                                        name: detailMap.fields[key] && detailMap.fields[key]['name'] ? detailMap.fields[key]['name'] : key,
                                        value: detailMap.fields[key] && detailMap.fields[key]['renderer'] ? detailMap.fields[key]['renderer'].call(this,value) : value,
                                        category: category
                                    });
                                });
                            }
                        }
                    );
                });
            });
            // console.log('prop grid');
            // console.log(v.up().down('unpropertygrid'));
            v.up().down('unpropertygrid').getStore().loadData(propertyRecord, true);
            // vm.set('propsData', propertyRecord);
            // vm.get('props').loadData(propertyRecord);
        }, function(ex) {
            // if(!Util.isDestroyed(v, vm)){
            //     vm.set('panel.saveDisabled', true);
            //     v.setLoading(false);
            // }
            console.log(ex);
        });
    },
});