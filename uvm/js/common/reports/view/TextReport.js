Ext.define('Ung.view.reports.TextReport', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.textreport',

    viewModel: true,
    controller: 'textreport',

    border: false,
    bodyBorder: false,

    padding: 10,

    style: {
        fontSize: '14px'
    }
});
