Ext.define('Ung.view.reports.GraphReport', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.graphreport',

    controller: 'graphreport',
    viewModel: true,

    border: false,
    bodyBorder: false,

    items: [{
        xtype: 'component',
        reference: 'graph',
        cls: 'chart'
    }]
});
