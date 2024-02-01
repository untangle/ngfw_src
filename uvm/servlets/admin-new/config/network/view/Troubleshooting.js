Ext.define('Ung.config.network.view.Troubleshooting', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.config-network-troubleshooting',
    itemId: 'troubleshooting',
    title: 'Troubleshooting'.t(),
    scrollable: true,
    withValidation: false,
    layout: 'fit',

    tbar: [{
        xtype: 'tbtext',
        padding: '8 5',
        style: { fontSize: '12px' },
        html: '<strong>' + 'Network Tests'.t() + '</strong>'
    }],

    items: [{
        xtype: 'tabpanel',
        itemId: 'troubleshooting',

        items: [{
            xtype: 'networktest',
            itemId: 'connectivity',
            title: 'Connectivity Test'.t(),
            viewModel: {
                data: {
                    description: 'The <b>Connectivity Test</b> verifies a working connection to the Internet.'.t(),
                    emptyText: 'Connectivity Test Output'.t(),
                },
                formulas: {
                    command: function (get) {
                        return "CONNECTIVITY";
                    },
                    arguments: function(get){
                        return {
                            "DNS_TEST_HOST": get('dnsTestHost'),
                            "TCP_TEST_HOST": get('tcpTestHost')
                        };
                    }
                }
            }
        }, {
            xtype: 'networktest',
            itemId: 'ping',
            title: 'Ping Test'.t(),

            commandFields: [{
                xtype: 'textfield',
                width: 200,
                emptyText : 'IP Address or Hostname'.t(),
                allowBlank: false,
                bind: '{destination}'
            }],

            viewModel: {
                data: {
                    description: 'The <b>Ping Test</b> can be used to test that a particular host or client can be pinged'.t(),
                    emptyText: 'Ping Test Output'.t(),
                    destination: null
                },
                formulas: {
                    command: function (get) {
                        return "REACHABLE";
                    },
                    arguments: function(get){
                        return {
                            "HOST": get('destination')
                        };
                    }
                }
            }
        }, {
            xtype: 'networktest',
            itemId: 'dns',
            title: 'DNS Test'.t(),

            commandFields: [{
                xtype: 'textfield',
                width: 200,
                emptyText : 'Hostname'.t(),
                allowBlank: false,
                bind: '{destination}'
            }],

            viewModel: {
                data: {
                    description: 'The <b>DNS Test</b> can be used to test DNS lookups'.t(),
                    emptyText: 'DNS Test Output'.t(),
                    destination: null
                },
                formulas: {
                    command: function (get) {
                        return "DNS";
                    },
                    arguments: function(get){
                        return {
                            "HOST": get('destination')
                        };
                    }
                }
            }
        }, {
            xtype: 'networktest',
            itemId: 'connection',
            title: 'Connection Test'.t(),

            commandFields: [{
                xtype: 'textfield',
                width: 200,
                emptyText : 'IP Address or Hostname'.t(),
                allowBlank: false,
                bind: '{destination}'
            }, {
                xtype: 'numberfield',
                minValue : 1,
                maxValue : 65536,
                width: 80,
                emptyText: 'Port'.t(),
                allowBlank: false,
                bind: '{port}'
            }],

            viewModel: {
                data: {
                    description: 'The <b>Connection Test</b> verifies that Arista can open a TCP connection to a port on the given host or client.'.t(),
                    emptyText: 'Connection Test Output'.t(),
                    destination: null,
                    port: null
                },
                formulas: {
                    command: function (get) {
                        return "CONNECTION";
                    },
                    arguments: function(get){
                        return {
                            "HOST": get('destination'),
                            "HOST_PORT": get('port')
                        };
                    }
                }
            }
        }, {
            xtype: 'networktest',
            itemId: 'traceroute',
            title: 'Traceroute Test'.t(),

            commandFields: [{
                xtype: 'textfield',
                width: 200,
                emptyText : 'IP Address or Hostname'.t(),
                allowBlank: false,
                bind: '{destination}'
            }, {
                xtype: 'combo',
                editable: false,
                width: 100,
                store: [['U','UDP'], ['T','TCP'], ['I','ICMP']],
                bind: '{protocol}'
            }],

            viewModel: {
                data: {
                    description: 'The <b>Traceroute Test</b> traces the route to a given host or client.'.t(),
                    emptyText: 'Traceroute Test Output'.t(),
                    destination: '',
                    protocol: 'U'
                },
                formulas: {
                    command: function (get) {
                        return "PATH";
                    },
                    arguments: function(get){
                        return {
                            "HOST": get('destination'),
                            "PROTOCOL": get('protocol')
                        };
                    }
                }
            }
        }, {
            xtype: 'networktest',
            itemId: 'download',
            title: 'Download Test'.t(),

            commandFields: [{
                xtype: 'combo',
                width: 500,
                store : [
                    ['http://cachefly.cachefly.net/50mb.test','http://cachefly.cachefly.net/50mb.test'],
                    ['http://cachefly.cachefly.net/5mb.test','http://cachefly.cachefly.net/5mb.test'],
                    ['http://download.thinkbroadband.com/50MB.zip','http://download.thinkbroadband.com/50MB.zip'],
                    ['http://download.thinkbroadband.com/5MB.zip','http://download.thinkbroadband.com/5MB.zip']
                ],
                bind: '{url}'
            }],

            viewModel: {
                data: {
                    description: 'The <b>Download Test</b> downloads a file.'.t(),
                    emptyText: 'Download Test Output'.t(),
                    url: 'http://cachefly.cachefly.net/5mb.test'
                },
                formulas: {
                    command: function (get) {
                        return "DOWNLOAD";
                    },
                    arguments: function(get){
                        return {
                            "URL": get('url'),
                        };
                    }
                }
            }
        }, {
            xtype: 'networktest',
            itemId: 'packet',
            title: 'Packet Test'.t(),

            commandFields: [{
                xtype: 'segmentedbutton',
                bind: '{mode}',
                margin: '0 5 0 0',
                items:[{
                    text: 'Basic'.t(),
                    value: 'basic'
                },{
                    text: 'Advanced'.t(),
                    value: 'advanced'
                }]
            }, {
                xtype: 'textfield',
                fieldLabel: 'Host'.t(),
                labelAlign: 'right',
                width: 220,
                emptyText : 'IP Address or Hostname'.t(),
                disabled: true,
                hidden: true,
                bind: {
                    value: '{destination}',
                    hidden: '{mode === "advanced"}',
                    disabled: '{mode === "advanced"}'
                }
            }, {
                xtype: 'numberfield',
                minValue : 1,
                maxValue : 65536,
                width: 80,
                emptyText: 'Port'.t(),
                disabled: true,
                hidden: true,
                bind: {
                    value: '{port}',
                    disabled: '{mode === "advanced"}',
                    hidden: '{mode === "advanced" }'
                }
            }, {
                xtype: 'combo',
                fieldLabel: 'Interface'.t(),
                width: 200,
                labelAlign: 'right',
                editable: false,
                disabled: true,
                hidden: true,
                forceSelection: true,
                bind: {
                    store: '{interfacesListSystemDev}',
                    value: '{interface}',
                    disabled: '{mode === "advanced"}',
                    hidden: '{mode === "advanced" }'
                },
                listeners:{
                    show: function(combo){
                        if (combo.getStore().count() > 0) {
                            combo.setValue(combo.getStore().first().get("field1"));
                        }
                    }
                }
            }, {
                xtype: 'textfield',
                fieldLabel: 'Arguments'.t(),
                labelAlign: 'right',
                width: 500,
                emptyText : 'Tcpdump Arguments'.t(),
                disabled: true,
                hidden: true,
                bind: {
                    value: '{tcpdumpArguments}',
                    hidden: '{mode === "basic"}',
                    disabled: '{mode === "basic"}'
                }
            }, {
                xtype: 'combo',
                fieldLabel: 'Timeout'.t(),
                labelAlign: 'right',
                editable: true,
                store: [[ 5, '5 seconds'.t()],
                        [ 30, '30 seconds'.t()],
                        [ 120, '120 seconds'.t()]],
                bind: '{timeout}'
            }],

            viewModel: {
                data: {
                    description: 'The <b>Packet Test</b> can be used to view packets on the network wire for troubleshooting.'.t(),
                    emptyText: 'Packet Test Output'.t(),
                    mode: 'basic',
                    destination: 'any',
                    port: '',
                    timeout: 5,
                    tcpdumpArguments: '-n -s 65535',
                    exportAction: true,
                },
                formulas: {
                    command: function (get) {
                        return "TRACE";
                    },
                    arguments: function(get){
                        var filename = "xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx".replace(/[xy]/g, function(c) {
                            var r = Math.random()*16|0, v = c == 'x' ? r : (r&0x3|0x8);
                            return v.toString(16);
                        }) + ".pcap";
                        this.set('exportRunFilename', filename);
                        return {
                            "TIMEOUT": get('timeout'),
                            "MODE": get('mode'),
                            "TRACE_ARGUMENTS": get('tcpdumpArguments'),
                            "HOST": get('destination'),
                            "HOST_PORT": get('port'),
                            "INTERFACE": get('interface'),
                            "FILENAME": filename
                        };
                    },
                    interface: {
                        single: true,
                        get: function(get){
                            return get('settings.interfaces.list')[0]["systemDev"];
                        }
                    },
                    interfacesListSystemDev: function (get) {
                        var data = [], i,
                            interfaces = get('settings.interfaces.list');
                        for (i = 0 ; i < interfaces.length; i += 1) {
                            data.push([interfaces[i].systemDev, interfaces[i].name]);
                        }
                        data.push(['tun0', 'OpenVPN']);
                        return data;
                    }
                }
            }
        }]
    }],
});
