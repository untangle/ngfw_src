import StringIO
import base64
import fileinput
import gettext
import locale
import os
import os.path
import popen2
import re
import shutil
import smtplib
import string
import tempfile

from email.MIMEMultipart import MIMEMultipart
from email.MIMEText import MIMEText
from email.MIMEImage import MIMEImage

_ = gettext.gettext
def N_(message): return message

class HtmlWriter():

    def __init__(self):
        self.__dir = tempfile.mkdtemp('HtmlWriter')

        self.__body_file = open('%s/body.html' % self.__dir, 'w')

        self.__table_of_contents = []
        self.__node_info = {}
        self.__image_num = 0

    def generate(self, date):
        self.__write_toc(date)

        input_files = ['%s/toc.html' % self.__dir,
                       '%s/body.html' % self.__dir]

        output_file = '%s/index.html' % self.__dir

        os.system('cat %s | tidy -i >%s 2>&1' % (string.join(input_files, ' '),
                                                 output_file))

        for f in input_files:
            os.remove(f)

    def mail(self):
        strFrom = 'amread@untangle.com'
        strTo = 'amread@untangle.com'

        msgRoot = MIMEMultipart('related')
        msgRoot['Subject'] = 'test message' # XXX
        msgRoot['From'] = strFrom
        msgRoot['To'] = strTo
        msgRoot.preamble = 'This is a multi-part message in MIME format.'

        msgAlternative = MIMEMultipart('alternative')
        msgRoot.attach(msgAlternative)

        msgText = MIMEText('This is the alternative plain text message.')
        msgAlternative.attach(msgText)

        index_file = open('%s/%s' % (self.__dir, 'index.html'), 'r')
        msgText = MIMEText(index_file.read(), 'html')
        msgAlternative.attach(msgText)

        for f in os.listdir(self.__dir):
            if f.endswith('.png'):
                # This example assumes the image is in the current directory
                fp = open('%s/%s' % (self.__dir, f), 'rb')
                msgImage = MIMEImage(fp.read())
                fp.close()
                # Define the image's ID as referenced above
                msgImage.add_header('Content-ID', f)
                msgRoot.attach(msgImage)

        smtp = smtplib.SMTP()
        smtp.connect('mail.untangle.com')
        smtp.sendmail(strFrom, strTo, msgRoot.as_string())
        smtp.quit()

    def cleanup(self):
        os.removedirs(self.__dir)

    def close(self):
        self.__body_file.close();

    def write(self, str):
        self.__body_file.write(str)

    def encode_image(self, path, base=None):
        b, ext = os.path.splitext(path)
        if not ext or ext == '':
            ext = '.png'
        cid = '%s%s' % (self.__image_num, ext)
        self.__image_num = self.__image_num + 1

        if base:
            filename = '%s/%s' % (base, path)
        else:
            filename = path

        shutil.copyfile(filename, '%s/%s' % (self.__dir, cid))

        return 'cid:%s' % cid

    def add_node_anchor(self, name):
        self.__table_of_contents.append(name)

        return self.get_node_info(name)

    def get_node_info(self, name):
        ni = self.__node_info.get(name, None)

        if not ni:
            r, w = popen2.popen2("apt-cache show untangle-vm")
            w.close()

            desc_icon = None
            display_name = None
            cid = None

            for l in r.readlines():
                if re.search('^Desc-Icon:', l):
                    desc_icon = l.split(':', 1)[1]
                elif desc_icon and re.search('^ ', l):
                    desc_icon =  desc_icon + l
                elif desc_icon:
                    input = StringIO.StringIO(desc_icon)
                    fd, fname = tempfile.mkstemp()
                    output = os.fdopen(fd, 'w')
                    base64.decode(input, output)
                    output.close()
                    cid = self.encode_image(fname)
                    os.remove(fname)

                    if re.search('^Display-Name:', l):
                        display_name = l.split(': ', 1)

            ni = NodeInfo(name, display_name, cid)
            self.__node_info[name] = ni

        return ni

    def __write_toc(self, date):
        toc_file = open('%s/toc.html' % self.__dir, 'w')

        day_of_week = date.strftime('%A')
        date_string = date.strftime(locale.nl_langinfo(locale.D_FMT))

        toc_file.write("""\
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">

  <head>
    <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
    <title>%s</title>
    <style type="text/css">
    </style>
  </head>

  <body style="background-color:#efefef;margin-top:0;font-family: Tahoma,Arial,sans-serif; ">
    <div style="margin:0 auto;width:700px;padding:20px 30px;background-color:#FFF;">
      <div style="margin-bottom:20px;  font-size: 20px;">
        <div style="margin-bottom:5px;">
          <div style="float:left;font-size:24px;font-weight:bold;">%s</div><div style="float:right;color:#009933">%s, %s</div>
        </div>
        <br/><br/>

        <table cellspacing="0" cellpadding="4"style="width:100%%;border:1px #ccc solid;font-size:14px;">
          <thead><tr><th colspan="2" align="left" style="text-align:left;background-color:#EFEFEF">%s</th></tr></thead>
          <tbody>
""" % (_('Reports'), _('Reports'), day_of_week, date_string, _('Report Items')))

        row_num = 0

        for name in self.__table_of_contents:
            if name == 'untangle-vm':
                name = 'untangle-node-reporting'

            ni = self.get_node_info(name)

            if row_num % 2 == 0:
                toc_file.write("""\
            <tr>
              <td style="width: 44px"><img alt="" src="%s"style="width:42px;height:42px;"/></td>
              <td><a href="%s">%s</a></td>
            </tr>""" % (ni.cid, self.add_node_anchor(name), ni.display_name))
            else:
                toc_file.write("""\
            <tr style="background-color:#EFEFEF;">
              <td style="width: 44px;background-color:#EFEFEF;"><img alt="" src="web-filter-small.png" width="42" height="42"/></td>
              <td><a href="#webfilter">Web Filter</a></td>
            </tr>""")

            row_num = row_num + 1


        toc_file.write("""\
          </tbody>
        </table>
      </div>
""")

        toc_file.close()

    def write_footer(self, writer):
        writer.write("""
</div>
</td>
</tr>
</tbody>
</table>
</div>
</body>

</html>
""")

class NodeInfo():
    def __init__(self, name, display_name, cid):
        self.__name = name
        self.__display_name = display_name
        self.__cid = cid

    @property
    def display_name(self):
        return self.__display_name

    @property
    def cid(self):
        return self.__cid

    @property
    def anchor(self):
        return '#%s' % self.__name
