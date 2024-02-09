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
        disabled: '{!state.on}',
    },

    layout: 'fit',
    items: [{
        xtype: 'component',
        reference: 'appchart',
        width: '100%',
        height: '100%'
    }]
});
