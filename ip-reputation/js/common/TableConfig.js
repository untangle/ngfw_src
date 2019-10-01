Ext.define('Ung.common.TableConfig.ipreputation', {
    singleton: true,

    sessionsFields: [{
        name: 'ip_reputation_blocked'
    }, {
        name: 'ip_reputation_firewall_flagged'
    }, {
        name: 'ip_reputation_rule_index'
    }, {
        name: 'ip_reputation_client_reputation',
        fromType: 'ip_reputation'
    }, {
        name: 'ip_reputation_client_threatmask',
        fromType: 'ip_threat'
    }, {
        name: 'ip_reputation_server_reputation',
        fromType: 'ip_reputation'
    }, {
        name: 'ip_reputation_server_threatmask',
        fromType: 'ip_threat'
    }],

    // To do categories, do nested. Then eventreport selection can construct.
    // Make sure that with thee groupings, export still works.  And reports in general.
    sessionsColumns: [{
        header: 'Blocked'.t() + ' (IP Reputation)',
        width: Renderer.booleanWidth,
        sortable: true,
        dataIndex: 'ip_reputation_blocked',
        filter: Renderer.booleanFilter
    }, {
        header: 'Flagged'.t() + ' (IP Reputation)',
        width: Renderer.booleanWidth,
        sortable: true,
        dataIndex: 'ip_reputation_flagged',
        filter: Renderer.booleanFilter
    }, {
        header: 'Rule Id'.t() + ' (IP Reputation)',
        width: Renderer.idWidth,
        sortable: true,
        flex:1,
        dataIndex: 'ip_reputation_rule_index',
        filter: Renderer.numericFilter
    }, {
        header: 'Client Reputation'.t() + ' (IP Reputation)',
        width: Renderer.idWidth,
        sortable: true,
        flex:1,
        dataIndex: 'ip_reputation_client_reputation',
        renderer: Renderer.ipReputation,
        filter: Renderer.numericFilter
    }, {
        header: 'Client Threatmask'.t() + ' (IP Reputation)',
        width: Renderer.idWidth,
        sortable: true,
        flex:1,
        dataIndex: 'ip_reputation_client_threatmask',
        renderer: Renderer.ipThreatmask,
        filter: Renderer.numericFilter
    }, {
        header: 'Server Reputation'.t() + ' (IP Reputation)',
        width: Renderer.idWidth,
        sortable: true,
        flex:1,
        dataIndex: 'ip_reputation_server_reputation',
        renderer: Renderer.ipReputation,
        filter: Renderer.numericFilter,
    }, {
        header: 'Server Threatmask'.t() + ' (IP Reputation)',
        width: Renderer.idWidth,
        sortable: true,
        flex:1,
        dataIndex: 'ip_reputation_server_threatmask',
        renderer: Renderer.ipThreatmask,
        filter: Renderer.numericFilter,
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

        tableConfig.tableConfig.sessions.listeners = {
            select: Ung.common.TableConfig.ipreputation.onSelectDetails
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
                    renderer: Renderer.ipPopularity
                },
                threathistory: {
                    name: 'Threat History'.t(),
                    renderer: function(value){
                        return Ext.String.format('{0} occurences'.t(), value);
                    }
                },
                reputation: {
                    name: 'Reputation'.t(),
                    renderer: Renderer.ipReputation
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
                    renderer: Renderer.ipReputation
                },
                min_reputation: {
                    name: 'Minimum Reputation'.t(),
                    renderer: Renderer.ipReputation
                },
                avg_reputation: {
                    name: 'Average Reputation'.t(),
                    renderer: Renderer.ipReputation
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
                            history.push(Renderer.timestamp(shistory['ts']) + ': ' + Renderer.ipReputation(shistory['reputation']));
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
                            // history.push(Renderer.timestamp(shistory['ts']) + ': ' + shistory['status'] + ': ' + shistory['threat_types'].join(',') );
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
            // propertyRecord = vm.get('propsData'),
            clientAddress = record.get('c_client_addr'),
            serverAddress = record.get('s_server_addr'),
            policyId = record.get('policy_id');
            // source = table.getColumnManager().getColumns()[cellIndex]['dataIndex'],
            // sourceType = (source == 's_server_addr' ? 'server' : 'client'),
            // vm = view.getViewModel(),
            // detailsGridId = 'threatDetail_' + sourceType,
            // detailsGrid = view.down('[itemId='+detailsGridId+']'),
            // detailsGridRecord = 'threatDetail_' + sourceType + '_record',
            // address = record.get(source),
            // reputation = record.get('ip_reputation_' +sourceType+'_reputation');

        // console.log('onSelectDetails: propertyRecord');
        // console.log(propertyRecord);

        var localNetworks = Rpc.directData('rpc.reportsManager.getReportInfo', "ip-reputation", policyId, "localNetworks");
        var ipAddresses = [];
        var urlAddresses = [];
        Ung.common.TableConfig.ipreputation.sourceTypes.forEach( function(sourceType){
            var reputation = record.get('ip_reputation_' +sourceType+'_reputation');
            if(reputation != null && reputation > 0){
                localNetworks.forEach(function(network){
                    var clientIsRemote = false;
                    if( (sourceType == 'client') &&
                        (record.get('ip_reputation_client_reputation') > 0) &&
                        (false === Util.ipMatchesNetwork(clientAddress, network['maskedAddress'], network['netmaskString'] ))){
                        ipAddresses.push(clientAddress);
                        clientIsRemote = true;
                    }
                    if( (sourceType == 'server') &&
                        (record.get('ip_reputation_server_reputation') > 0) &&
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

        // make appInstanceid a util method?
        var policy = Ext.getStore('policies').findRecord('policyId', record.get('policy_id'));
        var appInstance = Ext.Array.findBy(policy.get('instances').list, function (inst) {
            return inst.appName === "ip-reputation";
        });
        // console.log(appInstance);
        var app = Rpc.directData('rpc.appManager.app', appInstance.id);

        var rpcSequence = [];
        if(ipAddresses.length){
            ipAddresses.forEach(function(address){
                rpcSequence.push(Rpc.asyncPromise('rpc.reportsManager.getReportInfo', "ip-reputation", policyId, 'getIpHistory', [address]));
            });
        }
        if(urlAddresses.length){
            urlAddresses.forEach(function(address){
                rpcSequence.push(Rpc.asyncPromise('rpc.reportsManager.getReportInfo', "ip-reputation", policyId, 'getUrlHistory', [address]));
            });
        }

        Ext.Deferred.sequence(rpcSequence, this)
        .then(function(results){
            if(Util.isDestroyed(v)){
                return;
            }
            console.log("result=");
            console.log(results);

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
                        Ung.common.TableConfig.ipreputation.detailMaps,
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