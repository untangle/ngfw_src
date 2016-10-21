Ext.define('Ung.widget.Information', {
    extend: 'Ext.container.Container',
    alias: 'widget.informationwidget',

    controller: 'widget',

    hidden: true,
    border: false,
    baseCls: 'widget small',

    bind: {
        hidden: '{!widget.enabled}'
    },

    layout: {
        type: 'vbox',
        align: 'stretch'
    },

    items: [{
        xtype: 'container',
        layout: {
            type: 'hbox',
            align: 'top'
        },
        cls: 'header',
        style: {
            height: '50px'
        },
        items: [{
            xtype: 'component',
            flex: 1,
            html: '<h1>' + 'Information'.t() + '</h1>'
        }]
    }, {
        xtype: 'container',
        baseCls: 'info-widget',
        layout: {
            type: 'vbox',
            align: 'stretch'
        },
        items: [{
            xtype: 'component',
            cls: 'info-host',
            bind: { html: '{stats.hostname}' }
        }, {
            xtype: 'component',
            cls: 'info-version',
            bind: { html: '{stats.version}' }
        }, {
            layout: {
                type: 'table',
                columns: 2,
                tableAttrs: {
                    style: {
                        width: '100%'
                    }
                }
            },
            margin: '10 0',
            border: false,
            defaults: {
                border: false
            },
            items: [
                { html: 'uptime'.t() + ':', cellCls: 'info-label' }, { bind: { html: '{stats.uptimeFormatted}' }, cellCls: 'info-value' },
                { html: 'Server'.t() + ':', cellCls: 'info-label' }, { bind: { html: '{stats.appliance}' }, cellCls: 'info-value' },
                { html: 'CPU Count'.t() + ':', cellCls: 'info-label' }, { bind: { html: '{stats.numCpus}' }, cellCls: 'info-value' },
                { html: 'CPU Type'.t() + ':', cellCls: 'info-label' }, { bind: { html: '{stats.cpuModel}' }, cellCls: 'info-value' },
                { html: 'Architecture'.t() + ':', cellCls: 'info-label' }, { bind: { html: '{stats.architecture}'}, cellCls: 'info-value' },
                { html: 'Memory'.t() + ':', cellCls: 'info-label' }, { bind: { html: '{stats.totalMemory}' }, cellCls: 'info-value' },
                { html: 'Disk'.t() + ':', cellCls: 'info-label' }, { bind: { html: '{stats.totalDisk}' }, cellCls: 'info-value' }
            ]
        }]
    }]
});