Ext.define('Ung.store.ReportsTree', {
    extend: 'Ext.data.TreeStore',
    alias: 'store.reportstree',
    storeId: 'reportstree',
    filterer: 'bottomup',
    sorters: [{
        property: 'viewPosition',
        direction: 'ASC'
    }],

    // initialize the root which will be overriden later
    root: {
        text: 'All Reports'.t(),
        expanded: true,
        children: [{
            text: 'Loading...'.t(),
            icon: 'fa-spinner fa-spin',
            leaf: true
        }]
    },

    build: function () {
        var me = this, nodes = [], storeCat, category;
        Ext.Array.each(Ext.getStore('reports').getGroups().items, function (group) {
            storeCat = Ext.getStore('categories').findRecord('displayName', group._groupKey, 0, false, true, true);

            if (!storeCat) { return; }

            // create category node
            category = {
                text: group._groupKey,
                slug: storeCat.get('slug'),
                type: storeCat.get('type'), // app or system
                icon: storeCat.get('icon'),
                cls: 'x-tree-category',
                url: 'cat=' + storeCat.get('slug'),
                children: [],
                viewPosition: storeCat.get('viewPosition'),
                disabled: false
                // expanded: group._groupKey === vm.get('category.categoryName')
            };
            // add reports to each category
            Ext.Array.each(group.items, function (entry) {
                if(entry.removedFrom){
                    return;
                }

                category.children.push({
                    text: entry.get('localizedTitle'),
                    cat: storeCat.get('slug'),
                    slug: entry.get('slug'),
                    url: entry.get('url'),
                    uniqueId: entry.get('uniqueId'),
                    type: entry.get('type'),
                    readOnly: entry.get('readOnly'),
                    iconCls: 'fa ' + entry.get('icon'),
                    cls: 'x-tree-report',
                    table: entry.get('table'),
                    disabled: false,
                    leaf: true
                    // selected: uniqueId === vm.get('entry.uniqueId')
                });
            });
            nodes.push(category);
        });

        me.setRoot({
            text: 'All reports'.t(),
            slug: '/reports/',
            expanded: true,
            disabled: false, // !important
            children: nodes
        });
    }

});
