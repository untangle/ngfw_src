Ext.define('Ung.node.Firewall', {
    extend: 'Ext.tab.Panel',
    xtype: 'ung.untangle-node-firewall',
    layout: 'fit',

    defaults: {
        border: false
    },

    items: [{
        xtype: 'nodestatus',
        hasChart: true,
        viewModel: {
            data: {
                summary: 'Firewall is a simple application that flags and blocks sessions based on rules.'.t()
            }
        }
    }]
});
