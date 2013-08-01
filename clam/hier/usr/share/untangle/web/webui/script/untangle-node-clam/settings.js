Ung.NodeWin.dependency["untangle-node-clam"] = {
    name: "untangle-base-virus",
    fn: function() {
        if (!Ung.hasResource["Ung.Clam"]) {
            Ung.hasResource["Ung.Clam"] = true;
            Ung.NodeWin.registerClassName('untangle-node-clam', 'Ung.Clam');
            Ung.Clam = Ext.extend(Ung.Virus, {
                helpSourceName: 'virus_blocker_lite'
            });
        }
    }
};
//@ sourceURL=clam-settings.js
