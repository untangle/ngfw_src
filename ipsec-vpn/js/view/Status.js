Ext.define('Ung.apps.ipsecvpn.view.Status', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.app-ipsec-vpn-status',
    itemId: 'status',
    title: 'Status'.t(),

    layout: 'border',
    items: [{
        region: 'center',
        border: false,
        bodyPadding: 10,

        layout: {
            type: 'vbox',
            align: 'stretch'
        },

        scrollable: 'y',
        items: [{
            xtype: 'component',
            cls: 'app-desc',
            html: '<img src="/skins/modern-rack/images/admin/apps/ipsec-vpn_80x80.png" width="80" height="80"/>' +
                '<h3>IPsec VPN</h3>' +
                '<p>' + 'IPsec VPN provides secure network access and tunneling to remote users and sites using IPsec, GRE, L2TP, Xauth, and IKEv2 protocols.'.t() + '</p>'
        }, {
            xtype: 'applicense',
            hidden: true,
            bind: {
                hidden: '{!license || !license.trial}'
            }
        }, {
            xtype: 'appstate',
        }, {
            xtype: 'grid',
            title: 'Enabled IPsec Tunnels'.t(),
            itemId: 'tunnelStatus',
            trackMouseOver: false,
            sortableColumns: true,
            enableColumnHide: false,
            minHeight: 150,
            maxHeight: 800,
            resizable: true,
            margin: '10 0 10 0',
            viewConfig: {
                emptyText: '<p style="text-align: center; margin: 0; line-height: 2;"><i class="fa fa-info-circle fa-2x"></i> <br/>' + 'No IPsec Tunnels'.t() + ' ...</p>',
                stripeRows: false
            },

            recordJavaClass: 'com.untangle.app.ipsec_vpn.ConnectionStatusRecord',
            bind: '{tunnelStatusStore}',

            columns: [{
                header: 'Status'.t(),
                dataIndex: 'mode',
                renderer: function(value) {
                    var showtxt = 'Inactive'.t(),
                        showico = 'fa fa-circle fa-gray';
                    if (value.toLowerCase() === 'active') {
                        showtxt = 'Active'.t();
                        showico = 'fa fa-circle fa-green';
                    }
                    if (value.toLowerCase() === 'unknown') {
                        showtxt = 'Unknown'.t();
                        showico = 'fa fa-exclamation-triangle fa-orange';
                    }
                    return '<i class="' + showico + '">&nbsp;&nbsp;</i>' + showtxt;
                }
            }, {
                header: 'Local IP'.t(),
                dataIndex: 'src',
                width: 140
            }, {
                header: 'Remote Host'.t(),
                dataIndex: 'dst',
                width: 140
            }, {
                header: 'Local Network'.t(),
                dataIndex: 'tmplSrc',
                width: 200
            }, {
                header: 'Remote Network'.t(),
                dataIndex: 'tmplDst',
                width: 200
            }, {
                header: 'Description'.t(),
                dataIndex: 'proto',
                width: 200,
                flex: 1
            }, {
                header: 'Bytes In'.t(),
                dataIndex: 'inBytes',
                width: 100
            }, {
                header: 'Bytes Out'.t(),
                dataIndex: 'outBytes',
                width: 100
            }],
            bbar: [{
                text: 'Refresh'.t(),
                iconCls: 'fa fa-refresh',
                handler: 'getTunnelStatus'
            }]

        }, {
            xtype: 'component',
            html: '<BR>'
        }, {
            xtype: 'grid',
            title: 'Active VPN Sessions'.t(),
            itemId: 'virtualUsers',
            trackMouseOver: false,
            sortableColumns: true,
            enableColumnHide: false,
            minHeight: 150,
            maxHeight: 800,
            resizable: true,
            margin: '0 0 10 0',
            viewConfig: {
                emptyText: '<p style="text-align: center; margin: 0; line-height: 2;"><i class="fa fa-info-circle fa-2x"></i> <br/> ' + 'No VPN Sessions'.t() + ' ...</p>',
                stripeRows: false
            },

            recordJavaClass: 'com.untangle.app.ipsec_vpn.VirtualUserEntry',
            bind: '{virtualUserStore}',

            columns: [{
                header: 'IP Address'.t(),
                dataIndex: 'clientAddress',
                width: 150
            }, {
                header: 'Protocol'.t(),
                dataIndex: 'clientProtocol',
                width: 80
            }, {
                header: 'Username'.t(),
                dataIndex: 'clientUsername',
                width: 200,
                flex: 1
            }, {
                header: 'Interface'.t(),
                dataIndex: 'netInterface',
                width: 150
            }, {
                header: 'Connect Time'.t(),
                dataIndex: 'sessionCreation',
                width: 180,
                renderer: function(value) { return Util.timestampFormat(value); }
            }, {
                header: 'Elapsed Time'.t(),
                dataIndex: 'sessionElapsed',
                width: 180,
                renderer: function(value) {
                    var total = parseInt(value / 1000,10);
                    var hours = (parseInt(total / 3600,10) % 24);
                    var minutes = (parseInt(total / 60,10) % 60);
                    var seconds = parseInt(total % 60,10);
                    var result = (hours < 10 ? "0" + hours : hours) + ":" + (minutes < 10 ? "0" + minutes : minutes) + ":" + (seconds  < 10 ? "0" + seconds : seconds);
                    return result;
                }
            },{
                header: 'Disconnect'.t(),
                xtype: 'actioncolumn',
                width: 80,
                align: 'center',
                iconCls: 'fa fa-minus-circle',
                handler: 'disconnectUser',
            }],
            bbar: [{
                text: 'Refresh'.t(),
                iconCls: 'fa fa-refresh',
                handler: 'getVirtualUsers'
            }]
        }, {
            xtype: 'appreports'
        }]
    }, {
        region: 'west',
        border: false,
        width: 350,
        minWidth: 300,
        split: true,
        layout: 'fit',
        items: [{
            xtype: 'appmetrics',
        }],
        bbar: [{
            xtype: 'appremove',
            width: '100%'
        }]
    }]

});
