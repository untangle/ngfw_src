Ext.define('Ung.view.reports.EventReportController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.eventreport',

    control: {
        '#': {
            afterrender: 'onAfterRender',
            deactivate: 'onDeactivate'
        }
    },

    onAfterRender: function () {
        var me = this, vm = this.getViewModel(), i;

        me.modFields = { uniqueId: null };

        // remove property grid if in dashboard
        if (me.getView().up('#dashboard')) {
            me.getView().down('unpropertygrid').hide();
        }

        vm.bind('{entry}', function (entry) {

            if (entry.get('type') !== 'EVENT_LIST') { return; }

            if (me.modFields.uniqueId !== entry.get('uniqueId')) {
                me.modFields = {
                    uniqueId: entry.get('uniqueId'),
                    defaultColumns: entry.get('defaultColumns')
                };

                var grid = me.getView().down('grid');
                var identifier = 'eventsGrid-' + entry.get('uniqueId');
                grid.itemId = identifier;
                grid.stateId = identifier;

                me.tableConfig = Ext.clone(TableConfig.getConfig(entry.get('table')));
                me.defaultColumns = vm.get('widget.displayColumns') || entry.get('defaultColumns'); // widget or report columns

                var visibleColumns = Ext.clone(me.defaultColumns);
                var currentStorage = Ext.state.Manager.provider.get(identifier);
                if( currentStorage ){
                    currentStorage.columns.forEach( function( column ){
                        if( ( column.hidden !== undefined ) &&
                            ( column.hidden === false ) ){
                            visibleColumns.push(column.id);
                        }
                    });
                }

                me.tableConfig.columns.forEach( function(column){
                    if( column.columns ){
                        /*
                         * Grouping
                         */
                        column.columns.forEach( Ext.bind( function( subColumn ){
                            grid.initComponentColumn( subColumn );
                            if( subColumn.xtype == 'actioncolumn'){
                                subColumn.hidden = false;
                            }
                            if (!subColumn.filter &&
                                ( subColumn.xtype != 'actioncolumn' ) &&
                                ( subColumn.rtype != 'timestamp' ) ){
                                subColumn.filter = Renderer.stringFilter;
                            }
                        }, this ) );
                    }
                    grid.initComponentColumn( column );
                    if( column.xtype == 'actioncolumn'){
                        column.hidden = false;
                    }
                    if (!column.filter && 
                        ( column.xtype != 'actioncolumn' ) &&
                        ( column.rtype != 'timestamp' ) ){
                        column.filter = Renderer.stringFilter;
                    }
                });
                me.tableConfig.fields.forEach( function(field){
                    if (!field.sortType ){
                        field.sortType = 'asUnString';
                    }
                });

                grid.tableConfig = me.tableConfig;
                grid.setColumns(me.tableConfig.columns);
                grid.getColumns().forEach( function(column){
                    if( column.xtype == 'actioncolumn'){
                        return;
                    }
                    column.setHidden( Ext.Array.indexOf(visibleColumns, column.dataIndex) < 0 );
                    if( column.columns ){
                        column.columns.forEach( Ext.bind( function( subColumn ){
                            subColumn.setHidden( Ext.Array.indexOf(visibleColumns, column.dataIndex) < 0 );
                        }, this ) );
                    }
                });
                // Force state processing for this renamed grid
                grid.mixins.state.constructor.call(grid);

                var propertygrid = me.getView().down('#eventsProperties');
                vm.set( 'eventProperty', null );
                propertygrid.fireEvent('beforerender');
                propertygrid.fireEvent('beforeexpand');

                if (!me.getView().up('reportwidget')) {
                    me.fetchData();
                } else {
                    me.isWidget = true;
                }
                return;
            }

        }, me, { deep: true });

        // clear grid selection (hide event side data) when settings are open
        vm.bind('{settingsBtn.pressed}', function (pressed) {
            if (pressed) {
                me.getView().down('grid').getSelectionModel().deselectAll();
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
            v = me.getView(),
            vm = me.getViewModel();

        var limit = 1000;
        if( me.getView().up('reports-entry') ){
            limit = me.getView().up('reports-entry').down('#eventsLimitSelector').getValue();
        }
        me.entry = vm.get('entry');

        var startDate = vm.get('startDate');
        var endDate = vm.get('tillNow') ? null : vm.get('endDate');
        if (!me.getView().renderInReports) { // if not rendered in reports than treat as widget
            startDate = new Date(rpc.systemManager.getMilliseconds() - (vm.get('widget.timeframe') || 3600 * 24) * 1000);
            endDate = new Date(rpc.systemManager.getMilliseconds());
        }

        var grid = v.down('grid');

        me.getViewModel().set('eventsData', []);
        me.getView().setLoading(true);
        Rpc.asyncData('rpc.reportsManager.getEventsForDateRangeResultSet',
                        vm.get('entry').getData(), // entry
                        vm.get('sqlFilterData'), // etra conditions
                        limit,
                        startDate, // start date
                        endDate) // end date
            .then(function(result) {
                if (me.getView().up('reports-entry')) {
                    me.getView().up('reports-entry').down('#currentData').setLoading(false);
                }
                me.getView().setLoading(false);

                me.loadResultSet(result);

                if (cb) { cb(); }
            });
    },

    loadResultSet: function (reader) {
        var me = this,
            v = me.getView(),
            vm = me.getViewModel(),
            grid = me.getView().down('grid');

        this.getView().setLoading(true);
        grid.getStore().setFields( grid.tableConfig.fields );
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
        this.getView().setLoading(false);
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
        me.getView().up('reports-entry').lookupReference('settingsBtn').setPressed(false);

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
