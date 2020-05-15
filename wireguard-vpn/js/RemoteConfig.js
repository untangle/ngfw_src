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
    defaults: {
        margin: 10
    },

    items: [{
        xtype: 'panel',
        bodyStyle: {
            "background-color": 'white'
        },
        border: false,
        layout: {
            type: 'vbox'
        },
        items:[{
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
        },{
            xtype: 'image',
            itemId: 'qrcode',
            alt: 'Quick Reference Code'.t(),
            autoCreate: true,
            height: 400,
            width: 400,
            hidden: true,
            bind: {
                hidden: '{type != "qrcode" || error ? true : false}',
            },
            flex: 1
        },{
            xtype: 'copytoclipboard',
            stripPrefix: '<pre>',
            stripSuffix: '</pre>',
            targetKey: 'html',
            items:[{
                xtype: 'component',
                itemId: 'file',
                html: '',
            }],
            hidden: true,
            bind: {
                hidden: '{type != "file" || error? true : false}',
            },
            flex: 1
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
