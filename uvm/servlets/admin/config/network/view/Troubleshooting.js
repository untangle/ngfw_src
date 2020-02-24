Ext.define('Ung.config.network.view.Troubleshooting', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.config-network-troubleshooting',
    itemId: 'troubleshooting',
    title: 'Troubleshooting'.t(),
    scrollable: true,

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
                        return [
                            '/bin/bash',
                            '-c',
                            ['echo -n "Testing DNS ... " ; success="Successful";',
                                'dig ' + get('dnsTestHost') + ' > /dev/null 2>&1; if [ "$?" = "0" ]; then echo "OK"; else echo "FAILED"; success="Failure"; fi;',
                                'echo -n "Testing TCP Connectivity ... ";',
                                'echo "GET /" | netcat -q 0 -w 15 ' + get('tcpTestHost') + ' 80 > /dev/null 2>&1;',
                                'if [ "$?" = "0" ]; then echo "OK"; else echo "FAILED"; success="Failure"; fi;',
                                'echo "Test ${success}!"'].join('')
                        ];
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
                        return 'ping -c 5 ' + get('destination');
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
                        return [
                            '/bin/bash',
                            '-c',
                            ['host \'' + get('destination') +'\';',
                                'if [ "$?" = "0" ]; then echo "Test Successful"; else echo "Test Failure"; fi;'].join('')
                        ];
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
                    description: 'The <b>Connection Test</b> verifies that Untangle can open a TCP connection to a port on the given host or client.'.t(),
                    emptyText: 'Connection Test Output'.t(),
                    destination: null,
                    port: null
                },
                formulas: {
                    command: function (get) {
                        return [
                            '/bin/bash',
                            '-c',
                            ['echo 1 | netcat -q 0 -v -w 15 \'' + get('destination') + '\' \'' + get('port') +'\';',
                                'if [ "$?" = "0" ]; then echo "Test Successful"; else echo "Test Failure"; fi;'].join('')
                        ];
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
                        return [
                            '/bin/bash',
                            '-c',
                            ['traceroute' + ' -' + get('protocol') + ' ' + get('destination') + ';',
                                'if [ "$?" = "0" ]; then echo "Test Successful"; else echo "Test Failure"; fi;'].join('')
                        ];
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
                        return [
                            '/bin/bash',
                            '-c',
                            ['wget --output-document=/dev/null ' + ' \'' + get('url') + '\' ;'].join('')
                        ];
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
                    tcpdumpArguments: '',
                    exportAction: true,
                    exportRunFilename: '',
                    exportFilename: ''
                },
                formulas: {
                    command: function (get) {
                        var timeout = parseInt(get('timeout'), 10);
                        if(isNaN(timeout)){
                            timeout = 5;
                        }

                        var traceFixedOptionsTemplate = ['-U', '-l', '-v'];
                        var traceOverrideOptionsTemplate = ['-n', '-s 65535', '-i ' + get('interface')];
                        var traceOptions = traceFixedOptionsTemplate.concat(traceOverrideOptionsTemplate);
                        var traceExpression = [];

                        var traceArguments = '';
                        if (get('mode') == "advanced") {
                            traceArguments = get('tcpdumpArguments');
                        } else {
                            if (get('destination') !== null && get('destination').toLowerCase() !== 'any') {
                                traceExpression.push('host ' + get('destination'));
                            }
                            if (get('port') !== null && get('port') != "") {
                                traceExpression.push('port ' + get('port'));
                            }
                            traceArguments = traceOptions.join(' ') + ' ' + traceExpression.join( ' and ');
                            this.set('tcpdumpArguments', traceArguments );
                        }

                        // !!! path here is kind of dumb!
                        if(get('exportRunFilename') == ''){
                            this.set('exportRunFilename', "/tmp/network-tests/" +
                                "xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx".replace(/[xy]/g, function(c) {
                                    var r = Math.random()*16|0, v = c == 'x' ? r : (r&0x3|0x8);
                                    return v.toString(16);
                                }) + ".pcap" );
                        }

                        // !!! And so is filename...
                        var script = [
                            '/usr/share/untangle/bin/ut-network-tests-packet.py'+
                                ' --timeout ' + timeout  +
                                ' --filename ' + get('exportRunFilename').replace('\'','') +
                                ' --arguments \'' + traceArguments.replace('\'','') + '\''
                        ];
                        return ["/bin/bash","-c", script.join("")];
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
