Ext.define('Ung.apps.reports.MainController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.app-reports',

    control: {
        '#': {
            afterrender: 'getSettings',
            activate: 'checkGoogleDrive'
        },
        '#email-templates': {
            afterrender: 'emailTemplatesAfterRender'
        }
    },

    getSettings: function () {
        var me = this, v = me.getView(), vm = me.getViewModel();

        v.setLoading(true);
        Ext.Deferred.sequence([
            Rpc.directPromise('rpc.isExpertMode'),
            Rpc.asyncPromise(v.appManager, 'getSettings'),
            Rpc.asyncPromise(v.appManager, 'getFixedReportQueueSize')
        ]).then(function(result){
            if(Util.isDestroyed(v, vm)){
                return;
            }
            vm.set('isExpertMode', result[0]);
            vm.set('settings', result[1]);
            vm.set('reportQueueSize', result[2]);

            vm.set('panel.saveDisabled', false);
            v.setLoading(false);
        }, function(ex) {
            if(!Util.isDestroyed(v, vm)){
                vm.set('panel.saveDisabled', true);
                v.setLoading(false);
            }
            Util.handleException(ex);
        });
    },

    emailTemplatesAfterRender: function(){
        var me = this,
            v = me.getView(),
            vm = me.getViewModel();

        var dayOfWeekList = [];
        for( var i in Renderer.dayOfWeekMap ){
            dayOfWeekList.push( [Number(i) ,Renderer.dayOfWeekMap[i]] );
        }
        vm.set('dayOfWeekList', dayOfWeekList);

        Ext.Deferred.sequence([
            Rpc.asyncPromise('rpc.reportsManager.getCurrentApplications'),
            Rpc.asyncPromise('rpc.reportsManager.fixedReportsAllowGraphs'),
            Rpc.asyncPromise('rpc.reportsManager.getRecommendedReportIds'),
        ]).then(function(result){
            if(Util.isDestroyed(v, vm)){
                return;
            }

            var appCategories = [];
            result[0].list.forEach( function( app ){
                appCategories.push( app.displayName );
            });

            vm.set({
                appCategories: appCategories,
                configCategories: ['Hosts', 'Devices', 'Network', 'Administration', 'Events', 'System', 'Shield'],
                fixedReportsAllowGraphs: result[1],
                emailRecommendedReportIds: result[2].list
            });

            vm.set('panel.saveDisabled', false);
            v.setLoading(false);
        }, function(ex) {
            if(!Util.isDestroyed(v, vm)){
                vm.set('panel.saveDisabled', true);
                v.setLoading(false);
            }
            Util.handleException(ex);
        });
    },

    setSettings: function () {
        var me = this, v = this.getView(), vm = this.getViewModel();

        v.query('ungrid').forEach(function (grid) {
            var store = grid.getStore();
            if (store.getModifiedRecords().length > 0 ||
                store.getNewRecords().length > 0 ||
                store.getRemovedRecords().length > 0 ||
                store.isReordered) {
                store.each(function (record) {
                    if (record.get('markedForDelete')) {
                        record.drop();
                    }
                });
                store.isReordered = undefined;
                vm.set(grid.listProperty, Ext.Array.pluck(store.getRange(), 'data'));
            }
        });

        v.setLoading(true);
        Rpc.asyncData(v.appManager, 'setSettings', vm.get('settings'))
        .then(function(result){
            if(Util.isDestroyed(v, vm)){
                return;
            }
            Util.successToast('Settings saved');
            vm.set('panel.saveDisabled', false);
            v.setLoading(false);

            me.getSettings();
            Ext.fireEvent('resetfields', v);
        }, function(ex) {
            if(!Util.isDestroyed(v, vm)){
                vm.set('panel.saveDisabled', true);
                v.setLoading(false);
            }
            Util.handleException(ex);
        });
    },

    checkGoogleDrive: function () {
        var vm = this.getViewModel();

        Ext.Deferred.sequence([
            Rpc.asyncPromise('rpc.UvmContext.licenseManager.isLicenseValid', 'directory-connector'),
            Rpc.asyncPromise('rpc.appManager.app', 'directory-connector'),
        ]).then(function(result){
            if(Util.isDestroyed(vm)){
                return;
            }
            var googleDriveConfigured = false;
            if(result[0]){
                var directoryConnector = result[1];
                if(directoryConnector){
                    var googleManager = directoryConnector.getGoogleManager();
                    if(googleManager && googleManager.isGoogleDriveConnected()){
                        googleDriveConfigured = true;
                    }
                }
            }
            vm.set('googleDriveConfigured', googleDriveConfigured);
        }, function(ex) {
            Util.handleException(ex);
        });
    },

    reportTypeRenderer: function (value) {
        switch (value) {
            case 'TEXT': return 'Text'.t();
            case 'TIME_GRAPH': return 'Time Graph'.t();
            case 'TIME_GRAPH_DYNAMIC': return 'Time Graph Dynamic'.t();
            case 'PIE_GRAPH': return 'Pie Graph'.t();
            case 'EVENT_LIST': return 'Event List'.t();
        }
    },

    isDisabledCategory: function (view, rowIndex, colIndex, item, record) {
        if (!Ext.getStore('categories').findRecord('displayName', record.get('category'))) {
            return true;
        }
        return false;
    },

    configureGoogleDrive: function () {
        if(Rpc.directData('rpc.appManager.app', 'directory-connector')){
            Ung.app.redirectTo('#service/directory-connector/google');
        } else {
            Ext.MessageBox.alert('Error'.t(), 'Google Drive requires Directory Connector application.'.t());
        }
    },

    onUpload: function (btn) {
        var formPanel = btn.up('form');
        formPanel.submit({
            waitMsg: 'Please wait while data is imported...'.t(),
            success: function () {
                if(!Util.isDestroyed(formpanel)){
                    formPanel.down('filefield').reset();
                }
                // Ext.MessageBox.alert('Succeeded'.t(), 'Upload Data Succeeded'.t());
                Util.successToast('Upload Data Succeeded'.t());
            },
            failure: function (form, action) {
                // var errorMsg = 'Upload Data Failed'.t() + ' ' + action.result;
                // Ext.MessageBox.alert('Failed'.t(), errorMsg);
                Util.handleException('Upload Data Failed'.t() + ' ' + action.result);
            }
        });
    },

});

Ext.define('Ung.apps.reports.cmp.EmailTemplatesGridController', {
    extend: 'Ung.cmp.GridController',

    alias: 'controller.unreportsemailtemplatesgrid',

    reportRenderer: function( value, metaData ){
        var me = this,
            vm = me.getViewModel(),
            reportNames = [],
            allAdded = false,
            typeAdded = [],
            reportEntries = vm.get('settings').reportEntries.list;

        value.list.forEach( function( reportId ){
            if( reportId == '_recommended' ){
                reportNames.push( 'Recommended'.t() );
            }else{
                reportEntries.forEach( function( reportEntry ){
                    if( reportEntry.uniqueId == reportId ){
                        reportNames.push( reportEntry.title );

                    }
                });
            }
        });

        if(reportNames.length == 0){
            reportNames.push( 'None'.t() );
        }
        value = reportNames.join(', ');
        metaData.tdAttr = 'data-qtip="' + Ext.String.htmlEncode( value ) + '"';

        return value;
    },

    editorTitleChange: function( control, newValue, oldValue, eOpts, record ){
        var me = this,
            v = me.getView(),
            vm = me.getViewModel(),
            templates = vm.get('settings.emailTemplates.list');

        var currentRecord = (record === undefined) ? v.getSelectionModel().getSelection()[0] : record;
        var conflict = false;
        templates.forEach( function(template){
            if( template.templateId == currentRecord.get('templateId')){
                return;
            }
            if( template.title == newValue){
                conflict = true;
            }
        });
        if( conflict ){
            control.setValidation("Another Email Template has this title".t());
            return false;
        }
        control.setValidation(true);
    },

    sendReport: function(unk1, unk2, unk3, event, unk5, record){
        var v = this.getView();
        var dialog = v.add({
            xtype: 'app-reports-sendfixedreport',
            title: 'Send Email Report'.t(),
            record: record
        });
        dialog.setPosition(event.getXY());
        dialog.show();

    },

    sendDisabled: function(view, rowIndex, colIndex, item, record){
        return record.dirty;
    },

    runInterfaceTask: null,
    updateReportQueueSize: function(el){
        var me = this, vm = me.getViewModel();
        var queueSize = vm.get('reportQueueSize');
        if(queueSize > 0){
            var runInterfaceTaskDelay = 5000;
            if(me.runInterfaceTask == null){
                me.runInterfaceTask = new Ext.util.DelayedTask( Ext.bind(function(){
                    if(Util.isDestroyed(me, vm, el)){
                    return;
                    }
                    var queueSize = Rpc.directData(el.up('[itemId=appCard]').appManager, 'getFixedReportQueueSize');
                    vm.set('reportQueueSize', queueSize);
                    if(queueSize){
                        el.setHtml('Reports processing'.t() + ': ' + queueSize);
                        me.runInterfaceTask.delay( runInterfaceTaskDelay );
                        return;
                    }else{
                        el.setHtml('');
                        me.runInterfaceTask = null;
                    }
                }, me) );
                me.runInterfaceTask.delay( 100 );
            }
        }
    },

    statics: {
        intervalRender: function(value, cell, record){
            var renderedValue = Renderer.timeInterval(value);
            if(value == 604800 || value == 1 ){
                renderedValue += ' (' + Renderer.dayOfWeek(record.get('intervalWeekStart')) + ')';
            }
            return renderedValue;
        }
    }
});

Ext.define('Ung.apps.reports.cmp.UsersGridController', {
    extend: 'Ung.cmp.GridController',

    alias: 'controller.unreportsusersgrid',

    emailTemplatesRenderer: function (value) {
        var vm = this.getViewModel(),
            templates = vm.get('settings.emailTemplates.list');

        var titles = [];
        Ext.Array.each(templates, function (template) {
            if(value.list.indexOf(template.templateId) > -1){
                titles.push(template.title);
            }
        });

        return titles.join( ', ' );
    }

});

Ext.define ('Ung.model.EnabledReport', {
    extend: 'Ext.data.Model' ,
    fields: [
        { name: 'javaClass', type: 'string' },
        { name: 'list', type: 'auto' }
    ],
    proxy: {
        autoLoad: true,
        type: 'memory',
        reader: {
            type: 'json'
        }
    }
});

Ext.define('Ung.cmp.ReportTemplateSelectController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.unreportemplateselect',

    emailChartTypes: [
        'TEXT',
        'PIE_GRAPH',
        'TIME_GRAPH',
        'TIME_GRAPH_DYNAMIC'
    ],

    control: {
        '#': {
            reconfigure: 'onReconfigure',
            afterrender: 'onAfterRender',
        }
    },

    massageOut: function(){
        var me = this,
            v = this.getView(),
            vm = this.getViewModel();

        var store = v.getStore();
        var emailRecommendedReportIds = vm.get('emailRecommendedReportIds');
        var categories = vm.get(v.group + 'Categories' );

        var recommendedIds = [];
        vm.get('reportEntries').each(function(record ){
            var uniqueId = record.get('uniqueId');
            if( ( emailRecommendedReportIds.indexOf( uniqueId ) > -1 &&
                ( categories.indexOf(record.get('category') ) > -1 ) ) ){
                recommendedIds.push(uniqueId);
            }
        }, this, { filtered: true } );

        selectedIds = [];
        store.each( function(record){
            selectedIds.push(record.get('uniqueId'));
        });
        var onlyRecommended = ( Ext.Array.intersect(selectedIds, recommendedIds).length == recommendedIds.length ) &&
                              ( Ext.Array.merge(selectedIds, recommendedIds).length == recommendedIds.length );

        if( onlyRecommended ){
            selectedIds = ['_recommended'];
        }

        if( ( Ext.Array.intersect(selectedIds, me.originalValues).length != me.originalValues.length ) ||
            ( Ext.Array.merge(selectedIds, me.originalValues).length != me.originalValues.length ) ){
            /* Only update if fields changed */
            var fieldValue = {
                javaClass: 'java.util.LinkedList',
                list: selectedIds
            };
            var bindPath = v.initialConfig.bind.split('.');
            var fieldName = bindPath[1];
            vm.get('record').set(fieldName, fieldValue);
        }
    },

    onReconfigure: function(){
        var me = this,
            v = me.getView(),
            vm = me.getViewModel();

        var store = v.getStore();

        var recommended = false;
        var uniqueIds = [];
        me.originalValues = [];
        store.each( function( record ) {
            var value = record.get('field1');
            if( value == '_recommended'){
                recommended = true;
            }else{
                uniqueIds.push( value );
            }
            me.originalValues.push( value );
        });

        store.removeAll();

        var emailRecommendedReportIds = vm.get('emailRecommendedReportIds');
        var categories = vm.get(v.group + 'Categories' );
        vm.get('reportEntries').each(function(record ){
            var uniqueId = record.get('uniqueId');
            if( ( recommended &&
                  ( emailRecommendedReportIds.indexOf( uniqueId ) > -1 &&
                    categories.indexOf(record.get('category') ) > -1 ) ) ||
                uniqueIds.indexOf( uniqueId ) > -1 ){
                store.add(record);
            }
        }, this, { filtered: true } );

        v.store.sort([{
            property: 'category',
            direction: 'ASC'
        },{
            property: 'displayOrder',
            direction: 'ASC'
        }]);
    },

    onAfterRender: function(){
        var me = this,
            v = this.getView(),
            vm = this.getViewModel();

        // when record is modified update conditions menu
        this.recordBind = vm.bind({
            bindTo: '{record}',
        }, this.setMenuReports, this);

        var reportEntries = vm.get('reportEntries');
        var fixedReportsAllowGraphs = vm.get('fixedReportsAllowGraphs');
        var categories = vm.get( v.group + 'Categories');

        reportEntries.clearFilter();
        if(reportEntries.getSorters().length == 0){
            reportEntries.sort('category');
        }
        reportEntries.filterBy( function(record){
            if( fixedReportsAllowGraphs == false &&
                record.get('type').indexOf("_GRAPH") != -1){
                /* System doesn't allow graphs */
                return false;
            }

            if( me.emailChartTypes.indexOf( record.get('type') ) == -1 ){
                return false;
            }

            if( categories.indexOf( record.get('category') ) == -1 ){
                return false;
            }

            return true;
        });

        var categoryMenu = {};
        reportEntries.each( function(record){
            var category = record.get('category');
            if( !categoryMenu[category] ){
                categoryMenu[category] = {
                    showSeparator: false,
                    plain: true,
                    items: [],
                    mouseLeaveDelay: 0,
                    listeners: {
                        click: 'addReport'
                    }
                };
            }
            categoryMenu[category].items.push({
                text: record.get('title'),
                value: record.get('uniqueId'),
                tooltip: me.getTooltip( record )
            });
        });
        var menu = [];
        for( var category in categoryMenu ){
            menu.push({
                text: category,
                menu: categoryMenu[category]
            });
        }

        v.down('#addReportBtn').setMenu({
            showSeparator: false,
            plain: true,
            items: menu,
            mouseLeaveDelay: 0,
            listeners: {
                click: 'addReport'
            }
        });
    },

    addReport: function( menu, item ){
        var me = this,
            v = me.getView(),
            vm = me.getViewModel();

        if(item){
            vm.get('reportEntries').each(function( record ){
                if( record.get('uniqueId') == item.value ){
                    v.getStore().add( record );
                }
            }, this, { filtered: true } );
            this.setMenuReports();
        }
    },

    /**
     * Removes a condition from the rule
     */
    removeReport: function (view, rowIndex, colIndex, item, e, record) {
        this.getView().getStore().remove( record );
        this.setMenuReports();
    },

    /**
     * Updates the disabled/enabled status of the conditions in the menu
     */
    setMenuReports: function () {
        var v = this.getView(),
            menu = v.down('#addReportBtn').getMenu(),
            store = v.getStore();

        menu.items.each(function (item) {
            if(item.menu){
                item.menu.items.each(function (subitem) {
                    subitem.setDisabled(store.findRecord('uniqueId', subitem.value) ? true : false);
                });
            }else{
                item.setDisabled(store.findRecord('uniqueId', item.value) ? true : false);
            }
        });
    },

    getTooltip: function(record){
        var description = [];
        description.push( 'Description'.t() + ': ' + record.get('description') );
        description.push( 'Type'.t() + ': ' + this.getView().up('app-reports').getController().reportTypeRenderer( record.get('type') ) );
        description.push( 'Units'.t() + ': ' + record.get('units') );
        description.push( 'Display Order'.t() + ': ' + record.get('displayOrder') );
        return description.join( "<br>" );
    },

    categoryRenderer: function( value, metaData, record ){
        metaData.tdAttr = 'data-qtip="' + this.getTooltip( record ) + '"';
        return record.get('category');
    },

    titleRenderer: function( value, metaData, record ){
        metaData.tdAttr = 'data-qtip="' + this.getTooltip( record ) + '"';
        return record.get('title');
    },

    typeRenderer: function( value, metaData, record ){
        metaData.tdAttr = 'data-qtip="' + this.getTooltip( record ) + '"';
        return this.getView().up('app-reports').getController().reportTypeRenderer( record.get('type') );
    }

});


Ext.define('Ung.cmp.ReportTemplateSelect', {
    extend: 'Ext.grid.Panel',
    alias: 'widget.unreporttemplateselect',

    controller: 'unreportemplateselect',

    actions: {
        addReport: {
            itemId: 'addReportBtn',
            text: 'Add Report'.t(),
            iconCls: 'fa fa-plus'
        }
    },

    trackMouseOver: false,
    disableSelection: true,
    sortableColumns: false,
    enableColumnHide: false,
    padding: '10 0',
    tbar: ['@addReport'],

    columns: [{
        header: 'Category'.t(),
        menuDisabled: true,
        align: 'right',
        flex: 1,
        renderer: 'categoryRenderer'
    }, {
        header: 'Title'.t(),
        menuDisabled: true,
        align: 'right',
        flex: 1,
        renderer: 'titleRenderer'
    }, {
        header: 'Type'.t(),
        menuDisabled: true,
        align: 'right',
        flex: 1,
        renderer: 'typeRenderer'
    }, {
        xtype: 'actioncolumn',
        menuDisabled: true,
        sortable: false,
        width: 30,
        align: 'center',
        iconCls: 'fa fa-minus-circle fa-red',
        tdCls: 'action-cell-cond',
        handler: 'removeReport'
    }],

    massageOut: function(){
        return this.getController().massageOut();
    }

});

Ext.define('Ung.apps.reports.cmp.EmailTemplatesRecordEditor', {
    extend: 'Ung.cmp.RecordEditor',
    xtype: 'ung.cmp.unemailtemplatesrecordeditor',

    controller: 'unemailtemplatesrecordeditorcontroller'

});

Ext.define('Ung.apps.reports.cmp.EmailTemplatesRecordEditorController', {
    extend: 'Ung.cmp.RecordEditorController',
    alias: 'controller.unemailtemplatesrecordeditorcontroller',

    onApply: function () {
        var me = this,
            v = me.getView(),
            vm = me.getViewModel();

        v.query('unreporttemplateselect').forEach( function( selector ){
            selector.massageOut();
        });

        if (!this.action) {
            for (var fieldName in vm.get('record').modified) {
                v.record.set(fieldName, vm.get('record').get(fieldName));
            }
        }
        if (this.action === 'add') {
            this.mainGrid.getStore().add(v.record);
        }
        v.close();
    },

    editorTitleChange: function( me, newValue, oldValue, eOpts ){
        var vm = this.getViewModel();

        return this.getView().up('grid').getController().editorTitleChange( me, newValue, oldValue, eOpts, vm.get('record') );
    }

});

Ext.define('Ung.cmp.EmailTemplateSelectController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.unemailtemplateselect',

    control: {
        '#': {
            afterrender: 'onAfterRender',
        }
    },

    onAfterRender: function(){
        var v = this.getView(),
            vm = this.getViewModel();

        this.recordBind = vm.bind({
            bindTo: '{record}',
        }, this.setMenuTemplates, this);

        var emailTemplates = vm.get('emailTemplates');

        var menu = [];
        emailTemplates.each( function(record){
            menu.push({
                text: record.get('title'),
                value: record.get('templateId'),
            });
        });

        v.down('#addTemplateBtn').setMenu({
            showSeparator: false,
            plain: true,
            items: menu,
            mouseLeaveDelay: 0,
            listeners: {
                click: 'addTemplate'
            }
        });
    },

    addTemplate: function( menu, item ){
        var me = this,
            v = me.getView(),
            vm = me.getViewModel();

        if(item){
            vm.get('emailTemplates').each(function( record ){
                if( record.get('templateId') == item.value ){
                    v.getStore().add( {
                        field1: item.value
                    } );
                }
            }, this, { filtered: true } );
            this.setMenuTemplates();
        }
    },

    /**
     * Removes a condition from the rule
     */
    removeReport: function (view, rowIndex, colIndex, item, e, record) {
        this.getView().getStore().remove( record );
        this.setMenuTemplates();
    },

    /**
     * Updates the disabled/enabled status of the conditions in the menu
     */
    setMenuTemplates: function () {
        var v = this.getView(),
            vm = this.getViewModel(),
            menu = v.down('#addTemplateBtn').getMenu(),
            store = v.getStore();

        menu.items.each(function (item) {
            item.setDisabled(store.findRecord('field1', item.value) ? true : false);
        });

        var ids = [];
        store.each( function(record){
            ids.push( record.get('field1'));
        });
        var fieldValue = {
            javaClass: 'java.util.LinkedList',
            list: ids
        };
        var bindPath = v.initialConfig.bind.store.split('.');
        var fieldName = bindPath[1];
        vm.get('record').set(fieldName, fieldValue);

    },

    getTooltip: function(record){
        var vm = this.getViewModel(),
            emailTemplates = vm.get('emailTemplates');

        var emailTemplate = emailTemplates.findRecord('templateId', record.get('field1') );

        var description = [];
        description.push( 'Description'.t() + ': ' + emailTemplate.get('description') );
        return description.join( "<br>" );
    },

    templateRenderer: function( value, metaData, record ){
        var me = this,
            vm = me.getViewModel(),
            emailTemplates = vm.get('emailTemplates');

        metaData.tdAttr = 'data-qtip="' + this.getTooltip( record ) + '"';

        value = record.get('field1');
        var emailTemplate = emailTemplates.findRecord('templateId', value );

        return emailTemplate ? emailTemplate.get('title') : value;
    }
});


Ext.define('Ung.cmp.EmailTemplateSelect', {
    extend: 'Ext.grid.Panel',
    alias: 'widget.unemailtemplateselect',

    controller: 'unemailtemplateselect',

    actions: {
        addTemplate: {
            itemId: 'addTemplateBtn',
            text: 'Add Email Template'.t(),
            iconCls: 'fa fa-plus'
        }
    },

    trackMouseOver: false,
    disableSelection: true,
    sortableColumns: false,
    enableColumnHide: false,
    padding: '10 0',
    tbar: ['@addTemplate'],

    columns: [{
        header: 'Email Template'.t(),
        menuDisabled: true,
        align: 'right',
        flex: 1,
        renderer: 'templateRenderer'
    }, {
        xtype: 'actioncolumn',
        menuDisabled: true,
        sortable: false,
        width: 30,
        align: 'center',
        iconCls: 'fa fa-minus-circle fa-red',
        tdCls: 'action-cell-cond',
        handler: 'removeReport'
    }],

});
