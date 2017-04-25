Ext.define('Ung.widget.Information', {
    extend: 'Ext.container.Container',
    alias: 'widget.informationwidget',

    controller: 'widget',

    hidden: true,
    border: false,
    baseCls: 'widget small info-widget adding',

    bind: {
        hidden: '{!widget.enabled}'
    },

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
            html: '<span class="info-lbl">' + 'uptime'.t() + ':</span><span class="info-val">{stats.uptimeFormatted}</span><br/>' +
                '<span class="info-lbl">' + 'Server'.t() + ':</span><span class="info-val">{stats.appliance}</span><br/>' +
                '<span class="info-lbl">' + 'CPU Count'.t() + ':</span><span class="info-val">{stats.numCpus}</span><br/>' +
                '<span class="info-lbl">' + 'CPU Type'.t() + ':</span><span class="info-val">{stats.cpuModel}</span><br/>' +
                '<span class="info-lbl">' + 'Architecture'.t() + ':</span><span class="info-val">{stats.architecture}</span><br/>' +
                '<span class="info-lbl">' + 'Memory'.t() + ':</span><span class="info-val">{stats.totalMemory}</span><br/>' +
                '<span class="info-lbl">' + 'Disk'.t() + ':</span><span class="info-val">{stats.totalDisk}</span>'
        }
    }]
});
