Ext.define('Ung.widget.CpuLoad', {
    extend: 'Ext.container.Container',
    alias: 'widget.cpuloadwidget',

    requires: [
        'Ung.widget.CpuLoadController'
    ],

    controller: 'cpuload',
    viewModel: true,

    hidden: true,
    border: false,
    baseCls: 'widget',
    cls: 'small adding',

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
        html: '<h1>' + 'CPU Load'.t() + '</h1>'
    }, {
        xtype: 'container',
        /*
        layout: {
            type: 'vbox',
            align: 'stretch'
        },
        */
        items: [{
            xtype: 'component',
            height: 120,
            reference: 'cpulinechart'
        }, {
            xtype: 'component',
            reference: 'cpugaugechart',
            height: 140
        }, {
            xtype: 'component',
            cls: 'cpu-gauge',
            bind: {
                html: '{stats.oneMinuteLoadAvg}<br/><span>{loadLabel}</span>'
            }
        }]
    }]
});
