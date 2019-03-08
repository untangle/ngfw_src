Ext.define('Ung.config.network.view.Services', {
    extend: 'Ext.form.Panel',
    alias: 'widget.config-network-services',
    itemId: 'services',
    scrollable: true,

    withValidation: true, // requires validation on save
    viewModel: true,

    title: 'Services'.t(),
    bodyPadding: 10,

    tbar: [{
        xtype: 'tbtext',
        padding: '8 5',
        style: { fontSize: '12px' },
        html: '<strong>' + 'Local Services'.t() + '</strong>'
    }],

    defaults: {
        allowDecimals: false,
        minValue: 0,
        allowBlank: false,
        vtype: 'port',
        labelAlign: 'right'
    },

    items: [{
        xtype: 'component',
        html: '<br/>' + 'The specified HTTPS port will be forwarded from all interfaces to the local HTTPS server to provide administration and other services.'.t() + '<br/>',
        margin: '0 0 10 0'
    }, {
        xtype: 'numberfield',
        fieldLabel: 'HTTPS port'.t(),
        width: 200,
        name: 'httpsPort',
        bind: '{settings.httpsPort}',
        blankText: 'You must provide a valid port.'.t()
    }, {
        xtype: 'component',
        html: '<br/>' + 'The specified HTTP port will be forwarded on non-WAN interfaces to the local HTTP server to provide administration, blockpages, and other services.'.t() + '<br/>',
        margin: '0 0 10 0'
    }, {
        xtype: 'numberfield',
        fieldLabel: 'HTTP port'.t(),
        width: 200,
        name: 'httpPort',
        bind: '{settings.httpPort}',
        blankText: 'You must provide a valid port.'.t(),
    }]

});
