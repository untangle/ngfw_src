Ext.define('Ung.widget.report.Report', {
    extend: 'Ext.container.Container',
    alias: 'widget.reportwidget',
    requires: [
        'Ung.widget.report.ReportController',
        'Ung.widget.report.ReportModel',
        //'Ung.view.widget.editor.TimeWidget',
        //'Ung.view.widget.editor.PieWidget',
        'Ung.chart.TimeChart',
        'Ung.chart.PieChart',
        'Ung.chart.EventChart'
        //'Ung.model.Widget'
    ],

    controller: 'reportwidget',
    viewModel: {
        type: 'reportwidget'
    },

    config: {
        widget: null,
        entry: null
    },

    bind: {
        hidden: '{!widget.enabled}'
    },

    layout: {
        type: 'vbox',
        align: 'stretch'
    },
    border: false,
    baseCls: 'widget',

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
            bind: {
                html: '{title}'
            }
        }, {
            xtype: 'container',
            margin: '10 5 0 0',
            layout: {
                type: 'hbox',
                align: 'middle'
            },
            items: [/*{
                xtype: 'button',
                baseCls: 'action',
                text: '<i class="material-icons">settings_ethernet</i>',
                listeners: {
                    click: 'resizeWidget'
                }
            }, {
                xtype: 'button',
                baseCls: 'action',
                text: '<i class="material-icons">settings</i>',
                listeners: {
                    click: 'showEditor'
                }
            },*/ {
                xtype: 'button',
                baseCls: 'action',
                text: '<i class="material-icons">refresh</i>',
                listeners: {
                    click: 'fetchData'
                }
            }, {
                xtype: 'button',
                baseCls: 'action',
                text: '<i class="material-icons">call_made</i>',
                bind: {
                    href: '#reports/{widget.entryId}'
                },
                hrefTarget: '_self'
            }]
        }]
    }],

    listeners: {
        afterrender: 'fetchData',
        fetchdata: 'fetchData'
        //resize: 'resize'
    }

});