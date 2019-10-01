Ext.define('Ung.view.reports.EventReport', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.eventreport',

    config: {
        widget: null
    },

    layout: 'border',

    border: false,
    bodyBorder: false,

    defaults: {
        border: false
    },

    items: [{
        xtype: 'ungrid',
        stateful: true,
        itemId: 'eventsGrid',
        reference: 'eventsGrid',
        region: 'center',
        store: { data: [] },
        plugins: ['gridfilters'],
        selModel: {
            type: 'rowmodel'
        },
        // emptyText: '<p style="text-align: center; margin: 0; line-height: 2;"><i class="fa fa-info-circle fa-2x"></i> <br/>No Records!</p>',
        enableColumnHide: true,
        listeners: {
            select: 'onEventSelect'
        }
    }, {
        xtype: 'unpropertygrid',
        itemId: 'eventsProperties',
        reference: 'eventsProperties',
        features: [{
            ftype: 'grouping',
            groupHeaderTpl: ['{name}']
        }],
        region: 'east',
        title: 'Details'.t(),
        collapsible: true,
        collapsed: true,
        animCollapse: false,
        titleCollapse: true,
    }],

    controller: {
        control: {
            '#': {
                afterrender: 'onAfterRender',
                deactivate: 'onDeactivate',
                refresh: 'onRefresh'
            }
        },

        listen: {
            global: {
                defaultcolumnschange: 'onDefaultColumn'
            }
        },

        onAfterRender: function (view) {
            var me = this, vm = this.getViewModel();

            // find and set the widget component if report is rendered inside a widget
            view.setWidget(view.up('reportwidget'));

            // add store datachange listener here, as it won't work in view definition
            view.down('grid').getStore().on('datachanged', function () { me.onDataChanged(); } );

            // hide property grid if rendered inside widget
            if (view.getWidget() || view.up('new-widget')) {
                view.down('unpropertygrid').hide();
            }

            vm.bind('{entry}', function (entry) {
                if(Util.isDestroyed(me, view)){
                    return;
                }
                if (!entry || entry.get('type') !== 'EVENT_LIST') {
                    return;
                }

                // if rendered as widget, add to dashboard queue
                if (view.getWidget()) {
                    // if it's widgets it needs separate calls to setup the grid
                    me.tableConfig = Ext.clone(TableConfig.getConfig(entry.get('table')));
                    me.setupGrid();
                    // DashboardQueue.addFirst(view.getWidget());
                }
                // if rendered in creating new widget dialog, fetch data
                if (view.up('new-widget')) {
                    me.fetchData(true);
                }
            });
        },

        setupGrid: function () {
            var me = this, vm = me.getViewModel(), grid = me.getView().down('grid');
            var entry = vm.get('eEntry') || vm.get('entry');

            if (!entry) { return; }

            if (me.getView().up('reportwidget')) {
                me.isWidget = true;
            }

            me.tableConfig = Ext.clone(TableConfig.getConfig(entry.get('table')));

            if(me.tableConfig.setupGrid){
                me.tableConfig.setupGrid(me);
            }

            me.defaultColumns = me.isWidget ? vm.get('widget.displayColumns') : entry.get('defaultColumns'); // widget or report columns

            Ext.Array.each(me.tableConfig.fields, function (field) {
                if (!field.sortType) {
                    field.sortType = 'asUnString';
                }
            });

            Ext.Array.each(me.tableConfig.columns, function (column) {
                if (me.defaultColumns && !Ext.Array.contains(me.defaultColumns, column.dataIndex)) {
                    column.hidden = true;
                }
                if(!column.renderer && column.xtype != 'actioncolumn'){
                    column.renderer = Ung.view.reports.EventReport.renderer;
                }
                // TO REVISIT THIS BECASE OF STATEFUL
                // grid.initComponentColumn(column);
            });

            grid.reconfigure(me.tableConfig.columns);

            var propertygrid = me.getView().down('#eventsProperties');
            vm.set('eventProperty', null);
            propertygrid.fireEvent('beforerender');
            propertygrid.fireEvent('beforeexpand');

            // me.fetchData();
            // if (!me.getView().up('reportwidget')) {
            //     me.fetchData();
            // }
        },

        onDefaultColumn: function (defaultColumn) {
            var me = this, vm = me.getViewModel(),
                grid = me.getView().down('ungrid'),
                entry = vm.get('eEntry');
            if (!entry) { return; }
            Ext.Array.each(grid.getColumns(), function (column) {
                if (column.dataIndex === defaultColumn.get('value')) {
                    column.setHidden(!defaultColumn.get('isDefault'));
                    if (defaultColumn.get('isDefault')) {
                        entry.get('defaultColumns').push(column.dataIndex);
                    } else {
                        Ext.Array.remove(entry.get('defaultColumns'), column.dataIndex);
                    }

                }
            });
        },

        onDeactivate: function () {
            this.modFields = { uniqueId: null };
            this.getView().down('grid').getStore().loadData([]);
            this.getView().down('grid').getSelectionModel().deselectAll();
        },

        onRefresh: function(){
            if(this.tableConfig && this.tableConfig.refresh){
                this.tableConfig.refresh();
            }
        },

        fetchData: function (reset, cb) {
            var me = this,
                vm = me.getViewModel(),
                entry = vm.get('eEntry') || vm.get('entry'),
                reps = me.getView().up('#reports'),
                startDate, endDate;

            if (!entry) { return; }

            if (reset) {
                me.getView().down('grid').getStore().loadData([]);
                me.setupGrid();
            }


            var limit = 1000;
            if (me.getView().up('entry')) {
                limit = me.getView().up('entry').down('#eventsLimitSelector').getValue();
            }

            // date range setup
            if (!me.getView().renderInReports) {
                // if not rendered in reports than treat as widget so from server startDate is extracted the timeframe
                startDate = new Date(Util.getMilliseconds() - (Ung.dashboardSettings.timeframe * 3600 || 3600) * 1000);
                endDate = null;
            } else {
                // if it's a report, convert UI client start date to server date
                startDate = Util.clientToServerDate(vm.get('time.range.since'));
                endDate = Util.clientToServerDate(vm.get('time.range.until'));
            }

            me.getView().setLoading(true);
            if (reps) { reps.getViewModel().set('fetching', true); }

            Rpc.asyncData('rpc.reportsManager.getEventsForDateRangeResultSet',
                entry.getData(), // entry
                Ung.model.ReportCondition.collect(vm.get('query.conditions')),
                limit,
                startDate,
                endDate)
                .then(function(result) {
                    if(Util.isDestroyed(me)){
                        return;
                    }
                    me.getView().setLoading(false);
                    if (reps) {
                        reps.getViewModel().set('fetching', false);
                    }
                    me.loadResultSet(result);
                })
                .always(function () { // NGFW-11467
                    if(Util.isDestroyed(me)){
                        return;
                    }
                    if (cb) {
                        cb();
                    }
                    me.getView().setLoading(false);
                    if (reps) {
                        reps.getViewModel().set('fetching', false);
                    }
                });
        },

        loadResultSet: function (reader) {
            var me = this, grid = me.getView().down('grid');

            // this.getView().setLoading(true);
            grid.getStore().setFields( me.tableConfig.fields );
            var eventData = [];
            var result = [];
            while( true ){
                result = reader.getNextChunk(1000);
                if(result && result.list && result.list.length){
                    result.list.forEach(function(value){
                        eventData.push(value);
                    });
                    continue;
                }
                break;
            }
            reader.closeConnection();
            grid.getStore().loadData(eventData);
        },

        onEventSelect: function (el, record) {
            var me = this, vm = this.getViewModel(), propsData = [];

            if (me.isWidget) { return; }

            // when selecting an event hide Settings if open
            if (me.getView().up('entry')) {
                me.getView().up('entry').lookupReference('settingsBtn').setPressed(false);
            }

            if(me.tableConfig.listeners && me.tableConfig.listeners.select){
                me.tableConfig.listeners.select.apply(me, arguments);
            }
        },

        onDataChanged: function(){
            var me = this,
                v = me.getView(),
                vm = me.getViewModel();

            if( vm.get('eventProperty') == null ){
                v.down('grid').getSelectionModel().select(0);
            }
        }
    },
    statics:{
        renderer: function(value, meta, record, x,y, z, table){
            meta.tdAttr = 'data-qtip="' + Ext.String.htmlEncode( value ) + '"';
            return value;
        },

    }
});
