# cordova-plugin-advanced-geolocation - Changelog

## Version 0.5.0 - July 13, 2016

No breaking changes.

**Enhancements**
* Disable cellular data configuration option if Android API Level is less than v17. This functionality is not available on those phones and could cause app to crash.
* Updates docs. Added notices that Android N will have breaking changes in terms of how GPS is implemented.

## Version 0.4.0 - May 6, 2016

No breaking changes.

**Enhancements**
* Updated API docs to correctly reflect what data is returns by GPS and Network.

**Bug Fixes**
* GPSController no longer returns a fake location if parsedLocation is null.
* NetworkController also no longer returns a fake location if parsedLocation is null. 

## Version 0.3.1 - May 4, 2016

No breaking changes.

**Bug Fixes**
* Adds missing CellLocationController to plugin.xml

## Version 0.3.0 - April 22, 2016

No breaking changes.

**Enhancements**
* Closes #2 - Added ability to access cell tower meta-data
* Various doc improvements

## Version 0.2.0 - April 19, 2016

No breaking changes.

**Enhancements**
* Various doc improvements

**Bug Fixes**
* Closes #3 - Bug in `map.js` throwing null values.