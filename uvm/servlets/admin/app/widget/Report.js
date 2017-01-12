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
    baseCls: 'widget adding',

    items: [{
        xtype: 'component',
        cls: 'header',
        itemId: 'header',
        bind: {
            html: '{title}' +
                '<button class="action-btn"><i class="fa fa-refresh" data-action="refresh"></i></button>'
        }
    }],
    // {
    //     xtype: 'container',
    //     layout: {
    //         type: 'hbox',
    //         align: 'top'
    //     },
    //     cls: 'header',
    //     style: {
    //         height: '50px'
    //     },
    //     items: [{
    //         xtype: 'component',
    //         flex: 1,
    //         bind: {
    //             html: '{title}'
    //         }
    //     }, {
    //         xtype: 'container',
    //         margin: '10 5 0 0',
    //         layout: {
    //             type: 'hbox',
    //             align: 'middle'
    //         },
    //         items: [/*{
    //             xtype: 'button',
    //             baseCls: 'action',
    //             text: '<i class="material-icons">settings_ethernet</i>',
    //             listeners: {
    //                 click: 'resizeWidget'
    //             }
    //         }, {
    //             xtype: 'button',
    //             baseCls: 'action',
    //             text: '<i class="material-icons">settings</i>',
    //             listeners: {
    //                 click: 'showEditor'
    //             }
    //         },*/ {
    //             xtype: 'button',
    //             baseCls: 'action',
    //             text: '<i class="material-icons">refresh</i>',
    //             listeners: {
    //                 click: 'fetchData'
    //             }
    //         }, {
    //             xtype: 'button',
    //             baseCls: 'action',
    //             text: '<i class="material-icons">call_made</i>',
    //             bind: {
    //                 href: '#reports/{widget.entryId}'
    //             },
    //             hrefTarget: '_self'
    //         }]
    //     }]
    // }],

    fetchData: function () {
        var me = this,
            vm = this.getViewModel();

        if (vm) {
            var entry = vm.get('entry'),
                timeframe = vm.get('widget.timeframe');

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
                        me.fireEvent('afterdata');
                        me.lookupReference('chart').fireEvent('setseries', response.list);
                    }, function (exception) {
                        me.fireEvent('afterdata');
                        me.lookupReference('chart').lookupReference('loader').hide();
                        console.log(exception);
                        // Ung.Util.exceptionToast(exception);

                        if (me.down('#exception')) {
                            me.remove('exception');
                        }
                        me.add({
                            xtype: 'component',
                            itemId: 'exception',
                            cls: 'exception',
                            scrollable: true,
                            html: '<h3><i class="material-icons">error</i> <span>' + 'Widget error'.t() + '</span></h3>' +
                                '<p>' + 'There was an issue while fetching data!'.t() + '</p>'
                        });
                    });
            }
        }
    }
});