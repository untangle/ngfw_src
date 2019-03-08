// test
Ext.define('Ung.Setup', {
    extend: 'Ext.app.Application',
    namespace: 'Ung',
    autoCreateViewport: false,
    name: 'Ung',
    rpc: null,
    // controllers: ['Global'],
    mainView: 'Ung.Setup.Main',

    loading: function (msg) {
        this.getMainView().down('setupwizard').setLoading(msg);
    },

    launch: function () {
        // var cards = [], steps = [], wizard = this.getMainView().down('#wizard');
        // Ext.Array.each(rpc.wizardSettings.steps, function (step) {
        //     cards.push({ xtype: step });
        //     steps.push({ margin: '0 2' });
        // });
        // steps.pop(); // remove one element from steps to skip welcome

        // add vtypes
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
        // wizard.addTool(steps); // add progress steps
        // wizard.add(cards); // add cards
        // wizard.setActiveItem(0); // trigger the activate event on welcome
    },
});
