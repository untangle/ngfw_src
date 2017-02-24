Ext.define('Ung.node.SpamBlocker', {
    extend: 'Ext.tab.Panel',
    xtype: 'ung.untangle-node-spam-blocker',
    layout: 'fit',
    /* requires-start */
    requires: [
        'Ung.view.grid.Grid',
        'Ung.model.GenericRule'
    ],
    /* requires-end */
    defaults: {
        border: false
    },


    items: [{
        xtype: 'nodestatus',
        hasChart: true,
        viewModel: {
            data: {
                summary: "Spam Blocker detects, blocks, and quarantines spam before it reaches users' mailboxes.".t()
            }
        }
    }]
});
