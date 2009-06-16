import gettext
import reports.node.untangle_base_spam

_ = gettext.gettext

reports.engine.register_node(reports.node.untangle_base_spam.SpamBaseNode('untangle-node-phish', 'Phish', 'phish', 'Clam', _('phish'), _('clean')))
