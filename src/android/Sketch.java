package com.bendspoons.cordova.sketch;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaArgs;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.LOG;
import org.apache.cordova.PluginResult;
import org.apache.cordova.PluginResult.Status;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

/**
 * Created by jt on 29/03/16.
 */
public class Sketch extends CordovaPlugin {
    private static final String TAG = Sketch.class.getSimpleName();

    private static final int SKETCH_REQUEST_CODE = 0x0010;
    private static final int ANNOTATION_REQUEST_CODE = 0x0100;

    private DestinationType destinationType;
    private EncodingType encodingType;
    private InputType inputType;
    private String inputData;
    private CallbackContext callbackContext;

    private Integer showColorSelect = 1;
    private Integer showStrokeWidthSelect = 1;
    private Integer setStrokeWidth = 9;
    private String toolbarBgColor = "#f5f6f6";
    private String toolbarTextColor = "#007aff";
    private String textValues;

    @Override
    public boolean execute(String action, CordovaArgs args, CallbackContext callbackContext) throws JSONException {
        LOG.e(TAG, "Set showColorSelect with = " + args);
        if (!action.equals("getSketch")) {
            callbackContext.sendPluginResult(
                    new PluginResult(Status.INVALID_ACTION, "Unsupported action: " + action));
            return false;
        }

        try {
            JSONObject options = args.getJSONObject(0);
            LOG.e(TAG, "options = " + options);

            if (options.has("text")) {
                JSONObject tmpText = options.getJSONObject("text");
                this.textValues = tmpText.toString();
                LOG.e(TAG, "Set tmpText with = " + this.textValues);
            } else {
                this.textValues = "none";
                LOG.e(TAG, "Set showColorSelect DEFAULT = true");
            }

            if (options.has("showColorSelect")) {
                this.showColorSelect = options.getInt("showColorSelect");
                LOG.e(TAG, "Set showColorSelect with = " + this.showColorSelect);
            } else {
                this.showColorSelect = 1;
                LOG.e(TAG, "Set showColorSelect DEFAULT = true");
            }

            if (options.has("showStrokeWidthSelect")) {
                this.showStrokeWidthSelect = options.getInt("showStrokeWidthSelect");
                LOG.e(TAG, "Set showStrokeWidthSelect with = " + this.showStrokeWidthSelect);
            } else {
                this.showStrokeWidthSelect = 1;
                LOG.e(TAG, "Set showStrokeWidthSelect DEFAULT = true");
            }

            if (options.has("setStrokeWidth")) {
                Boolean androidOverride = false;
                if (options.has("setAndroidStrokeWidth")) {
                    if (options.getInt("setAndroidStrokeWidth") > 0) {
                        androidOverride = true;
                        this.setStrokeWidth = options.getInt("setAndroidStrokeWidth");
                        LOG.e(TAG, "Set setStrokeWidth setAndroidStrokeWidth with = " + ""+this.setStrokeWidth);
                    }
                }

                if(!androidOverride) {
                    if (options.getInt("setStrokeWidth") > 0) {
                        this.setStrokeWidth = options.getInt("setStrokeWidth");
                        LOG.e(TAG, "Set setStrokeWidth with = " + ""+this.setStrokeWidth);
                    } else {
                        this.setStrokeWidth = 9;
                        LOG.e(TAG, "Set setStrokeWidth ERROR VALUE DEFAULT = 9");
                    }
                }
            } else {
                this.setStrokeWidth = 9;
                LOG.e(TAG, "Set setStrokeWidth DEFAULT = 9");
            }



            if (options.has("toolbarBgColor")) {
                if (options.getString("toolbarBgColor").length() == 7) {
                    this.toolbarBgColor = options.getString("toolbarBgColor");
                    LOG.e(TAG, "Set toolbarBgColor with = " + this.toolbarBgColor);
                } else {
                    //this.toolbarBgColor = "#f5f6f6";
                    LOG.e(TAG, "Set toolbarBgColor ERROR VALUE DEFAULT = " + this.toolbarBgColor);
                }
            } else {
                //this.toolbarBgColor = "#f5f6f6";
                LOG.e(TAG, "Set toolbarBgColor DEFAULT = " + this.toolbarBgColor);
            }

            if (options.has("toolbarTextColor")) {
                if (options.getString("toolbarTextColor").length() == 7) {
                    this.toolbarTextColor = options.getString("toolbarTextColor");
                    LOG.e(TAG, "Set toolbarTextColor with = " + this.toolbarTextColor);
                } else {
                    //this.toolbarTextColor = "#007aff";
                    LOG.e(TAG, "Set toolbarTextColor ERROR VALUE DEFAULT = " + this.toolbarTextColor);
                }
            } else {
                //this.toolbarTextColor = "#007aff";
                LOG.e(TAG, "Set toolbarTextColor DEFAULT = " + this.toolbarTextColor);
            }

            int opt = options.getInt("destinationType");
            if (opt >= 0 && opt < DestinationType.values().length) {
                this.destinationType = DestinationType.values()[opt];
            } else {
                callbackContext.error("Invalid destinationType");
                return false;
            }

            opt = options.getInt("encodingType");
            if (opt >= 0 && opt < EncodingType.values().length) {
                this.encodingType = EncodingType.values()[opt];
            } else {
                callbackContext.error("Invalid encodingType");
                return false;
            }

            opt = options.getInt("inputType");
            if (opt >= 0 && opt < InputType.values().length) {
                this.inputType = InputType.values()[opt];
            } else {
                callbackContext.error("Invalid inputType");
                return false;
            }

            if (this.inputType != InputType.NO_INPUT) {
                String inputData = options.getString("inputData");

                if (inputData == null || inputData.isEmpty()) {
                    callbackContext.error("input data not given");
                    return false;
                }
                this.inputData = inputData;
            } else {
                this.inputData = null;
            }

            if (this.cordova != null) {
                if (this.inputData != null && !this.inputData.isEmpty()) {
                    doAnnotation();
                } else {
                    doSketch();
                }
            }

            this.callbackContext = callbackContext;
            return true;
        } catch (JSONException e) {
            callbackContext.sendPluginResult(new PluginResult(Status.JSON_EXCEPTION, e.getMessage()));
            return false;
        }
    }

    private void doSketch() {
        this.cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                final Intent touchDrawIntent = new Intent(Sketch.this.cordova.getActivity(), TouchDrawActivity.class);

                touchDrawIntent.putExtra(TouchDrawActivity.BACKGROUND_IMAGE_TYPE,
                        TouchDrawActivity.BackgroundImageType.COLOUR.ordinal());
                touchDrawIntent.putExtra(TouchDrawActivity.BACKGROUND_COLOUR, "#FFFFFF");

                touchDrawIntent.putExtra("showColorSelect", Sketch.this.showColorSelect.toString());
                touchDrawIntent.putExtra("showStrokeWidthSelect", Sketch.this.showStrokeWidthSelect.toString());
                touchDrawIntent.putExtra("setStrokeWidth", Sketch.this.setStrokeWidth.toString());

                touchDrawIntent.putExtra("toolbarBgColor", Sketch.this.toolbarBgColor);
                touchDrawIntent.putExtra("toolbarTextColor", Sketch.this.toolbarTextColor);

                if(Sketch.this.textValues != "none") {
                    touchDrawIntent.putExtra("textValues", Sketch.this.textValues);
                }

                if (Sketch.this.encodingType == EncodingType.PNG) {
                    touchDrawIntent.putExtra(TouchDrawActivity.DRAWING_RESULT_ENCODING_TYPE,
                            Bitmap.CompressFormat.PNG.ordinal());
                } else if (Sketch.this.encodingType == EncodingType.JPEG) {
                    touchDrawIntent.putExtra(TouchDrawActivity.DRAWING_RESULT_ENCODING_TYPE,
                            Bitmap.CompressFormat.JPEG.ordinal());
                }

                Sketch.this.cordova.getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Sketch.this.cordova.startActivityForResult(Sketch.this, touchDrawIntent, SKETCH_REQUEST_CODE);
                    }
                });
            }
        });
    }

    private void doAnnotation() {
        this.cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                final Intent touchDrawIntent = new Intent(Sketch.this.cordova.getActivity(), TouchDrawActivity.class);

                if (Sketch.this.inputType == InputType.DATA_URL) {
                    touchDrawIntent.putExtra(TouchDrawActivity.BACKGROUND_IMAGE_TYPE,
                            TouchDrawActivity.BackgroundImageType.DATA_URL.ordinal());
                } else if (Sketch.this.inputType == InputType.FILE_URI) {
                    Uri inputUri = Uri.parse(inputData);
                    String scheme = (inputUri != null && inputUri.getScheme() != null) ? inputUri.getScheme() : "";

                    if (scheme.equals(ContentResolver.SCHEME_CONTENT)) {
                        // Workaround for CB-9548 (https://issues.apache.org/jira/browse/CB-9548)
                        //  The Cordova camera plugin can sometimes return a content URI instead of a file URI
                        //  when the image is selected from the photo gallery.
                        //
                        //  However, the TouchDrawActivity can only accept a file URI or a data URI for the
                        //  background image. So, we need to read the background image data and pass it in a
                        //  format which can be handled by the TouchDrawActivity.

                        InputStream inStream = null;
                        try {
                            // Write background image to a temporary file and pass it as a file URL because
                            // there is no reliable way to get a file path from a content URI
                            // (http://stackoverflow.com/a/19985374)
                            ContentResolver contentResolver = Sketch.this.cordova.getActivity().getContentResolver();
                            inStream = contentResolver.openInputStream(inputUri);

                            if (inStream != null) {
                                File file = new File(Sketch.this.cordova.getActivity().getCacheDir(), UUID.randomUUID().toString());
                                FileOutputStream outStream = new FileOutputStream(file);
                                byte[] data = new byte[1024];
                                int bytesRead;

                                while ((bytesRead = inStream.read(data, 0, data.length)) != -1) {
                                    outStream.write(data, 0, bytesRead);
                                }
                                outStream.flush();
                                outStream.close();

                                Sketch.this.inputData = "file://" + file.getAbsolutePath();
                                touchDrawIntent.putExtra(TouchDrawActivity.BACKGROUND_IMAGE_TYPE,
                                        TouchDrawActivity.BackgroundImageType.FILE_URL.ordinal());
                            }
                        } catch (IOException e) {
                            String message = "Failed to read image data from " + inputUri;
                            LOG.e(TAG, message);
                            e.printStackTrace();

                            Sketch.this.callbackContext.error(message + ": " + e.getLocalizedMessage());
                            return;
                        } finally {
                            if (inStream != null) {
                                try {
                                    inStream.close();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }

                    } else if (scheme.equals(ContentResolver.SCHEME_FILE)) {
                        touchDrawIntent.putExtra(TouchDrawActivity.BACKGROUND_IMAGE_TYPE,
                                TouchDrawActivity.BackgroundImageType.FILE_URL.ordinal());
                    } else {
                        String message = "invalid scheme for inputData: " + inputData ;
                        File file = new File(inputData);

                        LOG.d(TAG, message);
                        if (file.exists() && !file.isDirectory()) {
                            touchDrawIntent.putExtra(TouchDrawActivity.BACKGROUND_IMAGE_TYPE,
                                    TouchDrawActivity.BackgroundImageType.FILE_URL.ordinal());
                            inputData = "file://" + file.getAbsolutePath();
                        } else {
                            Sketch.this.callbackContext.error(message);
                            return;
                        }
                    }
                }

                if (Sketch.this.encodingType == EncodingType.PNG) {
                    touchDrawIntent.putExtra(TouchDrawActivity.DRAWING_RESULT_ENCODING_TYPE,
                            Bitmap.CompressFormat.PNG.ordinal());
                } else if (Sketch.this.encodingType == EncodingType.JPEG) {
                    touchDrawIntent.putExtra(TouchDrawActivity.DRAWING_RESULT_ENCODING_TYPE,
                            Bitmap.CompressFormat.JPEG.ordinal());
                }

                touchDrawIntent.putExtra("showColorSelect", Sketch.this.showColorSelect.toString());
                touchDrawIntent.putExtra("showStrokeWidthSelect", Sketch.this.showStrokeWidthSelect.toString());
                touchDrawIntent.putExtra("setStrokeWidth", Sketch.this.setStrokeWidth.toString());

                touchDrawIntent.putExtra("toolbarBgColor", Sketch.this.toolbarBgColor);
                touchDrawIntent.putExtra("toolbarTextColor", Sketch.this.toolbarTextColor);

                touchDrawIntent.putExtra(TouchDrawActivity.BACKGROUND_IMAGE_URL, inputData);
                Sketch.this.cordova.getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Sketch.this.cordova.startActivityForResult(Sketch.this, touchDrawIntent, ANNOTATION_REQUEST_CODE);
                    }
                });
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, final Intent intent) {
        if (resultCode == Activity.RESULT_CANCELED) {
            this.callbackContext.success("");
            return;
        }

        if (resultCode == Activity.RESULT_OK && this.cordova != null) {
            this.cordova.getThreadPool().execute(new Runnable() {
                @Override
                public void run() {
                    saveDrawing(intent);
                }
            });
            return;
        }

        if (resultCode == TouchDrawActivity.RESULT_TOUCHDRAW_ERROR) {
            Bundle extras = intent.getExtras();
            String errorMessage = "Failed to generate sketch.";

            if (extras != null) {
                errorMessage += " " + extras.getString(TouchDrawActivity.DRAWING_RESULT_ERROR);
            }

            this.callbackContext.error(errorMessage);
        }
    }

    private void saveDrawing(Intent intent) {
        Bundle extras = intent.getExtras();
        byte[] drawingData = null;
        String output = null;

        if (extras != null &&
                extras.containsKey(TouchDrawActivity.DRAWING_RESULT_PARCELABLE)) {
            drawingData = extras.getByteArray(TouchDrawActivity.DRAWING_RESULT_PARCELABLE);
        }

        if (drawingData == null || drawingData.length == 0) {
            LOG.e(TAG, "Failed to read sketch result from activity");
            this.callbackContext.error("Failed to read sketch result from activity");
            return;
        }

        try {
            String ext = "";

            if (encodingType == EncodingType.JPEG) {
                ext = "jpeg";
            } else if (encodingType == EncodingType.PNG) {
                ext = "png";
            }

            if (destinationType == DestinationType.DATA_URL) {
                output = "data:image/" + ext + ";base64," + Base64.encodeToString(drawingData, Base64.DEFAULT);
            } else if (destinationType == DestinationType.FILE_URI) {
                // Save the drawing to the app's cache dir
                String fileName = String.format("sketch-%s.%s", UUID.randomUUID(), ext);
                File filePath = new File(this.cordova.getActivity().getCacheDir(), fileName);

                FileOutputStream fos = new FileOutputStream(filePath);
                fos.write(drawingData);
                fos.close();

                // Add the drawing to photo gallery
                String appName = getApplicationLabelOrPackageName(this.cordova.getActivity());
                String mediaStoreUrl = MediaStore.Images.Media.insertImage(this.cordova.getActivity().getContentResolver(),
                        filePath.getAbsolutePath(), fileName,
                        (appName != null && !appName.isEmpty()) ? "Generated by " + appName : "");

                LOG.d(TAG, (mediaStoreUrl != null) ?
                        "Drawing saved to media store: " + mediaStoreUrl :
                        "Failed to save drawing to media store");

                // We need to return the file saved to the cache dir instead of the
                // file in the photo gallery because the Cordova file plugin cannot open content URIs
                output = "file://" + filePath.getAbsolutePath();
                LOG.d(TAG, "Drawing saved to: " + output);
            }
        } catch(Exception e) {
            LOG.e(TAG, "Error generating output from drawing: " + e.getMessage());

            this.callbackContext.error("Failed to generate output from drawing: "
                    + e.getMessage());
            return;
        }

        this.callbackContext.success(output);
    }

    // Based on http://stackoverflow.com/a/16444178
    private String getApplicationLabelOrPackageName(Context context) {
        PackageManager pm = context.getPackageManager();
        ApplicationInfo appInfo = context.getApplicationInfo();

        if (pm == null || appInfo == null) {
            return "";
        }

        try {
            String label = (String) pm.getApplicationLabel(pm.getApplicationInfo(appInfo.packageName, 0));
            if (label != null && !label.isEmpty()) {
                return label;
            }
        } catch (PackageManager.NameNotFoundException e) {
            LOG.w(TAG, "Failed to determine app label");
        }

        return appInfo.packageName;
    }

    enum DestinationType {
        DATA_URL,
        FILE_URI
    }

    enum EncodingType {
        PNG,
        JPEG
    }

    enum InputType {
        NO_INPUT,
        DATA_URL,
        FILE_URI
    }
}
