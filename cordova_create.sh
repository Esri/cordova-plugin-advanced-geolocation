#!/bin/bash

set -e # Exit script on any error

# "This is a shell script for manually creating a cordova repo"
# "for the purpose of making changes or updating the github repo"

read -p "Press [Enter] to create Cordova project…"

cordova create cordova-geo com.esri.cordova.geolocation AdvancedGeolocation "$1" || exit 1

read -p "Press [Enter] to continue…"

echo "cd to new project directory"
cd cordova-geo/

echo "add android platform to project"
cordova platform add android

echo "cd to android platform directory"
cd platforms/android/

echo "clone github repo to temp dir"
git clone https://github.com/andygup/cordova-plugin-advanced-geolocation.git temp

echo "moving temp dir into android dir"
mv temp/.gitignore .
mv temp/.git .

echo "creating www directory and copy contents over"
mkdir -p assets/www/plugins/cordova-plugin-advanced-geolocation/www
cp temp/www/AdvancedGeolocation.js assets/www/plugins/cordova-plugin-advanced-geolocation/www/
cp temp/sample/map.js assets/www/js/
cp temp/sample/blue-pin.png assets/www/img/
cp temp/sample/green-pin.png assets/www/img/
cp temp/sample/sample-map.html assets/www/
cp -r src/ java/

echo "deleting temp directory"
rm -rf temp

echo "reset git"
git reset --hard HEAD # git will think you deleted all the important files
git status # most likely there will be a MainActivity.java untracked

echo "DONE! Please see shell script for additional manual tasks."

<<COMMENT

# Added permissions in AndroidManifest.xml

    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />

# Added plugin to config.xml

    <feature name="AdvancedGeolocation">
        <param name="android-package" value="com.esri.cordova.geolocation.AdvancedGeolocation" />
    </feature>

# Add reference to cordova_plugins.js

module.exports = [
    {
        "file": "plugins/cordova-plugin-advanced-geolocation/www/AdvancedGeolocation.js",
        "id": "cordova-plugin-advanced-geolocation.AdvancedGeolocation",
        "clobbers": [
            "AdvancedGeolocation"
        ]
    }
];

Wrap AdvancedGeolocation.js in

cordova.define("cordova-plugin-advanced-geolocation.AdvancedGeolocation", function(require, exports, module) {

});

AndroidManifest.xml

<uses-sdk android:minSdkVersion="14" android:targetSdkVersion="22" />

# In config.xml switch out index.html to sample-map.html
# Debug the app and see if everything works!


COMMENT
