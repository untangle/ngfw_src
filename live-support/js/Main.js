Ext.define('Ung.apps.livesupport.Main', {
    extend: 'Ung.cmp.AppPanel',
    alias: 'widget.app-live-support',
    controller: 'app-live-support',

    viewModel: {
        data: {
            companyName: '',
            serverUID: '',
            fullVersionAndRevision: ''
        }
    },

    items: [{
        xtype: 'app-live-support-status'
    }]
});
