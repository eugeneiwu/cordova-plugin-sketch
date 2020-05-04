# cordova-plugin-sketch

This plugin defines a navigator.Sketch object, which supplies an interface to launch a sketch pad, allowing the user to draw something or annotate a picture.

## Installation

```sh
cordova plugin add https://github.com/bendspoons/cordova-plugin-sketch.git
```

## Sketch.getSketch

Displays a dialog which will allow user draw something or annotate a picture based on input options.

```javascript
navigator.sketch.getSketch(onSuccess, onError, options);
```

## Description

Displays a drawable pad based upon the supplied options, then allow user to draw something or annotate the picture, change the width or color of the pen, erase the wrong drawing before finish. The available options include:

- destinationType (optional)
- encodingType (optional)
- inputType (optional)
- inputData (optional)

NEW

- showStrokeWidthSelect (optional)
- showColorSelect (optional)
- setStrokeWidth (optional)
- toolbarBgColor (optional)
- toolbarTextColor (optional)

The destinationType is the return image type, the available options are: DATA_URL, FILE_URI. If DestinationType is DATA_URL the plugin will return a string. If the DestinationType is FILE_URI, the plugin will return a file URI. The default is DATA_URL.

The EncodingType is the return image encode type, the available options are: JPEG, PNG. The plugin will encode the return image based on EncodingType. If you didn't define EncodingType, the plugin will return a PNG format image.

The inputType describes how to use the data provided in inputData. If you provide a Data URI or file URI as background picture, user can make annotation on the picture, if you provide nothing, user can draw a picture on a blank background, e.g. a signature.

The inputData is a string or file URI of the background picture, depending on inputType.

showStrokeWidthSelect 0 (= do not show) or 1 (= show), defaults to 1

showColorSelect 0 (= do not show) or 1 (= show), defaults to 1

setStrokeWidth 1 - 24 (theoretically even higher possible, but system only display 1 - 24), defaults to 1

toolbarBgColor Color of the Toolbars (top, bottom), Hex Color like #FA2000, no short like #FFF allowed, defaults to #000000

toolbarBgColor Color of the Toolbars (top, bottom), Hex Color like #FFFFFF, no short like #FFF allowed, defaults to #FFFFFF


When the user presses "done", returns the image of the user sketched as an Data URI or file URI depending on input DestinationType.

If the user presses "cancel", the result is `null`.

## Supported Platforms

- Android 4.0 +
- iOS 8.0 +
- <strike>Windows 8.1 +</strike> (only original functions from blinkmobile!)

## Example

Create a button on your page

```html
<button id="cordova-plugin-sketch-open">Sketch</button>
<img id="myImage" height="400" width="600"/>
```

Then add click event

```javascript
document.getElementById("cordova-plugin-sketch-open").addEventListener("click", getSketch, false);

function getSketch(){
  var image = document.getElementById('myImage');
  navigator.sketch.getSketch(onSuccess, onFail, {
    destinationType: navigator.sketch.DestinationType.DATA_URL,
    encodingType: navigator.sketch.EncodingType.JPEG,
    inputType : navigator.sketch.InputType.FILE_URI,
    inputData : image.src
  });
}

function onSuccess(imageData) {
  if(imageData == null) { return; }
  setTimeout(function() {
  // do your thing here!
    var image = document.getElementById('myImage');
    if(imageData.indexOf("data:image") >= 0 ) {
      image.src = imageData;
    } else {
      image.src = "data:image/png;base64," + imageData;
    }
  }, 0);
}

function onFail(message) {
    setTimeout(function() {
      console.log('plugin message: ' + message);
    }, 0);
}
```
