Ext.define('Ung.apps.webfilter.MainController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.app-web-filter',

    control: {
        '#': {
            afterrender: 'getSettings',
        },
        '#site-lookup': {
            afterrender: 'initSiteLookup'
        }
    },

    getSettings: function () {
        var v = this.getView(), vm = this.getViewModel();

        v.setLoading(true);
        Rpc.asyncData(v.appManager, 'getSettings')
        .then( function(result){
            if(Util.isDestroyed(v, vm)){
                return;
            }

            vm.set('settings', result);

            vm.set('panel.saveDisabled', false);
            v.setLoading(false);
        },function(ex){
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

            var filters = store.getFilters().clone();
            store.clearFilter(true);

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
                vm.set(grid.listProperty, Ext.Array.pluck(store.getRange(), 'data'));
            }
            filters.each( function(filter){
                store.addFilter(filter);
            });
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

    clearHostCache: function () {
        var v = this.getView();
        Ext.MessageBox.wait('Clearing Category URL Cache...'.t(), 'Please Wait'.t());

        Rpc.asyncData(v.appManager, 'clearCache')
        .then(function(result){
            Ext.MessageBox.hide();
            Util.successToast('The Category URL Cache was cleared succesfully.'.t());
        }, function(ex) {
            Util.handleException(ex);
            Ext.MessageBox.hide();
        });
    },

    initSiteLookup: function() {
        var vm = this.getViewModel();
        vm.set('siteLookupInput', '');
        vm.set('siteLookupAddress', '');
        vm.set('siteLookupCategory', '');
        vm.set('siteLookupCheckbox', false);
        vm.set('siteLookupSuggest', '');
    },

    handleSiteLookup: function() {
        var v = this.getView(), vm = this.getViewModel();

        var inputField = v.down("[fieldIndex='siteLookupInput']");

        if (inputField.getValue().length == 0) {
            Ext.MessageBox.alert('Site URL is empty'.t() , 'You must enter the Site URL for category search.'.t());
            return;
        }

        v.setLoading(true);
        Rpc.asyncData(v.appManager, 'lookupSite', inputField.getValue())
        .then(function(result){
            if(Util.isDestroyed(v, vm, inputField)){
                return;
            }
            v.setLoading(false);

            var showAddress = v.down("[fieldIndex='siteLookupAddress']");
            var showCategory = v.down("[fieldIndex='siteLookupCategory']");
            var suggestBox = v.down("[fieldIndex='siteLookupSuggest']");
            var masterList = vm.get('settings.categories.list');
            showAddress.setValue(inputField.getValue());
            inputField.setValue('');

            var currentCategories = [];
            var suggested = null;

            if(result && result.list && masterList ){
                result.list.forEach(function(id){
                    var category = Ext.Array.findBy(masterList, function(cat){
                        if(cat.id == id){
                            return true;
                        }
                    });
                    if(category){
                        currentCategories.push(category.name);
                        if(suggested == null){
                            suggested = category.id;
                        }
                    }
                });
            }
            showCategory.setValue(currentCategories.join(","));
            suggestBox.setValue(suggested);
        }, function(ex) {
            if(!Util.isDestroyed(v)){
                v.setLoading(false);
                return;
            }
            Util.handleException(ex);
        });
    },

    handleCategorySuggest: function() {
        var v = this.getView(), vm = this.getViewModel();

        var showAddress = v.down("[fieldIndex='siteLookupAddress']");
        var suggestBox = v.down("[fieldIndex='siteLookupSuggest']");

        if (showAddress.getValue().length == 0) {
            Ext.MessageBox.alert('Last Search URL is empty'.t() , 'You must search for a Site URL before you can suggest a different category.'.t());
            return;
        }

        v.setLoading(true);
        Rpc.asyncData(v.appManager, 'recategorizeSite', showAddress.getValue(), suggestBox.getValue())
        .then(function(result){
            if(Util.isDestroyed(v, vm, showAddress, suggestBox)){
                return;
            }
            v.setLoading(false);

            var showCategory = v.down("[fieldIndex='siteLookupCategory']");
            var checkBox = v.down("[fieldIndex='siteLookupCheckbox']");

            if (result == suggestBox.getValue()) {
                Ext.MessageBox.alert('Suggestion Submitted'.t(), showAddress.getValue() + ' - ' + vm.get('categories').findRecord('id', result, 0, false, false, true).get('name'));
            } else {
                Ext.MessageBox.alert('Unable to submit suggestion.'.t(), 'Please try again later.'.t());
            }

            showAddress.setValue('');
            showCategory.setValue('');
            suggestBox.setValue('');
            checkBox.setValue(false);

        }, function(ex) {
            if(!Util.isDestroyed(v)){
                v.setLoading(false);
                return;
            }
            Util.handleException(ex);
        });
    },

});

Ext.define('Ung.apps.webfilter.cmp.SearchTermsGridController', {
    extend: 'Ung.cmp.GridController',
    alias: 'controller.unwebfiltersearchtermsgrid',

    handleImport: function(btn){
        var me = this;

        btn.up('form').submit({
            waitMsg: 'Please wait while the settings are uploaded...'.t(),
            success: function(form, action) {
                if (!action.result) {
                    Ext.MessageBox.alert('Warning'.t(), 'Import failed.'.t());
                    return;
                }
                if (!action.result.success) {
                    Ext.MessageBox.alert('Warning'.t(), action.result.msg);
                    return;
                }

                var blocked = false;
                var flagged = false;
                form.getValues().defaultActions.forEach(function(action){
                    if(action == 'block'){
                        blocked = true;
                    }
                    if(action == 'flag'){
                        flagged = true;
                    }
                });

                var fileType = form.getValues().fileType;
                var jsonArray = [];
                if(fileType == 'COMMA'){
                    action.result.msg.split("\n").forEach( function(line){
                        line = line.trim();
                        if(!line.length || line[0] == '#'){
                            return;
                        }
                        if(line.indexOf(',') > -1){
                            line.split(",").forEach( function(term){
                                term = term.trim();
                                jsonArray.push({
                                    string: term,
                                    description: term,
                                    blocked: blocked,
                                    flagged: flagged,
                                    javaClass: "com.untangle.uvm.app.GenericRule"
                                });
                            });
                        }
                    });
                }else if(fileType == 'NEWLINE'){
                    action.result.msg.split("\n").forEach( function(line){
                        line = line.trim();
                        if(!line.length){
                            return;
                        }
                        jsonArray.push({
                            string: line,
                            description: line,
                            blocked: blocked,
                            flagged: flagged,
                            javaClass: "com.untangle.uvm.app.GenericRule"
                        });
                    });

                }else{
                    jsonArray = action.result.msg;
                }

                me.importHandler(form.getValues().importMode, jsonArray);
                me.importDialog.close();
            },
            failure: function(form, action) {
                Ext.MessageBox.alert('Warning'.t(), action.result.msg);
            }
        });
    }
});