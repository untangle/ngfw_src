Ext.define('Ung.node.CaptivePortal', {
    extend: 'Ext.tab.Panel',
    xtype: 'ung.untangle-node-captive-portal',
    layout: 'fit',

    defaults: {
        border: false
    },

    items: [{
        xtype: 'nodestatus',
        hasChart: true,
        viewModel: {
            data: {
                summary: 'Captive Portal allows administrators to require network users to complete a defined process, such as logging in or accepting a network usage policy, before accessing the internet.'.t()
            }
        }
    }]
});
