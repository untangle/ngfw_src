Ext.define('Ung.apps.bandwidthcontrol.ConfWizard', {
    extend: 'Ext.window.Window',
    alias: 'widget.app-tunnel-vpn-wizard',
    title: '<i class="fa fa-magic"></i> ' + 'Bandwidth Control Setup Wizard'.t(),
    modal: true,

    controller: 'app-tunnel-vpn-wizard',
    viewModel: {
        type: 'app-tunnel-vpn-wizard'
    },

    width: 800,
    height: 450,

    layout: 'card',

    defaults: {
        border: false,
        scrollable: 'y',
        bodyPadding: 10
    },

    items: [{
        title: 'Welcome'.t(),
        header: false,
        itemId: 'welcome',
        layout: {
            type: 'vbox'
        },
        items: [{
            xtype: 'component',
            html: '<h2>' + 'Welcome to the Tunnel VPN Wizard!'.t() + '</h2>',
        }, {
            xtype: 'component',
            html: '<p>' + 'This wizard will configure a new tunnel VPN connection to a remote VPN server/service.'.t() + '</p>',
        }]
    }, {
        title: 'VPN Service'.t(),
        header: false,
        itemId: 'vpn-service',
        items: [{
            xtype: 'component',
            html: '<h2>' + 'Choose a the VPN service provider'.t() + '</h2>'
        }, {
            xtype: 'combo',
            fieldLabel: 'Configuration'.t(),
            margin: 10,
            editable: false,
            bind: '{selectedProvider}',
            store: [
                ['Untangle', 'Untangle'.t()],
                ['NordVPN', 'NordVPN'.t()],
                ['ExpressVPN', 'ExpressVPN'.t()],
                ['Custom', 'Custom'.t()]
            ]
        }]
    }, {
        title: 'Configure VPN Service'.t(),
        header: false,
        itemId: 'upload',
        items: [{
            xtype: 'component',
            margin: 10,
            bind: {
                html: '{providerTitle}'
            }
        },{
            xtype: 'component',
            margin: 10,
            bind: {
                html: '{providerInstructions}'
            }
        },{
            xtype: 'form',
            name: 'upload_form',
            border: false,
            margin: '0 0 0 0',
            items: [{
                xtype: 'fileuploadfield',
                name: 'upload_file',
                buttonText: 'Upload Config File'.t(),
                buttonOnly: true,
                listeners: { 'change': 'uploadFile' }
            },{
                xtype: 'hidden',
                name: 'type',
                value: 'tunnel_vpn'
            },{
                xtype: 'hidden',
                name: 'argument',
                bind: {
                    value: '{provider}',
                },
            }]
        }]
    }, {
        title: 'Traffic'.t(),
        header: false,
        itemId: 'traffic',
        items: [{
            xtype: 'component',
            html: '<h2>' + 'This step configures which hosts on your network will use the VPN'.t() + '</h2>'
        }, {
            xtype: 'checkbox',
            bind: "{trafficConfig.allTraffic}",
            fieldLabel: "All outbound hosts should use the Tunnel (when connected)".t(),
            labelWidth: 250,
            checked: true
        }, {
            xtype: 'component',
            margin: '10 0 5 0',
            hidden: false,
            bind: {
                hidden: '{!trafficConfig.allTraffic}',
            },
            html: '<strong>' + 'FIXME configure traffic'.t() + '</strong><br/>'
        }]
    }, {
        title: 'Finish'.t(),
        header: false,
        itemId: 'finish',
        items: [{
            xtype: 'component',
            html: '<h2>' + 'Congratulations'.t() + '</h2>'
        }, {
            xtype: 'component',
            html: '<p><strong>' + 'A new tunnel VPN connection has been added.'.t() + '</strong></p>'
        }]
    }],

    dockedItems: [{
        xtype: 'toolbar',
        dock: 'bottom',
        ui: 'footer',
        defaults: {
            minWidth: 200
        },
        items: [{
            hidden: true,
            bind: {
                text: 'Previous'.t() + ' - <strong>' + '{prevBtnText}' + '</strong>',
                hidden: '{!prevBtn}'
            },
            iconCls: 'fa fa-chevron-circle-left',
            handler: 'onPrev'
        }, '->',  {
            hidden: true,
            bind: {
                text: 'Next'.t() + ' - <strong>' + '{nextBtnText}' + '</strong>',
                hidden: '{!nextBtn}'
            },
            iconCls: 'fa fa-chevron-circle-right',
            iconAlign: 'right',
            handler: 'onNext'
        }, {
            text: 'Close'.t(),
            hidden: true,
            bind: {
                hidden: '{nextBtn}'
            },
            iconCls: 'fa fa-check',
            handler: 'onFinish'
        }]
    }]


});
