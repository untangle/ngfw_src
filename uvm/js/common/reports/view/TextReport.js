Ext.define('Ung.view.reports.TextReport', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.textreport',

    viewModel: true,
    controller: 'textreport',

    border: false,
    bodyBorder: false,

    padding: '50 10',

    bodyStyle: {
        fontFamily: 'Roboto Condensed, Arial, sans-serif',
        textAlign: 'center',
        fontSize: '16px'
    }
});
