Ext.define('Ung.config.network.view.DnsServer', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.config-network-dns-server',
    itemId: 'dns-server',
    title: 'DNS Server'.t(),

    html: '',
    listeners: {
        afterrender: function (view) {
            view.setHtml('<iframe src="/vue/NgfwDns" class="vue-iframe"/>');
        }
    }
});
