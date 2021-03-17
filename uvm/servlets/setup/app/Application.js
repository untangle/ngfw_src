// Setup Wizard application
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
                // No remote configuration
                rpc.wizardSettings.steps = ['Welcome', 'License', 'ServerSettings', 'Interfaces', 'Internet', 'InternalNetwork', 'AutoUpgrades', 'Complete'];
            }else{
                if(rpc.remoteReachable){
                    // Can get to remote server, so show stub
                    rpc.wizardSettings.steps = ['Welcome'];
                }else{
                    // Need to configure internet to be reachable to remote.
                    Util.setRpcJsonrpc("admin");
                    rpc.wizardSettings.steps = ['Welcome', 'Internet', 'Complete'];
                }
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
            passwordConfirmCheckText: 'Passwords do not match'.t()
        });
    }
});
