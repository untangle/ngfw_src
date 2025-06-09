// Legacy Setup Wizard application
Ext.define('Ung.Setup', {
    extend: 'Ext.app.Application',
    namespace: 'Ung',
    autoCreateViewport: false,
    name: 'Ung',
    rpc: null,
    mainView: 'Ung.Setup.Main',

    loading: function (msg) {
        this.getMainView().down('setupwizard').setLoading(msg);
    },

    launch: function () {
        // Configure steps if they're not defined.

        if (!rpc.wizardSettings.steps || rpc.wizardSettings.steps.length == 0) {
            // Wizard steps not defined.
            if(!rpc.remote){
                // Local configuration
                rpc.wizardSettings.steps = ['Welcome', 'License', 'ServerSettings', 'Interfaces', 'Internet', 'InternalNetwork', 'Wireless', 'AutoUpgrades', 'Complete'];
            }else{
                rpc.wizardSettings.steps = ['Welcome', 'Internet', 'Complete'];
            }
        }
        Ext.apply(Ext.form.field.VTypes, {
            ipAddress: function (val) {
                return val.match(this.ipAddressRegex);
            },
            ipAddressText: 'Please enter a valid IP Address',
            ipAddressRegex: /\b(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\b/,

            passwordConfirmCheck: function (val, field) {
                var pass_original = Ext.getCmp(field.comparePasswordField);
                return val === pass_original.getValue();
            },
            passwordConfirmCheckText: 'Passwords do not match'.t(),

            ip4Address: function (val) {
                return val.match(this.ip4AddressRegex);
            },
            ip4AddressText: 'Invalid IPv4 Address.'.t(),
            ip4AddressRegex: /^(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$/
        });
    }
});

// New Setup Wizard application
Ext.define('Ung.SetupWizard', {
    extend: 'Ext.container.Viewport',

    viewModel: {
        data: {
            resuming: false,
            remoteReachable: null
        }
    },
    layout: 'fit',
    padding: 20,
    listeners: {
        afterrender: function (view) {
            view.setHtml('<iframe src="/console/setup/" style="position: absolute; top: 0; left: 0; width: 100%; height: 100%; border: none;"></iframe>');
        },
    }

});
