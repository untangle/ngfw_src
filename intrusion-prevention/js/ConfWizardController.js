Ext.define('Ung.apps.intrusionprevention.ConfWizardController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.app-intrusion-prevention-wizard',

    control: {
        '#': {
            afterrender: 'onAfterRender'
        },
        'window > panel': {
            activate: 'onActivateCard'
        }
    },

    onAfterRender: function () {
        var me = this,
            v = this.getView(),
            vm = this.getViewModel();

        v.setLoading(true);
        Ext.Deferred.sequence([
            Rpc.asyncPromise('rpc.metricManager.getMetricsAndStats'),
            function(){ return Ext.Ajax.request({
                url: "/admin/download",
                method: 'POST',
                params: {
                    type: "IntrusionPreventionSettings",
                    arg1: "wizard",
                    arg2: vm.get('instance.id')
                },
                scope: this,
                timeout: 600000
            });
        }], this).then(function (result) {
            if(Util.isDestroyed(me, vm)){
                return;
            }

            var stats = result[0];
            var memoryTotal = stats.systemStats["MemTotal"];
            var architecture = stats.systemStats["architecture"];
            if( architecture == "i386"){
                architecture = "32";
            } else if( architecture == "amd64") {
                architecture = "64";
            } else {
                architecture = "unknown";
            }

            var wizardDefaults = Ext.decode( result[1].responseText );

            var wizardProfile = null;
            wizardDefaults.profiles.forEach( function(profile){
                var match = false;
                systemStats = profile.systemStats;
                for( var statKey in systemStats ){
                    if( statKey == "MemTotal") {
                        match = Ext.isEmpty(systemStats[statKey]) || ( memoryTotal < parseFloat(systemStats[statKey] * 1.10 ) ) ;
                    } else if( statKey == "architecture") {
                        match = ( architecture == systemStats[statKey] );
                    } else {
                        match = ( stats.systemStats[statKey] == systemStats[statKey] );
                    }
                    if(!match){
                        break;
                    }
                }
                if( match && wizardProfile == null){
                    wizardProfile = profile;
                    wizardProfile.profileVersion = wizardDefaults.version;
                }
            });
            if(wizardProfile == null){
                wizardProfile = wizardDefaults.profiles[0];
            }

            vm.set('wizardProfile', wizardProfile);
            var settings = vm.get('settings');
            settings.configured = true;

            v.setLoading(false);
        }, function(ex){
            if(!Util.isDestroyed(me, v )){
                vm.set('nextBtn', null);
                v.setLoading(false);
            }
            Util.handleException(ex);
        });
    },

    onActivateCard: function (panel) {
        var v = this.getView(),
            vm = this.getViewModel(),
            layout = this.getView().getLayout();

        vm.set('prevBtn', layout.getPrev());
        if (layout.getPrev()) {
            vm.set('prevBtnText', layout.getPrev().getTitle());
        }
        vm.set('nextBtn', layout.getNext());
        if (layout.getNext()) {
            vm.set('nextBtnText', layout.getNext().getTitle());
        }

        var activeItem = v.getLayout().getActiveItem();
        var recommended = [];
        if(activeItem.getItemId() == "classtypes"){
            vm.get('wizardProfile').activeGroups.classtypesSelected.forEach( function(classtype){
                recommended.push(classtype.substring(1));
            });
            v.down('[name=classtypes_recommended_settings]').setHtml( recommended.join(', ') );

            var classtypesCheckboxGroup = {
                xtype: 'checkboxgroup',
                name: 'classtypes_selected',
                columns: 3,
                items: []
            };

            vm.get('classtypes').each(function(record){
                classtypesCheckboxGroup.items.push({
                    boxLabel: record.get( 'name' ) + ' (' + record.get( 'priority' ) + ')',
                    inputValue: record.get( 'name' ) ,
                    checked: vm.get('wizardProfile').activeGroups.classtypesSelected.indexOf('+' + record.get('name')) > -1,
                    listeners: {
                        render: function(c){
                            Ext.QuickTips.register({
                                target:  c.boxLabelEl, 
                                text: record.get( 'description' ),
                                dismissDelay: 5000
                            });
                        },
                        destroy: function(c){
                            Ext.QuickTips.unregister(c.boxLabelEl);
                        }
                    }
                });
            });
            var classtypesCustomSettings = v.down('[name=classtypes_custom_settings]');
            classtypesCustomSettings.remove( classtypesCustomSettings.getComponent(0) );
            classtypesCustomSettings.add( classtypesCheckboxGroup );
        }

        if(activeItem.getItemId() == "categories"){
            recommended = [];
            vm.get('wizardProfile').activeGroups.categoriesSelected.forEach( function(category){
                recommended.push(category);
            });
            v.down('[name=categories_recommended_settings]').setHtml( recommended.join(', ') );

            var categoriesCheckboxGroup = {
                xtype: 'checkboxgroup',
                name: 'categories_selected',
                columns: 4,
                items: []
            };

            vm.get('categories').each(function(record){
                categoriesCheckboxGroup.items.push({
                    boxLabel: record.get( 'name' ),
                    inputValue: record.get( 'name' ),
                    checked: vm.get('wizardProfile').activeGroups.categoriesSelected.indexOf(record.get('name')) > -1,
                    listeners: {
                        render: function(c){
                            Ext.QuickTips.register({
                                target:  c.boxLabelEl, 
                                text: record.get( 'description' ),
                                dismissDelay: 5000
                            });
                        },
                        destroy: function(c){
                            Ext.QuickTips.unregister(c.boxLabelEl);
                        }
                    }
                });
            });
            var categoriesCustomSettings = v.down('[name=categories_custom_settings]');
            categoriesCustomSettings.remove( categoriesCustomSettings.getComponent(0) );
            categoriesCustomSettings.add( categoriesCheckboxGroup );
        }

        if(activeItem.getItemId() == "finish"){
            var activeGroups = {};

            var classtypesCard = v.down('[itemId=classtypes]');
            activeGroups.classtypes = classtypesCard.down('[name=classtypes]').getValue();
            if(activeGroups.classtypes == "custom"){
                activeGroups.classtypesSelected = classtypesCard.down('[name=classtypes_selected]').getValue().split(',');
            }

            var categoriesCard = v.down('[itemId=categories]');
            activeGroups.categories = categoriesCard.down('[name=categories]').getValue();
            if(activeGroups.categories == "custom"){
                activeGroups.categoriesSelected = categoriesCard.down('[name=categories_selected]').getValue().split(',');
            }

            var settings = vm.get('settings');
            settings.profileId = vm.get('wizardProfile').profileId;
            settings.activeGroups = activeGroups;
            settings.configured = true;
            vm.set('settings', settings);

            var app = this.getView().up('#appCard');
            app.getController().setSettings({
                activeGroups: activeGroups,
                profileId: settings.profileId,
                configured: true
            });

            var appState = app.down('appstate');
            var appStateVm = appState.getViewModel();
            if(appStateVm.get('instance.targetState') != 'RUNNING' || appStateVm.get('instance.runState') != 'RUNNING'){
                app.down('appstate').down('button[cls=power-btn]').click();
            }
        }

    },

    onNext: function () {
        var v = this.getView(),
            vm = this.getViewModel(),
            activeItem = v.getLayout().getActiveItem();

        v.getLayout().next();
    },

    onPrev: function () {
        var v = this.getView();
        if (v.getLayout().getPrev()) {
            v.getLayout().prev();
        }
    },

    onFinish: function () {
        this.getView().close();
    }});
