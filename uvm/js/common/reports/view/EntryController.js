Ext.define('Ung.view.reports.EntryController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.entry',

    control: {
        '#': {
            afterrender: 'onAfterRender',
            deactivate: 'onDeactivate'
        }
    },

    refreshTimeout: null,

    onDeactivate: function () {
        this.reset();
        this.getViewModel().set({
            entry: null,
            eEntry: null
        });
    },

    onAfterRender: function () {
        var me = this, vm = this.getViewModel();

        /**
         * each time report selection changes
         */
        vm.bind('{entry}', function (entry) {
            if (!entry) { return; }

            vm.set('eEntry', null);
            me.setReportCard(entry.get('type'));

            me.reload(true); // important to fetch data in reports view

            // check if widget in admin context
            if (Ung.app.context === 'ADMIN') {
                // widget = Ext.getStore('widgets').findRecord('entryId', entry.get('uniqueId')) || null;
                vm.set('widget', Ext.getStore('widgets').findRecord('entryId', entry.get('uniqueId')));
            }
        });

        vm.bind('{entry.table}', function (table) {
            vm.set('f_tableconfig', table ? TableConfig.generate(table) : []);
        });

        // each time the eEntry changes by selecting 'Settings'
        vm.bind('{eEntry}', function (eEntry) {
            me.getView().up('#reports').getViewModel().set('editing', eEntry ? true : false);

            if (!vm.get('entry')) { return; }

            // on cancel when eEntry turns null reloads the selected entry if exists
            if (!eEntry) {
                // me.reload(true);
                me.getView().down('graphreport').fireEvent('styleschanged');
                return;
            }

            // if eEntry is readOnly, alter the title to avoid initial validation error
            if (eEntry.get('readOnly')) {
                eEntry.set('title', eEntry.get('title') + ' ' + '[new]'.t());
            }

            // if not defaultColumns initialize with [] to avoid editing errors
            if (!eEntry.get('defaultColumns')) {
                eEntry.set('defaultColumns', []);
            }

            // transform eEntry text and time data columns to usable data for the stores
            vm.set('textColumns', Ext.Array.map(eEntry.get('textColumns') || [], function (col) { return { str: col }; }));
            vm.set('timeDataColumns', Ext.Array.map(eEntry.get('timeDataColumns') || [], function (col) { return { str: col }; }));
        });

        // each time the eEntry type is changed check if editing form is valid
        // vm.bind('{eEntry.type}', function (type) {

        //     if (!type) { return; }
        //     me.setReportCard(type);
        //     // defer needed for form validation
        //     Ext.defer(function () {
        //         if (me.getView().down('form').isValid()) {
        //             vm.set('validForm', true);
        //             me.reload(true);
        //         } else {
        //             vm.set('validForm', false);
        //             me.reset();
        //         }
        //     }, 100);
        // });

        // watch since date switching and reload the report
        vm.bind('{sinceDate.value}', function () {
            // vm.set({
            //     f_startdate: Util.serverToClientDate(new Date((Math.floor(Util.getMilliseconds()/60000) * 60000) - vm.get('sinceDate.value') * 3600 * 1000)),
            //     f_enddate: null
            // });
            me.reload();
        });

        // watch custom range switch on/off
        vm.bind('{r_customRangeCk.value}', function (checked) {
            // vm.set({
            //     f_startdate: Util.serverToClientDate(new Date((Math.floor(Util.getMilliseconds()/60000) * 60000) - vm.get('sinceDate.value') * 3600 * 1000)),
            //     f_enddate: null
            // });
            if (checked) {
                // when checked, disable autorefresh because of the fixed range
                me.lookup('r_autoRefreshBtn').setPressed(false);
            } else {
                // when unckecked, reload the current data from Since
                me.reload();
            }
        });

        // watch auto refresh button switch on/off
        vm.bind('{r_autoRefreshBtn.pressed}', function (pressed) {
            if (pressed) {
                me.reload();
            } else {
                if (me.refreshTimeout) {
                    clearTimeout(me.refreshTimeout);
                    me.refreshTimeout = null;
                }
            }
        });

        // vm.bind('{f_startdate}', function () { me.reload(); });
        // vm.bind('{f_enddate}', function () { me.reload(); });
    },

    /**
     * sets active card based on report type
     */
    setReportCard: function (type) {
        var me = this, reportCard = '';
        switch(type) {
        case 'TEXT': reportCard = 'textreport'; break;
        case 'PIE_GRAPH':
        case 'TIME_GRAPH':
        case 'TIME_GRAPH_DYNAMIC': reportCard = 'graphreport'; break;
        case 'EVENT_LIST': reportCard = 'eventreport'; break;
        }
        me.getView().down('#reportCard').setActiveItem(reportCard);
    },

    filterData: function (min, max) {
        // aply filtering only on timeseries
        if (this.getViewModel().get('entry.type').indexOf('TIME_GRAPH') >= 0) {
            this.getView().down('#currentData').getStore().clearFilter();
            this.getView().down('#currentData').getStore().filterBy(function (point) {
                var t = point.get('time_trunc').time;
                return t >= min && t <= max ;
            });
        }
    },

    /**
     * reloads the reports
     * reset = false just fetches the data
     */
    reload: function (reset) {
        var me = this, vm = me.getViewModel(),
            entry = vm.get('eEntry') || vm.get('entry'), ctrl;

        if (!entry) { return; }

        vm.set('validForm', true); // te remove the valid warning

        if (!vm.get('r_customRangeCk.value')) {
            vm.set({
                f_startdate: Util.serverToClientDate(new Date((Math.floor(Util.getMilliseconds()/600000) * 600000) - vm.get('sinceDate.value') * 3600 * 1000)),
                f_enddate: null
            });
        }

        switch(entry.get('type')) {
        case 'TEXT': ctrl = me.getView().down('textreport').getController(); break;
        case 'EVENT_LIST': ctrl = me.getView().down('eventreport').getController(); break;
        default: ctrl = me.getView().down('graphreport').getController();
        }

        if (!ctrl) {
            console.error('Entry controller not found!');
            return;
        }

        if (reset) {
            me.reset();
        }

        // if (reps) { reps.getViewModel().set('fetching', true); }
        ctrl.fetchData(reset, function (data) {
            // if (reps) { reps.getViewModel().set('fetching', false); }
            // if autorefresh enabled refetch data in 5 seconds
            vm.set('reportData', data);

            if (vm.get('r_autoRefreshBtn.pressed')) {
                me.refreshTimeout = setTimeout(function () {
                    me.reload();
                }, 5000);
            }
        });
    },

    reset: function () {
        var me = this, vm = me.getViewModel(),
            entry = vm.get('eEntry') || vm.get('entry'), ctrl;

        // if (!entry) { return; }
        me.getView().down('graphreport').getController().reset();

        // switch(entry.get('type')) {
        // case 'TEXT': ctrl = me.getView().down('textreport').getController(); break;
        // case 'EVENT_LIST': ctrl = me.getView().down('eventreport').getController(); break;
        // default: ctrl = me.getView().down('graphreport').getController();
        // }

        // if (!ctrl) {
        //     console.error('Entry controller not found!');
        //     return;
        // }
        // if (Ext.isFunction(ctrl.reset)) {
        //     ctrl.reset();
        // }
    },

    refresh: function () {
        this.reload(false);
    },

    resetAndReload: function () {
        this.reload(true);
    },

    // resetView: function(){
    //     var grid = this.getView().down('grid');
    //     Ext.state.Manager.clear(grid.stateId);
    //     grid.filters.clearFilters();
    //     grid.reconfigure(null, grid.tableConfig.columns);

    //     grid.getColumns().forEach( function(column){
    //         if( column.xtype == 'actioncolumn'){
    //             return;
    //         }
    //         column.setHidden( Ext.Array.indexOf(grid.visibleColumns, column.dataIndex) < 0 );
    //         if( column.columns ){
    //             column.columns.forEach( Ext.bind( function( subColumn ){
    //                 subColumn.setHidden( Ext.Array.indexOf(grid.visibleColumns, column.dataIndex) < 0 );
    //             }, this ) );
    //         }
    //     });
    // },


    // TABLE COLUMNS / CONDITIONS

    addSqlCondition: function () {
        var me = this, vm = me.getViewModel(),
            conds = vm.get('_sqlConditions') || [];

        conds.push({
            autoFormatValue: false,
            column: me.getView().down('#sqlConditionsCombo').getValue(),
            javaClass: 'com.untangle.app.reports.SqlCondition',
            operator: '=',
            value: ''
        });

        me.getView().down('#sqlConditionsCombo').setValue(null);

        vm.set('_sqlConditions', conds);
        me.getView().down('#sqlConditions').getStore().reload();
    },

    removeSqlCondition: function (table, rowIndex) {
        var me = this, vm = me.getViewModel(),
            conds = vm.get('_sqlConditions');
        Ext.Array.removeAt(conds, rowIndex);
        vm.set('_sqlConditions', conds);
        me.getView().down('#sqlConditions').getStore().reload();
    },

    sqlColumnRenderer: function (val) {
        return '<strong>' + TableConfig.getColumnHumanReadableName(val) + '</strong> <span style="float: right;">[' + val + ']</span>';
    },
    // TABLE COLUMNS / CONDITIONS END


    // // DASHBOARD ACTION
    dashboardAddRemove: function () {
        var vm = this.getViewModel(), widget = vm.get('widget'), entry = vm.get('entry'), action;

        if (!widget) {
            action = 'add';
            widget = Ext.create('Ung.model.Widget', {
                displayColumns: entry.get('displayColumns'),
                enabled: true,
                entryId: entry.get('uniqueId'),
                javaClass: 'com.untangle.uvm.DashboardWidgetSettings',
                refreshIntervalSec: 60,
                timeframe: '',
                type: 'ReportEntry'
            });
        } else {
            action = 'remove';
        }

        Ext.fireEvent('widgetaction', action, widget, entry, function (wg) {
            vm.set('widget', wg);
            Util.successToast('<span style="color: yellow; font-weight: 600;">' + vm.get('entry.title') + '</span> ' + (action === 'add' ? 'added to' : 'removed from') + ' Dashboard!');
        });
    },

    // titleChange: function( control, newValue) {
    //     var me = this, vm = me.getViewModel();

    //     var currentRecord = vm.get('entry');

    //     var titleConflictSave = false;
    //     var titleConflictSaveNew = false;
    //     var sameCustomizableReport = false;
    //     var sameReport = false;
    //     Rpc.asyncData('rpc.reportsManager.getReportEntries')
    //         .then(function(result) {
    //             result.list.forEach( function(reportEntry) {
    //                 if( ( reportEntry.category + '/' + reportEntry.title.trim() )  == ( currentRecord.get('category') + '/' + newValue.trim() ) ){
    //                     titleConflictSave = true;
    //                     titleConflictSaveNew = true;

    //                     if( reportEntry.uniqueId == currentRecord.get('uniqueId') ){
    //                         sameReport = true;
    //                     }
    //                     if( sameReport &&
    //                         currentRecord.get('readOnly') == false){
    //                         sameCustomizableReport = true;
    //                         titleConflictSave = false;
    //                     }
    //                 }
    //             });

    //             if (control){
    //                 if( titleConflictSave && !sameReport ){
    //                     control.setValidation('Another report within this category has this title'.t());
    //                 }else{
    //                     control.setValidation(true);
    //                 }
    //             }

    //             var messages = [];
    //             if(currentRecord.get('readOnly')){
    //                 messages.push( '<i class="fa fa-info-circle fa-lg"></i>&nbsp;' + 'This default report is read-only. Delete and Save are disabled.'.t());
    //             }
    //             if( ( titleConflictSaveNew && !sameCustomizableReport ) || titleConflictSaveNew){
    //                 messages.push( '<i class="fa fa-info-circle fa-lg"></i>&nbsp;'+ 'Change Title to Save as New Report.'.t());
    //             }
    //             vm.set('reportMessages',  messages.join('<br>'));

    //             if(!titleConflictSave){
    //                 vm.set('entry.title', newValue);
    //             }
    //         });
    // },

    /**
     * updates an existing custom report
     */
    updateReport: function () {
        var me = this,
            v = me.getView(),
            vm = me.getViewModel(),
            entry = vm.get('entry'),
            eEntry = vm.get('eEntry'), tdcg, tdc = [];

        // update timeDataColumns or textColumns
        if (eEntry.get('type') === 'TIME_GRAPH') {
            tdcg = v.down('#timeDataColumnsGrid');
            tdcg.getStore().each(function (col) { tdc.push(col.get('str')); });
            eEntry.set('timeDataColumns', tdc);
        }
        if (eEntry.get('type') === 'TEXT') {
            tdcg = v.down('#textColumnsGrid');
            tdcg.getStore().each(function (col) { tdc.push(col.get('str')); });
            eEntry.set('textColumns', tdc);
        }

        v.setLoading(true);
        Rpc.asyncData('rpc.reportsManager.saveReportEntry', eEntry.getData())
            .then(function() {
                v.setLoading(false);

                var modFields = entry.copyFrom(eEntry);

                // if title or category changed, update route
                if (Ext.Array.contains(modFields, 'category') || Ext.Array.contains(modFields, 'title')) {
                    Ext.getStore('reportstree').build();
                    Ung.app.redirectTo('#reports/' + entry.get('category').replace(/ /g, '-').toLowerCase() + '/' + entry.get('title').replace(/\s+/g, '-').toLowerCase());
                }

                vm.set('eEntry', null);
                vm.notify();

                Util.successToast('<span style="color: yellow; font-weight: 600;">' + vm.get('entry.title') + '</span> report updated!');
            });
    },

    /**
     * creates a new report
     */
    saveNewReport: function () {
        var me = this,
            v = this.getView(),
            vm = this.getViewModel(),
            entry = vm.get('eEntry'), tdcg, tdc = [];

        entry.set('uniqueId', 'report-' + Math.random().toString(36).substr(2));
        entry.set('readOnly', false);

        // update timeDataColumns or textColumns
        if (entry.get('type') === 'TIME_GRAPH') {
            tdcg = v.down('#timeDataColumnsGrid');
            tdcg.getStore().each(function (col) { tdc.push(col.get('str')); });
            entry.set('timeDataColumns', tdc);
        }
        if (entry.get('type') === 'TEXT') {
            tdcg = v.down('#textColumnsGrid');
            tdcg.getStore().each(function (col) { tdc.push(col.get('str')); });
            entry.set('textColumns', tdc);
        }

        v.setLoading(true);
        Rpc.asyncData('rpc.reportsManager.saveReportEntry', entry.getData())
            .then(function() {
                v.setLoading(false);
                Ext.getStore('reports').add(entry);
                Util.successToast('<span style="color: yellow; font-weight: 600;">' + entry.get('title') + ' report added!');
                Ung.app.redirectTo('#reports/' + entry.get('category').replace(/ /g, '-').toLowerCase() + '/' + entry.get('title').replace(/\s+/g, '-').toLowerCase());

                Ext.getStore('reportstree').build(); // rebuild tree after save new
                me.reload();
            });
    },

    /**
     * removes a custom created report
     */
    removeReport: function () {
        var me = this, vm = this.getViewModel(),
            entry = vm.get('entry');

        Ext.MessageBox.confirm('Warning'.t(),
            'Deleting this report will also remove Dashboard widgets containing this report!'.t() + '<br/><br/>' +
            'Do you want to continue?'.t(),
        function (btn) {
            if (btn === 'yes') {
                if (vm.get('widget')) {
                    // remove it from dashboard first
                    Ext.fireEvent('widgetaction', 'remove', vm.get('widget'), entry, function (wg) {
                        vm.set('widget', wg);
                        me.removeReportAction(entry.getData());
                    });
                } else {
                    me.removeReportAction(entry.getData());
                }
            }
        });

    },

    removeReportAction: function (entry) {
        var vm = this.getViewModel();
        Rpc.asyncData('rpc.reportsManager.removeReportEntry', entry)
            .then(function () {
                Ung.app.redirectTo('#reports/' + entry.category.replace(/ /g, '-').toLowerCase());
                Util.successToast(entry.title + ' ' + 'deleted successfully'.t());
                vm.set('eEntry', null);
                var removableRec = Ext.getStore('reports').findRecord('uniqueId', entry.uniqueId);
                if (removableRec) {
                    Ext.getStore('reports').remove(removableRec); // remove record
                    Ext.getStore('reportstree').build(); // rebuild tree after save new
                }
            }, function (ex) {
                Util.handleException(ex);
            });
    },

    /**
     * exports an image with current graph chart
     */
    downloadGraph: function () {
        var me = this, vm = me.getViewModel(), now = new Date();
        try {
            me.getView().down('#graphreport').getController().chart.exportChart({
                filename: (vm.get('entry.category') + '-' + vm.get('entry.title') + '-' + Ext.Date.format(now, 'd.m.Y-Hi')).replace(/ /g, '_'),
                type: 'image/png'
            });
        } catch (ex) {
            console.log(ex);
            Util.handleException('Unable to download!');
        }
    },

    /**
     * exports Events list from an Event Report
     */
    exportEventsHandler: function () {
        var me = this, vm = me.getViewModel(), entry = vm.get('entry').getData(), columns = [], startDate, endDate;
        if (!entry) { return; }

        var grid = me.getView().down('eventreport > ungrid');

        if (!grid) {
            console.log('Grid not found');
            return;
        }

        Ext.Array.each(grid.getColumns(), function (col) {
            if (col.dataIndex && !col.hidden) {
                columns.push(col.dataIndex);
            }
        });

        var conditions = [];
        Ext.Array.each(Ext.clone(vm.get('globalConditions')), function (cnd) {
            delete cnd._id;
            conditions.push(cnd);
        });

        // startDate converted from UI to server date
        startDate = Util.clientToServerDate(vm.get('startDate'));
        // endDate converted from UI to server date
        endDate = Util.clientToServerDate(vm.get('endDate'));

        Ext.MessageBox.wait('Exporting Events...'.t(), 'Please wait'.t());
        var downloadForm = document.getElementById('downloadForm');
        downloadForm['type'].value = 'eventLogExport';
        downloadForm['arg1'].value = (entry.category + '-' + entry.title + '-' + Ext.Date.format(vm.get('startDate'), 'd.m.Y-H:i') + '-' + Ext.Date.format(vm.get('endDate'), 'd.m.Y-H:i')).replace(/ /g, '_');
        downloadForm['arg2'].value = Ext.encode(entry);
        downloadForm['arg3'].value = conditions.length > 0 ? Ext.encode(conditions) : '';
        downloadForm['arg4'].value = columns.join(',');
        downloadForm['arg5'].value = startDate ? startDate.getTime() : -1;
        downloadForm['arg6'].value = endDate ? endDate.getTime() : -1;
        downloadForm.submit();
        Ext.MessageBox.hide();
    },

    editEntry: function () {
        var me = this, vm = me.getViewModel();
        vm.set('eEntry', vm.get('entry').copy(null));
        vm.notify();
    },

    cancelEdit: function () {
        var me = this, vm = me.getViewModel();
        vm.set('eEntry', null);
        vm.set('validForm', true);
        vm.notify();

        // if New Entry was initiated from a category view
        if (!vm.get('entry')) {
            me.getView().up('#reports').lookup('cards').setActiveItem('category');
            return;
        }

        me.setReportCard(vm.get('entry.type'));
    },

    onTextColumnsChanged: function (store) {
        var vm = this.getViewModel(), tdc = [];
        // update validation counter
        vm.set('textColumnsCount', store.getCount());
        // update the actual entry
        store.each(function (col) { tdc.push(col.get('str')); });
        vm.set('eEntry.textColumns', tdc);
    },

    onTimeDataColumnsChanged: function (store) {
        var vm = this.getViewModel(), tdc = [];
        // update validation counter
        vm.set('timeDataColumnsCount', store.getCount());
        // update the actual entry
        store.each(function (col) { tdc.push(col.get('str')); });
        vm.set('eEntry.timeDataColumns', tdc);
    },

    removeTextColumn: function (view, rowIndex, colIndex, item, e, record) {
        var me = this, vm = me.getViewModel(), store = view.getStore(), tdc = [];
        store.remove(record);
        view.refresh();
    },

    removeTimeDataColumn: function (view, rowIndex, colIndex, item, e, record) {
        var me = this, vm = me.getViewModel(), store = view.getStore(), tdc = [];
        store.remove(record);
        // vm.set('timeDataColumns', Ext.Array.removeAt(vm.get('timeDataColumns'), rowIndex));
        // store.commitChanges();
        // store.reload();
        // record.drop();
        // store.each(function (col) { tdc.push(col.get('str')); });
        // vm.set('eEntry.timeDataColumns', tdc);
    },

    exportSettings: function () {
        var me = this, vm = me.getViewModel(),
            rep = vm.get('entry').getData();

        delete rep._id;
        delete rep.localizedTitle;
        delete rep.localizedDescription;
        delete rep.slug;
        delete rep.categorySlug;
        delete rep.url;
        delete rep.icon;

        var exportForm = document.getElementById('exportGridSettings');
        exportForm.gridName.value = 'Report-' + rep.title.replace(/ /g, '_');
        exportForm.gridData.value = Ext.encode([rep]);
        exportForm.submit();
    }
});
