Ext.define('Ung.view.dashboard.Manager', {
    extend: 'Ext.grid.Panel',
    alias: 'widget.dashboardmanager',
    controller: 'dashboardmanager',

    itemId: 'dashboardManager',
    reference: 'dashboardManager',

    bodyBorder: false,
    animCollapse: false,
    floatable: false,
    cls: 'widget-manager',
    // disableSelection: true, // if disabled the drag/drop ordering does not work
    split: true,
    hideHeaders: true,
    rowLines: false,
    store: 'widgets',
    border: false,
    viewConfig: {
        plugins: {
            ptype: 'gridviewdragdrop',
            dragText: 'Drag and drop to reorganize'.t(),
            dragZone: {
                onBeforeDrag: function (data, e) {
                    return Ext.get(e.target).hasCls('fa-align-justify');
                }
            }
        },
        stripeRows: false,
        getRowClass: function (record) {
            var cls = !record.get('enabled') ? 'disabled' : '';
            cls += record.get('markedForDelete') ? ' will-remove' : '';
            return cls;
        },
        listeners: {
            drop: 'onDrop'
        }
    },
    columns: [{
        width: 28,
        align: 'center',
        hidden: true,
        renderer: function (val, meta) {
            meta.tdCls = 'reorder';
            return '<i class="fa fa-align-justify"></i>';
        }
    }, {
        width: 20,
        align: 'center',
        sortable: false,
        hideable: false,
        resizable: false,
        menuDisabled: true,
        //handler: 'toggleWidgetEnabled',
        dataIndex: 'enabled',
        renderer: 'enableRenderer'
    }, {
        dataIndex: 'entryId',
        renderer: 'widgetTitleRenderer',
        flex: 1
    }, {
        xtype: 'widgetcolumn',
        width: 30,
        align: 'center',
        widget: {
            xtype: 'button',
            width: 24,
            enableToggle: true,
            iconCls: 'fa fa-trash fa-gray',
            focusable: false,
            bind: {
                iconCls: 'fa fa-trash {record.markedForDelete ? "fa-red" : "fa-gray"}',
            },
            handler: function (btn) {
                btn.lookupViewModel().get('record').set('markedForDelete', btn.pressed);
            }
        }
    }],
    listeners: {
        itemmouseleave : 'onItemLeave',
        cellclick: 'onItemClick'
    },
    fbar: ['->', {
        text: 'Cancel'.t(),
        iconCls: 'fa fa-ban',
        handler: 'onCancel'
    }, {
        text: 'Apply'.t(),
        iconCls: 'fa fa-floppy-o',
        handler: 'applyChanges'
    }],

    dockedItems: [{
        xtype: 'toolbar',
        dock: 'top',
        ui: 'footer',
        style: { background: '#D8D8D8' },
        items: [{
            iconCls: 'fa fa-cog',
            focusable: false,
            text: 'Widgets'.t(),
            menu: {
                plain: true,
                showSeparator: false,
                mouseLeaveDelay: 0,
                items: [
                    { text: 'Import'.t(), iconCls: 'fa fa-download', handler: 'importWidgets' },
                    { text: 'Export'.t(), iconCls: 'fa fa-upload', handler: 'exportWidgets' },
                    '-',
                    {
                        text: 'Add Common Widgets'.t(),
                        iconCls: 'fa fa-plus-circle',
                        menu: {
                            plain: true,
                            showSeparator: false,
                            mouseLeaveDelay: 0,
                            items: [
                                { text: 'Information'.t(), value: 'informationwidget' },
                                { text: 'Resources'.t(), value: 'resourceswidget' },
                                { text: 'CPU Load'.t(), value: 'cpuloadwidget' },
                                { text: 'Network Information'.t(), value: 'networkinformationwidget' },
                                { text: 'Network Layout'.t(), value: 'networklayoutwidget' },
                                { text: 'Map Distribution'.t(), value: 'mapdistributionwidget' },
                                { text: 'Policy Overview'.t(), value: 'policyoverviewwidget' },
                                { text: 'Notifications'.t(), value: 'notificationswidget' }
                            ],
                            listeners: {
                                click: function (menu, item) {
                                    Ext.getStore('widgets').add(Ext.create('Ung.model.Widget', {
                                        type: item.text.replace(' ', '')
                                    }));
                                }
                            }
                        }
                    },
                    // { text: 'Add Report Widget'.t(), iconCls: 'fa fa-plus-circle', itemId: 'reportsMenu' },
                    { text: 'Create New Report Widget'.t(), iconCls: 'fa fa-area-chart', handler: 'newReportWidget' },
                    '-',
                    { text: 'Reorder'.t(), iconCls: 'fa fa-sort', handler: 'showOrderingColumn' },
                    '-',
                    { text: 'Reset to Defaults'.t(), iconCls: 'fa fa-rotate-left', handler: 'resetDashboard' }
                ]
                // listeners: {
                //     beforeshow: 'populateReportsMenu'
                // }
            }
        }, '->', {
            iconCls: 'fa fa-times',
            focusable: false,
            handler: 'onCancel'
        }]
    }],

});
