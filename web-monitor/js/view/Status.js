Ext.define('Ung.apps.webmonitor.view.Status', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.app-web-monitor-status',
    itemId: 'status',
    title: 'Status'.t(),
    scrollable: true,
    withValidation: false,
    viewModel: true,

    layout: 'border',
    items: [{
        region: 'center',
        border: false,
        bodyPadding: 10,
        scrollable: 'y',
        items: [{
            xtype: 'component',
            cls: 'app-desc',
            html: '<img src="/icons/apps/web-monitor.svg" width="80" height="80"/>' +
                '<h3>Web Monitor</h3>' +
                '<p>' + 'Web Monitor scans and categorizes web traffic to monitor network usage.'.t() +
                '<br><br>' + 'Upgrade to Web Filter to control network usage and enforce policies that allow, block, flag or alert web traffic.'.t() +
                ' <a target="_blank" href="' + rpc.uriManager.getUriWithPath('https://edge.arista.com/shop/web-filter') + '">LEARN MORE<a/></p>'
        }, {
            xtype: 'applicense',
            hidden: true,
            bind: {
                hidden: '{!license || !license.trial}'
            }
        }, {
            xtype: 'appstate',
        }, {
            xtype: 'appreports'
        }]
    }, {
        region: 'west',
        border: false,
        width: Math.ceil(Ext.getBody().getViewSize().width / 4),
        split: true,
        items: [{
            xtype: 'appsessions',
            region: 'north',
            height: 200,
            split: true,
        }, {
            xtype: 'appmetrics',
            region: 'center'
        }],
        bbar: [{
            xtype: 'appremove',
            width: '100%'
        }]
    }]
});
