Ext.define('Ung.cmp.Rules', {
    extend: 'Ext.grid.Panel',
    xtype: 'ung.cmp.rules',

    requires: [
        'Ung.cmp.RulesController',
        'Ung.cmp.RuleEditor'
    ],

    controller: 'rules',

    plugins: [{
        ptype: 'rowediting',
        clicksToMoveEditor: 1,
        autoCancel: false
    }],

    config: {
        conditions: null,
        conditionsMap: null
    },

    tbar: [{
        text: 'Add Rule'.t(),
        iconCls: 'fa fa-plus'
    }],

    trackMouseOver: false,
    disableSelection: true,
    // columnLines: true,

});