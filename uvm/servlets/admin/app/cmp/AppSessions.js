Ext.define('Ung.cmp.AppSessions', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.appsessions',

    title: 'Sessions'.t(),
    border: false,
    /* requires-start */
    requires: [
        'Ung.cmp.AppSessionsController'
    ],
    /* requires-end */
    controller: 'appsessions',
    viewModel: true,

    disabled: true,
    bind: {
        disabled: '{instance.targetState !== "RUNNING"}',
    },

    layout: 'fit',
    // bodyPadding: '10 10 10 0',
    items: [{
        xtype: 'component',
        reference: 'nodechart',
        width: '100%',
        height: '100%'
        // height: 150
    }]
});
