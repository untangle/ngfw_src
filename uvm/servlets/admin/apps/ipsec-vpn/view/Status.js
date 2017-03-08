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
            html: '<img src="/skins/modern-rack/images/admin/apps/untangle-node-ipsec-vpn_80x80.png" width="80" height="80"/>' +
                '<h3>IPsec VPN</h3>' +
                '<p>' + 'IPsec VPN provides secure network access and tunneling to remote users and sites using IPsec, GRE, L2TP, Xauth, and IKEv2 protocols.'.t() + '</p>'
        }, {
            xtype: 'appstate',
        }, {
            xtype: 'grid',
            title: 'Enabled IPsec Tunnels'.t(),
            itemId: 'tunnelStatus',
            trackMouseOver: false,
            sortableColumns: false,
            enableColumnHide: false,
            // forceFit: true,
            minHeight: 150,
            maxHeight: 250,
            margin: '0 0 10 0',
            viewConfig: {
                emptyText: '<p style="text-align: center; margin: 0; line-height: 2;"><i class="fa fa-info-circle fa-2x"></i> <br/> ' + 'No IPsec Tunnels'.t() + ' ...</p>',
                stripeRows: false
            },

            collapsed: true,
            disabled: true,
            collapsible: true,
            hideCollapseTool: true,
            animCollapse: false,
            bind: {
                collapsed: '{instance.targetState !== "RUNNING"}',
                disabled: '{instance.targetState !== "RUNNING"}',
                store: { data: '{tunnelStatus}' }
            },

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
                width: 180
                // renderer: function(value) { return i18n.timestampFormat(value); }
            }, {
                header: 'Elapsed Time'.t(),
                dataIndex: 'sessionElapsed',
                width: 180
                // renderer: elapsedFormat
            },{
                header: 'Disconnect'.t(),
                xtype: 'actioncolumn',
                width: 80
                // items: [{
                //     iconCls: 'icon-delete-row',
                //     tooltip: i18n._("Click to disconnect client"),
                //     handler: Ext.bind(function(view, rowIndex, colIndex, item, e, record) {
                //         this.gridVirtualUsers.setLoading(i18n._("Disconnecting..."));
                //         this.getRpcNode().virtualUserDisconnect(Ext.bind(function(result, exception) {
                //             this.gridVirtualUsers.setLoading(false);
                //             if(Ung.Util.handleException(exception)) return;
                //             // it takes a second or two for the node to HUP the pppd daemon and the ip-down script
                //             // to call the node to remove the client from the active user list so instead of
                //             // calling reload here we just remove the disconnected row from the grid
                //             this.gridVirtualUsers.getStore().remove(record);
                //         }, this), record.get("clientAddress"), record.get("clientUsername"));
                //     }, this)
                // }]
            }],
            bbar: [{
                text: 'Refresh'.t(),
                iconCls: 'fa fa-refresh',
                handler: 'getTunnelStatus'
            }]

        }, {
            xtype: 'grid',
            title: 'Active VPN Sessions'.t(),
            itemId: 'virtualUsers',
            trackMouseOver: false,
            sortableColumns: false,
            enableColumnHide: false,
            // forceFit: true,
            minHeight: 150,
            maxHeight: 250,
            margin: '10 0 10 0',
            viewConfig: {
                emptyText: '<p style="text-align: center; margin: 0; line-height: 2;"><i class="fa fa-info-circle fa-2x"></i> <br/>' + 'No Virtual Users'.t() + ' ...</p>',
                stripeRows: false
            },

            collapsed: true,
            disabled: true,
            collapsible: true,
            // titleCollapse: true,
            hideCollapseTool: true,
            animCollapse: false,
            bind: {
                collapsed: '{instance.targetState !== "RUNNING"}',
                disabled: '{instance.targetState !== "RUNNING"}',
                store: { data: '{virtualUsers}' }
            },

            columns: [{
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
                width: 140
            }, {
                header: 'Remote Network'.t(),
                dataIndex: 'tmplDst',
                width: 140
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
            }, {
                header: 'Status'.t(),
                dataIndex: 'mode',
                renderer: function(value) {
                    var showtxt = 'Inactive'.t(),
                        showico = 'ua-cell-disabled';
                    if (value.toLowerCase() === 'active') {
                        showtxt = 'Active'.t();
                        showico = 'ua-cell-enabled';
                    }
                    if (value.toLowerCase() === 'unknown') {
                        showtxt = 'Unknown'.t();
                    }
                    return '<div class="' + showico + '">' + showtxt + '</div>';
                }
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
