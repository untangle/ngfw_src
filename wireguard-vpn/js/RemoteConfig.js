Ext.define('Ung.apps.wireguard-vpn.RemoteConfig', {
    extend: 'Ext.window.Window',
    alias: 'widget.app-wireguard-vpn-remote-config',
    renderTo: Ext.getBody(),
    constrain: true,

    scrollable: true,

    onEsc: Ext.emptyFn,
    closable: false,
    modal: true,

    controller: 'app-wireguard-vpn-remote-config',

    viewModel:{
        data: {
            minDate: null,
            maxDate: null
        }
    },

    bodyStyle: {
        background: 'white'
    },

    // NGFW-13550 give a predefined width/height for the dialog
    layout: 'fit',
    width: 500,
    height: 500,

    items: [{
        xtype: 'panel',

        bodyStyle: {
            "background-color": 'white'
        },
        bodyPadding: 10,
        border: false,
        layout: {
            type: 'vbox',
            align: 'stretch'
        },
        scrollable: true,
        // NGFW-13550 - move type selection into a docked toolbar
        dockedItems: [{
            xtype: 'toolbar',
            dock: 'top',
            padding: 10,
            items: [{
                fieldLabel: 'Type'.t(),
                xtype: 'combobox',
                editable: false,
                hidden: false,
                bind:{
                    value: '{type}',
                    hidden: '{error}',
                },
                queryMode: 'local',
                store: [
                    [ 'qrcode', 'Quick Reference Code'.t()],
                    [ 'file', 'Configuration File'.t()]
                ],
                forceSelection: true,
            }]
        }],
        items:[{
            // NGFW-13550 - put qr-code image in a container so it's not stretched by above layout stretch
            xtype: 'container',
            layout: 'center',
            items: [{
                xtype: 'panel',
                border: false,
                itemId: 'qrcode',
                alt: 'Quick Reference Code'.t(),
                autoCreate: true,
            }],
            hidden: true,
            bind: {
                hidden: '{type != "qrcode" || error ? true : false}',
            },
        },{
            xtype: 'copytoclipboard',
            left: 'true',
            value:{
                key: 'html',
                stripPrefix: '<pre>',
                stripSuffix: '</pre>'
            },
            items:[{
                xtype: 'component',
                itemId: 'file',
                html: '',
                flex: 1,
            }],
            hidden: true,
            bind: {
                hidden: '{type != "file" || error? true : false}',
            },
        },{
            xtype: 'component',
            style: {
                'color': 'red',
                'font-weight': 'bold'
            },
            html: 'Unable to retrieve configuration'.t(),
            hidden: true,
            bind: {
                hidden: '{!error}',
            }
        }]
    }],

    buttons: [{
        text: 'Close'.t(),
        iconCls: 'fa fa-ban',
        handler: 'closeWindow'
    }]
});
