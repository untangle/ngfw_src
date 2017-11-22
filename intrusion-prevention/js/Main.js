Ext.define('Ung.apps.intrusionprevention.Main', {
    extend: 'Ung.cmp.AppPanel',
    alias: 'widget.app-intrusion-prevention',
    controller: 'app-intrusion-prevention',

    viewModel: {
        stores: {
            activeGroups:{
                storeId: 'profileStore',
                fields: [{
                    name: 'categories'
                },{
                    name: 'categoriesSelected'
                },{
                    name: 'classtypes',
                },{
                    name: 'classtypesSelected',
                }],
                data: '{settings.activeGroups}',
                listeners:{
                    datachanged: 'storedatachanged'
                }
            },
            rules: {
                storeId: 'rulesStore',
                fields: [{
                    name: 'sid',
                    sortType: 'asInt'
                },{
                    name: 'classtype'
                },{
                    name: 'category'
                },{
                    name: 'msg'
                },{
                    name: 'rule'
                },{
                    name: 'path'
                },{
                    name: 'log',
                    type: 'boolean'
                },{
                    name: 'block',
                    type: 'boolean'
                }],
                data: '{settings.rules.list}',
                groupField: 'classtype',
                sorters: [{
                    property: 'sid',
                    direction: 'ASC'
                }],
                listeners:{
                    datachanged: 'storedatachanged'
                }
            },
            variables: {
                storeId: 'variablesStore',
                fields: [{
                    name: 'variable',
                },{
                    name: 'definition'
                },{
                    name: 'description'
                }],
                data: '{settings.variables.list}',
                sorters: [{
                    property: 'variable',
                    direction: 'ASC'
                }],
                listeners:{
                    datachanged: 'storedatachanged'
                }
            }
        },
        formulas: {
            getWizardClasstypes: function(get) {
                var record = get('activeGroups').first();
                var profileId = get('settings.profileId');
                var currentClasstypes = record.get('classtypes');
                var profileClasstypes = [];
                if(currentClasstypes == 'custom'){
                    profileClasstypes = record.get('classtypesSelected');
                }else{
                    get('wizardDefaults').profiles.forEach(function(profile){
                        if(profile.profileId == profileId){
                            profileClasstypes = profile.activeGroups.classtypesSelected;
                        }
                    });
                }

                var selected = [];
                profileClasstypes.forEach(function(classtype){
                    if(classtype[0] == '-'){
                        return;
                    }
                    if(classtype[0] == '+'){
                        classtype = classtype.substring(1);
                    }
                    selected.push(classtype);
                });

                var value = 'Recommended'.t();
                if(currentClasstypes == 'custom'){
                    value = 'Custom'.t() + Ext.String.format( ': {0}', selected.join(', ') );
                }
                return {
                    value: value,
                    tip: selected.join(', ')
                };
            },
            getWizardCategories: function(get){
                var record = get('activeGroups').first();
                var profileId = get('settings.profileId');
                var currentCategories = record.get('categories');
                var profileCategories = [];
                if(currentCategories == 'custom'){
                    profileCategories = record.get('categoriesSelected');
                }else{
                    get('wizardDefaults').profiles.forEach(function(profile){
                        if(profile.profileId == profileId){
                            profileCategories = profile.activeGroups.categoriesSelected;
                        }
                    });
                }

                var selected = [];
                profileCategories.forEach(function(category){
                    if(category[0] == '-'){
                        return;
                    }
                    if(category[0] == '+'){
                        category = category.substring(1);
                    }
                    selected.push(category);
                });

                var value = 'Recommended'.t();
                if(currentCategories == 'custom'){
                    value = 'Custom'.t() + Ext.String.format( ': {0}', selected.join(', ') );
                }
                return {
                    value: value,
                    tip: selected.join(', ')
                };
            }
        },
    },

    tabBar: {
        items: [{
            xtype: 'component',
            margin: '0 0 0 10',
            cls: 'view-reports',
            autoEl: {
                tag: 'a',
                href: '#reports/intrusion-prevention',
                html: '<i class="fa fa-line-chart"></i> ' + 'View Reports'.t()
            },
            hidden: true,
            bind: {
                hidden: '{instance.runState !== "RUNNING"}'
            }
        }]
    },

    items: [
        { xtype: 'app-intrusion-prevention-status' },
        { xtype: 'app-intrusion-prevention-rules' },
        { xtype: 'app-intrusion-prevention-variables' }
    ]

});
