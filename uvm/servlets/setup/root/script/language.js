Ext.define('Ung.Lang.Main', {
    extend: 'Ext.container.Viewport',

    layout: 'center',
    padding: 20,
    items: [{
        xtype: 'container',
        padding: '0 0 300 0',
        layout: {
            type: 'vbox',
            align: 'center'
        },
        items: [{
            xtype: 'component',
            margin: '0 0 20 0',
            style: { textAlign: 'center' },
            html: '<img src="images/BrandingLogo.png" width=150 height=96/>'
        }, {
            xtype: 'container',
            layout: {
                type: 'hbox',
                align: 'end'
            },
            items: [{
                xtype: 'combo',
                fieldLabel: 'Please select your language',
                labelAlign: 'top',
                editable: false,
                valueField: 'code',
                displayField: 'languageName',
                store: {},
                queryMode: 'local'
            }, {
                xtype: 'button',
                margin: '0 0 0 5',
                text: 'Continue',
                iconCls: 'fa fa-play',
                handler: 'setLanguage'
            }]
        }]
    }],
    listeners: {
        afterrender: 'onAfterRender'
    },
    controller: {
        onAfterRender: function (view) {
            var combo = view.down('combo');
            combo.getStore().loadData(Ung.app.languageList.list);
            combo.setValue(Ung.app.languageSource + '-' + Ung.app.language);
        },

        setLanguage: function () {
            var me = this,
                source_language = me.getView().down('combo').getValue().split('-', 2);
            rpc.setup.setLanguage(function (result, ex) {
                if (ex) {
                    Ext.Msg('Error!', 'Unable to set the language');
                    return;
                }
                // Send the user to the setup wizard.
                window.location.href = 'index.do';
            }, source_language[1], source_language[0]);
        }
    }
});

Ext.define('Ung.Lang', {
    extend: 'Ext.app.Application',
    namespace: 'Ung',
    name: 'Ung',
    rpc: null,

    mainView: 'Ung.Lang.Main',

    launch: function () {
        rpc.setup = new JSONRpcClient('/setup/JSON-RPC').SetupContext;
    }
});
