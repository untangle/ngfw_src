Ext.define('Ung.view.reports.Reports', {
    extend: 'Ext.panel.Panel',
    xtype: 'ung.reports-vue',
    itemId: 'reports',

    layout: 'fit',
    border: false,

    viewModel: {
        data: {
            query: {}
        }
    },

    listeners: {
        activate: function (panel) {
            var target = panel.down('#iframeHolder');
            Util.attachIframeToTarget(target, '/console/reports', false);
        }
    },

    items: [{
        xtype: 'container',
        itemId: 'iframeHolder',
        layout: 'fit'
    }]
});
