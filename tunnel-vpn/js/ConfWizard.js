Ext.define('Ung.apps.tunnel-vpn.view.ConfWizard', {
    extend: 'Ext.window.Window',
    alias: 'widget.app-tunnel-vpn-wizard',
    title: '<i class="fa fa-magic"></i> ' + 'Tunnel VPN Setup Wizard'.t(),
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
            fieldLabel: 'Provider'.t(),
            width: 400,
            margin: 10,
            editable: false,
            bind: '{selectedProvider}',
            store: [
                ['Untangle', 'Untangle'.t()],
                ['NordVPN', 'NordVPN'.t()],
                ['ExpressVPN', 'ExpressVPN'.t()],
                ['CustomZip', 'Custom zip file'.t()],
                ['CustomZipPass', 'Custom zip file with username/password'.t()],
                ['CustomOvpn', 'Custom ovpn file'.t()],
                ['CustomOvpnPass', 'Custom ovpn file with username/password'.t()],
                ['CustomConf', 'Custom conf file'.t()],
                ['CustomConfPass', 'Custom conf file with username/password'.t()],
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
                xtype: 'container',
                layout: 'hbox',
                items: [{
                    xtype: 'filefield',
                    label: 'Upload Config File'.t(),
                    name: 'upload_file',
                    buttonText: 'Select Config File...'.t(),
                    width: 300,
                    listeners: {
                        change: 'uploadFile'
                    }
                },{
                    xtype: 'label',
                    bind: '{fileResult}'
                }]
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
        },{
            xtype: 'textfield',
            name: 'username',
            fieldLabel: 'Username'.t(),
            hidden: true,
            bind: {
                value: '{username}',
                hidden: '{usernameHidden}'
            },
            listeners: {
                change: 'nextCheckConfig'
            }
        },{
            xtype: 'textfield',
            fieldLabel: 'Password'.t(),
            name: 'password',
            hidden: true,
            bind: {
                value: '{password}',
                hidden: '{passwordHidden}'
            },
            listeners: {
                change: 'nextCheckConfig'
            }
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
        }, {
            xtype: 'component',
            html: '<p>' + 'Now you can configure the <i>Rules</i> to control what traffic uses this Tunnel VPN connection.'.t() + '</p>'
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
                hidden: '{!nextBtn}',
                disabled: '{nextEnabled == false}'
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
