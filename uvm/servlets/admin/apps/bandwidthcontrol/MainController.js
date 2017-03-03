Ext.define('Ung.apps.bandwidthcontrol.MainController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.app-bandwidthcontrol',

    control: {
        '#status': {
            afterrender: 'onAfterRender'
        }
    },

    onAfterRender: function (view) {
        console.log(this.getView().appManager);
        this.getViewModel().set('isConfigured', this.getView().appManager.getSettings().configured);
    },

    runWizard: function (btn) {
        this.wizard = this.getView().add({
            xtype: 'app-bandwidthcontrol-wizard',
            appManager: this.getView().appManager
        });
        this.wizard.show();
    }
});
