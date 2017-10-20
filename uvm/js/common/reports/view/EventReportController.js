Ext.define('Ung.view.reports.EventReportController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.eventreport',

    control: {
        '#': {
            afterrender: 'onAfterRender',
            deactivate: 'onDeactivate'
        }
    },

    listen: {
        global: {
            defaultcolumnschange: 'onDefaultColumn'
        }
    },

    onAfterRender: function () {
        var me = this, vm = this.getViewModel();

        me.modFields = { uniqueId: null };

        // remove property grid if in dashboard
        if (me.getView().up('#dashboard')) {
            me.getView().down('unpropertygrid').hide();
        }

        vm.bind('{entry}', function (entry) {
            if (entry.get('type') !== 'EVENT_LIST') { return; }
            vm.set('eventsData', []);
            me.fetchData();
        });

        // // update tableColumns when table is changed
        // vm.bind('{eEntry.table}', function (table) {
        //     console.log(table);
        //     if (!table) {
        //         vm.set('tableColumns', []);
        //         return;
        //     }
        //     var tableConfig = TableConfig.generate(table);

        //     if (vm.get('eEntry.type') !== 'EVENT_LIST') {
        //         vm.set('tableColumns', tableConfig.comboItems);
        //         return;
        //     }

        //     // for EVENT_LIST setup the columns
        //     var defaultColumns = Ext.clone(vm.get('eEntry.defaultColumns'));

        //     // initially set none as default
        //     Ext.Array.each(tableConfig.comboItems, function (item) {
        //         item.isDefault = false;
        //     });

        //     Ext.Array.each(vm.get('eEntry.defaultColumns'), function (defaultColumn) {
        //         var col = Ext.Array.findBy(tableConfig.comboItems, function (item) {
        //             return item.value === defaultColumn;
        //         });
        //         // remove default columns if not in TableConfig
        //         if (!col) {
        //             vm.set('eEntry.defaultColumns', Ext.Array.remove(defaultColumns, defaultColumn));
        //         } else {
        //             // otherwise set it as default
        //             col.isDefault = true;
        //         }
        //     });
        //     console.log(tableConfig.comboItems);
        //     vm.set('tableColumns', tableConfig.comboItems);
        //     me.fetchData();
        // });
    },

    setupGrid: function () {
        var me = this, vm = me.getViewModel(), grid = me.getView().down('grid');

        if (!me.entry) { return; }

        if (me.getView().up('reportwidget')) {
            me.isWidget = true;
        }

        me.tableConfig = Ext.clone(TableConfig.getConfig(me.entry.get('table')));
        me.defaultColumns = me.isWidget ? vm.get('widget.displayColumns') : me.entry.get('defaultColumns'); // widget or report columns

        Ext.Array.each(me.tableConfig.fields, function (field) {
            if (!field.sortType) {
                field.sortType = 'asUnString';
            }
        });

        Ext.Array.each(me.tableConfig.columns, function (column) {
            if (!Ext.Array.contains(me.defaultColumns, column.dataIndex)) {
                column.hidden = true;
            }
            // TO REVISIT THIS BECASE OF STATEFUL
            // grid.initComponentColumn(column);
            if (column.rtype) {
                column.renderer = 'columnRenderer';
            }
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
        this.getViewModel().set('eventsData', []);
        this.getView().down('grid').getSelectionModel().deselectAll();
    },

    fetchData: function (reset, cb) {
        var me = this,
            vm = me.getViewModel(),
            reps = me.getView().up('#reports'),
            startDate, endDate;


        var limit = 1000;
        if( me.getView().up('entry') ){
            limit = me.getView().up('entry').down('#eventsLimitSelector').getValue();
        }
        // console.log(vm.get('eEntry'));
        // console.log(vm.get('entry'));
        me.entry = vm.get('eEntry') || vm.get('entry');

        me.setupGrid();

        // date range setup
        if (!me.getView().renderInReports) {
            // if not rendered in reports than treat as widget so from server startDate is extracted the timeframe
            startDate = new Date(rpc.systemManager.getMilliseconds() - (Ung.dashboardSettings.timeframe * 3600 || 3600) * 1000);
            endDate = null;
        } else {
            // if it's a report, convert UI client start date to server date
            startDate = Util.clientToServerDate(vm.get('f_startdate'));
            endDate = Util.clientToServerDate(vm.get('f_enddate'));
        }

        me.getView().setLoading(true);
        if (reps) { reps.getViewModel().set('fetching', true); }

        Rpc.asyncData('rpc.reportsManager.getEventsForDateRangeResultSet',
                        me.entry.getData(), // entry
                        vm.get('sqlFilterData'), // etra conditions
                        limit,
                        startDate, // start date
                        endDate) // end date
            .then(function(result) {
                if (me.getView().up('entry')) {
                    me.getView().up('entry').down('#currentData').setLoading(false);
                }
                me.getView().setLoading(false);
                if (reps) { reps.getViewModel().set('fetching', false); }

                me.loadResultSet(result);

                if (cb) { cb(); }
            });
    },

    loadResultSet: function (reader) {
        var me = this,
            v = me.getView(),
            vm = me.getViewModel(),
            grid = me.getView().down('grid');

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
        vm.set('eventsData', eventData);
        // this.getView().setLoading(false);
    },

    onEventSelect: function (el, record) {
        var me = this, vm = this.getViewModel(), propsData = [];

        if (me.isWidget) { return; }

        Ext.Array.each(me.tableConfig.columns, function (column) {
            propsData.push({
                name: column.header,
                value: record.get(column.dataIndex)
            });
        });

        vm.set('propsData', propsData);
        // when selecting an event hide Settings if open
        if (me.getView().up('entry')) {
            me.getView().up('entry').lookupReference('settingsBtn').setPressed(false);
        }

    },

    onDataChanged: function(){
        var me = this,
            v = me.getView(),
            vm = me.getViewModel();

        if( vm.get('eventProperty') == null ){
            v.down('grid').getSelectionModel().select(0);
        }

        if( v.up().down('ungridstatus') ){
            v.up().down('ungridstatus').fireEvent('update');
        }
    }

});
