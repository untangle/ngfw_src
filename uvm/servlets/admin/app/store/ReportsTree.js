Ext.define('Ung.store.ReportsTree', {
    extend: 'Ext.data.TreeStore',
    alias: 'store.reportstree',
    storeId: 'reportstree',
    filterer: 'bottomup',

    build: function () {
        var me = this, nodes = [], storeCat, category;
        Ext.Array.each(Ext.getStore('reports').getGroups().items, function (group) {
            storeCat = Ext.getStore('categories').findRecord('displayName', group._groupKey);

            if (!storeCat) { return; }

            // create category node
            category = {
                text: group._groupKey,
                slug: storeCat.get('slug'),
                type: storeCat.get('type'), // app or system
                icon: storeCat.get('icon'),
                cls: 'x-tree-category',
                url: storeCat.get('slug'),
                children: [],
                // expanded: group._groupKey === vm.get('category.categoryName')
            };
            // add reports to each category
            Ext.Array.each(group.items, function (entry) {
                category.children.push({
                    text: entry.get('title'),
                    slug: entry.get('slug'),
                    url: entry.get('url'),
                    uniqueId: entry.get('uniqueId'),
                    type: entry.get('type'),
                    readOnly: entry.get('readOnly'),
                    iconCls: 'fa ' + entry.get('icon'),
                    cls: 'x-tree-report',
                    leaf: true
                    // selected: uniqueId === vm.get('entry.uniqueId')
                });
            });
            nodes.push(category);
        });

        me.setRoot({
            text: 'All reports',
            slug: 'reports',
            expanded: true,
            children: nodes
        });
    }

});
