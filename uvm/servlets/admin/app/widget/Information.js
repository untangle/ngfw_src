Ext.define('Ung.widget.Information', {
    extend: 'Ext.container.Container',
    alias: 'widget.informationwidget',

    controller: 'widget',

    border: false,
    baseCls: 'widget small info-widget',

    layout: {
        type: 'vbox',
        align: 'stretch'
    },

    items: [{
        xtype: 'component',
        cls: 'header',
        html: '<h1>' + 'Information'.t() + '</h1>'
    }, {
        xtype: 'component',
        cls: 'info-host',
        padding: 5,
        bind: {
            html: '<p class="hostname">{stats.hostname}</p><p class="version">' + 'version'.t() + ': {stats.version}</p>'
        }
    }, {
        xtype: 'component',
        margin: '10 0',
        bind : {
            html: '<table>' +
                    '<tr><td>' + 'uptime'.t() + ':</td><td>{stats.uptimeFormatted}</td></tr>' +
                    '<tr><td>' + 'Server'.t() + ':</td><td>{stats.appliance}</td></tr>' +
                    '<tr><td>' + 'CPU Count'.t() + ':</td><td>{stats.numCpus}</td></tr>' +
                    '<tr><td>' + 'CPU Type'.t() + ':</td><td>{stats.cpuModel}</td></tr>' +
                    '<tr><td>' + 'Architecture'.t() + ':</td><td>{stats.architecture}</td></tr>' +
                    '<tr><td>' + 'Memory'.t() + ':</td><td>{stats.totalMemory}</td></tr>' +
                    '<tr><td>' + 'Disk'.t() + ':</td><td>{stats.totalDisk}</td></tr>' +
                  '</table>'
        }
    }]
});
