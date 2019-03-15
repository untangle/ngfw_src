Ext.define('Ung.apps.intrusionprevention.MainController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.app-intrusion-prevention',

    control: {
        '#': {
            afterrender: 'getSettings',
        }
    },

    getSettings: function () {
        var me = this, v = this.getView(), vm = this.getViewModel();

        v.setLoading(true);

        var getSignatures = function(){};
        if(vm.get('signaturesList') == null){
            getSignatures = function(){
                return Ext.Ajax.request({
                    url: "/admin/download",
                    method: 'POST',
                    params: {
                        type: "IntrusionPreventionSettings",
                        arg1: "signatures",
                        arg2: !Util.isDestroyed(vm) ? vm.get('instance.id') : null
                    },
                    timeout: 600000});
            };
        }

        Ext.Deferred.sequence([
            Rpc.asyncPromise(v.appManager, 'getAppStatus'),
            Rpc.directPromise('rpc.companyName'),
            Rpc.asyncPromise('rpc.metricManager.getMemTotal'),
            Rpc.asyncPromise(v.appManager, 'getSettings'),
            Rpc.directPromise('rpc.isExpertMode'),
            getSignatures
        ]).then(function(result){
            if(Util.isDestroyed(me, v, vm)){
                return;
            }

            var status = result[0];

            vm.set({
                lastUpdateCheck: (status['lastUpdateCheck'] && status['lastUpdateCheck'] !== null && status['lastUpdateCheck'].time !== 0 ) ? Renderer.timestamp(status['lastUpdateCheck']) : "Never".t(),
                lastUpdate: (status['lastUpdate'] && status['lastUpdate'] !== null && status['lastUpdate'].time !== 0 ) ? Renderer.timestamp(status['lastUpdate']) : "Never".t(),
                daemonErrors: "",
                companyName: result[1],
                system_memory: result[2],
                settings: result[3],
                isExpertMode: result[4],
                homeNetworks: (status.homeNetworks != null ? '[' + status.homeNetworks.join(', ') + ']' : ''),
                defaultNetwork: (status.homeNetworks != null ? status.homeNetworks[0] : '192.168.1.0/24')
            });

            v.setLoading('Loading signatures...'.t());
            var dt = new Ext.util.DelayedTask( Ext.bind(function(){
                me.buildVariables();
                me.buildSignatures(result[5]);
                me.buildErrors(status["errors"]);
                v.setLoading('');
                v.setLoading(false);
            }, me));
            dt.delay(100);
            vm.set('panel.saveDisabled', false);

        }, function (ex) {
            if(!Util.isDestroyed(me, v )){
                vm.set('panel.saveDisabled', true);
                v.setLoading(false);
            }
            Util.handleException(ex);
        });
    },

    buildVariables: function(){
        var me = this, vm = this.getViewModel();

        var networkVariablesList = [{
            value: 'recommended', 
            description: 'Recommended'.t() 
        }];

        vm.get('variables').each( function(variable){
            var value = Ung.apps.intrusionprevention.MainController.variableValueResolve.call(me, variable);
            if(Ung.model.intrusionprevention.rule.ipv4NetworkRegex.test(value)){
                networkVariablesList.push({
                    value: '$' + variable.get('name'),
                    description: Ext.String.format( '{0} - {1}', variable.get('name'), variable.get('value')),
                    detail: value
                });
            }
        });
        vm.set('networkVariablesList', networkVariablesList);
    },

    buildSignatures: function(reserved){
        var v = this.getView(), vm = this.getViewModel();

        // var t0 = performance.now();
        // var t1 = performance.now();
        // var t2 = performance.now();

        var signatures = [];
        if(typeof(reserved) == 'undefined'){
            /**
             * Rebuild using exisitng list, removing custom signatures.
             */
            var newSignatures = [];
            vm.get('signaturesList').forEach( function(signature){
                if(signature.get('default') == true){
                    newSignatures.push(signature);
                }
            });
            signatures = newSignatures;
        }else{
            /**
             * Build from default signature database.
             */
            var filenameRegex = new RegExp("^# filename: (emerging\-|)(.+)\.rules$");
            // var filenameStrips = ['emerging-'];
            var matches;
            var category;
            reserved.responseText.split("\n").forEach(function(line){
                // if(category == 'attack_response'){
                // if(category == 'games'){
                //     return false;
                // }
                line = line.trim();
                if(line){
                    // if(line.startsWith('# filename')){
                    //     console.log(line);
                    // }
                    // if(line && line != "#" && !line.startsWith("# ") && !line.startsWith("#**") ){
                    if( filenameRegex.test( line ) ){
                        var matches = line.match(filenameRegex);
                        if( matches ){
                            category = matches[2];
                            // console.log(category);
                        }
                    }else if( Ung.model.intrusionprevention.signature.isValid( line ) ){
                        // if(category == 'files'){
                            // console.log(line);
                            signatures.push(new Ung.model.intrusionprevention.signature(line, category, true));
                        // }
                    // }else{
                    //     console.log("invalid signature:" + line);
                    }
                }
            });
        }
        // console.log(signatures);
        // t1 = performance.now();
        // console.log('build signatures: ' + (t1-t0));
        // console.log(signatures.length);

        // Process custom signatures
        vm.get('settings.signatures.list').forEach(function(settingsSignature){
            signatures.push(new Ung.model.intrusionprevention.signature(settingsSignature.signature, settingsSignature.category, false));
        });
        vm.set('signaturesList', signatures);

        // Add protocols found in Suricata rules.
        var conditions = v.down('[name=rules]').getController().getConditions();
        var protocols = conditions['PROTOCOL'].values;
        var found;
        signatures.forEach(function(signature){
            var signatureProtocol = signature.get('protocol');
            found = false;
            protocols.forEach( function(protocol){
                if(protocol[0] == signatureProtocol){
                    found = true;
                }
            });
            if(found == false){
                protocols.push([signatureProtocol, signatureProtocol]);
            }
        });
    },


    buildErrors: function(errors){
        var parsedErrors = [],
            vm = this.getViewModel();

        errors.split("\n").forEach( function(error){
            var matches = Ung.apps.intrusionprevention.MainController.regexDaemonError.exec(error);
            if(matches){
                parsedErrors.push(matches[1]);
            }
        });
        vm.set('daemonErrors', parsedErrors.join("\n"));
    },

    setSettings: function (additionalChanged) {
        var me = this, v = this.getView(), vm = this.getViewModel();

        if (!Util.validateForms(v)) {
            return;
        }

        v.setLoading(true);

        v.query('ungrid').forEach(function (grid) {
            var settingsProperty = grid.listProperty;
            if(grid.getBind().store){
                var bindName = grid.getBind().store.stub.name;
                settingsProperty = grid.getBind().store.owner.config.stores[bindName].data;
                settingsProperty = settingsProperty.substring(1,settingsProperty.length - 1);
            }

            if(settingsProperty){
                var store = grid.getStore();
                if(settingsProperty == 'signaturesList'){
                    var signatures = [];
                    store.each(function(record){
                        if(!record.get('default') && !record.get('markedForDelete')) {
                            signatures.push({
                                javaClass: 'com.untangle.app.intrusion_prevention.IntrusionPreventionSignature',
                                signature: record.getRecord(),
                                category: record.get('category')
                            });
                        }
                    }, this, true);
                    vm.set('settings.signatures.list', signatures);
                }else{
                    if (store.getModifiedRecords().length > 0 ||
                        store.getNewRecords().length > 0 ||
                        store.getRemovedRecords().length > 0 ||
                        store.isReordered) {
                        store.each(function (record) {
                            if (record.get('markedForDelete')) {
                                record.drop();
                            }
                        }, this, true);
                        store.isReordered = undefined;
                        vm.set(settingsProperty, Ext.Array.pluck(store.getRange(), 'data'));
                    }
                }
            }
        });

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

    isVariableUsed: function(variable) {
        var vm = this.getViewModel();

        if(Ext.isEmpty(variable)) {
            return false;
        }

        var signature, originalId, signatureMatches, variableMatches, j, internalId, d;
        // var isUsed = false;
        var isUsed = {};

        vm.get('signatures').each(function(record){
            var signatureMatches = Ung.apps.intrusionprevention.MainController.regexSignature.exec( record.get('signature') );
            if( signatureMatches ) {
                for( j = 1; j < signatureMatches.length; j++ ) {
                    variableMatches = Ung.apps.intrusionprevention.MainController.regexSignatureVariable.exec( signatureMatches[j] );
                    if( variableMatches && variableMatches.shift() == '$'+variable){
                        // isUsed = true;
                        if(!('signatures' in isUsed)){
                            isUsed['signatures'] = [];
                        }
                        // isUsed['signatures'].push(record.get('id'));
                        isUsed['signatures'].push(record);
                        // return false;
                    }
                }
            }
        });

        vm.get('rules').each(function(rule){
            if(rule.get('action') == 'whitelist'){
                if( rule.get('sourceNetworks') == '$'+variable || 
                    rule.get('destinationNetworks') == '$'+variable){
                    if(!('rules' in isUsed)){
                        isUsed['rules'] = [];
                    }
                    // isUsed['rules'].push(rule.get('id'));
                    isUsed['rules'].push(rule);
                }
            }
        });

        return isUsed;
    },

    rulesChanged: function(store, record, action, recordActions, data){
        // console.log('rulesChanged');
        this.getViewModel().get('signatures').each(function(signature){
            var matchingRules = signature.get('matchingRules'); 
            var index = matchingRules.indexOf(record);
            if(index != -1){
                matchingRules.splice(index, 1);
                signature.set('matchingRules', matchingRules);
            }
        });
        this.getView().down('[itemId=signatures]').getView().refresh();

        var me = this;
        me.watchSignatureStoreTask = new Ext.util.DelayedTask( Ext.bind(function(){
            var me = this,
                vm = me.getViewModel(),
                store = vm.get('signatures');
            if(store == null){
                me.watchSignatureStoreTask.delay( 500 );
            }else{
                var status = me.ruleSignatureMatches();
                vm.set({
                    signatureStatusTotal: store.getCount(),
                    signatureStatusLog: Ext.Array.sum(Ext.Object.getValues(status.log)),
                    signatureStatusBlock: Ext.Array.sum(Ext.Object.getValues(status.block)),
                    signatureStatusDisable: Ext.Array.sum(Ext.Object.getValues(status.disable))
                });
            }
        }, me) );
        me.watchSignatureStoreTask.delay( 500 );
    },

    signaturesChanged: function(store){
        // console.log('signaturesChanged');
        var me = this;

        me.watchSignatureStoreTask = new Ext.util.DelayedTask( Ext.bind(function(){
            var me = this,
                vm = me.getViewModel(),
                store = vm.get('signatures');
            if(store == null){
                me.watchSignatureStoreTask.delay( 500 );
            }else{
                var status = me.ruleSignatureMatches();
                vm.set({
                    signatureStatusTotal: store.getCount(),
                    signatureStatusLog: Ext.Array.sum(Ext.Object.getValues(status.log)),
                    signatureStatusBlock: Ext.Array.sum(Ext.Object.getValues(status.block)),
                    signatureStatusDisable: Ext.Array.sum(Ext.Object.getValues(status.disable))
                });
            }
        }, me) );
        me.watchSignatureStoreTask.delay( 500 );
    },

    variablesChanged: function(store){
        this.buildVariables();
        this.getView().down('[itemId=rules]').getView().refresh();
    },

    /**
     * ExtJs does not seem to allow tooltip binding, so to get around this, create
     * model variable with 'value' and 'tip' keys and when the value changes,
     * look at the bind parent value for that object and set the tooltip.
     */
    bindChange: function(cmp){
        cmp.getEl().set({
            'data-qtip': cmp.bind.value.stub.parentValue.tip
        });
    },

    ruleSignatureMatches: function(matchRule, conditions){
        var me = this,
            v = me.getView(),
            vm = me.getViewModel(),
            signatures = vm.get('signatures'),
            status = {
                log: {},
                block: {},
                disable: {}
            };
        // var t0 = performance.now();

        if(signatures == null){
            return status;
        }

        if(conditions == null){
            conditions = v.down('[name=rules]').getController().getConditions();
        }

        var rules = vm.get('rules');
        if(matchRule != null){
            rules = Ext.create('Ext.data.Store');
            rules.add(matchRule);
        }

        var signatureRecommendedAction, signatureCurrentAction;
        signatures.each( function(signature){
            signatureRecommendedAction = signature.get('recommendedAction');
            var disabled = false;
            if(!matchRule){
                disabled = true;
            }
            rules.each(function(rule){
                if(rule.get('enabled')){
                    var action = rule.get('action');

                    if(rule.matchSignature(signature, conditions, vm) == true){
                        if(action == 'whitelist'){
                            if(signature.data['matchingRules'].indexOf(rule) == -1){
                                signature.data['matchingRules'].push(rule);
                            }
                            return true;
                        }
                        disabled = false;
                        if(action == 'default'){
                            action = signatureRecommendedAction;
                        }
                        if(action == 'blocklog'){
                            action = ( signatureRecommendedAction == 'log' || signatureRecommendedAction == 'block') ? 'block' : 'disable';
                        }
                        if(!matchRule){
                            if(signature.data['matchingRules'].indexOf(rule) == -1){
                                signature.data['matchingRules'].push(rule);
                            }
                        }
                        status[action][signature.get('id')] = true;
                        return false;
                    }
                }
            });

            if(!matchRule && ( disabled == true)){
                status['disable'][signature.get('id')] = true;
            }
        }, this, true);
        // console.log(performance.now()- t0);
        // console.log(status);

        // console.log(status);
        return status;
    },

    statics:{
        // Example: Jan 29 09:57:07 devhostname.example.com suricata[126833]: [126833] <Error> -- [ERRCODE: SC_ERR_INVALID_ACTION(142)] - An invalid...
        regexDaemonError:  /\] \- (.*)$/,
        regexSignatureVariable:  /\$([A-Za-z0-9\_]+)/,
        regexSignature: /^([#]+|)(alert|log|pass|activate|dynamic|drop|sdrop|reject)\s+(tcp|udp|icmp|ip)\s+([^\s]+)\s+([^\s]+)\s+([^\s]+)\s+([^\s]+)\s+([^\s]+)\s+\((.+)\)$/,

        ruleActionsRenderer: function(value, meta, record, x,y, z, table){
            var me = this;

            var variableStore = me.getView().up('apppanel').getViewModel().get('variables');

            var actionValue = Ung.apps.intrusionprevention.Main.ruleActions.findRecord('value', value, 0, false, false, true).get('description');
            var summarySuffix = '';
            var detailSuffix = '';
            if( value == 'whitelist' ){
                var summaryEntries = [];
                var detailEntries = [];
                Ung.apps.intrusionprevention.Main.actionNetworks.forEach( function(network){
                    var value = record.get(network['key']);
                    if(value == 'recommended'){
                        summaryEntries.push('Recommended'.t());
                        detailEntries.push('Recommended'.t());
                    }else{
                        summaryEntries.push(network['label'] + ': !' + value);
                        detailEntries.push(network['label'] + ': !' + Ung.apps.intrusionprevention.MainController.variableValueResolve.call(me, variableStore.findRecord('name', value.substr(1), 0, false, false, true)));
                    }
                });
                summarySuffix = ' [' + summaryEntries.join(', ') + ']';
                detailSuffix = ' [' + detailEntries.join(', ') + ']';
            }
            meta.tdAttr = 'data-qtip="' + Ext.String.htmlEncode( actionValue + detailSuffix ) + '"';
            return actionValue + summarySuffix;
        },

        idRenderer: function( value, metaData, record, rowIdx, colIdx, store ){
            var sid = record.get('sid');
            var gid = record.getOption('gid');
            if(!gid){
                gid = '1';
            }
            metaData.tdAttr = 'data-qtip="' + Ext.String.htmlEncode( "Sid".t() + ": " + sid + ", " + "Gid".t() + ":" + gid) + '"';
            return value;
        },

        classtypeRenderer: function( value, metaData, record, rowIdx, colIdx, store ){
            var classtypeRecord = Ung.apps.intrusionprevention.Main.classtypes.findRecord('value', value, 0, false, false, true);
            var description = value;
            if( classtypeRecord != null ){
                description = classtypeRecord.get('description');
            }
            metaData.tdAttr = 'data-qtip="' + Ext.String.htmlEncode( description ) + '"';
            return value;
        },

        categoryRenderer: function( value, metaData, record, rowIdx, colIdx, store ){
            var description = value;
            var categoryRecord = Ung.apps.intrusionprevention.Main.categories.findRecord('value', value, 0, false, false, true);
            if( categoryRecord != null ){
                description = categoryRecord.get('description');
            }
            metaData.tdAttr = 'data-qtip="' + Ext.String.htmlEncode( description  ) + '"';
            return value;
        },

        referencesMap: {
            "bugtraq": "http://www.securityfocus.com/bid/",
            "cve": "http://cve.mitre.org/cgi-bin/cvename.cgi?name=",
            "nessus": "http://cgi.nessus.org/plugins/dump.php3?id=",
            "arachnids": "http://www.whitehats.com/info/IDS",
            "mcafee": "http://vil.nai.com/vil/content/v",
            "osvdb": "http://osvdb.org/show/osvdb/",
            "msb": "http://technet.microsoft.com/en-us/security/bulletin/",
            "url": "http://"
        },

        referenceRenderer: function( value, metaData, record, rowIdx, colIdx, store ){
            var references = [];
            var optionReferences = record.getOption('reference');
            if(optionReferences != null){
                if(!Array.isArray(optionReferences)){
                    optionReferences = [optionReferences];
                }
                optionReferences.forEach(function(reference){
                    if(typeof(reference) != 'string'){
                        return false;
                    }
                    reference = reference.split(',', 2);
                    if(Ung.apps.intrusionprevention.MainController.referencesMap[reference[0]]){
                        reference[1].split(',').forEach( function(target){
                            target = target.trim(target);
                            if((target.charAt(0) == '"') &&
                                (target.charAt(target.length - 1) == '"')){
                                target = target.substr(1,target.length - 2);
                            }
                            var url = Ung.apps.intrusionprevention.MainController.referencesMap[reference[0]] + target;
                            references.push('<a href="'+ url + '" class="fa fa-search fa-black" style="text-decoration: none; color:black" target="_reference"></a>');
                        });
                    }
                });
            }
            metaData.tdAttr = 'data-qtip="' + Ext.String.htmlEncode( record.build() ) + '"';
            return references.join("");
        },

        signatureRenderer: function(value, metaData, record, rowIdx, colIdx, store){
            if(record.build){
                metaData.tdAttr = 'data-qtip="' + Ext.String.htmlEncode( record.build() ) + '"';
            }
            return Ext.String.htmlEncode( value );
        },

        recommendedActionRenderer: function( value, metaData, record, rowIdx, colIdx, store ){
            var description = value;
            var actionRecord = Ung.apps.intrusionprevention.Main.signatureActions.findRecord('value', value, 0, false, false, true);
            if( actionRecord != null ){
                description = actionRecord.get('description');
            }
            metaData.tdAttr = 'data-qtip="' + Ext.String.htmlEncode( description  ) + '"';
            return Ext.String.htmlEncode( description );
        },
        signatureRuleActionRenderer: function(value, metaData, record, rowIdx, colIdx, store){
            var ruleDescription = '';

            var rule = null;
            record.get('matchingRules').forEach( function(matchingRule){
                if(matchingRule.get('action') != 'whitelist'){
                    rule = matchingRule;
                    return false;
                }
            } );

            if(rule != null){
                var ruleAction = rule.get('action');
                var signatureRecommendedAction = record.get('recommendedAction');
                actionDescription = rule.get('action');
                ruleDescription = Ext.String.format( ' (' + 'Rule: {0}, Action:{1}'.t() + ')'.t(), rule.get('description'), Ung.apps.intrusionprevention.Main.ruleActions.findRecord('value', ruleAction, 0, false, false, true).get('description'));
                if(ruleAction == 'default'){
                    actionDescription = Ung.apps.intrusionprevention.Main.signatureActions.findRecord('value', signatureRecommendedAction, 0, false, false, true).get('description');
                }else if(ruleAction == 'blocklog'){
                    if(signatureRecommendedAction == 'log'){
                        actionDescription = Ung.apps.intrusionprevention.Main.signatureActions.findRecord('value', 'block', 0, false, false, true).get('description');
                    }else{
                        actionDescription = Ung.apps.intrusionprevention.Main.signatureActions.findRecord('value', 'disable', 0, false, false, true).get('description');
                    }
                }else{
                    actionDescription = Ung.apps.intrusionprevention.Main.signatureActions.findRecord('value', actionDescription, 0, false, false, true).get('description');
                }
            }else{
                actionDescription = Ung.apps.intrusionprevention.Main.signatureActions.findRecord('value', 'disable', 0, false, false, true).get('description');
                ruleDescription = ' (' + 'No rule match'.t() + ')';
            }
            metaData.tdAttr = 'data-qtip="' + Ext.String.htmlEncode( actionDescription  ) + Ext.String.htmlEncode( ruleDescription  ) + '"';
            return Ext.String.htmlEncode( actionDescription );
        },

        variableProcessValue: function(name, value){
            var vm = (this.getView().up('apppanel') || this.getView()).getViewModel();

            if(value == 'default'){
                switch(name){
                    case 'HOME_NET':
                        value = vm.get('homeNetworks');
                        break;
                    default:
                        value = 'unknown';
                }
            }
            return value;
        },

        variableValueResolve: function(record){
            var vm = (this.getView().up('apppanel') || this.getView()).getViewModel(),
                variablesStore = vm.get('variables'),
                expandedValue = record.get('value'),
                resolvedRecord = null,
                variableMatches = null,
                resolvedValue = null;

            do{
                variableMatches = Ung.apps.intrusionprevention.MainController.regexSignatureVariable.exec( expandedValue );
                if(variableMatches){
                    resolvedRecord = variablesStore.findRecord('name', variableMatches[1], 0, false, false, true);
                    if(resolvedRecord){
                        resolvedValue = resolvedRecord.get('value');
                        expandedValue = expandedValue.replace(variableMatches[0], Ung.apps.intrusionprevention.MainController.variableProcessValue.call(this, variableMatches[1], resolvedValue));
                    }
                }
            }while(variableMatches != null);

            return Ung.apps.intrusionprevention.MainController.variableProcessValue.call(this, record.get('name'), expandedValue);
        },

        variableValueRenderer: function(value, metaData, record, rowIdx, colIdx, store){
            metaData.tdAttr = 'data-qtip="' + Ext.String.htmlEncode( Ung.apps.intrusionprevention.MainController.variableValueResolve.call(this, record) ) + '"';
            return value;
        },

        regexNumericCondition :  /^(\d+|(\d+\-\d+)|[\d+,]+)$/,
        conditionValidateNumeric: function(value){
            return Ung.apps.intrusionprevention.MainController.regexNumericCondition.exec(value) ? true : 'Invalid numeric value'.t();
        },
}
});

Ext.define('Ung.apps.intrusionprevention.cmp.RuleGridController', {
    extend: 'Ung.cmp.GridController',
    alias: 'controller.unintrusionrulegrid',

    // !!! need to run on grid initializer
    processRuleSignature: function(record){
        var me = this,
            vm = me.getViewModel(),
            signatures = vm.get('signatures');
        console.log('processRule');
        console.log(record);
        console.log(signatures);
        // foreach signature:
        // if networks != recommended (or new flag to indicate signature change):
        //      Lookup rule in signatures.
        //      Perform deep copy
        //      set "rule generated signature" flag.
        //      Modify signature as appropriate but keep default flag.
        //      
        this.getView().up('apppanel').getController().signaturesChanged();
    },

    getConditions: function(){
        var conditions = {};
        this.getView().editorFields.forEach( function(field){
            if(field.xtype == 'ipsrulesconditionseditor'){
                conditions = field.conditions;
            }
        });
        return conditions;
    },

    getComparators: function(){
        var comparators = {};
        this.getView().editorFields.forEach( function(field){
            if(field.xtype == 'ipsrulesconditionseditor'){
                comparators = field.comparators;
            }
        });
        return comparators;
    },

    onDragDrop: function(){
        this.getView().up('apppanel').getController().signaturesChanged();
    },

    // deleteRecord: function(view, rowIndex, colIndex, item, e, record){
    //     this.processRule(record);
    //     this.callParent(arguments);
    // }
    //

});

Ext.define('Ung.apps.intrusionprevention.cmp.RulesRecordEditor', {
    extend: 'Ung.cmp.RecordEditor',
    xtype: 'ung.cmp.unintrusionrulesrecordeditor',

    controller: 'unintrusionrulesrecordeditorcontroller',

    doDestroy: function(){
        // var masterGrid = this.getController().masterGrid;
        this.callParent();
        // delete using rule id lookup.
        //masterGrid.up('apppanel').getController().signaturesChanged();
        // console.log(this);
        // console.log(this.getController());
        //masterGrid.getController().processRuleSignature(this.record);
    }

});

Ext.define('Ung.apps.intrusionprevention.cmp.RulesRecordEditorController', {
    extend: 'Ung.cmp.RecordEditorController',
    alias: 'controller.unintrusionrulesrecordeditorcontroller',

    onAfterRender: function (view) {
        this.masterGrid = view.up('grid');

        this.callParent([view]);

        var formpanel = view.down('form');
        formpanel.addDocked({
            xtype: 'toolbar',
            dock: 'bottom',
            items: [{
                xtype: 'tbtext',
                name: 'matchStatus',
                html: '',
            }]
        });
    }
});

Ext.define('Ung.apps.intrusionprevention.cmp.IpsRulesConditionsEditor', {
    extend: 'Ung.cmp.ConditionsEditor',
    alias: 'widget.ipsrulesconditionseditor',

    controller: 'ipsrulesconditionseditorcontroller',


});

Ext.define('Ung.apps.intrusionprevention.cmp.IpsRulesConditionsEditorController', {
    extend: 'Ung.cmp.ConditionsEditorController',
    alias: 'controller.ipsrulesconditionseditorcontroller',

    onAfterRender: function (component) {
        var me = this;

        me.callParent([component]);

        me.updateMatchStatus();
    },

    forceValidate: function(){
        this.callParent();

        this.updateMatchStatus();
    },

    updateMatchStatusCalculator: function(){
        var view = this.getView(),
            store = view.down('grid').getStore(),
            rule = new Ung.model.intrusionprevention.rule({
                action: 'default',
                enabled: 'true',
                conditions: {
                    list: Ext.Array.pluck(store.getRange(), 'data')
                }
            }),
            status = view.up('apppanel').getController().ruleSignatureMatches(rule),
            matchStatus = view.up('form').down('[name=matchStatus]');

        var affectedCount = 0;
        Object.keys(status).forEach(function(k){
            affectedCount += Object.keys(status[k]).length;
        });

        matchStatus.update('Affected signatures:'.t() + ' ' + affectedCount);
    },

    updateMatchStatusCalculatorTask: null,
    updateMatchStatus: function(){
        var me= this;

        if(me.updateMatchStatusCalculatorTask == null){
            me.updateMatchStatusCalculatorTask = new Ext.util.DelayedTask( Ext.bind(this.updateMatchStatusCalculator, me) );
        }
        me.updateMatchStatusCalculatorTask.delay( 500 );
    }
});

Ext.define('Ung.apps.intrusionprevention.cmp.SignaturesRecordEditor', {
    extend: 'Ung.cmp.RecordEditor',
    xtype: 'ung.cmp.unintrusionsignaturesrecordeditor',

    controller: 'unintrusionsignaturesrecordeditorcontroller',
});

Ext.define('Ung.apps.intrusionprevention.cmp.SignaturesRecordEditorController', {
    extend: 'Ung.cmp.RecordEditorController',
    alias: 'controller.unintrusionsignaturesrecordeditorcontroller',

    editorGidChange: function( me, newValue, oldValue, eOpts ){
        var v = this.getView(),
            vm = this.getViewModel();

        if( ! /^[0-9]+$/.test( newValue )){
            me.setValidation("Gid must be numeric".t());
            return false;
        }

        var record = vm.get('record');
        if(v.getController().validateId(v) == false){
            return false;
        }
        record.setOption('gid', newValue);
        record.setId();
        record.set('signature', record.build());

        me.setValidation(true);
    },

    editorSidChange: function( me, newValue, oldValue, eOpts ){
        var v = this.getView(),
            vm = this.getViewModel();

        if( ! /^[0-9]+$/.test( newValue )){
            me.setValidation("Sid must be numeric".t());
            return false;
        }

        var record = vm.get('record');
        if(v.getController().validateId(v) == false){
            return false;
        }

        record.setOption('sid', newValue);
        record.setId();
        record.set('signature', record.build());

        me.setValidation(true);
    },

    validateId: function(view){
        var vm = this.getViewModel(),
            gidComponent = view.down('[name=gid]'),
            gidValue = gidComponent.getValue(),
            sidComponent = view.down('[name=sid]'),
            sidValue = sidComponent.getValue(),
            match = false;

        var compareRecord = view.record;
        vm.get('signatures').each( function( record ) {
            if( record.get('_id') != compareRecord.get('_id') ){
                if( gidValue == record.get('gid') &&
                    sidValue == record.get('sid')){
                    match = true;
                }
            }
        }, this, true);

        gidComponent.setValidation( match ? "Gid/Sid already in use.".t() : true );
        sidComponent.setValidation( match ? "Gid/Sid already in use.".t() : true );
    },

    editorClasstypeChange: function( me, newValue, oldValue, eOpts ){
        var vm = this.getViewModel();
        if( newValue == null || Ung.apps.intrusionprevention.Main.classtypes.findExact('value', newValue) == null ){
            me.setValidation("Unknown classtype".t());
            return false;
        }
        var record = vm.get('record');
        record.set('classtype', newValue);
        record.set('signature', record.build());
        me.setValidation(true);
    },

    editorMsgChange: function( me, newValue, oldValue, eOpts ){
        var vm = this.getViewModel();
        if( /[";]/.test( newValue ) ){
            me.setValidation( 'Msg contains invalid characters.'.t() );
            return false;
        }
        var record = vm.get('record');
        var content = record.getOption('content');
        if((content == "matchme" && oldValue == "new signature") || ( content == oldValue)){
            record.setOption('content', newValue);
        }
        record.setOption('msg', newValue);
        record.set('signature', record.build());
        me.setValidation(true);
    },

    editorRecommendedActionChange: function( me, newValue, oldValue, eOpts ){
        var vm = this.getViewModel(),
            record = vm.get('record');
        if( newValue == null || Ung.apps.intrusionprevention.Main.signatureActions.findExact('value', newValue) == null ){
            me.setValidation("Unknown action".t());
            return false;
        }
        record.set('recommendedAction', newValue);
        record.set('signature', record.build());
        me.setValidation(true);
    },

    editorSignatureChange: function( me, newValue, oldValue, eOpts ){
        if(!Ung.model.intrusionprevention.signature.isValid(newValue)){
            me.setValidation( 'Signature is invalid.'.t() );
            return false;
        }
        var vm = this.getViewModel(),
            record = vm.get('record');
        var signature = new Ung.model.intrusionprevention.signature(newValue, record.get('category'), false);

        for( var key in signature.data){
            if(key == '_id'){
                continue;
            }
            record.set(key, signature.data[key]);
        }
        me.setValidation(true);
    }

});

Ext.define('Ung.apps.intrusionprevention.cmp.SignatureGridFilter', {
    extend: 'Ung.cmp.GridFilter',
    alias: 'widget.signatureungridfilter',

    controller: 'signatureungridfilter',

    viewModel: {
        data: {
            filterValueDisabled: true
        },
        stores: {
            fields: {
                fields: [{
                    name: 'value',
                },{
                    name: 'description'
                }],
                sorters: [{
                    property: 'value',
                    direction: 'ASC'
                }],
                data: '{fieldsData}'
            },
            comparators: {
                fields: [{
                    name: 'value',
                },{
                    name: 'description'
                }],
                sorters: [{
                    property: 'value',
                    direction: 'DESC'
                }],
                data: '{comparatorsData}'
            },
            values: {
                fields: [{
                    name: 'value',
                },{
                    name: 'description'
                }],
                sorters: [{
                    property: 'value',
                    direction: 'DESC'
                }],
                data: '{valuesData}'
            }
        }
    }

});

Ext.define('Ung.apps.intrusionprevention.cmp.SignatureGridFilterController', {
    extend: 'Ung.cmp.GridFilterController',
    alias: 'controller.signatureungridfilter',

    control: {
        'signatureungridfilter': {
            afterrender: 'afterrenderGetConditions'
        }
    },

    afterrenderGetConditions: function(){
        var me = this,
            v = me.getView(),
            vm = me.getViewModel();

        v.insert(3, {
            xtype: 'button',
            baseCls: 'x-toolbar-item x-btn-default-toolbar-small',
            margin: '2 4 0 0',
            text: 'Create Rule',
            iconCls: 'fa fa-plus-circle fa-lg',
            listeners: {
                click: 'createRuleFromFilter'
            },
            disabled: true,
            bind:{
                disabled: '{!matchesFound}'
            }
        });
        v.insert(2, {
            xtype: 'combo',
            name: 'filterValue',
            _neverDirty: true,
            listeners: {
                change: 'changeFilterSearch',
                buffer: 500
            },
            listConfig: {
                itemTpl: '<div data-qtip="{description}">{value}</div>'
            },
            queryMode: 'local',
            valueField: 'value',
            displayField: 'value',
            triggers: {
                clear: {
                    cls: 'x-form-clear-trigger',
                    hidden: true,
                    handler: function (field) {
                        field.setValue('');
                    }
                }
            },
            disabled: true,
            hidden: true,
            bind:{
                store: '{values}',
                value: '{searchValue}',
                disabled: '{filterValueDisabled}',
                hidden: '{filterValueDisabled}'
            }
        });
        v.insert(2,{
            xtype: 'combo',
            padding: '2 0 0 0',
            _neverDirty: true,
            listeners: {
                change: 'changeFilterComparator',
                buffer: 500
            },
            queryMode: 'local',
            valueField: 'value',
            displayField: 'description',
            bind:{
                store: '{comparators}',
                value: '{comparator}'
            }
        });
        v.insert(2,{
            xtype: 'combo',
            padding: '2 0 0 0',
            _neverDirty: true,
            labelWidth: 'auto',
            name: 'filterField',
            listeners: {
                change: 'changeField'
            },
            queryMode: 'local',
            valueField: 'value',
            displayField: 'description',
            bind:{
                store: '{fields}',
                value: '{field}'
            }
        });

        var filterConditions = [];
        Ext.Object.each(v.up('apppanel').down('[name=rules]').getController().getConditions(), function(k,v){
            filterConditions[k] = v;
            if(k == 'ACTION'){
                filterConditions['ACTIONR'] = Ext.clone(v);
                filterConditions['ACTIONR']['displayName'] = 'Rule Action'.t();
            }
        });
        vm.set('filterConditions', filterConditions);

        var fieldsData = [];
        for(var name in filterConditions){
            if(name == 'SYSTEM_MEMORY'){
                continue;
            }
            fieldsData.push({
                value: name,
                description: filterConditions[name]['displayName']
            });
        }
        vm.set('fieldsData', fieldsData);
        vm.set('field', 'MSG');
    },

    changeField: function(eleme, newValue){
        var me = this,
            view = me.getView(),
            vm = me.getViewModel(),
            conditionComparator =vm.get('filterConditions')[newValue].comparator,
            currentComparator = vm.get('comparator');

        view.up('apppanel').down('[name=rules]').getController().getComparators().forEach(function(comparator){
            if(comparator['name'] == conditionComparator){
                vm.set('comparatorsData', comparator.store);
                var found = false;
                comparator.store.forEach(function(sv){
                    if(currentComparator == sv[0]){
                        found = true;
                    }
                });
                if(!found){
                    vm.set('comparator', comparator.defaultValue);
                }
            }
        });

        vm.set('searchValue', '');
        if(vm.get('filterConditions')[newValue].store || vm.get('filterConditions')[newValue].values){
            if(!vm.get('filterConditions')[newValue].store){
                vm.get('filterConditions')[newValue].store = Ext.create('Ext.data.ArrayStore', {
                    fields: [ 'value', 'description' ],
                    data: vm.get('filterConditions')[newValue].values
                });
            }
            vm.set('searchValue', vm.get('filterConditions')[newValue].store.first().get('value'));
            vm.set('valuesData', Ext.Array.pluck(vm.get('filterConditions')[newValue].store.getRange(), 'data'));
            vm.set('filterValueDisabled', false);
            vm.set('filterSearchDisabled', true);
        }else{
            vm.set('filterValueDisabled', true);
            vm.set('filterSearchDisabled', false);
        }
    },

    changeFilterComparator:function(field){
        var me = this,
            value = field.getValue(),
            grid = this.getView().up('grid') ? this.getView().up('grid') : this.getView().up('panel').down('grid'),
            store = grid.getStore(),
            routeFilter = field.up('panel').routeFilter;

        /**
         * Remove only the filters added through filter data box
         * leave alone the grid filters from columns or routes
         */
        store.getFilters().each(function (filter) {
            if (filter.isGridFilter || filter.source === 'route') {
                return;
            }
            // If filter string is not empty, allow event. Prevent if empty.
            store.removeFilter(filter, value != '' ? true : false);
        });

        // add route filter
        if (routeFilter) {
            store.getFilters().add(routeFilter);
        }

        this.createFilter(grid, store, routeFilter);
    },

    createFilter: function(grid, store, routeFilter){
        var me = this,
            vm = me.getViewModel();

        var searchValue = vm.get('searchValue');

        if(searchValue == ''){
            return;
        }

        var rule = new Ung.model.intrusionprevention.rule({
            action: 'default',
            enabled: 'true',
            conditions: {
                list: [{
                    type: vm.get('field'),
                    comparator: vm.get('comparator'),
                    value: searchValue
                }]
            }
        });

        var status = me.getView().up('apppanel').getController().ruleSignatureMatches(rule, vm.get('filterConditions'));
        store.getFilters().add(function (record) {
            for(var action in status){
                if(status[action][record.data['id']]){
                    return true;
                }
            }
            return false;
        });
    },

    createRuleFromFilter: function(button){
        var me = this,
            vm = me.getViewModel(),
            field = vm.get('field'),
            comparator = vm.get('comparator'),
            value = vm.get('searchValue');

        var newRule = {
            'javaClass': 'com.untangle.app.intrusion_prevention.IntrusionPreventionRule',
            'enabled': true,
            'id': -1,
            'description': '"' + vm.get('fields').findRecord( 'value', field, 0, false, false, true).get('description') + '" ' + vm.get('comparators').findRecord('value', comparator, 0, false, false, true).get('description') + ' "' + value + '"',
            'conditions': {
                'javaClass': "java.util.LinkedList",
                'list': [{
                    'javaClass': 'com.untangle.app.intrusion_prevention.IntrusionPreventionRuleCondition',
                    'type': field,
                    'comparator': comparator,
                    'value': value
                }]
            },
            'action': 'log'
        };
        var rulesTab = button.up('tabpanel').setActiveTab('rules');

        var dt = new Ext.util.DelayedTask( Ext.bind(function(){
            rulesTab.getController().addRecord(null, null, newRule);
        }, me));
        dt.delay(100);
    }
});


Ext.define('Ung.apps.intrusionprevention.cmp.SignatureGridController', {
    extend: 'Ung.cmp.GridController',
    alias: 'controller.unintrusionsignaturesgrid',

    updateSearchStatusBar: function(){
        // var v = this.getView(),
        //     vm = this.getViewModel(),
        //     appvm = v.up('apppanel').getController().getViewModel(); 
        // // var searchStatus = v.down("[name=searchStatus]");
        // // var hasLogOrBlockFilter = ( v.down("[name=searchLog]").getValue() === true ) || ( v.down("[name=searchBlock]").getValue() === true );
        // // var hasFilter = hasLogOrBlockFilter || ( v.down("[name=searchFilter]").getValue().length >= 2 );
        // var hasFilter = v.down("[name=searchFilter]").getValue().length > 1;
        // var statusText = "", logOrBlockText = "", totalEnabled = 0;
        // // if(!hasLogOrBlockFilter) {
        // //     v.getStore().each(function( record ){
        // //         if( ( record.get('log')) || ( record.get('block')) ) {
        // //             totalEnabled++;
        // //         }
        // //     });
        // //     logOrBlockText = Ext.String.format( '{0} logging or blocking'.t(), totalEnabled);
        // // }
        // if(hasFilter) {
        //     statusText = Ext.String.format( '{0} matching signature(s) found'.t(), v.getStore().getCount() );
        //     // if(!hasLogOrBlockFilter) {
        //     //     statusText += ', ' + logOrBlockText;
        //     // }
        // } else {
        //     statusText = Ext.String.format( '{0} signatures available'.t(), v.getStore().getCount());
        //     // statusText = Ext.String.format( 'Signatures available: {signatureStatusTotal}, Log: {1}, Block: {2}, Disabled: {3}'.t(), appvm.get('signatureStatusTotal'), appvm.get('signatureStatusLog'), appvm.get('signatureStatusBlock'), appvm.get('signatureStatusDisable'));
        // }
        // // searchStatus.update( statusText );
        // vm.set('searchStatus', statusText);
    },

    exportData: function(){
        var grid = this.getView(),
            gridName = (grid.name !== null) ? grid.name : grid.recordJavaClass,
            vm = grid.up('app-intrusion-prevention').getController().getViewModel();

        Ext.MessageBox.wait('Exporting Settings...'.t(), 'Please wait'.t());

        var downloadForm = document.getElementById('downloadForm');
        downloadForm["type"].value = "IntrusionPreventionSettings";
        downloadForm["arg1"].value = "export";
        downloadForm["arg2"].value = vm.get('instance.id');
        downloadForm["arg3"].value = gridName.trim().replace(/ /g, '_');
        downloadForm["arg4"].value = Ext.encode({signatures: grid.up('app-intrusion-prevention').getController().getChangedDataRecords('signatures')});
        downloadForm.submit();

        Ext.MessageBox.hide();
    },

    importHandler: function(importMode, newData){
        this.callParent(arguments);
        // var store = this.getView().getStore();

        // store.each( function(record){
        //     me.updateSignature(record, 'log', record.get('log') );
        //     me.updateSignature(record, 'block', record.get('block') );

        // });
    },

    signaturesReconfigure: function( me , store , columns , oldStore , oldColumns , eOpts ){
        me.getController().updateSearchStatusBar();
    },

    updateSignature: function( record, updatedKey, updatedValue){
        var i, regex;
        var signatureValue = record.get('signature');
        var updatedSubkey = null;

        if(!Ung.model.intrusionprevention.signature.isValid(signatureValue)){
            return;
        }

        // Action replacement
        record.set(updatedKey, updatedValue);
        // if( updatedKey != 'log' && updatedKey != 'block' ){
        if( updatedKey != 'action'){
            record.setOption(updatedKey, updatedValue);
        }
        record.set('signature', record.build());
    }
});

Ext.define('Ung.apps.intrusionprevention.cmp.VariablesRecordEditor', {
    extend: 'Ung.cmp.RecordEditor',
    xtype: 'ung.cmp.unintrusionvariablesrecordeditor',

    controller: 'unintrusionvariablesrecordeditorcontroller',
});

Ext.define('Ung.apps.intrusionprevention.cmp.VariablesRecordEditorController', {
    extend: 'Ung.cmp.RecordEditorController',
    alias: 'controller.unintrusionvariablesrecordeditorcontroller',

    editorVariableChange: function( me, newValue, oldValue, eOpts ){
        var v = this.getView();
        var vm = this.getViewModel();

        var match = false;
        vm.get('variables').each( function( storeRecord ) {
            if( ( storeRecord !== v.record ) && ( newValue == storeRecord.get('name') ) ){
                match = true;
            }
        });
        if(match){
            me.setValidation("Variable name already in use.".t());
            return false;
        }
        me.setValidation(true);

        if(!Ext.Object.isEmpty(v.up('app-intrusion-prevention').getController().isVariableUsed(newValue))){
            me.setReadOnly(true);
            me.up("").down("[name=activeVariable]").setVisible(true);
        }else{
            me.setReadOnly(false);
            me.up("").down("[name=activeVariable]").setVisible(false);
        }
    }

});

Ext.define('Ung.apps.intrusionprevention.cmp.VariablesGridController', {
    extend: 'Ung.cmp.GridController',

    alias: 'controller.unintrusionvariablesgrid',

    editorWin: function (record) {
        this.dialog = this.getView().add({
            xtype: 'ung.cmp.unintrusionvariablesrecordeditor',
            record: record
        });
        this.dialog.show();
    },

    exportData: function(){
        var grid = this.getView(),
            gridName = (grid.name !== null) ? grid.name : grid.recordJavaClass,
            vm = grid.up('app-intrusion-prevention').getController().getViewModel();

        Ext.MessageBox.wait('Exporting Settings...'.t(), 'Please wait'.t());

        var downloadForm = document.getElementById('downloadForm');
        downloadForm["type"].value = "IntrusionPreventionSettings";
        downloadForm["arg1"].value = "export";
        downloadForm["arg2"].value = vm.get('instance.id');
        downloadForm["arg3"].value = gridName.trim().replace(/ /g, '_');
        downloadForm["arg4"].value = Ext.encode({variables: grid.up('app-intrusion-prevention').getController().getChangedDataRecords('variables')});
        downloadForm.submit();

        Ext.MessageBox.hide();
    },

    deleteRecord: function (view, rowIndex, colIndex, item, e, record) {
        var isUsed = this.getView().up('app-intrusion-prevention').getController().isVariableUsed( record.get('name') );
        if( !Ext.Object.isEmpty(isUsed)){
            var messages = [];
            if('rules' in isUsed){
                isUsed['rules'].forEach( function(rule){
                    messages.push(Ext.String.format( '{0}Rule:{1} {0}{1}{2}'.t(), '<b>', '</b>', rule.get('description') ));
                });
            }
            if('signatures' in isUsed){
                var maxShow = 5;
                var remaining = isUsed['signatures'].length - maxShow; 
                Ext.Array.each(isUsed['signatures'], function(signature){
                    messages.push(Ext.String.format( '{0}Signature:{1} {0}{1}{2}'.t(), '<b>', '</b>', signature.build()));
                    maxShow--;
                    if(!maxShow){
                        return false;
                    }
                });
                if(remaining){
                    messages.push(Ext.String.format('{0} more affected signatures...'.t(), remaining));
                }
            }
            Ext.MessageBox.alert( Ext.String.format("Cannot Delete Variable {0}".t(), record.get('name')), "Variable in use:".t() + "<ul>" + '<li>'+messages.join("<li>") + "</ul>");
        }else{
            if (record.get('markedForNew')) {
                record.drop();
            } else {
                record.set('markedForDelete', true);
            }
        }
    }
});

Ext.define('Ung.model.intrusionprevention.signature',{
    extend: 'Ext.data.Model',
    fields:[{
        name: 'reserved',
        type: 'boolean'
    },{
        name: 'default',
        type: 'boolean'
    },{
        name: 'protocol',
        type: 'string'
    },{
        name: 'lnet',
        type: 'string'
    },{
        name: 'lport',
        type: 'string'
    },{
        name: 'direction',
        type: 'string'
    },{
        name: 'rnet',
        type: 'string'
    },{
        name: 'rport',
        type: 'string'
    },{
        name: 'options',
    },{
        name: 'category',
        type: 'string',
        defaultValue: 'misc'
    },{
        name: 'classtype',
        type: 'string'
    },{
        name: 'recommendedAction',
        type: 'string'
    },{
        name: 'matchingRules'
    },{
        name: 'sid',
        type: 'string'
    },{
        name: 'gid',
        type: 'string'
    },{
        name: 'msg',
        type: 'string'
    },{
        name: 'signature',
        type: 'string'
    },{
        name: 'defaultSignature',
        type: 'string'
    },{
        name: 'networkChanged',
        type: 'boolean'
    }],

    optionsMapIndexes: {},

    constructor: function(signature, category, reserved, session){
        var me = this;
        var data = signature;
        var valid = false;
        if(typeof signature == "string"){
            data = {
                reserved: reserved,
                default: reserved ? true : false,
                protocol: '',
                lnet: '',
                lport: '',
                direction: '',
                rnet: '',
                rport: '',
                options: [],
                category: category,
                recommendedAction: 'log',
                matchingRules: [],
                sid: '1',
                gid: '1',
                networkChanged: false
            };

            valid = Ung.model.intrusionprevention.signature.signatureRegex.test(signature);
            if(valid){
                var matches = Ung.model.intrusionprevention.signature.signatureRegex.exec(signature);

                data['signature'] = signature;
                alert = matches[2].toLowerCase();
                if(matches[3] != ""){
                    data['protocol'] = matches[4].toLowerCase();
                    data['lnet'] = matches[5];
                    data['lport'] = matches[6];
                    data['direction'] = matches[7];
                    data['rnet'] = matches[8];
                    data['rport'] = matches[9];
                }
                data['options'] = matches[10].trim().split(';');
                data['options'].forEach( function(option, index, options){
                    options[index] = option.trim();
                });

                if(matches[1] == '#'){
                    data['recommendedAction'] = 'disable';
                }else{
                    if(matches[2] == 'alert'){
                        data['recommendedAction'] = 'log';
                    }else if(matches[2] == 'reject'){
                        data['recommendedAction'] = 'block';
                    }
                }
            }
        }

        this.callParent([data], session);

        if(typeof signature == "string" && valid){
            me.optionsMapIndexes = {};
            var key = null;
            var foundOptions = [];
            me.data['options'].forEach(function(option, index){
                key = option.substr(0, option.indexOf(':')).trim();
                Ung.model.intrusionprevention.signature.optionsMap.forEach(function(mappedOption){
                    if(mappedOption.name == key){
                        foundOptions.push(key);
                        me.optionsMapIndexes[mappedOption] = index;
                        var value = me.massageGetOptionValue(option.substr(option.indexOf(':') + 1));
                        if(option.defaultValue){
                            value = value ? value : option.defaultValue;
                        }
                        me.data[mappedOption.name] = value;
                    }
                });
            });
            Ung.model.intrusionprevention.signature.optionsMap.forEach(function(mappedOption){
                if(foundOptions.indexOf(mappedOption.name) == -1){
                    me.data[mappedOption.name] = mappedOption.defaultValue;
                }
            });
            this.setId();
        }
    },

    setId: function(){
        this.set('id', this.data['sid'] + '_' + this.data['gid']);
    },

    set: function(fieldName, newValue, options) {
        var result = this.callParent(arguments);
        var me = this;

        if(fieldName != 'signature'){
            // Write back to options
            Ung.model.intrusionprevention.signature.optionsMap.forEach(function(option){
                if(fieldName == option.name){
                    me.setOption(option.name, newValue);
                }
            });
        }else if(fieldName == 'lnet' || fieldName == 'rnet'){
            me.set('networkChanged', true);
        }

        return result;
    },

    massageGetOptionValue: function(value){
        value = value.trim();
        if(value[0] == '"' && value[value.length -1] == '"'){
            value = value.substring(1,value.length - 1);
        }
        return value;
    },

    getOption: function(key){
        var me = this,
            value = null;

        var options = this.get('options');

        var kv = null;
        if(me.optionsMapIndexes && key in me.optionsMapIndexes){
            kv = options[me.optionsMapIndexes[key]].split(':');
            value = me.massageGetOptionValue(kv[1]);
        }else{
            options.forEach( function( option, index, optionsMetadata){
                kv = option.split(':');
                if(kv[0].trim() == key){
                    if(me.optionsMapIndexes && !(key in me.optionsMapIndexes)){
                        me.optionsMapIndexes[key] = index;
                    }
                    kv[1] = me.massageGetOptionValue(kv[1]);
                    if(value != null){
                        // Found second key of same name; create array for return.
                        value = [value];
                    }else if(Array.isArray(value)){
                        value.push(kv[1]);
                    }else{
                        value = kv[1];
                    }
                }
            });
        }
        return value;
    },

    setOption: function( key, value){
        var options = this.get('options');
        var found = false;
        var kv;
        options.forEach( function( option, index, optionsMetadata){
            kv = option.split(':');
            if(kv[0].trim() == key){
                kv[1] = kv[1].trim();
                var preserveQuotes = false;
                if(kv[1][0] == '"' && kv[1][kv[1].length -1] == '"'){
                    preserveQuotes = true;
                }
                kv[1] = (preserveQuotes ? '"' : '' ) + value + (preserveQuotes ? '"' : '' );
                found = true;
                options[index] = kv.join(':');
            }
        });
        if(!found){
            options.push(key + ':' + value);
        }
        this.set('options', options);
    },

    getRecord: function(){
        if(this.get('reserved') == true){
            return {
                sid: this.get('sid'),
                gid: this.get('gid'),
                recommendedAction: this.get('recommendedAction')
            };
        }else{
            return this.build();
        }
    },

    // option to build as recommmended and with rule
    build: function(){
        var signatureAction = 'alert';
        var action = this.get('recommendedAction');
        if( action == 'block' ){
            signatureAction = 'reject';
        }else if(action == 'disable'){
            signatureAction = '#' + signatureAction;
        }

        // Modify lnet/rnet if whitelist rule attached
        var lnet = this.get('lnet');
        var rnet = this.get('rnet');
        this.get('matchingRules').forEach( function(rule){
            if(rule.get('action') == 'whitelist'){
                lnet = rule.addSignatureNetwork("source", lnet, true);
                rnet = rule.addSignatureNetwork("destination", rnet, true);
            }
        });

        return signatureAction + " " +
            (this.get('protocol') ? this.get('protocol') + ' ' : '') +
            (this.get('lnet') ? lnet + ' ' : '') +
            (this.get('lport') ? this.get('lport') + ' ' : '') +
            (this.get('direction') ? this.get('direction') + ' ' : '') +
            (this.get('rnet') ? rnet + ' ' : '') +
            (this.get('rport') ? this.get('rport') + ' ' : '') +
            "(" + this.get('options').join(';') + ")";
    },

    copy: function(){
        var newSignature = new Ung.model.intrusionprevention.signature(this.build(), this.get('category'), this.get('reserved'));
        Ext.Object.merge(newSignature.data, this.data);

        var gid = parseInt(newSignature.get('gid'), 10);
        if( gid < Ung.model.intrusionprevention.signature.customGid){
            gid = Ung.model.intrusionprevention.signature.customGid;
        }else{
            gid += 1;
        }
        newSignature.set('gid', gid.toString());
        newSignature.setId();

        // Somethng about id?

        return newSignature;
    },
    
    statics:{
        customGid: 2400,
        signatureRegex: /^([#\s]+|)(alert|log|pass|activate|dynamic|drop|reject|sdrop)\s+(([^\s]+)\s+([^\s]+)\s+([^\s]+)\s+(\-\>|<>)\s+([^\s]+)\s+([^\s]+)\s+|)\((.+)\)/,
        optionsMap: [{
            name: 'gid',
            defaultValue: '1'
        },{
            name: 'sid'
        },{
            name: 'classtype',
            defaultValue: 'general'
        },{
            name: 'msg'
        }],
        rebuildSignatureKeys:[
            'gid',
            'sid',
            'classtype',
            'category',
            'msg',
            'recommendedAction'
        ],
        isValid: function(signature){
            return Ung.model.intrusionprevention.signature.signatureRegex.test(signature);
        }
    }
});

Ext.define('Ung.model.intrusionprevention.rule',{
    extend: 'Ext.data.Model',
    fields:[{
        name: 'javaClass',
        type: 'string',
        defaultValue: 'com.untangle.app.intrusion_prevention.IntrusionPreventionRule'
    },{
        name: 'action',
        type: 'string'
    },{
        name: 'conditions'
    },{
        name: 'description',
        type: 'string'
    },{
        name: 'enabled',
        type: 'boolean'
    },{
        name: 'id',
        type: 'string'
    },{
        name: 'sourceNetworks',
        type: 'string',
        defaultValue: 'recommended'
    },{
        name: 'destinationNetworks',
        type: 'string',
        defaultValue: 'recommended'
    }],

    hasCustomNetworks: function(){
        // If true, this rule creates a signature copy.  Otherwise it does not.
        var me = this;
        return ( me.get('sourceNetworks') != 'recommended' ) || ( me.get('destinationNetworks') != 'recommended' );
    },

    matchSignature: function(signature, editorConditions, vm){
        var me = this;
        if(!me.get('enabled')){
            return false;
        }
        var allMatch = true;
        me.get('conditions').list.forEach(function(condition){
            var match = true;
            var editorCondition = editorConditions[condition.type];
            if(!editorCondition){
                console.log('Unable to find condition: ' + condition.type);
            }

            var signatureValue = null;
            var conditionKey = condition.type.toLowerCase();
            switch(conditionKey){
                case 'system_memory':
                    signatureValue = vm.get('system_memory');
                    break;
                case 'signature':
                    signatureValue = signature.data['signature'];
                    break;
                case 'classtype':
                    signatureValue = signature.data['classtype'];
                    break;
                case 'msg':
                    signatureValue = signature.data['msg'];
                    break;
                case 'src_addr':
                    signatureValue = signature.data['lnet'];
                    break;
                case 'src_port':
                    signatureValue = signature.data['lport'];
                    break;
                case 'dst_addr':
                    signatureValue = signature.data['rnet'];
                    break;
                case 'dst_port':
                    signatureValue = signature.data['rport'];
                    break;
                case 'custom':
                    signatureValue = signature.data['reserved'] ? "false" : "true";
                    break;
                case 'action':
                    signatureValue = signature.data['recommendedAction'];
                    break;
                default:
                    signatureValue = signature.data[conditionKey];
            }

            if(typeof(signatureValue) == 'string'){
                signatureValue = signatureValue.toLowerCase();
            }

            switch(editorCondition.comparator){
                case 'numeric':
                    match = me.matchesNumeric(parseInt(signatureValue, 10), condition.comparator, condition.value);
                    break;
                case 'boolean':
                    var conditionValue = condition.value;
                    if(typeof(conditionValue) != 'object'){
                        if( typeof(conditionValue) == 'string' ){
                            conditionValue = conditionValue.toLowerCase().split(',');
                        }else{
                            conditionValue = ["true"];
                        }
                    }
                    match = me.matchesIn(signatureValue, condition.comparator, conditionValue);
                    break;
                case 'text':
                    match = me.matchesText(signatureValue, condition.comparator, condition.value.toLowerCase());
                    break;
                case 'network':
                    match = me.matchesNetwork(signatureValue.toLowerCase(), condition.comparator, condition.value.toLowerCase());
                    break;
                case 'port':
                    match = me.matchesPort(signatureValue.toLowerCase(), condition.comparator, condition.value.toLowerCase());
                    break;
                default:
                    // !!! throw exception
                    console.log('unknown comparator:' + editorCondition.comparator);
                    match = false;
            }

            if(!match){
                allMatch = false;
            }
        });

        return allMatch;
    },

    matchesNumeric: function(signatureValue, comparator, conditionValue){
        var me = this,
            conditionValues;
        if(conditionValue.indexOf(',') != -1){
            conditionValues = conditionValue.split(',');
            conditionValues.forEach( function(number, index){
                conditionValues[index] = parseInt(number, 10);
            });
            return me.matchesIn(signatureValue, comparator, conditionValues);
        }else if(conditionValue.indexOf('-') != -1){
            conditionValues = conditionValue.split('-');
            return me.matchesIn(signatureValue, comparator, conditionValues[0].trim(), conditionValues[1].trim());
        }
        conditionValue = parseInt(conditionValue, 10);
        switch(comparator){
            case "=":
                return signatureValue == conditionValue;
            case "!=":
                return signatureValue != conditionValue;
            case "<=":
                return signatureValue <= conditionValue;
            case "<":
                return signatureValue < conditionValue;
            case ">":
                return signatureValue > conditionValue;
            case ">=":
                return signatureValue >= conditionValue;
            case "substr":
                return signatureValue.toString().indexOf(conditionValue.toString()) != -1;
            case "!substr":
                return signatureValue.toString().indexOf(conditionValue.toString()) == -1;
        }

        return false;
    },

    matchesIn: function(signatureValue, comparator, conditionStartValue, conditionStopValue){
        var isIn;
        if(conditionStopValue){
            isIn = (signatureValue >= parseInt(conditionStartValue, 10) && signatureValue <= parseInt(conditionStopValue, 10));
        }else{
            isIn = Ext.Array.contains(conditionStartValue, signatureValue);
        }

        if(comparator == "="){
            return isIn;
        }else if(comparator == "!="){
            return !isIn;
        }

        return false;
    },

    matchesText: function(signatureValue, comparator, conditionValue){
        switch(comparator){
            case "=":
                return signatureValue == conditionValue;
            case "!=":
                return signatureValue != conditionValue;
            case "substr":
                return signatureValue.indexOf(conditionValue) != -1;
            case "!substr":
                return signatureValue.indexOf(conditionValue) == -1;
        }

        return false;
    },

    ipv4NetworkToLong: function(ip, prefix){
        if(prefix == 0){
            return 0;
        }
        var network = 0;
        ip.split('.').forEach( function(octet){
            network <<= 8;
            network += parseInt(octet, 10);
        });
        return ( (network >>> 0) & ( ( ~0 << (32 - prefix)) >>> 0) );
    },

    /**
     * Match network as follows:
     *
     * = or !=              Exact string match or signature contains exact network match.
     * substr or !substr    Text substring match.
     *
     * If signatureValue is a list, any match of the list is considered a valid match.
     * For example, if the list contains a lit of IP addresses like [1.2.3.4, 2.3.4.5], a conditionValue
     * of 1.2.3.0/24 or 2.0.0.0/8 would match even though it's not matching the entire list.
     *
     * @param  {[type]} signatureValue Either a single value or a list if it is inside square brackets.
     * @param  {[type]} comparator  =, !=, substr, ~substr
     * @param  {[type]} conditionValue Value to check.
     * @return {[type]}             true if match, false if not.
     */
    matchesNetwork: function(signatureValue, comparator, conditionValue){
        var equalComparator = comparator.substring(comparator.length-1) == '=';

        var matches;
        var signatureValues = [];
        if(signatureValue[0] == '['){
            signatureValues = signatureValue.substring(1,signatureValue.length-1).split(/\s*,\s*/);
        }else{
            signatureValues.push(signatureValue);
        }

        var targetPrefix = 32;
        if( equalComparator &&
            Ung.model.intrusionprevention.rule.ipv4NetworkRegex.test(conditionValue)){
            matches = Ung.model.intrusionprevention.rule.ipv4NetworkRegex.exec(conditionValue);
            targetPrefix = matches[4] ? parseInt(matches[4], 10) : 32;
            conditionValue = this.ipv4NetworkToLong(matches[1], targetPrefix);
        }

        var record = Ext.Array.findBy(signatureValues, Ext.bind(function(value){
            var matchValue = value;
            if(equalComparator){
                if(Ung.model.intrusionprevention.rule.ipv4NetworkRegex.test(value)){
                    matches = Ung.model.intrusionprevention.rule.ipv4NetworkRegex.exec(value);
                    matchValue = this.ipv4NetworkToLong(matches[1], targetPrefix);
                }
                if(matchValue == conditionValue){
                    return true;
                }
            }else{
                if(value.indexOf(conditionValue) != -1 ){
                    return true;
                }
            }
        }, this) );

        switch(comparator){
            case "=":
                return record != null;
            case "!=":
                return record == null;
            case "substr":
                return record != null;
            case "!substr":
                return record == null;
        }

        return false;
    },

    matchesPort: function(signatureValue, comparator, conditionValue){
        var equalComparator = comparator.substring(comparator.length-1) == '=';

        var signatureValues = [];
        if(signatureValue[0] == '['){
            signatureValues = signatureValue.substring(1,signatureValue.length-1).split(/\s*,\s*/);
        }else{
            signatureValues.push(signatureValue);
        }

        var record = Ext.Array.findBy(signatureValues, Ext.bind(function(value){
            var matchValue = value;
            if(equalComparator){
                if(matchValue == conditionValue){
                    return true;
                }
            }else{
                if(value.indexOf(conditionValue) != -1 ){
                    return true;
                }
            }
        }, this) );

        switch(comparator){
            case "=":
                return record != null;
            case "!=":
                return record == null;
            case "substr":
                return record != null;
            case "!substr":
                return record == null;
        }

        return false;
    },

    addSignatureNetwork: function(sourceType, network, negate){
        if(this.get(sourceType+"Networks") != "recommended"){
            if(network.indexOf('[') > -1){
                network = network.substr(network.indexOf('[') + 1, network.lastIndexOf(']') -1);
            }

            var addNetwork = this.get(sourceType+"Networks");
            if(negate == true){
                addNetwork = '!' + addNetwork;
            }

            if(network == 'any'){
                network = addNetwork;
            }else{
                network = Ext.String.format( '[{0},{1}]', network, addNetwork);
            }
        }
        return network;
    },


    statics:{
        ipv4NetworkRegex: /((\d{1,3}\.){3,3}\d{1,3})(\/(\d{1,2}|)|)/
    }
});


Ext.define ('Ung.apps.intrusionprevention.model.Condition', {
    extend: 'Ext.data.Model' ,
    fields: [
        { name: 'javaClass', type: 'string', defaultValue: ''},
        { name: 'type', type: 'string' },
        { name: 'comparator', type: 'string', defaultValue: '='},
        { name: 'value', type: 'auto', defaultValue: '' }
    ],
    proxy: {
        autoLoad: true,
        type: 'memory',
        reader: {
            type: 'json'
        }
    }
});
