Ext.define('Ung.config.about.view.Server', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.config-about-server',
    itemId: 'server',
    scrollable: true,

    title: 'Server'.t(),

    bodyPadding: 10,

    defaults: {
        xtype: 'fieldset',
        padding: 10
    },

    items: [{
        title: 'About'.t(),
        items: [{
            xtype: 'component',
            bind:{
                html: 'Do not publicly post or share the system or account information.'.t() + '<br/>' +
                    'UID'.t() + ': ' + '{serverUID}' + 
                    '{serialNumber}'
            }
        },{
            xtype: 'component',
            itemId: 'account',
            html: '',
            hidden: true
        }]
    }, {
        bind: {
            html: 'Build'.t() + ': <strong>' + '{fullVersionAndRevision}' + '</strong> <br />' +
                'Kernel'.t() + ': <strong>' + '{kernelVersion}' + '</strong> <br />' +
                'History'.t() + ': <strong>' + '{modificationState}' + '</strong> <br />' +
                'Reboots'.t() + ': <strong>' + '{rebootCount}' + '</strong> <br />' +
                'Current active device count'.t() + ': <strong>' + '{activeSize}' + '</strong> <br />' +
                'Highest active device count since reboot'.t() + ': <strong>' + '{maxActiveSize}' + '</strong>'
        }
    }]

});
