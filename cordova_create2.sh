#!/bin/bash


mkdir -p assets/www/plugins/cordova-plugin-advanced-geolocation/www
cp www/AdvancedGeolocation.js assets/www/plugins/cordova-plugin-advanced-geolocation/www/
cp sample/map.js assets/www/js/
cp sample/blue-pin.png assets/www/img/
cp sample/green-pin.png assets/www/img/
cp sample/sample-map.html assets/www/
cp -r src/ java/