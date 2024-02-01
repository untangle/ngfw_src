Ext.define('Ung.view.dashboard.NewWidget', {
    extend: 'Ext.window.Window',
    alias: 'widget.new-widget',

    title: 'Add/Edit Widgets',
    controller: 'new-widget',

    modal: true,
    width: 800,
    height: 500,
    layout: 'border',

    renderTo: Ext.getBody(),

    viewModel: {
        data: {
            widget: null,
            entry: null,
            onDashboard: false,
        },
        formulas: {
            _autorefresh: {
                get: function(get) {
                    var interval = get('widget.refreshIntervalSec');
                    return interval ? true : false;
                },
                set: function (value) {
                    this.set('widget.refreshIntervalSec', value ? 60 : '');
                }
            },
            _timeframe: {
                get: function(get) {
                    return get('widget.timeframe') ? get('widget.timeframe') / 3600 : '';
                },
                set: function (value) {
                    this.set('widget.timeframe', value ? value * 3600 : null);
                }
            },
            _timeframeck: {
                get: function(get) {
                    return get('_timeframe') === '';
                },
                set: function (value) {
                    this.set('_timeframe', value ? '' : 1);
                }
            },
            _columnsck: {
                get: function(get) {
                    return get('widget.displayColumns') ? false : true;
                },
                set: function (value) {
                    if (value) {
                        this.set('widget.displayColumns', null);
                    } else {
                        this.set('widget.displayColumns', this.get('entry.defaultColumns'));
                    }
                }
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
        useArrows: true,
        rootVisible: false,

        viewConfig: {
            selectionModel: {
                type: 'treemodel'
            }
        },
        store: 'reportstree',
        columns: [{
            xtype: 'treecolumn',
            flex: 1,
            dataIndex: 'text',
            renderer: 'treeNavNodeRenderer'
        }],

        tbar: [{
            xtype: 'textfield',
            emptyText: 'Filter reports ...',
            flex: 1,
            triggers: {
                clear: {
                    cls: 'x-form-clear-trigger',
                    hidden: true,
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
        height: 'auto',

        // split: true,
        // maxHeight: 140,
        // minHeight: 140,
        // layout: 'fit',

        dockedItems: [{
            xtype: 'component',
            dock: 'top',
            ui: 'footer',
            margin: '10 0',
            style: {
                textAlign: 'center',
                fontSize: '11px',
                color: '#333'
            },
            hidden: true,
            bind: {
                hidden: '{!(entry && onDashboard)}',
                html: '<i class="fa fa-info-circle fa-lg"></i> ' + 'This report widget already exists in Dashboard</strong>!',
            }
        }],

        items: [{
            // xtype: 'form',
            border: false,
            bodyPadding: 10,
            layout: {
                type: 'vbox'
                // align: 'stretch'
            },
            hidden: true,
            bind: {
                hidden: '{!entry}'
            },

            items: [{
                xtype: 'checkbox',
                boxLabel: '<strong>' + 'Enabled'.t() + '</strong>',
                bind: '{widget.enabled}'
            }, {
                xtype: 'combo',
                fieldLabel: '<strong>' + 'Auto Refresh'.t() + '</strong>',
                allowBlank: false,
                margin: '0 5',
                store: [
                    [10, '10 ' + 'seconds'.t()],
                    [30, '30 ' + 'seconds'.t()],
                    [60, '1 ' + 'minute'.t()],
                    [120, '2 ' + 'minutes'.t()],
                    [300, '5 ' + 'minutes'.t()],
                    [0, 'never'.t()]
                ],
                editable: false,
                queryMode: 'local',
                bind: '{widget.refreshIntervalSec}'
            }, {
                xtype: 'checkbox',
                reference: 'columnsck',
                fieldLabel: '<strong>' + 'Columns'.t() + '</strong>',
                labelWidth: 'auto',
                boxLabel: 'report defaults',
                hidden: true,
                // publishes: 'value',
                bind: {
                    value: '{_columnsck}',
                    hidden: '{entry.type !== "EVENT_LIST"}'
                }
            }, {
                xtype: 'tagfield',
                hidden: true,
                width: '100%',
                store: {
                    fields: ['name', 'text']
                },
                displayField: 'text',
                valueField: 'name',
                bind: {
                    value: '{widget.displayColumns}',
                    hidden: '{_columnsck}'
                },
                queryMode: 'local',
                // listeners: {
                //     change: 'updateColumns'
                // }
            }]
        }],

        buttons: [{
            text: 'Cancel'.t(),
            iconCls: 'fa fa-ban',
            handler: function (btn) {
                btn.up('window').close();
            }
        }, {
            text: 'Add'.t(),
            iconCls: 'fa fa-plus-circle',
            hidden: true,
            bind: { hidden: '{!entry}' },
            handler: 'onAdd'
        }]
    }]
});
