#!/usr/bin/python

import copy
import getopt
import json
import os
import re
import sys
import time

from selenium import webdriver
import selenium.webdriver.chrome.service as ChromeDriverService
import selenium.webdriver.chrome.options as ChromeDriverOptions
from selenium.webdriver.support.ui import WebDriverWait
from selenium.common.exceptions import TimeoutException

from timeit import default_timer as timer

from PIL import Image

Debug = False

class WebBrowser:
    service = None
    driver = None

    temp_directory = "/tmp/webbrowser"
    chrome_driver = "/usr/lib/chromium/chromedriver"
    chrome_browser = "/usr/bin/chromium"

    xvfb_command = None

    cropped_image = None

    def __init__(self, resolution="1024x768", sequence="1", screen="5"):
        self.resolution = resolution
        self.sequence = sequence
        self.screen = screen

        self.xvfb_command = "Xvfb" + " :" + self.sequence + " -screen " + self.screen + " " + resolution + "x8"
        # print self.xvfb_command

        os.system("/usr/bin/pkill -f \"" + self.xvfb_command + "\"")
        os.system("nohup " + self.xvfb_command + " >/dev/null 2>&1 &")

        self.service = ChromeDriverService.Service( 
            executable_path=self.chrome_driver, 
            service_args=[
                "--log-path=" + self.temp_directory + "/log.txt", 
                "--verbose"
            ], 
            env={"DISPLAY": ":" + self.sequence + "." + self.screen} 
        )
        self.service.start()

        self.capabilities = {
            'chrome.binary': self.chrome_browser,
            'chromeOptions': {
                "args" : [
                    "--no-sandbox",
                    "--ash-host-window-bounds=" + resolution,
                    "--user-data-dir=" + self.temp_directory
                ]
            }
        }
        self.driver = webdriver.Remote( self.service.service_url, self.capabilities)

    def __del__(self):
        self.driver.quit()
        self.service.stop()
        os.system("/usr/bin/pkill -f \"" + self.xvfb_command + "\"")

    def auth_cookie_exists(self):
        for cookie in self.driver.get_cookies():
            if "auth-" in cookie["name"]:
                return cookie
        return None

    def authenticate(self, url, username, password):
        self.go(url)
        usernameInput = self.driver.find_element_by_id("username")
        passwordInput = self.driver.find_element_by_id("password")
        button = self.driver.find_element_by_tag_name("button")

        usernameInput.clear()
        usernameInput.send_keys(username)
        passwordInput.clear()
        passwordInput.send_keys(password)
        button.click()

        try:
            WebDriverWait(self.driver,10).until(
                lambda x: self.auth_cookie_exists()
            )
        except TimeoutException:
            print "Unable to obtain authentication cookie"

    def go(self, url):
        # print url
        self.driver.get(url)

    def wait_for_progressbar(self):
        """
        Wait for all progress bars to be hidden
        """
        last_progressbar_count = 0
        max_sleep = 20
        while max_sleep > 0:
            elements = self.driver.find_elements_by_css_selector("div[id^=loadmask][role=progressbar]")
            if len(elements) != last_progressbar_count:
                last_progressbar_count = len(elements)
                continue

            all_none_displayed = True
            for element in elements:
                try:
                    style = element.get_attribute("style")
                    if not "display: none;" in style:
                        all_none_displayed = False
                except Exception:
                    pass
            if all_none_displayed is True:
                break
            time.sleep(.5)
            max_sleep -= 1

    def crop(self, screen, searches, matches):

        if len(searches) != len(matches):
            print "searches length != matches length"
            return False

        url = screen["url"]
        replacements = {}
        if "path" in screen:
            replacements["%path%"] = screen["path"]
        else:
            path = url.replace("-", "");
            replacements["%path%"] = url[url.find('#') + 1:].replace("_", "").replace("/", "-").replace("apps-1","app")

        images = {}
        for index, value in enumerate(matches):
            images[index] = None

            for key in value.keys():
                for rkey in replacements.keys():
                    if rkey in value[key]:
                        value[key] = value[key].replace(rkey, replacements[rkey]) 
                if type(value[key]) in [str, unicode]:
                    value[key] = re.compile( ur''+value[key].replace("\\\\", "\\"), re.UNICODE )

            for rkey in replacements.keys():
                if rkey in searches[index]:
                        searches[index] = searches[index].replace(rkey, replacements[rkey]) 

        max_width = 0
        max_height = 0

        self.driver.get_screenshot_as_file(self.temp_directory + "/temp_crop.png")
        img = Image.open(self.temp_directory + "/temp_crop.png")

        for search in searches:
            for element in self.driver.find_elements_by_css_selector(search):
                try:
                    width = element.size['width']
                    height = element.size['height']
                    if width == 0 and height == 0:
                        continue

                    for index, value in enumerate(matches):
                        if not images[index] is None:
                            continue

                        match = True
                        for key in value.keys():
                            if key[0] == "!":
                                if value[key].search( element.get_attribute(key[1:]) ) is not None:
                                    match = False
                                    break
                            else:
                                if value[key].search( element.get_attribute(key) ) is None:
                                    match = False
                                    break

                        if match is True:
                            # print "cropping " + element.get_attribute("id")
                            x = int( element.location['x'] )
                            y = int( element.location['y'] )
                            # print ( x, y, x + width, y + height )
                            images[index] = img.crop( ( x, y, x + width, y + height ) )
                            # save piece for debugging....
                            # images[index].save(self.temp_directory + "/cropped" + str(index) + ".png")
                            if width > max_width:
                                max_width = width

                            max_height = max_height + height
                            break

                except Exception:
                    pass


        all_cropped_exists = True
        for id in images:
            if images[id] is None:
                print "Cannot create - missing for " + matches[id]["id"].pattern
                all_cropped_exists = False
                continue
        if all_cropped_exists is False:
            return False

        # print images
        # print max_width
        # print max_height

        self.cropped_image = Image.new("RGB", (max_width, max_height), "white" )
        x = 0
        y = 0;
        for id in images:
            if images[id] is None:
                print "Cannot create - missing for " + str(id)
                continue
            # print id
            # print images[id].size
            # print ( x, y, x + images[id].size[0], y + images[id].size[1])
            # print "pasting " + str(id)
            self.cropped_image.paste(images[id], ( x, y, x + images[id].size[0], y + images[id].size[1]) )
            y = y + images[id].size[1]

        return True

    def build_file_name_path(self, name):
        return self.temp_directory + "/" + self.resolution + "_" + name + ".png"

    def screenshot(self, name):
        if self.cropped_image != None:
            self.cropped_image.save( self.build_file_name_path(name) )
            self.cropped_image = None
        else:
            self.driver.get_screenshot_as_file( self.build_file_name_path(name) )

def usage():
    """
    Show usage
    """
    print "usage"

def main(argv):
    want_screen_name = None
    want_resolution = None

    try:
        opts, args = getopt.getopt(argv, "hs:d", ["help", "screen=", "resolution=", "debug"] )
    except getopt.GetoptError:
        usage()
        sys.exit(2)

    for opt, arg in opts:
        if opt in ( "-h", "--help"):
            usage()
            sys.exit()
        if opt in ( "-d", "--debug"):
            Debug = True
        if opt in ( "-s", "--screen"):
            want_screen_name = arg
        if opt in ( "-resolution", "--resolution"):
            want_resolution = arg

    settings_file = open( "screenshot.json" )
    settings = json.load( settings_file)
    settings_file.close()

    if want_resolution is not None:
        settings["resolutions"].append(want_resolution)

    total_screens = 0
    total_screens_failed = 0
    total_screens_created = 0
    for resolution in settings["resolutions"]:
        if ( want_resolution is not None ) and ( resolution != want_resolution ):
            print "Skipping resolution: "+resolution
            continue
        print "Using resolution: "+resolution

        web_browser = WebBrowser(resolution=resolution)

        base_url = settings["authentication"]["url"]

        if "user" in settings["authentication"]:
            web_browser.authenticate(url=base_url, username=settings["authentication"]["user"], password=settings["authentication"]["password"])

        for screen in settings["screens"]:
            total_screens += 1
            if want_screen_name is not None and want_screen_name not in screen["name"]:
                print "Skipping screen: " + screen["name"] 
                continue

            start = timer()
            print "Capturing screen: " + screen["name"] + "...",
            sys.stdout.flush()

            web_browser.go(base_url + screen["url"])
            web_browser.wait_for_progressbar()

            start = timer()
            success = True
            if "type" in screen:
                for operation in settings["types"][screen["type"]]:
                    if operation["action"] == "crop":
                        success = web_browser.crop( screen, copy.deepcopy( operation["searches"] ), copy.deepcopy( operation["matches"] ) )
                    if success is False:
                        break

            if success is True:
                web_browser.screenshot(screen["name"])
                total_screens_created += 1
            else:
                total_screens_failed += 1
                print "Cannot capture screen",

            print str(timer() - start)

        del web_browser

    print "Total screens available: %d, failed: %d, created: %d" % ( total_screens, total_screens_failed, total_screens_created)

if __name__ == "__main__":
    main( sys.argv[1:] )