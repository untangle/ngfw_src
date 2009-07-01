#!/usr/bin/python

PREFIX = '@PREFIX@'
REPORTS_PYTHON_DIR = '%s/usr/lib/python2.5' % PREFIX
REPORTS_OUTPUT_BASE = '%s/usr/share/untangle/web/reports' % PREFIX
NODE_MODULE_DIR = '%s/reports/node' % REPORTS_PYTHON_DIR

import sys

if PREFIX != '':
    sys.path.insert(0, REPORTS_PYTHON_DIR)

import gettext
import mx
import smtplib
import sql_helper

from email.MIMEMultipart import MIMEMultipart
from email.MIMEText import MIMEText
from email.MIMEImage import MIMEImage

_ = gettext.gettext

def mail(sender, receiver, date_str, host, port):
    msgRoot = MIMEMultipart('related')
    msgRoot['Subject'] = _('New Untangle Reports Available')
    msgRoot['From'] = sender
    msgRoot['To'] = receiver

    msgAlternative = MIMEMultipart('alternative')
    msgRoot.attach(msgAlternative)

    if port == "443":
        url_str = "https://%s/reports/index.jsp?date=%s" % (host, date_str)
    else:
        url_str = "https://%s:%s/reports/index.jsp?date=%s" \
            % (host, port, date_str)

    msgText = MIMEText("""\
Please see your reports available at: %s""" % url_str)
    msgAlternative.attach(msgText)

    smtp = smtplib.SMTP()
    smtp.connect('localhost')
    smtp.sendmail(sender, receiver, msgRoot.as_string())
    smtp.quit()

def __get_hostname():
    conn = sql_helper.get_connection()

    try:
        curs = conn.cursor()

        curs.execute(_("""\
SELECT is_hostname_public, has_public_address, hostname, public_port
FROM settings.u_address_settings"""))

        row = curs.fetchone()

        rv = None

        if row:
            is_hostname_public, has_public_address, hostname, public_port = row

            if is_hostname_public:
                rv = (hostname, public_port)
    finally:
        conn.commit()

    if not rv:
        f = open('/usr/share/untangle/conf/networking.sh')
        lines = f.readlines()
        f.close()

        addr = [e.split('=')[1] for e in l if e.startswith('HTTPS_PUBLIC_ADDR=')][0].strip()
        port = [e.split('=')[1] for e in l if e.startswith('HTTPS_PUBLIC_PORT=')][0].strip()

        rv = (addr, port)

    return rv

def __get_mail_info():
    conn = sql_helper.get_connection()

    try:
        curs = conn.cursor()

        curs.execute("""\
SELECT report_email, from_address FROM settings.u_mail_settings""")

        row = curs.fetchone()

        if row:
            rv = (row[0], row[1].split(','))
        else:
            rv = None
    finally:
        conn.commit()

    return rv


date = mx.DateTime.today()
date_str = '%d-%02d-%02d' % (date.year, date.month, date.day)
host, port = __get_hostname()
sender, receivers = __get_mail_info()
for receiver in receivers:
    mail(sender, receiver, date_str, host, port)
