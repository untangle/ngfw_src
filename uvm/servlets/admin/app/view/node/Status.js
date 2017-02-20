Ext.define('Ung.view.node.Status', {
    extend: 'Ext.panel.Panel',
    xtype: 'nodestatus',

    requires: [
        'Ung.chart.NodeChart'
    ],

    layout: {
        type: 'vbox'
    },

    config: {
        summary: null,
        hasChart: null,
        hasMetrics: true
    },

    defaults: {
        border: false,
        xtype: 'panel',
        baseCls: 'status-item',
        padding: 10,
        bodyPadding: '10 0 0 0'
    },

    title: 'Status'.t(),
    scrollable: true,
    items: [],

    initComponent: function () {

        var vm = this.getViewModel(),
            items = [{
                title: 'Summary'.t(),
                //baseCls: 'status-item',
                items: [{
                    xtype: 'component',
                    bind: '{summary}'
                }]
            }];

        if (vm.get('nodeProps.hasPowerButton')) {
            items.push({
                title: 'Power'.t(),
                //baseCls: 'status-item',
                //cls: isRunning? 'app-on': 'app-off',
                items: [{
                    xtype: 'component',
                    html: 'test',
                    bind: {
                        html: '{powerMessage}'
                    }
                }, {
                    xtype: 'button',
                    margin: '5 0',
                    cls: 'power-btn',
                    bind: {
                        text: '{powerButton}',
                        userCls: '{nodeInstance.targetState}'
                    },
                    handler: 'onPower'
                }]
            });
        }

        if (this.getHasChart()) {
            items.push({
                title: 'Sessions'.t(),
                items: [{
                    xtype: 'nodechart'
                }]
            });
        }

        if (this.getHasMetrics()) {
            items.push({
                title: 'Metrics'.t(),
                baseCls: 'status-item',
                items: [{
                    xtype: 'grid',
                    header: false,
                    hideHeaders: true,
                    baseCls: 'metrics-grid',
                    focusable: false,
                    width: 300,
                    border: false,
                    bodyBorder: false,
                    disableSelection: true,
                    trackMouseOver: false,
                    viewConfig: {
                        stripeRows: false
                    },
                    bind: {
                        store: '{nodeMetrics}'
                    },
                    columns: [{
                        dataIndex: 'displayName',
                        flex: 1,
                        align: 'right',
                        renderer: function (value) {
                            return value.t() + ':';
                        }
                    }, {
                        dataIndex: 'value'
                    }]
                }]
            });
        }

        Ext.apply(this, { items: items });
        this.callParent(arguments);
    }

});
