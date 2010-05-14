#!/usr/bin/python

# $HeadURL: svn://chef/work/src/buildtools/rake-util.rb $
# Copyright (c) 2003-2009 Untangle, Inc.
#
# This program is free software; you can redistribute it and/or modify
# it under the terms of the GNU General Public License, version 2,
# as published by the Free Software Foundation.
#
# This program is distributed in the hope that it will be useful, but
# AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
# NONINFRINGEMENT.  See the GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program; if not, write to the Free Software
# Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
#
# Aaron Read <amread@untangle.com>

PREFIX = '@PREFIX@'
REPORTS_PYTHON_DIR = '%s/usr/lib/python2.5' % PREFIX
REPORTS_OUTPUT_BASE = '%s/usr/share/untangle/web/reports' % PREFIX
NODE_MODULE_DIR = '%s/reports/node' % REPORTS_PYTHON_DIR

BODY_TEMPLATE_SIMPLE = """
The %(company)s Summary Reports for %(date_start)s - %(date_end)s are attached.
The PDF file requires Adobe Reader to view.
"""

BODY_TEMPLATE_LINK = BODY_TEMPLATE_SIMPLE + """
For more in-depth online reports, click %(link)s to view
Online %(company)s Reports.
"""

ATTACHMENT_TOO_BIG_TEMPLATE = """
The detailed reports were %sMB, which is too large to attach to this email:
the user-defined limit is currently %sMB.
"""

HTML_LINK_TEMPLATE = '<a href="%s">here</a>'

import sys

if PREFIX != '':
    sys.path.insert(0, REPORTS_PYTHON_DIR)

import datetime
import gettext
import locale
import mx
import os.path
import smtplib
import reports
import reports.i18n_helper
import reports.sql_helper as sql_helper
import tempfile
import shutil

from reports.log import *
logger = getLogger(__name__)

from email import Encoders
from email.MIMEBase import MIMEBase
from email.MIMEImage import MIMEImage
from email.MIMEMultipart import MIMEMultipart
from email.MIMEText import MIMEText

_ = reports.i18n_helper.get_translation('untangle-vm').lgettext

def mail_reports(end_date, report_days, file, mail_reports,
                 attach_csv, attachment_size_limit):
    if attach_csv:
        zip_dir = __make_zip_file(end_date, report_days, mail_reports)
        zip_file = '%s/reports.zip' % zip_dir
    else:
        zip_file = None

    receivers, sender = __get_mail_info()
    company_name = __get_branding_info()

    url = __get_url(end_date)
    report_users = __get_report_users()

    for receiver in receivers:
        has_web_access = receiver in report_users
        mail(file, zip_file, sender, receiver, end_date, company_name,
             has_web_access, url, report_days, attachment_size_limit)

    if zip_file:
        shutil.rmtree(zip_dir)

def mail(file, zip_file, sender, receiver, date, company_name,
         has_web_access, url, report_days, attachment_size_limit):
    msgRoot = MIMEMultipart('alternative')

    h = { 'company': company_name,
          'date_start': (date - datetime.timedelta(days=report_days+1)).strftime(locale.nl_langinfo(locale.D_FMT)),
          'date_end': (date - datetime.timedelta(days=1)).strftime(locale.nl_langinfo(locale.D_FMT)) }

    if has_web_access and url:
        h.update({'link' : url})
        msg_plain = BODY_TEMPLATE_LINK % h
        h.update({'link' : HTML_LINK_TEMPLATE % (url,)})
        msg_html = BODY_TEMPLATE_LINK % h
    else:
        msg_plain = msg_html = BODY_TEMPLATE_SIMPLE % h

    if zip_file:
      attachment_size = os.path.getsize(zip_file) / float(10**6)
      attachment_too_big = attachment_size > attachment_size_limit
      if attachment_too_big:
          note = ATTACHMENT_TOO_BIG_TEMPLATE % (attachment_size,
                                                attachment_size_limit)
          msg_plain += note
          msg_html += note

    msgRoot.attach(MIMEText(msg_plain, 'plain'))
    msgRoot.attach(MIMEText("<HTML>" + msg_html + "</HTML>", 'html'))

    tmpMsgRoot = msgRoot
    msgRoot = MIMEMultipart('related')
    msgRoot.attach(tmpMsgRoot)

    if report_days == 1:
        a = "Daily"
    elif report_days == 7:
        a = "Weekly"
    else:
        a = "Monthly"
    msgRoot['Subject'] = _('New %s %s Reports Available') % (company_name, a)
    msgRoot['From'] = sender
    msgRoot['To'] = receiver

    part = MIMEBase('application', "pdf")
    part.set_payload(open(file, 'rb').read())
    Encoders.encode_base64(part)
    part.add_header('Content-Disposition', 'attachment; filename="reports-%s-%s.pdf"'
                    % (h['date_end'].replace('/', '_'),a))
    msgRoot.attach(part)

    if zip_file and not attachment_too_big:
        part = MIMEBase('application', "zip")
        part.set_payload(open(zip_file, 'rb').read())
        Encoders.encode_base64(part)
        part.add_header('Content-Disposition', 'attachment; filename="reports-%s.zip"'
                        % (h['date_end'].replace('/', '_'),a))
        msgRoot.attach(part)

    smtp = smtplib.SMTP()
    smtp.connect('localhost')
    logger.info("Emailing out: from '%s' to '%s'" % (sender, receiver))
    smtp.sendmail(sender, receiver, msgRoot.as_string())
    smtp.quit()

def __get_mail_info():
    conn = sql_helper.get_connection()

    report_email = None
    receivers = []

    try:
        curs = conn.cursor()

        curs.execute("""\
SELECT from_address FROM settings.u_mail_settings
""")

        row = curs.fetchone()
        if row:
            report_email = row[0]

        curs.execute("""\
SELECT count(*) FROM settings.n_reporting_settings
JOIN u_node_persistent_state USING (tid)
WHERE target_state = 'running' OR target_state = 'initialized'
""")
        row = curs.fetchone()
        if row:
            count = row[0]
            if count == 0:
                # reports is not installed
                receivers = []

            curs.execute("SELECT report_email FROM settings.u_mail_settings")
            row = curs.fetchone()
            if row:
                receiver_str = row[0]

            if not receiver_str or receiver_str.strip() == '':
                receivers = []
            else:
                receivers = receiver_str.split(',')

        conn.commit()
    except:
        conn.rollback();
        logger.warn('could not get mail info', exc_info=True)

    return (receivers, report_email)

def __get_branding_info():
    company = "Untangle"

    if (os.path.isfile("/etc/untangle/oem/oem.py")):
        sys.path.append("/etc/untangle/oem")
        import oem
        company = oem.oemName()

    conn = sql_helper.get_connection()

    try:
        curs = conn.cursor()

        curs.execute("""\
SELECT company_name FROM settings.uvm_branding_settings""")

        row = curs.fetchone()

        if row:
            company = row[0]
    except Exception, e:
        pass
    finally:
        conn.commit()

    return company

def __get_url(date):
    rv = None

    try:
        f = open( "%s/usr/share/untangle/conf/networking.sh" % PREFIX )
        for line in f:
            if ( line.startswith( "UVM_PUBLIC_URL=" )):
                public_host = line.replace( "UVM_PUBLIC_URL=", "" )
                public_host = public_host.replace( "\"", "" )
                public_host = public_host.strip()
                rv = 'https://%s/reports?time=%s' \
                     % ( public_host, date.strftime(locale.nl_langinfo(locale.D_FMT)), )
                break

    except Exception, e:
        logger.warn('could not get hostname', exc_info=True)

    return rv

def __get_report_users():
    rv = set()

    conn = sql_helper.get_connection()
    try:
        curs = conn.cursor()
        curs.execute('SELECT email FROM settings.u_user WHERE reports_access IS TRUE')

        for r in curs.fetchall():
            rv.add(r[0])

    except Exception, e:
        logger.warn('could not get hostname', exc_info=True)
    finally:
        conn.commit()

    return rv

def __make_zip_file(end_date, report_days, mail_reports):
    start_date = end_date - mx.DateTime.DateTimeDelta(report_days)

    tmp_dir = tempfile.mkdtemp()
    base_dir = '%s/reports' % tmp_dir
    os.mkdir(base_dir)

    for r in mail_reports:
        report_name = r.name
        report_dir = '%s/%s' % (base_dir, report_name)
        os.mkdir(report_dir)

        empty = True

        for s in r.sections:
            if isinstance(s, reports.DetailSection):
                filename = '%s/%s.csv' % (report_dir, s.name)
                s.write_csv(filename, start_date, end_date)
                empty = False

        if empty:
            f = open('%s/no-reports' % report_dir, 'w')
            try:
                f.write(_('This report has no detail data.'))
            finally:
                f.close()

    os.system("""
{ pushd %s ;
  zip -r reports.zip ./reports ;
  popd ; } > /dev/null 2>&1"""
              % tmp_dir)

    return tmp_dir
