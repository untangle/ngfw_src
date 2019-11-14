Ext.define('Ung.config.events.MainController', {
    extend: 'Ext.app.ViewController',

    alias: 'controller.config-events',

    control: {
        '#': {
            afterrender: 'loadEvents',
        },
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
