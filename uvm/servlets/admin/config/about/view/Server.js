Ext.define('Ung.config.about.view.Server', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.config.about.server',

    title: 'Server'.t(),

    scrollable: true,
    bodyPadding: 10,

    defaults: {
        xtype: 'fieldset',
        padding: 10
    },

    items: [{
        title: 'About'.t(),
        items: [{
            xtype: 'component',
            html: 'Do not publicly post or share the UID or account information.'.t() + '<br/>' +
                'UID'.t() + ': ' + rpc.serverUID
        }]
    }, {
        bind: {
            html: 'Build'.t() + ': <strong>' + rpc.fullVersionAndRevision + '</strong> <br />' +
                'Kernel'.t() + ': <strong>' + '{kernelVersion}' + '</strong> <br />' +
                'History'.t() + ': <strong>' + '{modificationState}' + '</strong> <br />' +
                'Reboots'.t() + ': <strong>' + '{rebootCount}' + '</strong> <br />' +
                'Current active device count'.t() + ': <strong>' + '{activeSize}' + '</strong> <br />' +
                'Highest active device count since reboot'.t() + ': <strong>' + '{maxActiveSize}' + '</strong>'
        }
    }]

});
