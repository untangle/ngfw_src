Ext.define('Ung.widget.Report', {
    extend: 'Ext.container.Container',
    alias: 'widget.reportwidget',
    requires: [
        //'Ung.widget.report.ReportController',
        'Ung.widget.ReportModel',
        'Ung.chart.TimeChart',
        'Ung.chart.PieChart',
        'Ung.chart.EventChart'
    ],

    controller: 'widget',
    viewModel: {
        type: 'reportwidget'
    },
    config: {
        widget: null,
        entry: null
    },

    hidden: true,
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

    fetchData: function () {
        var me = this,
            entry = me.getViewModel().get('entry'),
            timeframe = me.getViewModel().get('widget.timeframe');

        // console.log('fetch data - ', entry.get('title'));

        if (entry.get('type') === 'EVENT_LIST') {
            // fetch event data
            //console.log('Event List');
            me.fireEvent('afterdata');
        } else {
            // fetch chart data
            this.lookupReference('chart').fireEvent('beginfetchdata');
            Rpc.getReportData(entry.getData(), timeframe)
                .then(function (response) {
                    me.lookupReference('chart').fireEvent('setseries', response.list);
                    me.fireEvent('afterdata');
                }, function (exception) {
                    console.log(exception);
                    Ung.Util.exceptionToast(exception);
                    me.fireEvent('afterdata');
                });
        }
    }
});