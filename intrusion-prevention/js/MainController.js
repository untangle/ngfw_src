Ext.define('Ung.apps.intrusionprevention.MainController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.app-intrusion-prevention',

    control: {
        '#': {
            afterrender: 'getSettings',
        },
    },

    getSettings: function () {
        var me = this, v = this.getView(), vm = this.getViewModel();

        v.setLoading(true);

        var t0 = performance.now();
        Ext.Deferred.sequence([
            Rpc.asyncPromise(v.appManager, 'getLastUpdateCheck'),
            Rpc.asyncPromise(v.appManager, 'getLastUpdate'),
            Rpc.directPromise('rpc.companyName'),
            Rpc.asyncPromise('rpc.metricManager.getMemTotal'),
            function(){ return Ext.Ajax.request({
                url: "/admin/download",
                method: 'POST',
                params: {
                    type: "IntrusionPreventionSettings",
                    arg1: "load",
                    arg2: vm.get('instance.id')
                },
                timeout: 600000});
            },function(){ return Ext.Ajax.request({
                url: "/admin/download",
                method: 'POST',
                params: {
                    type: "IntrusionPreventionSettings",
                    arg1: "signatures",
                    arg2: vm.get('instance.id')
                },
                timeout: 600000});
        }]).then(function(result){
            if(Util.isDestroyed(me, vm)){
                return;
            }

            var t1 = performance.now();
            // console.log(t1-t0);

            var settings = null;
            try{
                settings = Ext.decode( result[4].responseText);
            }catch(error){
                v.setLoading(false);
                Util.handleException({
                    message: 'Intrusion Prevention settings file is corrupt.'.t()
                });
                return;
            }
            vm.set({
                lastUpdateCheck: (result[0] !== null && result[0].time !== 0 ) ? Renderer.timestamp(result[0]) : "Never".t(),
                lastUpdate: (result[1] !== null && result[1].time !== 0 ) ? Renderer.timestamp(result[1]) : "Never".t(),
                companyName: result[2],
                system_memory: result[3],
                settings: settings,
                profileStoreLoad: true,
                signaturesStoreLoad: true,
                variablesStoreLoad: true
            });
            me.buildSignatures( result[5], settings);
            vm.set('panel.saveDisabled', false);
            v.setLoading(false);

        }, function (ex) {
            if(!Util.isDestroyed(me, v )){
                vm.set('panel.saveDisabled', true);
                v.setLoading(false);
            }
            Util.handleException(ex);
        });
    },

    buildSignatures: function(reserved, settinns){
        var me = this, v = this.getView(), vm = this.getViewModel();

        var t0 = performance.now();
        var t1 = performance.now();
        var t2 = performance.now();

        var filenameRegex = new RegExp("^# filename: (emerging\-|)(.+)\.rules$");
        var filenameStrips = ['emerging-'];
        var matches;
        var category;
        var signatures = [];
        reserved.responseText.split("\n").forEach(function(line){
            // if(category == 'attack_response'){
            //     return false;
            // }
            line = line.trim();
           if(line){
            // if(line && line != "#" && !line.startsWith("# ") && !line.startsWith("#**") ){
                if( filenameRegex.test( line ) ){
                    var matches = line.match(filenameRegex);
                    if( matches ){
                        category = matches[2];
                        // console.log(category);
                    }
                }else if( Ung.model.intrusionprevention.signature.isValid( line ) ){
                    signatures.push(new Ung.model.intrusionprevention.signature(line, category, true));
                // }else{
                //     console.log("invalid signature:" + line);
                }
            }
        });
        // console.log(signatures);
        t1 = performance.now();
        // console.log(t1-t0);

        // Process custom signatures
        vm.get('settings.signatures.list').forEach(function(settingsSignature){
            if(typeof(settingsSignature) == 'object'){
                signatures.forEach(function( signature ){
                    if(signature.get('gid') == settingsSignature['gid'] &&
                       signature.get('sid') == settingsSignature['sid']){
                        signature.set( 'log', settingsSignature['log']);
                        signature.set( 'block', settingsSignature['block']);
                        signature.set( 'default', false);
                    }
                });
            }else{
                signatures.push(new Ung.model.intrusionprevention.signature(settingsSignature, null, false));
            }
        });
        vm.set({
            signaturesList: signatures
        });
    },

    // getChangedDataRecords: function(target){
    //     var v = this.getView();
    //     var changed = {};
    //     v.query('app-intrusion-prevention-' + target).forEach(function(grid){
    //         var store = grid.getStore();
    //         store.getModifiedRecords().forEach( function(record){
    //             console.log(record.get('_id'));
    //             var data = {
    //                 op: 'modified',
    //                 recData: record.getRecord ? record.getRecord() : record.data
    //             };
    //             if(record.get('markedForDelete')){
    //                 data.op = 'deleted';
    //             }else if(record.get('markedForNew')){
    //                 data.op = 'added';
    //             }
    //             // console.log(record);
    //             // changed[record.get('_id')] = record.getRecord ? record.getRecord() : data;
    //             changed[record.get('_id')] = data;
    //         });
    //         store.commitChanges();
    //     });

    //     return changed;
    // },

    // getChangedData: function(){
    //     var me = this, vm = this.getViewModel();

    //     var settings = vm.get('settings');
    //     var changedDataSet = {};
    //     var keys = Object.keys(settings);
    //     for( var i = 0; i < keys.length; i++){
    //         if( ( keys[i] == "signatures" ) ||
    //             ( keys[i] == "variables" ) ||
    //             ( keys[i] == "rules" ) ||
    //             ( keys[i] == "profileId" ) ||
    //             ( keys[i] == "activeGroups") ){
    //             continue;
    //         }
    //         changedDataSet[keys[i]] = settings[keys[i]];
    //     }

    //     changedDataSet.rules = me.getChangedDataRecords('rules');
    //     changedDataSet.signatures = me.getChangedDataRecords('signatures');
    //     changedDataSet.variables = me.getChangedDataRecords('variables');

    //     return changedDataSet;
    // },

    setSettings: function (additionalChanged) {
        var me = this, v = this.getView(), vm = this.getViewModel();

        if (!Util.validateForms(v)) {
            return;
        }

        v.setLoading(true);

        v.query('ungrid').forEach(function (grid) {

            // path for listProperty=grid.bind.owner.config.stores.variables.data
            var settingsProperty = grid.listProperty;
            if(grid.getBind().store){
                var bindName = grid.getBind().store.stub.name;
                settingsProperty = grid.getBind().store.owner.config.stores[bindName].data;
                settingsProperty = settingsProperty.substring(1,settingsProperty.length - 1);
            }
            // console.log('settingsProperty=' + settingsProperty);

            if(settingsProperty){
                var store = grid.getStore();
                if(settingsProperty == 'signaturesList'){
                    var signatures = [];
                    store.each(function(record){
                        if(!record.get('default') && !record.get('markedForDelete')) {
                        // if(!record.get('markedForDelete')) {
                            signatures.push(record.getRecord());
                        }
                    });
                    // console.log('signatures=');
                    // console.log(signatures);
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
                        });
                        store.isReordered = undefined;
                        // console.log('GRID MODEL='+grid.recordModel);
                        // console.log(Ext.ClassManager.get(grid.recordModel).fields);
                        var recordFields = Ext.Array.pluck(Ext.ClassManager.get(grid.recordModel).fields, 'name');
                        var data = Ext.Array.pluck(store.getRange(), 'data');
                        data.forEach(function(record){
                            delete record['_id'];
                            delete record['markedForNew'];
                        });
                        // or cleanup in sync?
                        // data.forEach( function(record){
                        //     delete record['_id'];
                        //     for(var fieldName in record){
                        //         if(Ext.Array.indexOf(recordFields, fieldName) == -1){
                        //             delete record[fieldName];
                        //         }
                        //     }
                        // });
                        vm.set(settingsProperty, data);
                    }
                }
            }
        });

        // var changedData = me.getChangedData();
        // if(arguments.length == 1){
        //     if(additionalChanged){
        //         changedData= Ext.Object.merge(changedData,additionalChanged);
        //     }
        // }

        // console.log(vm.get('settings'));

        Ext.Ajax.request({
            url: "/admin/download",
            jsonData: vm.get('settings'),
            method: 'POST',
            params: {
                type: "IntrusionPreventionSettings",
                arg1: "save",
                arg2: vm.get('instance.id')
            },
            scope: this,
            timeout: 600000
        }).then(function(result){
            if(Util.isDestroyed(me, v, vm)){
                return;
            }

            var response = Ext.decode( result.responseText );
            vm.set({
                profileStoreLoad: true,
                signaturesStoreLoad: true,
                variablesStoreLoad: true
            });

            if( !response.success) {
                Ext.MessageBox.alert("Error".t(), "Unable to save settings".t());
            } else {
                Rpc.asyncData(v.appManager, 'reconfigure')
                .then( function(result){
                    if(Util.isDestroyed(me, v, vm)){
                        return;
                    }
                    v.setLoading(false);
                    Util.successToast('Settings saved...');
                    v.down('appstate').getController().reload();
                    me.getSettings();
                    Ext.fireEvent('resetfields', v);
                }, function(response){
                    if(!Util.isDestroyed(me, v, vm)){
                        v.setLoading(false);
                        vm.set('panel.saveDisabled', true);
                    }
                    Util.handleException(response);
                });
            }
        }, function(response){
            Ext.MessageBox.alert("Error".t(), "Unable to save settings".t());
            if(!Util.isDestroyed(me, v, vm)){
                v.setLoading(false);
                Util.successToast('Unable to save settings...');
                return;
            }
            Util.handleException(response);
        });

    },

    regexSignatureVariable :  /^\$([A-Za-z0-9\_]+)/,
    regexSignature: /^([#]+|)(alert|log|pass|activate|dynamic|drop|sdrop|reject)\s+(tcp|udp|icmp|ip)\s+([^\s]+)\s+([^\s]+)\s+([^\s]+)\s+([^\s]+)\s+([^\s]+)\s+\((.+)\)$/,
    isVariableUsed: function(variable) {
        var me = this, vm = this.getViewModel();

        if(Ext.isEmpty(variable)) {
            return false;
        }

        var signature, originalId, signatureMatches, variableMatches, j, internalId, d;
        var isUsed = false;
        vm.get('signatures').each(function(record){
            var signatureMatches = me.regexSignature.exec( record.get('signature') );
            if( signatureMatches ) {
                for( j = 1; j < signatureMatches.length; j++ ) {
                    variableMatches = me.regexSignatureVariable.exec( signatureMatches[j] );
                    if( variableMatches && variableMatches.shift().indexOf(variable)!= -1) {
                        isUsed = true;
                        return false;
                    }
                }
            }

        });
        return isUsed;
    },

    // runWizard: function (btn) {
    //     this.wizard = this.getView().add({
    //         xtype: 'app-intrusion-prevention-wizard'
    //     });
    //     this.wizard.show();
    // },

    storeDataChanged: function( store ){
        /*
         * Inexplicably, extjs does not see 'inline' data loads as "proper" store
         * reloads so it will never fire the 'load' event which logically sounds
         * like the correct event to listen to.
         *
         * The problem occurs on saves where data is "reloaded" but seen in
         * the store as a wholesale change.  Which is ridiculous and on the next
         * save in the same session, causes to see all records as modified
         * and therefore, send send ALL data back.
         *
         * To get around this, we have the inline loader routines set the
         * 'storeId'Load variable and if we see it here, cause all of those changes
         * to be "commited" since nothing has changed.
         *
         * Thanks, ExtJs.
         *
         */
        var vm = this.getViewModel();
        var storeId = store.getStoreId();
        if(vm.get( storeId + 'Load') == true){
            store.commitChanges();
            vm.set( storeId + 'Load', false);
        }
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

    statics:{
        ruleActionsRenderer: function(value, meta, record, x,y, z, table){
            var displayValue = table.up('grid').getController().getViewModel().get('ruleActionsStore').findRecord('value', value, 0, false, false, true).get('display');
            meta.tdAttr = 'data-qtip="' + Ext.String.htmlEncode( displayValue ) + '"';
            return displayValue;
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
            var vm = this.getViewModel();
            var classtypeRecord = Ung.apps.intrusionprevention.Main.classtypes.findRecord('name', value);
            if( classtypeRecord != null ){
                description = classtypeRecord.get('description');
            }
            metaData.tdAttr = 'data-qtip="' + Ext.String.htmlEncode( description ) + '"';
            return value;
        },

        categoryRenderer: function( value, metaData, record, rowIdx, colIdx, store ){
            var vm = this.getViewModel();
            var description = value;
            var categoryRecord = Ung.apps.intrusionprevention.Main.categories.findRecord('name', value);
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
            return references.join("");
        },
        ruleActionPrecedence: {
            'default': 0, 
            'log' : 1, 
            'blocklog': 2, 
            'block': 3, 
            'disable': 4
        },
        ruleSortActionPrecedence: function( record1, record2){
            var action1 = record1.get('action');
            var action2 = record2.get('action');
            return ( Ung.apps.intrusionprevention.MainController.ruleActionPrecedence[action1] > Ung.apps.intrusionprevention.MainController.ruleActionPrecedence[action2] ) ? 1 : ( (action1 == action2) ? 0 : -1); 
        },
    }
});

Ext.define('Ung.apps.intrusionprevention.cmp.RuleGridController', {
    extend: 'Ung.cmp.GridController',
    alias: 'controller.unintrusionrulegrid',

    control: {
        '#': {
            reconfigure: 'updateRuleStatus',
        },
        'checkcolumn': {
            checkchange: 'updateRuleStatus'
        },
        'ungrid':{
            edit: 'updateRuleStatus'
        },
        'window': {
            close: 'updateRuleStatus'
        }
    },


    updateRuleStatus: function(){
        var me = this,
            v = me.getView(),
            vm = me.getViewModel();
        var conditions = Ext.Array.findBy(v.editorFields, function(field){
            if(field.xtype == 'conditionseditor'){
                return true;
            }
        }).conditions;

        var status = {
            log: {},
            block: {},
            disable: {}
        };
        var t0 = performance.now();
        var t1 = performance.now();
        var statusIndex;
        var signatures = vm.get('signatures');
        vm.get('rules').each(function(rule){
            if(rule.get('enabled')){
                var ruleAction = rule.get('action');
                var ruleActionDefault = ( ruleAction == 'default' );
                statusIndex = ruleAction;
                signatures.each( function(signature){
                    if(rule.matchSignature(signature, conditions, vm)){
                        if(ruleActionDefault){
                            statusIndex = signature.data['block'] ? 'block' : ( signature.data['log'] ? 'log' : 'disable');
                        }
                        if(ruleAction == 'blocklog'){
                            statusIndex = signature.data['log'] ? 'block' : 'disable';
                        }

                        var signatureId = signature.data['id'];
                        for(var action in status){
                            if(action == statusIndex){
                                continue;
                            }
                            if(status[action][signatureId]){
                                status[action][signatureId] = false;
                                break;
                            }
                        }
                        status[statusIndex][signatureId] = true;
                    }
                });
                // console.log('rule ' + rule.get('description'));
                // console.log(performance.now()- t0);
                t0 = t1;
                t1 = performance.now();
            }
        });
        t0 = performance.now();
        var found, 
            signatureId;
        signatures.each( function(signature){
            signatureId = signature.data['id'] ;
            found = false;
            for(var action in status){
                if(action == 'disable'){
                    continue;
                }
                if(status[action][signatureId]){
                    found = true;
                    break;
                }
            }
            if(!found){
                status['disable'][signatureId] = true;
            }
        });
        // console.log('disabled');
        t1 = performance.now();
        // console.log(t1-t0);

        var ruleStatusBar = v.down("[name=ruleStatus]");
        var statusLengths = {};
        for(var statusKey in status){
            statusLengths[statusKey] = Ext.Array.sum(Ext.Object.getValues(status[statusKey]));
        }
        
        ruleStatusBar.update(Ext.String.format( 'Log: {0}, Block:{1}, Disabled:{2}'.t(), statusLengths['log'], statusLengths['block'], statusLengths['disable']));
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
        var v = this.getView();
        var vm = this.getViewModel();

        // Perform validation
        if( ! /^[0-9]+$/.test( newValue )){
            me.setValidation("Sid must be numeric".t());
            return false;
        }

        // !!! check gid+sid combo

        me.setValidation(true);
    },

    editorSidChange: function( me, newValue, oldValue, eOpts ){
        var v = this.getView();
        var vm = this.getViewModel();

        // Perform validation
        if( ! /^[0-9]+$/.test( newValue )){
            me.setValidation("Sid must be numeric".t());
            return false;
        }
        var record = vm.get('record');
        var originalRecord = v.record;

        var gid = record.get('gid');

        // !!! make this a function to share with gid validation.
        var match = false;
        vm.get('signatures').each( function( storeRecord ) {
            if( storeRecord != originalRecord && storeRecord.get('sid') == newValue) {
                var signatureGid = "1";
                signatureValue = storeRecord.get("signature");

                if( storeRecord.get('gid') == originalRecord.get('gid') ){
                    match = true;
                    return false;
                }
            }
        }, this);

        if( match === true ){
            me.setValidation("Sid already in use.".t());
            return false;
        }

        me.setValidation(true);
        // this.getView().up('grid').getController().updateSignature( this.getViewModel().get('record'), 'sid', newValue );
    },

    editorClasstypeChange: function( me, newValue, oldValue, eOpts ){
        var vm = this.getViewModel();
        if( newValue == null || Ung.apps.intrusionprevention.Main.classtypes.findExact('name', newValue) == null ){
            me.setValidation("Unknown classtype".t());
            return false;
        }
        me.setValidation(true);
    },

    editorMsgChange: function( me, newValue, oldValue, eOpts ){
        if( /[";]/.test( newValue ) ){
            me.setValidation( 'Msg contains invalid characters.'.t() );
            return false;
        }
        me.setValidation(true);
    },

    editorLogChange: function( me, newValue, oldValue, eOpts ) {
        if( newValue === false) {
            this.getViewModel().get('record').set('block', false);
        }
    },

    editorBlockChange: function( me, newValue, oldValue, eOpts ) {
        if( newValue === true ){
            this.getViewModel().get('record').set('log', true);
        }
    },

    editorSignatureChange: function( me, newValue, oldValue, eOpts ){
        if(!Ung.model.intrusionprevention.signature.isValid(newValue)){
            me.setValidation( 'Signature is invalid.'.t() );
            return false;
        }
        me.setValidation(true);
    }

});

Ext.define('Ung.apps.intrusionprevention.cmp.SignatureGridController', {
    extend: 'Ung.cmp.GridController',

    alias: 'controller.unintrusionsignaturesgrid',

    logBeforeCheckChange: function ( elem, rowIndex, checked ){
        var record = elem.getView().getRecord(rowIndex);
        record.set('default', false);
        if( !checked){
            record.set('log', false);
            record.set('block', false );
        }else{
            record.set('log', true);
        }
    },

    blockBeforeCheckChange: function ( elem, rowIndex, checked ){
        var record = elem.getView().getRecord(rowIndex);
        record.set('default', false);
        if(checked) {
            record.set('log', true );
            record.set('block', true);
        }else{
            record.set('block', false);
        }
    },

    updateSearchStatusBar: function(){
        var v = this.getView();
        var searchStatus = v.down("[name=searchStatus]");
        var hasLogOrBlockFilter = ( v.down("[name=searchLog]").getValue() === true ) || ( v.down("[name=searchBlock]").getValue() === true );
        var hasFilter = hasLogOrBlockFilter || ( v.down("[name=searchFilter]").getValue().length >= 2 );
        var statusText = "", logOrBlockText = "", totalEnabled = 0;
        if(!hasLogOrBlockFilter) {
            v.getStore().each(function( record ){
                if( ( record.get('log')) || ( record.get('block')) ) {
                    totalEnabled++;
                }
            });
            logOrBlockText = Ext.String.format( '{0} logging or blocking'.t(), totalEnabled);
        }
        if(hasFilter) {
            statusText = Ext.String.format( '{0} matching signature(s) found'.t(), v.getStore().getCount() );
            if(!hasLogOrBlockFilter) {
                statusText += ', ' + logOrBlockText;
            }
        } else {
            statusText = Ext.String.format( '{0} available signatures'.t(), v.getStore().getCount()) + ', ' + logOrBlockText;
        }
        searchStatus.update( statusText );
    },

    searchFilter: Ext.create('Ext.util.Filter', {
        filterFn: function(){}
    }),
    filterSearch: function(elem, newValue, oldValue, eOpts){
        var store = this.getView().getStore();
        if( newValue ){
            if( newValue.length > 1 ){
                var re = new RegExp(newValue, 'gi');
                this.searchFilter.setFilterFn( function(record){
                    return re.test(record.get('category')) ||
                        re.test(record.get('signature'));
                });
                store.addFilter( this.searchFilter );
            }
        }else{
            store.removeFilter( this.searchFilter );
            if(store.filters.length === 0){
                this.getView().reconfigure();
            }
        }
        this.updateSearchStatusBar();
    },

    logFilter: Ext.create('Ext.util.Filter', {
        property: 'log',
        value: true
    }),

    filterLog: function(elem, newValue, oldValue, eOpts){
        var store = this.getView().getStore();
        if( newValue ){
            store.addFilter( this.logFilter );
        }else{
            store.removeFilter( this.logFilter );
            if(store.filters.length === 0){
                this.getView().reconfigure();
            }
        }
        this.updateSearchStatusBar();
    },

    blockFilter: Ext.create('Ext.util.Filter', {
        property: 'block',
        value: true
    }),

    filterBlock: function(elem, newValue, oldValue, eOpts){
        var store = this.getView().getStore();
        if( newValue ){
            store.addFilter( this.blockFilter );
        }else{
            store.removeFilter( this.blockFilter );
            if(store.filters.length === 0){
                this.getView().reconfigure();
            }
        }
        this.updateSearchStatusBar();
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
        var me = this;

        this.callParent(arguments);
        var store = this.getView().getStore();

        store.each( function(record){
            me.updateSignature(record, 'log', record.get('log') );
            me.updateSignature(record, 'block', record.get('block') );

        });
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
        if( updatedKey != 'log' && updatedKey != 'block' ){
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
            if( ( storeRecord !== v.record ) && ( newValue == storeRecord.get('variable') ) ){
                match = true;
            }
        });
        if(match){
            me.setValidation("Variable name already in use.".t());
            return false;

        }
        me.setValidation(true);

        var activeVariable = v.up('app-intrusion-prevention').getController().isVariableUsed(newValue);
        me.setReadOnly(activeVariable);
        me.up("").down("[name=activeVariable]").setVisible(activeVariable);
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
        if( this.getView().up('app-intrusion-prevention').getController().isVariableUsed( record.get('variable') ) ){
            Ext.MessageBox.alert( "Cannot Delete Variable".t(), "Variable is used by one or more signatures.".t() );
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
        type: 'string'
    },{
        name: 'classtype',
        type: 'string'
    },{
        name: 'block',
        type: 'boolean'
    },{
        name: 'log',
        type: 'boolean'
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
                log: false,
                block: false,
                sid: '1',
                gid: '1'
            };

            var action = 'alert';
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
                    data['log'] = false;
                    data['block'] = false;
                }else{
                    if(action == 'alert'){
                        data['log'] = true;
                        data['block'] = false;
                    }else if(action == 'drop'){
                        data['log'] = true;
                        data['block'] = true;
                    }else if(action == 'sdrop'){
                        data['log'] = false;
                        data['block'] = true;
                    }
                }
            }
        }

        this.callParent([data], session);

        if(typeof signature == "string" && valid){
            me.optionsMapIndexes = {};
            var key = null;
            me.data['options'].forEach(function(option, index){
                key = option.substr(0, option.indexOf(':')).trim();
                Ung.model.intrusionprevention.signature.optionsMap.forEach(function(mappedOption){
                    if(mappedOption.name == key){
                        me.optionsMapIndexes[mappedOption] = index;
                        var value = me.massageGetOptionValue(option.substr(option.indexOf(':') + 1));
                        if(option.defaultValue){
                            value = value ? value : option.defaultValue;
                        }
                        me.data[mappedOption.name] = value;
                    }
                });
            });
            me.data['id'] = me.data['sid'] + '_' + me.data['gid'];
        }
    },

    get: function(fieldName) {
        var me = this;
        var result = this.callParent(arguments);

        Ung.model.intrusionprevention.signature.optionsMap.forEach(function(option){
            if(fieldName == option.name){
                result = me.getOption(option.name);
            }
        });

        return result;
    },

    set: function(fieldName, newValue, options) {
        var me = this;

        var signatureChange = ( fieldName == 'signature' ) && me.data['signature'] && ( newValue != me.data['signature'] );

        var result = this.callParent(arguments);

        if(fieldName != 'signature'){
            Ung.model.intrusionprevention.signature.optionsMap.forEach(function(option){
                if(fieldName == option.name){
                    me.setOption(option.name, newValue);
                }
            });
            if(Ung.model.intrusionprevention.signature.rebuildSignatureKeys.indexOf(fieldName) != -1){
                var newSignature = me.build();
                if(me.data['signature'] != newSignature){
                    me.set('signature', newSignature);
                }
            }
        }else if(signatureChange){
            var signature = new Ung.model.intrusionprevention.signature(newValue, me.data['category'], me.data['reserved']);

            Ung.model.intrusionprevention.signature.optionsMap.forEach(function(option){
                var value = signature.getOption(option.name);
                if(option.defaultValue){
                    value = value ? value : option.defaultValue;
                }
                me.set(option.name, value);
            });
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

        // optionmap

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
                log: this.get('log'),
                block: this.get('block')
            };
        }else{
            return this.build();
        }
    },

    build: function(){
        var me = this;

        var action = 'alert';
        var log =this.get('log');

        var block = this.get('block');
        if( log && block ){
            action = 'drop';
        }else if(!log && !block){
            action = '#' + action;
        }

        return action + " " +
            (this.get('protocol') ? this.get('protocol') + ' ' : '') +
            (this.get('lnet') ? this.get('lnet') + ' ' : '') +
            (this.get('lport') ? this.get('lport') + ' ' : '') +
            (this.get('direction') ? this.get('direction') + ' ' : '') +
            (this.get('rnet') ? this.get('rnet') + ' ' : '') +
            (this.get('rport') ? this.get('rport') + ' ' : '') +
            "(" + this.get('options').join(';') + ")";
    },
    
    statics:{
        // signatureRegex: /^([#\s]+|)(alert|log|pass|activate|dynamic|drop|reject|sdrop)\s+((tcp|udp|icmp|ip|http|ftp|tls|smb|dns|smtp)\s+([^\s]+)\s+([^\s]+)\s+(\-\>|<>)\s+([^\s]+)\s+([^\s]+)\s+|)\((.+)\)/,
        signatureRegex: /^([#\s]+|)(alert|log|pass|activate|dynamic|drop|reject|sdrop)\s+(([^\s]+)\s+([^\s]+)\s+([^\s]+)\s+(\-\>|<>)\s+([^\s]+)\s+([^\s]+)\s+|)\((.+)\)/,
        optionsMap: [{
            name: 'gid',
            defaultValue: '1'
        },{
            name: 'sid'
        },{
            name: 'classtype',
            defaultValue: 'unknown'
        },{
            name: 'msg'
        }],
        rebuildSignatureKeys:[
            'gid',
            'sid',
            'classtype',
            'category',
            'msg',
            'log',
            'block'
        ],
        isValid: function(signature){
            return Ung.model.intrusionprevention.signature.signatureRegex.test(signature);
        }
    }
});

Ext.define('Ung.model.intrusionprevention.rule',{
    extend: 'Ext.data.Model',
    fields:[{
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
    }],

    matchSignature: function(signature, editorConditions, vm){
        var me = this;
        if(!me.get('enabled')){
            return false;
        }
        var allMatch = true;
        me.get('conditions').list.forEach(function(condition){
            var match = true;
            var editorCondition = editorConditions[condition.type];

            var targetConditionValue = null;
            var conditionKey = condition.type.toLowerCase();
            switch(conditionKey){
                case 'system_memory':
                    targetConditionValue = vm.get('system_memory');
                    break;
                case 'signature':
                    targetConditionValue = signature.data['signature'];
                    break;
                case 'classtype':
                    targetConditionValue = signature.data['classtype'];
                    break;
                case 'msg':
                    targetConditionValue = signature.data['msg'];
                    break;
                case 'src_addr':
                    targetConditionValue = signature.data['lnet'];
                    break;
                case 'src_port':
                    targetConditionValue = signature.data['lport'];
                    break;
                case 'dst_addr':
                    targetConditionValue = signature.data['rnet'];
                    break;
                case 'dst_port':
                    targetConditionValue = signature.data['rport'];
                    break;
                default:
                    targetConditionValue = signature.data[conditionKey];
            }

            switch(editorCondition.comparator){
                case 'numeric':
                    match = me.matchesNumeric(parseInt(targetConditionValue, 10), condition.comparator, parseInt(condition.value, 10) );
                    break;
                case 'boolean':
                    var listValue = condition.value;
                    if(typeof(listValue) != 'object'){
                        listValue = listValue.split(',');
                    }
                    match = me.matchesIn(targetConditionValue, condition.comparator, listValue);
                    break;
                case 'text':
                    match = me.matchesText(targetConditionValue, condition.comparator, condition.value);
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

    matchesNumeric: function(sourceValue, comparator, targetValue){
        switch(comparator){
            case "=":
                return sourceValue == targetValue;
            case "!=":
                return sourceValue != targetValue;
            case "<=":
                return sourceValue <= targetValue;
            case "<":
                return sourceValue < targetValue;
            case ">":
                return sourceValue > targetValue;
            case ">=":
                return sourceValue >= targetValue;
        }

        return false;
    },

    matchesText: function(sourceValue, comparator, targetValue){
        switch(comparator){
            case "=":
                return sourceValue == targetValue;
            case "!=":
                return sourceValue != targetValue;
            case "substr":
                return sourceValue.indexOf(targetValue) != -1;
            case "!substr":
                return sourceValue.indexOf(targetValue) == -1;
        }

        return false;
    },

    matchesIn: function(sourceValue, comparator, targetValue){
        var isIn = Ext.Array.contains(targetValue, sourceValue);

        // console.log(isIn);

        if(comparator == "="){
            return isIn;
        }else if(comparator == "!="){
            return !isIn;
        }

        return false;
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
