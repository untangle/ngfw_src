Ext.define('Ung.node.Reports', {
    extend: 'Ext.tab.Panel',
    xtype: 'ung.untangle-node-reports',
    layout: 'fit',

    defaults: {
        border: false
    },

    items: [{
        xtype: 'nodestatus',
        hasChart: false,
        hasMetrics: false,
        viewModel: {
            data: {
                summary: 'Reports records network events to provide administrators the visibility and data necessary to investigate network activity.'.t()
            }
        }
    }]
});
