Ung.Settings.dependency["untangle-node-clam"] = {
    name : "untangle-base-virus",
    fn : function() {
        if (!Ung.hasResource["Ung.Clam"]) {
            Ung.hasResource["Ung.Clam"] = true;
            Ung.Settings.registerClassName('untangle-node-clam', 'Ung.Clam');
            Ung.Clam = Ext.extend(Ung.Virus, {});
        }
    }
}
