Ext.define('Ung.Setup.AutoUpgrades', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.AutoUpgrades',

    title: 'Auto Upgrades'.t(),
    description: 'Automatic Upgrades and Command Center Access'.t(),

    layout: {
        type: 'vbox',
        align: 'stretch'
    },

    items: [{
        xtype: 'container',
        items: [{
            xtype: 'checkbox',
            boxLabel: '<strong>' + 'Automatically Install Upgrades'.t() + '</strong>',
            bind: { value: '{systemSettings.autoUpgrade}' }
        }, {
            xtype: 'component',
            margin: '0 0 0 20',
            html: 'Automatically install new versions of the software when available.'.t() + '<br/>' +
                'This is the recommended choice for most sites.'.t()
        }]
    }, {
        xtype: 'container',
        margin: '20 0 0 0',
        items: [{
            xtype: 'checkbox',
            boxLabel: '<strong>' + 'Connect to Command Center'.t() + '</strong>',
            bind: { value: '{systemSettings.cloudEnabled}' }
        }, {
            xtype: 'component',
            margin: '0 0 0 20',
            html: Ext.String.format('Remain securely connected to the Command Center for cloud management, hot fixes, and support access.'.t(), rpc.oemName) + '<br/>' +
                'This is the recommended choice for most sites.'.t()
        }]
    }],

    listeners: {
        activate: 'getSettings',
        save: 'onSave'
    },

    controller: {

        getSettings: function () {
            var me = this, vm = me.getViewModel();

            Ung.app.loading('Loading Automatic Upgrades Settings'.t());
            rpc.systemManager.getSettings(function (result, ex) {
                Ung.app.loading(false);
                if (ex) { Util.handleException(ex); return; }
                vm.set('systemSettings', result);

                // keep initial values for checking changes
                me.initialValues = {
                    autoUpgrade: result.autoUpgrade,
                    cloudEnabled: result.cloudEnabled
                };
            });
        },

        onSave: function (cb) {
            var me = this, vm = me.getViewModel();
            // if no changes skip to next step
            if (
                me.initialValues.autoUpgrade === vm.get('systemSettings.autoUpgrade') &&
                me.initialValues.cloudEnabled === vm.get('systemSettings.cloudEnabled')
            ) { cb(); return; }

            // if cloud enabled, enable support also
            if (vm.get('systemSettings.cloudEnabled')) {
                vm.set('systemSettings.supportEnabled', true);
            }

            Ung.app.loading('Saving Settings ...'.t());
            rpc.systemManager.setSettings(function (result, ex) {
                Ung.app.loading(false);
                if (ex) { Util.handleException(ex); return; }
                cb();
            }, vm.get('systemSettings'));
        }
    }
});
