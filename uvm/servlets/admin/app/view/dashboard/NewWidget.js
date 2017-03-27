Ext.define('Ung.view.dashboard.NewWidget', {
    extend: 'Ext.window.Window',
    alias: 'widget.new-widget',

    title: 'Add Widget'.t(),
    controller: 'new-widget',

    modal: true,
    width: 800,
    height: 500,
    layout: 'border',

    viewModel: {
        data: {
            widget: null,
            onDashboard: false,
        },
        formulas: {
            _timeframe: {
                get: function(get) {
                    return get('widget.timeframe') ? get('widget.timeframe') / 3600 : '';
                },
                set: function (value) {
                    console.log(value);
                    this.set('widget.timeframe', value ? value * 3600 : null);
                }
            },
            _activeCenterCard: function (get) {
                return get('entry') ? 1 : 0;
            }
        }
    },

    items: [{
        xtype: 'treepanel',
        reference: 'tree',
        region: 'west',
        weight: 20,
        width: 250,
        minWidth: 250,
        split: true,
        // border: false,
        // singleExpand: true,
        useArrows: true,
        rootVisible: false,

        viewConfig: {
            selectionModel: {
                type: 'treemodel'
                // pruneRemoved: false
            }
        },
        store: 'reportstree',
        columns: [{
            xtype: 'treecolumn',
            flex: 1,
            dataIndex: 'text',
            // scope: 'controller',
            renderer: 'treeNavNodeRenderer'
        }],

        tbar: [{
            xtype: 'textfield',
            emptyText: 'Filter reports ...',
            // enableKeyEvents: true,
            flex: 1,
            triggers: {
                clear: {
                    cls: 'x-form-clear-trigger',
                    hidden: true,
                    // handler: 'onTreeFilterClear'
                }
            },
            listeners: {
                change: 'filterTree',
                buffer: 300
            }
        }],
        listeners: {
            select: 'selectNode'
        }
    }, {
        region: 'center',
        reference: 'report',
        layout: 'fit',
        items: [{
            xtype: 'container',
            layout: 'center',
            bind: {
                hidden: '{entry}'
            },
            items: [{
                xtype: 'component',
                width: '100%',
                style: {
                    textAlign: 'center',
                    color: '#777'
                },
                html: '<i class="fa fa-info-circle fa-lg"></i> <br/>' + 'Select a report first ...'
            }]
        }]
    }, {
        region: 'south',
        // title: 'Settings'.t(),
        height: 200,
        split: true,
        maxHeight: 200,
        minHeight: 200,
        layout: 'fit',
        items: [{
            // xtype: 'form',
            border: false,
            bodyPadding: 10,
            layout: {
                type: 'vbox',
                align: 'stretch'
            },
            disabled: true,
            bind: {
                disabled: '{!entry}'
            },
            items: [{
                xtype: 'checkbox',
                boxLabel: '<strong>' + 'Enabled'.t() + '</strong>',
                bind: '{widget.enabled}'
            }, {
                xtype: 'component',
                html: '<strong>' + 'Refresh Interval'.t() + '</strong>:<br/> <span style="font-size: 11px; color: #777;">' + 'Leave blank for no Auto Refresh'.t() + '</span>'
            }, {
                xtype: 'container',
                layout: { type: 'hbox', align: 'middle' },
                margin: '2 0 10 0',
                items: [{
                    xtype: 'numberfield',
                    width: 50,
                    maxValue: 600,
                    minValue: 10,
                    allowBlank: true,
                    margin: '0 5 0 0',
                    bind: '{widget.refreshIntervalSec}'
                }, {
                    xtype: 'component',
                    style: {
                        fontSize: '11px',
                        color: '#777'
                    },
                    html: '(seconds)'.t()
                }]
            }, {
                xtype: 'component',
                html: '<strong>' + 'Timeframe'.t() + '</strong>:<br/> <span style="font-size: 11px; color: #777;">' + 'The number of hours to query the latest data. Leave blank for last day.'.t() + '</span>'
            }, {
                xtype: 'container',
                layout: { type: 'hbox', align: 'middle' },
                margin: '2 0',
                items: [{
                    xtype: 'numberfield',
                    width: 50,
                    maxValue: 72,
                    minValue: 1,
                    allowBlank: true,
                    margin: '0 5 0 0',
                    bind: '{_timeframe}'
                }, {
                    xtype: 'component',
                    style: {
                        fontSize: '11px',
                        color: '#777'
                    },
                    html: '(hours)'.t()
                }, {
                    xtype: 'button',
                    iconCls: 'fa fa-refresh',
                    tooltip: 'Refresh'.t(),
                    margin: '0 0 0 5',
                    handler: 'refreshEntry'
                }]
            }]
        }],

        buttons: [{
            text: 'Remove'.t(),
            iconCls: 'fa fa-trash-o fa-red',
            hidden: true,
            bind: { hidden: '{!(entry && onDashboard)}' },
            handler: function (btn) {
                alert('To Do!');
            }
        }, '->', {
            text: 'Cancel'.t(),
            iconCls: 'fa fa-ban',
            handler: function (btn) {
                btn.up('window').close();
            }
        }, {
            text: 'Add'.t(),
            iconCls: 'fa fa-plus-circle',
            hidden: true,
            bind: { hidden: '{!(entry && !onDashboard)}' },
            handler: function (btn) {
                alert('To Do!');
            }
        }, {
            text: 'Save'.t(),
            iconCls: 'fa fa-floppy-o',
            hidden: true,
            bind: { hidden: '{!(entry && onDashboard)}' },
            handler: function (btn) {
                alert('To Do!');
            }
        }]
    }],

    // dockedItems: [{
    //     xtype: 'toolbar',
    //     dock: 'bottom',
    //     weight: 0,



    // }]
});
