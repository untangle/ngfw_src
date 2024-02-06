Ext.define('Ung.config.network.view.DhcpServer', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.config-network-dhcp-server',
    itemId: 'dhcp-server',
    title: 'DHCP Server'.t(),

    html: '',
    listeners: {
        afterrender: function (view) {
            view.setHtml('<iframe src="/vue/NgfwDhcp" class="vue-iframe"/>');
        }
    }
});
