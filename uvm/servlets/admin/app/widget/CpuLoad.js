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
            html: '<h1>' + 'CPU Load'.t() + '</h1>'
        }]
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
        }, {
            xtype: 'component',
            itemId: 'loader',
            cls: 'loader',
            hideMode: 'display',
            html: '<div class="spinner"><div class="bounce1"></div><div class="bounce2"></div><div class="bounce3"></div></div>'
        }]
    }]
});