Ext.define('Ung.config.events.MainController', {
    extend: 'Ext.app.ViewController',

    alias: 'controller.config-events',

    control: {
        '#': { afterrender: 'loadEvents', },
        '#syslog': { 
            afterrender: function() {
                var me = this;
                Ext.Function.defer(function() { me.onSyslogRulesGridChange(); }, 1000);
            } 
        }
    },

    loadEvents: function () {
        var v = this.getView(),
            vm = this.getViewModel();
        v.setLoading(true);

        Ext.Deferred.sequence([
            Rpc.asyncPromise('rpc.eventManager.getSettings'),
            Rpc.asyncPromise('rpc.eventManager.getClassFields'),
            Rpc.asyncPromise('rpc.eventManager.getTemplateParameters'),
            Rpc.asyncPromise('rpc.eventManager.defaultEmailSettings')
        ], this).then(function(result) {
            if(Util.isDestroyed(v, vm)){
                return;
            }

            var classFields = {};
            var className;
            var classes = [];
            for(className in result[1]){
                classes.push([className, result[1][className]["description"]]);
                var conditions = [];
                var conditionsOrder = [];
                result[1][className]["fields"].forEach(function(field){
                    var fieldNames = field.name.split('.');
                    var ignoreFieldName = fieldNames[fieldNames.length-1]; 
                    if(ignoreFieldName == 'class' || ignoreFieldName == 'timeStamp' ){
                        return;
                    }

                    if(field.name){
                        field.displayName = field.name;
                    }

                    if(field.values){
                        field.type = 'select';
                        field.comparator = 'boolean';
                    }else{
                        switch(field.type.toLowerCase()){
                            case 'boolean':
                                field.type = 'boolean';
                                field.comparator = 'boolean';
                                field.values = [[
                                    true,"True".t()
                                ],[
                                    false,"False".t()
                                ]];
                                break;
                            case 'double':
                            case 'float':
                            case 'integer':
                            case 'int':
                            case 'long':
                            case 'short':
                                field.type = 'numberfield';
                                field.comparator = 'numeric';
                                break;
                            default:
                                field.type = 'textfield';
                                field.comparator = 'text';
                                break;
                        }
                    }

                    var newCondition = Ext.clone(Ung.cmp.ConditionsEditor.defaultCondition);
                    Ext.merge(newCondition, field);
                    conditions.push(newCondition);
                    conditionsOrder.push(newCondition.name);
                });
                classFields[className] = {
                    'description': result[1][className]['description'],
                    'conditions': Ext.Array.toValueMap(conditions, 'name'),
                    'conditionsOrder': conditionsOrder
                };
            }

            var allClassFields = Ext.clone(classFields);
            allClassFields['All'] = {
                description: 'Match all classes (NOT RECOMMENDED!)'.t(),
                conditions: [],
                conditionsOrder: []
            };
            var allClasses = Ext.clone(classes);
            allClasses.push(['All', allClassFields['All']['description']]);

            var templateParametersStore = new Ext.data.JsonStore({
                fields: ['name', 'description'],
                data: result[2]
            });

            vm.set({
                settings: result[0],
                classFields: classFields,
                allClassFields: allClassFields,
                classes: Ext.create('Ext.data.ArrayStore', {
                    fields: [ 'name', 'description' ],
                    sorters: [{
                        property: 'name',
                        direction: 'ASC'
                    }],
                    data: classes
                }),
                allClasses: Ext.create('Ext.data.ArrayStore', {
                    fields: [ 'name', 'description' ],
                    sorters: [{
                        property: 'name',
                        direction: 'ASC'
                    }],
                    data: allClasses
                }),
                targetFields: Ext.create('Ext.data.ArrayStore', {
                    fields: [ 'name', 'description' ],
                    sorters: [{
                        property: 'name',
                        direction: 'ASC'
                    }],
                    data: []
                }),
                templateParametersStore: templateParametersStore,
                templateDefaults: result[3].map,
                templateUnmatched: ''
            });

            if(vm.get("settings.syslogServers.list").length > 0){
                vm.set('syslogServersGridEmpty', false);
            }else{
                vm.set('syslogServersGridEmpty', true);
            }

            vm.set('panel.saveDisabled', false);
            v.setLoading(false);
        }, function(ex) {
            if(!Util.isDestroyed(v, vm)){
                vm.set('panel.saveDisabled', true);
                v.setLoading(false);
            }
        });
    },

    saveSettings: function () {
        var me = this,
            v = this.getView(),
            vm = this.getViewModel();

        if (!Util.validateForms(v)) {
            return;
        }

        v.setLoading(true);

        v.query('ungrid').forEach(function (grid) {
            var store = grid.getStore();
            /**
             * Important!
             * update custom grids only if are modified records or it was reordered via drag/drop
             */
            if (store.getModifiedRecords().length > 0 || store.isReordered) {
                store.each(function (record) {
                    if (record.get('markedForDelete')) {
                        record.drop();
                    }
                });
                if(grid.itemId == 'syslogservers') {
                    store.each(function (record) {
                        if(!record.get('tag')) {
                            record.set('tag', Ext.String.format('uvm-to-{0}', record.get('host')));
                        }
                    });
                }
                store.isReordered = undefined;
                vm.set(grid.listProperty, Ext.Array.pluck(store.getRange(), 'data'));
                store.commitChanges();
            }
        });

        Rpc.asyncData('rpc.eventManager.setSettings', vm.get('settings'))
        .then(function() {
            if(Util.isDestroyed(me, v)){
                return;
            }
            me.loadEvents();
            Util.successToast('Events'.t() + ' ' + 'settings saved!'.t());
            Ext.fireEvent('resetfields', v);

            vm.set('panel.saveDisabled', false);
            v.setLoading(false);
            Ext.Function.defer(function() { me.onSyslogRulesGridChange(); }, 1000);
        }, function(ex) {
            if(!Util.isDestroyed(v, vm)){
                vm.set('panel.saveDisabled', true);
                v.setLoading(false);
            }
        });
    },

    /**
     * Format the template preview for display in HTML contexT:
     *     -    If any unmatched %paramters% are found, highlight with cite.
     *     -    Replace spaces wth &nbsp;
     * @param  template to modify.
     * @return Modified template.
     */
    templateFormat: function(template){
        var vm = this.getViewModel();
        template = template.replace( /\%[^\%\s]*\%/gm, function(parameter){
            vm.set('templateUnmatched', 'One or more customization parameters are unknown'.t() );
            return '<cite>' + parameter+ '</cite>';
        });
        template = template.replace( /\s/g, '&nbsp;');
        return template;
    },

    /**
     * On template fields change, set a preview task to fire in 500ms.
     * After fired, display results in preview.
     */
    getPreviewTask: null,
    templateChange: function(cmp, newValue, oldValue, eOpts){
        var v = this.getView(),
            vm = this.getViewModel();

        var alertRule = {
            javaClass: 'com.untangle.uvm.event.AlertRule',
            conditions: {
                javaClass: 'java.util.LinkedList',
                list: [{
                    comparator: '=',
                    field: 'class',
                    fieldValue: '*SystemStatEvent*',
                    javaClass: 'com.untangle.uvm.event.EventRuleCondition'
                }]
            },
            description: 'Preview Rule'.t(),
            ruleId: -1,
            enabled: true,
            thresholdEnabled: false,
            thresholdTimeframeSec: 60,
            thresholdGroupingField: null,
            email: true,
            emailLimitFrequency: false,
            emailLimitFrequencyMinutes: 0,

        };

        var event = {
            javaClass: 'com.untangle.uvm.logging.SystemStatEvent',
            load15: 0.08,
            load5: 0.02,
            load1: 0
        };

        var me = this;
        var meChange = me;
        if(me.getPreviewTask == null){
            me.getPreviewTask = new Ext.util.DelayedTask( Ext.bind(function(){
                var me = this,
                    vm = me.getViewModel();

                    Ext.Deferred.sequence([
                        Rpc.asyncPromise('rpc.eventManager.emailAlertFormatPreview', alertRule, event, v.down('[itemId=emailSubject]').getValue(), v.down('[itemId=emailBody]').getValue(), v.down('[itemId=emailConvert]').getValue()),
                    ], this).then(function(result) {
                        if(Util.isDestroyed(vm)){
                            return;
                        }

                        vm.set('templateUnmatched', '');
                        vm.set('previewSubject', me.templateFormat(result[0].map['emailSubject']));
                        vm.set('previewBody', me.templateFormat(result[0].map['emailBody']));
                        meChange.getPreviewTask = null;

                    }, function(ex) {
                        if(!Util.isDestroyed(vm)){
                            vm.set('panel.saveDisabled', true);
                            v.setLoading(false);
                        }
                        meChange.getPreviewTask = null;
                    });
            }, me) );
        }
        me.getPreviewTask.delay( 500 );

    },

    /**
     * Pull default templates and populate.
     */
    templateDefaults: function(){
        var me = this,
            vm = this.getViewModel();

        Rpc.asyncData('rpc.eventManager.defaultEmailSettings')
        .then(function(result) {
            if(Util.isDestroyed(me, vm)){
                return;
            }
            for(var key in result.map){
                var value = result.map[key];
                if(value == "true" || value == "false"){
                    vm.set("settings." + key, (value == "true") ? true : false);
                }else{
                    vm.set("settings." + key, null);
                }
            }
        });
    },

    onSyslogServersGridChange: function(store, record){
        var vm = this.getViewModel();
        if((store.getModifiedRecords().length > 0 ||
                store.getNewRecords().length > 0 ||
                store.getRemovedRecords().length > 0)) {
            vm.set('syslogRuleGridDisabled', true);
            // Check if modified records only have reserved boolean modified 
            // and accordingly set skipDetectGridChanges flag
            var isGridPropertyChanged;
            store.getModifiedRecords().forEach(function(modifiedRecord) {
                if(modifiedRecord.modified) {
                    var keys = Object.keys(modifiedRecord.modified);
                    if(!(keys.length === 1 && keys[0] === 'reserved')) {
                        isGridPropertyChanged = true;
                    }
                }
            });
            store.skipDetectGridChanges = !isGridPropertyChanged;
        } else {
            vm.set('syslogRuleGridDisabled', false);
        }
    },

    onSyslogRulesGridChange: function(store, record) {
        var view = this.getView(),
            vm = this.getViewModel(),
            usedServerIds = {},
            serverList = [];
        store = store ? store : view.down('config-events-syslog').down('[itemId=syslogrules]').getStore();

        // Find serverId's of syslog servers which are used in syslog rules         
        store.each(function(rulesRecord) {
            if(!rulesRecord.get('markedForDelete')) {
                if(rulesRecord.data.syslogServers && rulesRecord.data.syslogServers.list) {
                    serverList = rulesRecord.data.syslogServers.list;
                    if(!Ext.isArray(serverList)) {
                        serverList = [ serverList ];
                    }
                    serverList.forEach(function(serverId) {
                        usedServerIds[serverId] = true;
                    });
                }
            }
        });

        // Mark used syslog server records as reserved
        var syslogServerStore = view.down('config-events-syslog').down('[itemId=syslogservers]').getStore();
        syslogServerStore.each(function(serversRecord) {
            if(usedServerIds[serversRecord.get('serverId')]) {
                serversRecord.set('reserved', true);
            } else {
                serversRecord.set('reserved', false);
            }
        });
        vm.set('syslogRuleGridDisabled', false);
    },

    statics: {
        conditionsClass: {
            header: 'Class'.t(),
            width: Renderer.conditionsWidth,
            flex: 2,
            dataIndex: 'conditions',
            renderer: function(){
                return Ung.config.events.ConditionsEditor.classRenderer.apply(this, arguments);
            }
        },
        conditions: {
            header: 'Conditions'.t(),
            width: Renderer.conditionsWidth,
            flex: 2,
            dataIndex: 'conditions',
            renderer: function(){
                return Ung.config.events.ConditionsEditor.conditionsRenderer.apply(this, arguments);
            }
        }
    }

});

Ext.define('Ung.config.events.SyslogRulesController', {
    extend: 'Ung.cmp.GridController',
    alias: 'controller.uneventssyslogrulesgrid',

    sysLogServersRenderer: function(value, column) {
        value.javaClass = "java.util.LinkedList";
        if(value.list) {
            if(!Ext.isArray(value.list)) {
                value.list = [ value.list ];
            }
            return this.getSysLogsServerNameFromId(value.list, column);
        } else {
            value.list = [];
        }
        var noServerStr = '<i>' + 'No Sys Log Server'.t() + '<i>';
        column.tdAttr = 'data-qtip="' + Ext.String.htmlEncode(noServerStr) + '"';
        return noServerStr;
    },

    /**
     * Method to get list of syslog server hostnames from syslog server Id list
     * @param list List of sysolg server id's
     * @return List of syslog server host names whose id's are present in serverId list
     */
    getSysLogsServerNameFromId: function(serverIds, column) {
        if(serverIds.length == 0) {
            var noServerStr = '<i>' + 'No Sys Log Server'.t() + '<i>';
            column.tdAttr = 'data-qtip="' + Ext.String.htmlEncode(noServerStr) + '"';
            return noServerStr;
        }

        var serverList = this.getViewModel().get('settings.syslogServers.list'),
            data = serverList.filter(function(server) {
                return serverIds.includes(server.serverId);
            }).map(function(server) {
                return server.description;
            });
        column.tdAttr = 'data-qtip="' + Ext.String.htmlEncode(data.join(", ")) + '"';
        return data.join(", ");
    }
});

Ext.define('Ung.config.events.SyslogRulesEditor', {
    extend: 'Ung.cmp.RecordEditor',
    xtype: 'ung.cmp.unsyslogruleseditor',
    itemId: 'syslogruleseditor',

    controller: 'unsyslogruleseditorcontroller'
});

Ext.define('Ung.config.events.SyslogRulesEditorController', {
    extend: 'Ung.cmp.RecordEditorController',
    alias: 'controller.unsyslogruleseditorcontroller',

    control: {
        '#syslogruleseditor': {
            afterrender: 'afterSysLogRuleEditorRender'
        }
    },

    afterSysLogRuleEditorRender: function() {
        var view = this.getView();
            actionContainer = view.down('#actioncontainer');
        actionContainer.removeAll();
        actionContainer.add({
            xtype: 'checkboxgroup',
            fieldLabel: 'Syslog Servers'.t(),
            useParentDefinition: true,
            itemId: 'syslogserverscheckbox',
            labelWidth: 155,
            readOnlyCls: 'x-item-disabled',
            bind: {
                value: '{record.syslogServers}'
            },
            columns: 3,
            vertical: true,
            items: this.getSyslogServerCBItems()
        });
    },

    getSyslogServerCBItems: function() {
        var view = this.getView(),
            items = [],
            syslogServerStore = view.up('config-events-syslog').down('[itemId=syslogservers]').getStore();

        syslogServerStore.each(function(record) {
            if(record.get('serverId') > 0 && !record.get('markedForDelete')) {
                var readOnly = !record.get('enabled'),
                    qtip = readOnly ? Ext.String.format('Enable the server with host {0}'.t(), record.get('host')) : record.get('host');
                items.push({ 
                    boxLabel: record.get('description'),
                    name: 'list', 
                    inputValue: Number(record.get('serverId')),
                    autoEl: {
                        tag: 'div',
                        'data-qtip': qtip
                    },
                    readOnly: readOnly
                });
            }
        });
        return items;
    }
});
