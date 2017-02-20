Ext.define('Ung.node.ApplicationControl', {
    extend: 'Ext.tab.Panel',
    xtype: 'ung.untangle-node-application-control',
    layout: 'fit',

    defaults: {
        border: false
    },

    items: [{
        xtype: 'nodestatus',
        hasChart: true,
        viewModel: {
            data: {
                summary: 'Application Control scans sessions and identifies the associated applications allowing each to be flagged and/or blocked.'.t()
            }
        }
    }]
});
