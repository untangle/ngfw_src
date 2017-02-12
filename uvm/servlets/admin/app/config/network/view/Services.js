Ext.define('Ung.config.network.view.Services', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.config.network.services',

    viewModel: true,

    title: 'Services'.t(),
    bodyPadding: 10,

    tbar: [{
        xtype: 'displayfield',
        padding: '0 10',
        value: '<strong>' + 'Local Services'.t() + '</strong>'
    }],

    defaults: {
        allowDecimals: false,
        minValue: 0,
        allowBlank: false,
        vtype: 'port'
    },

    items: [{
        xtype: 'component',
        html: '<br/>' + 'The specified HTTPS port will be forwarded from all interfaces to the local HTTPS server to provide administration and other services.'.t() + '<br/>',
        margin: '0 0 10 0'
    }, {
        xtype: 'numberfield',
        fieldLabel: 'HTTPS port'.t(),
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
        name: 'httpPort',
        bind: '{settings.httpPort}',
        blankText: 'You must provide a valid port.'.t(),
    }]

});