import gettext
import reports.i18n_helper
import reports.node.untangle_base_spam

_ = reports.i18n_helper.get_translation('untangle-node-phish').lgettext

reports.engine.register_node(reports.node.untangle_base_spam.SpamBaseNode('untangle-node-phish', 'Phish', 'phish', 'Clam', _('Phish'), _('Clean'), _('Hourly Phish Rate'), _('Daily Phish Rate'), _('Top Ten Phishing Victims')))
