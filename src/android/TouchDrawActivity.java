package com.bendspoons.cordova.sketch;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.view.Display;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.util.Base64;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.util.DisplayMetrics;
import android.graphics.drawable.GradientDrawable;

import org.apache.cordova.LOG;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

public class TouchDrawActivity extends Activity {
    public static final String DRAWING_RESULT_PARCELABLE = "drawing_result";
    public static final String DRAWING_RESULT_ERROR = "drawing_error";
    public static final String BACKGROUND_IMAGE_TYPE = "background_image_type";
    public static final String BACKGROUND_IMAGE_URL = "background_image_url";
    public static final String BACKGROUND_COLOUR = "background_colour";
    public static final String STROKE_WIDTH = "stroke_width";
    public static final int RESULT_TOUCHDRAW_ERROR = Activity.RESULT_FIRST_USER;
    public static final String DRAWING_RESULT_SCALE = "drawing_scale";
    public static final String DRAWING_RESULT_ENCODING_TYPE = "drawing_encoding_type";

    public int showColorSelect = 1;
    public int showStrokeWidthSelect = 1;
    // see mStrokeWidth Width public static final Integer
    // public int setStrokeWidth = 1;

    public String toolbarBgColor;
    public String toolbarTextColor;

    public JSONObject textValues;
    public String backBtn = "Back";
    public String clearBtn = "Clear";
    public String clearTitle = "Clear canvas";
    public String clearText = "Clear complete canvas?";
    public String colorBtn = "Stroke color";
    public String strokeBtn = "Stroke width";
    public String saveBtn = "Save";

    private Paint mPaint;
    private int mStrokeWidth = 9;
    private int mScale = 75;
    private Bitmap mBitmap;
    private TouchDrawView mTdView;
    private BackgroundImageType mBackgroundImageType = BackgroundImageType.COLOUR;
    private String mBackgroundColor = "#FFFFFF";
    private String mBackgroundImageUrl = "";
    private Bitmap.CompressFormat mEncodingType = Bitmap.CompressFormat.PNG;
    private int a, r, g, b; //Decoded ARGB color values for the background and erasing

    // Labels and values for stroke colour and width selection buttons
    //private static final String[] STROKE_COLOUR_LABELS = {"RED", "BLUE", "GREEN", "BLACK"};
    //private static final int[] STROKE_COLOUR_VALUES = {Color.RED, Color.BLUE, Color.GREEN, Color.BLACK};

    private static final String[] STROKE_COLOUR_LABELS = {"Schwarz", "Blau", "Rot", "Magenta", "Gelb", "Cyan", "Grau", "Dunkelgrau", "Hellgrau", "Radiergummi"};
    private static final int[] STROKE_COLOUR_VALUES = {Color.BLACK, Color.BLUE, Color.RED, Color.MAGENTA, Color.YELLOW, Color.CYAN, Color.GRAY, Color.DKGRAY, Color.LTGRAY, Color.WHITE};

    private static final String[] STROKE_WIDTH_LABELS = {"1x", "2x", "3x", "4x", "5x", "6x", "7x", "8x", "9x"};
    private static final Integer[] STROKE_WIDTH_VALUES = {1,2,3,4,5,6,7,8,9};

    public enum BackgroundImageType {
        DATA_URL,
        FILE_URL,
        COLOUR
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle intentExtras = getIntent().getExtras();

        if (intentExtras != null) {
            mBackgroundImageType = BackgroundImageType.values()[
                    intentExtras.getInt(BACKGROUND_IMAGE_TYPE, BackgroundImageType.COLOUR.ordinal())];
            mBackgroundImageUrl = intentExtras.getString(BACKGROUND_IMAGE_URL, mBackgroundImageUrl);
            mBackgroundColor = intentExtras.getString(BACKGROUND_COLOUR, mBackgroundColor);
            mStrokeWidth = intentExtras.getInt(STROKE_WIDTH, mStrokeWidth);
            mScale = intentExtras.getInt(DRAWING_RESULT_SCALE, mScale);
            mEncodingType = Bitmap.CompressFormat.values()[
                    intentExtras.getInt(DRAWING_RESULT_ENCODING_TYPE, mEncodingType.ordinal())];

            showColorSelect = Integer.parseInt(intentExtras.getString("showColorSelect"));
            showStrokeWidthSelect = Integer.parseInt(intentExtras.getString("showStrokeWidthSelect"));
            mStrokeWidth = Integer.parseInt(intentExtras.getString("setStrokeWidth"));

            toolbarBgColor = intentExtras.getString("toolbarBgColor");
            toolbarTextColor = intentExtras.getString("toolbarTextColor");

            String tmpTextValues = intentExtras.getString("textValues");
            LOG.e("APP", "=> " + tmpTextValues);

            if(tmpTextValues != null) {
                try {
                    textValues = new JSONObject(tmpTextValues);
                } catch(JSONException e) {
                    LOG.e("APP", "ERROR PARSING => " + tmpTextValues);
                }

                try {
                    try {
                        backBtn = new String(textValues.getString("backBtn").getBytes("ISO-8859-1"), "UTF-8");
                        clearBtn = new String(textValues.getString("clearBtn").getBytes("ISO-8859-1"), "UTF-8");
                        clearTitle = new String(textValues.getString("clearTitle").getBytes("ISO-8859-1"), "UTF-8");
                        clearText = new String(textValues.getString("clearText").getBytes("ISO-8859-1"), "UTF-8");
                        colorBtn = new String(textValues.getString("colorBtn").getBytes("ISO-8859-1"), "UTF-8");
                        strokeBtn = new String(textValues.getString("strokeBtn").getBytes("ISO-8859-1"), "UTF-8");
                        saveBtn = new String(textValues.getString("saveBtn").getBytes("ISO-8859-1"), "UTF-8");
                    } catch(UnsupportedEncodingException e2) {
                        LOG.e("APP", e2.getMessage());
                        e2.printStackTrace();
                    }
                } catch(JSONException e) {
                    LOG.e("APP", e.getMessage());
                    e.printStackTrace();
                }
            }
        }

        if (mBackgroundImageType != BackgroundImageType.COLOUR && mBackgroundImageUrl.isEmpty()) {
            Intent drawingResult = new Intent();

            drawingResult.putExtra(DRAWING_RESULT_ERROR,
                    "Background image url not given (and background image type != colour)");
            setResult(RESULT_TOUCHDRAW_ERROR, drawingResult);
            finish();
            return;
        }

        RelativeLayout tDLayout = new RelativeLayout(this);
        tDLayout.setHapticFeedbackEnabled(true);
        tDLayout.setLayoutParams(new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT));

        LinearLayout buttonBar = createButtonBar();
        buttonBar.setId(getNextViewId());
        RelativeLayout.LayoutParams buttonBarLayoutParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        buttonBarLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        buttonBar.setLayoutParams(buttonBarLayoutParams);
        tDLayout.addView(buttonBar);

        LinearLayout toolBar = createToolBar();
        toolBar.setId(getNextViewId());
        RelativeLayout.LayoutParams toolBarLayoutParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        toolBarLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        toolBar.setLayoutParams(toolBarLayoutParams);
        tDLayout.addView(toolBar);

        FrameLayout tDContainer = new FrameLayout(this);
        RelativeLayout.LayoutParams tDViewLayoutParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
        tDViewLayoutParams.addRule(RelativeLayout.BELOW, buttonBar.getId());
        tDViewLayoutParams.addRule(RelativeLayout.ABOVE, toolBar.getId());
        tDContainer.setLayoutParams(tDViewLayoutParams);
        mTdView = new TouchDrawView(this);
        tDContainer.addView(mTdView);
        tDLayout.addView(tDContainer);

        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(tDLayout);

        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        mPaint.setColor(Color.BLACK);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setStrokeWidth(mStrokeWidth);
    }

    public LinearLayout createButtonBar() {
        LinearLayout buttonBar = new LinearLayout(this);

        Button cancelButton = new Button(this);
        //cancelButton.setText("< zurück");
        cancelButton.setText(backBtn);
        cancelButton.setTypeface(Typeface.SANS_SERIF);
        cancelButton.setBackgroundColor(Color.parseColor(toolbarBgColor));
        cancelButton.setTextColor(Color.parseColor(toolbarTextColor));
        cancelButton.setLayoutParams(new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.MATCH_PARENT, (float) 0.30));
        cancelButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                v.setPressed(true);
                cancelDrawing();
            }
        });

        Button eraseButton = new Button(this);
        //eraseButton.setText("Löschen");
        eraseButton.setText(clearBtn);
        eraseButton.setTypeface(Typeface.SANS_SERIF);
        eraseButton.setBackgroundColor(Color.parseColor(toolbarBgColor));
        eraseButton.setTextColor(Color.parseColor(toolbarTextColor));
        eraseButton.setLayoutParams(new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.MATCH_PARENT, (float) 0.30));
        eraseButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                eraseDrawing();
            }
        });

        buttonBar.addView(cancelButton);

        buttonBar.addView(eraseButton);

        return buttonBar;
    }

    public LinearLayout createToolBar() {
        LinearLayout toolBar = new LinearLayout(this);

        Button doneButton = new Button(this);
        //doneButton.setText("Speichern");
        doneButton.setText(saveBtn);
        doneButton.setTypeface(Typeface.SANS_SERIF);
        doneButton.setBackgroundColor(Color.parseColor(toolbarBgColor));
        doneButton.setTextColor(Color.parseColor(toolbarTextColor));
        doneButton.setLayoutParams(new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.MATCH_PARENT, (float) 0.30));
        doneButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                finishDrawing();
            }
        });

        if(showColorSelect == 1) {
            toolBar.addView(createColourSpinner());
        }

        if(showStrokeWidthSelect == 1) {
            toolBar.addView(createWidthSpinner());
        }

        toolBar.addView(doneButton);

        return toolBar;
    }

    public Spinner createColourSpinner() {
        //final String strokeColourLabelPrefix = "Textfarbe";
        final String strokeColourLabelPrefix = colorBtn;
        Spinner spinner = new Spinner(this);

        ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(this,
                android.R.layout.simple_spinner_dropdown_item, STROKE_COLOUR_LABELS) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView v = (TextView) super.getView(position, convertView, parent);


                v.setBackgroundColor(Color.parseColor(toolbarBgColor));
                v.setTextColor(Color.parseColor(toolbarTextColor));
                v.setTypeface(Typeface.SANS_SERIF);
                v.setText(strokeColourLabelPrefix);
                return v;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                TextView v = (TextView) super.getDropDownView(position, convertView,parent);

                //v.setTypeface(Typeface.SANS_SERIF);
                v.setBackgroundColor(STROKE_COLOUR_VALUES[position]);
                //v.setTypeface(Typeface.SANS_SERIF);
                v.setText(" ");

                if(STROKE_COLOUR_VALUES[position] != mPaint.getColor()) {
                    GradientDrawable drawable = new GradientDrawable();
                    drawable.setShape(GradientDrawable.RECTANGLE);
                    drawable.setStroke(20, Color.WHITE);
                    drawable.setColor(STROKE_COLOUR_VALUES[position]);
                    v.setBackground(drawable);
                } else {
                    GradientDrawable drawable = new GradientDrawable();
                    drawable.setShape(GradientDrawable.RECTANGLE);
                    drawable.setStroke(20, STROKE_COLOUR_VALUES[position]);
                    drawable.setColor(STROKE_COLOUR_VALUES[position]);
                    v.setBackground(drawable);
                }
                //v.setText(STROKE_COLOUR_LABELS[position]);
                return v;
            }
        };
        spinner.setAdapter(adapter);

        spinner.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                if (mPaint != null && position >= 0 && position < STROKE_COLOUR_VALUES.length) {
                    mPaint.setColor(STROKE_COLOUR_VALUES[position]);

                    //adapterView.setBackgroundColor(STROKE_COLOUR_VALUES[position]);
                    ((TextView) view).setBackgroundColor(Color.parseColor(toolbarBgColor));
                    ((TextView) view).setTextColor(Color.parseColor(toolbarTextColor));
                    ((TextView) view).setTypeface(Typeface.SANS_SERIF);
                    ((TextView) view).setText(strokeColourLabelPrefix);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
        spinner.setBackgroundColor(Color.parseColor(toolbarBgColor));
        spinner.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);

        //View v = spinner.getSelectedView();
        //((TextView)v).setTextColor(Color.parseColor(toolbarTextColor));

        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.width = (int) (getScreenWidth(this) * 0.3);

        //LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, (float) 0.30);
        spinner.setLayoutParams(layoutParams);
        spinner.setSelection(Arrays.asList(STROKE_COLOUR_LABELS).indexOf("Schwarz"));

        return spinner;
    }

    public Spinner createWidthSpinner() {
        //final String strokeWidthLabelPrefix = "Strichstärke ";
        final String strokeWidthLabelPrefix = strokeBtn;
        Spinner spinner = new Spinner(this);

        final ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(this,
                android.R.layout.simple_spinner_dropdown_item, STROKE_WIDTH_LABELS) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView v = (TextView) super.getView(position, convertView, parent);

                v.setBackgroundColor(Color.parseColor(toolbarBgColor));
                v.setTextColor(Color.parseColor(toolbarTextColor));
                v.setTypeface(Typeface.SANS_SERIF);
                //v.setText(strokeWidthLabelPrefix + STROKE_WIDTH_LABELS[Arrays.asList(STROKE_WIDTH_VALUES).indexOf(mStrokeWidth)]);
                v.setText(strokeWidthLabelPrefix + mStrokeWidth);
                //v.setText(strokeWidthLabelPrefix);
                return v;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                TextView v = (TextView) super.getDropDownView(position, convertView,parent);

                v.setTypeface(Typeface.SANS_SERIF);
                v.setText(STROKE_WIDTH_LABELS[position]);
                return v;
            }
        };
        spinner.setAdapter(adapter);

        spinner.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                mStrokeWidth = STROKE_WIDTH_VALUES[position];

                ((TextView) view).setBackgroundColor(Color.parseColor(toolbarBgColor));
                ((TextView) view).setTextColor(Color.parseColor(toolbarTextColor));
                ((TextView) view).setTypeface(Typeface.SANS_SERIF);
                ((TextView) view).setText(strokeWidthLabelPrefix + STROKE_WIDTH_LABELS[Arrays.asList(STROKE_WIDTH_VALUES).indexOf(mStrokeWidth)]);
                ((TextView) view).setText(strokeWidthLabelPrefix + mStrokeWidth);
                //((TextView) view).setText(strokeWidthLabelPrefix);

                if (mPaint != null) {
                    mPaint.setStrokeWidth(mStrokeWidth);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
        spinner.setBackgroundColor(Color.parseColor(toolbarBgColor));
        spinner.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);


        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.width = (int) (getScreenWidth(this) * 0.4);

        //LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, (float) 0.30);
        spinner.setLayoutParams(layoutParams);
        spinner.setSelection(Arrays.asList(STROKE_WIDTH_LABELS).indexOf("1x"));

        return spinner;
    }

    public void eraseDrawing() {

        new AlertDialog.Builder(this)
                //.setTitle("Eintrag löschen")
                .setTitle(clearTitle)
                //.setMessage("Den gesamten Inhalt jetzt zurücksetzen?")
                .setMessage(clearText)

                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        try{
                            switch (mBackgroundImageType) {
                                case COLOUR:
                                    mBitmap.eraseColor(Color.argb(a, r, g, b));
                                    break;
                                case FILE_URL:
                                    mBitmap = loadMutableBitmapFromFileURI(new URI(mBackgroundImageUrl));
                                    break;
                                case DATA_URL:
                                    mBitmap = loadMutableBitmapFromBase64DataUrl(mBackgroundImageUrl);
                                    break;
                                default:
                                    return;
                            }
                        } catch (URISyntaxException e) {
                            handleFileUriError(e);
                            return;
                        } catch (FileNotFoundException e) {
                            handleFileIOError(e);
                            return;
                        }

                        mBitmap = Bitmap.createScaledBitmap(mBitmap, mTdView.mCanvas.getWidth(),
                                mTdView.mCanvas.getHeight(), false);
                        mTdView.mCanvas = new Canvas(mBitmap);
                        mTdView.invalidate();
                    }
                })

                .setNegativeButton(android.R.string.no, null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    public void cancelDrawing() {
        Intent drawingResult = new Intent();

        setResult(Activity.RESULT_CANCELED, drawingResult);
        finish();
    }

    public Bitmap scaleBitmap(Bitmap bitmap) {
        int origWidth = bitmap.getWidth();
        int origHeight = bitmap.getHeight();
        int newWidth, newHeight;

        if (mScale < 100) {
            newWidth = (int) (origWidth * (mScale / 100.0));
            newHeight = (int)(origHeight * (mScale / 100.0));
        } else {
            return bitmap;
        }

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
    }

    @Override
    public void onBackPressed() {
        cancelDrawing();
        super.onBackPressed();
    }

    public void finishDrawing() {
        ByteArrayOutputStream drawing = new ByteArrayOutputStream();
        scaleBitmap(mBitmap).compress(mEncodingType, 100, drawing);

        Intent drawingResult = new Intent();
        drawingResult.putExtra(DRAWING_RESULT_PARCELABLE, drawing.toByteArray());
        setResult(Activity.RESULT_OK, drawingResult);
        finish();
    }

    @Override
    public void finish() {
        if (mBitmap != null) {
            mBitmap.recycle();
            mBitmap = null;
            System.gc();
        }

        super.finish();
    }

    public class TouchDrawView extends View {
        public Canvas  mCanvas;
        private Path mPath;
        private Paint   mBitmapPaint;

        @SuppressWarnings("deprecation")
        public TouchDrawView(Context context) {
            super(context);
            Display display = getWindowManager().getDefaultDisplay();
            int canvasWidth;
            int canvasHeight;

            if (mBackgroundImageType == BackgroundImageType.COLOUR) {
                if (Build.VERSION.SDK_INT >= 13) {
                    Point p = new Point();
                    display.getSize(p);

                    canvasWidth = p.x;
                    canvasHeight = p.y;
                } else {
                    canvasWidth = display.getWidth();      // Deprecated in SDK Versions >= 13
                    canvasHeight = display.getHeight();    // Deprecated in SDK Versions >= 13
                }

                mBitmap = Bitmap.createBitmap(canvasWidth, canvasHeight,
                        Bitmap.Config.ARGB_8888);
                mCanvas = new Canvas(mBitmap);

                // Decode the hex color code for the background to ARGB
                a = 0xFF;       // alpha value (0 -> transparent, FF -> opaque)
                r = Integer.valueOf("" + mBackgroundColor.charAt(1) +
                        mBackgroundColor.charAt(2), 16);
                g = Integer.valueOf("" + mBackgroundColor.charAt(3) +
                        mBackgroundColor.charAt(4), 16);
                b = Integer.valueOf("" + mBackgroundColor.charAt(5) +
                        mBackgroundColor.charAt(6), 16);
                mCanvas.drawARGB(a, r, g, b);
            } else {
                try {
                    if (mBackgroundImageType == BackgroundImageType.FILE_URL) {
                            mBitmap = loadMutableBitmapFromFileURI(new URI(mBackgroundImageUrl));

                            if (mBitmap == null) {
                                throw new IOException("Failed to read file: " + mBackgroundImageUrl);
                            }
                    } else if (mBackgroundImageType == BackgroundImageType.DATA_URL) {
                        mBitmap = loadMutableBitmapFromBase64DataUrl(mBackgroundImageUrl);
                    }
                } catch (URISyntaxException e) {
                    handleFileUriError(e);
                    return;
                } catch (IOException e) {
                    handleFileIOError(e);
                    return;
                }
                mCanvas = new Canvas(mBitmap);
            }
            mPath = new Path();
            mBitmapPaint = new Paint(Paint.DITHER_FLAG);
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);

            float newWidth = w;
            float newHeight = h;

            float bitmapWidth = mBitmap.getWidth();
            float bitmapHeight = mBitmap.getHeight();

            if (mBackgroundImageType != BackgroundImageType.COLOUR) {
                if (w != bitmapWidth || h != bitmapHeight) {
                    float xRatio = w / bitmapWidth;
                    float yRatio = h / bitmapHeight;

                    float dominatingRatio = Math.min(xRatio, yRatio);

                    newWidth = dominatingRatio * bitmapWidth;
                    newHeight = dominatingRatio * bitmapHeight;

                }
            }

            mBitmap = Bitmap.createScaledBitmap(mBitmap, Math.round(newWidth),
                    Math.round(newHeight), false);

            mCanvas.setBitmap(mBitmap);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            canvas.drawColor(Color.argb(a, r, g, b));
            canvas.drawBitmap(mBitmap, 0, 0, mBitmapPaint);
            canvas.drawPath(mPath, mPaint);
        }

        private float mX, mY;
        private static final float TOUCH_TOLERANCE = 4;

        private void touch_start(float x, float y) {
            mPath.reset();
            mPath.moveTo(x, y);
            mX = x;
            mY = y;
        }
        private void touch_move(float x, float y) {
            float dx = Math.abs(x - mX);
            float dy = Math.abs(y - mY);
            if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
                mPath.quadTo(mX, mY, (x + mX)/2, (y + mY)/2);
                mX = x;
                mY = y;
            }
        }
        private void touch_up() {
            mPath.lineTo(mX, mY);
            // commit the path to our offscreen
            mCanvas.drawPath(mPath, mPaint);
            // kill this so we don't double draw
            mPath.reset();
        }
        @Override
        public boolean onTouchEvent(MotionEvent event) {
            float x = event.getX();
            float y = event.getY();

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    touch_start(x, y);
                    invalidate();
                    break;
                case MotionEvent.ACTION_MOVE:
                    touch_move(x, y);
                    invalidate();
                    break;
                case MotionEvent.ACTION_UP:
                    touch_up();
                    invalidate();
                    break;
            }
            return true;
        }
    }

    private Bitmap loadMutableBitmapFromFileURI(URI uri) throws FileNotFoundException, URISyntaxException {
        if (!uri.getScheme().equals("file")) {
            throw new URISyntaxException(mBackgroundImageUrl, "invalid scheme");
        }

        if (uri.getQuery() != null) {
            // Ignore query parameters in the uri
            uri = new URI(uri.toString().split("\\?")[0]);
        }
        File file = new File(uri);

        if (!file.exists()) {
            throw new FileNotFoundException("File not found: " + file.getAbsolutePath());
        }

        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inMutable = true;
        opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
        return BitmapFactory.decodeFile(file.getAbsolutePath(), opts);
    }

    private Bitmap loadMutableBitmapFromBase64DataUrl(String base64DataUrl) throws URISyntaxException {
        if (base64DataUrl == null || base64DataUrl.isEmpty() ||
                !base64DataUrl.matches("data:.*;base64,.*")) {
            throw new URISyntaxException(base64DataUrl, "invalid data url");
        }

        String base64 = base64DataUrl.split("base64,")[1];
        byte[] imgData = Base64.decode(base64, Base64.DEFAULT);

        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inMutable = true;
        opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
        return BitmapFactory.decodeByteArray(imgData, 0, imgData.length, opts);
    }

    private void handleFileUriError(URISyntaxException e) {
        Intent result = new Intent();

        result.putExtra(DRAWING_RESULT_ERROR,
                e.getReason() + ": " + e.getInput());
        TouchDrawActivity.this.setResult(RESULT_TOUCHDRAW_ERROR, result);
        TouchDrawActivity.this.finish();
    }

    private void handleFileIOError(IOException e) {
        Intent result = new Intent();

        result.putExtra(DRAWING_RESULT_ERROR, e.getLocalizedMessage());
        TouchDrawActivity.this.setResult(RESULT_TOUCHDRAW_ERROR, result);
        TouchDrawActivity.this.finish();
    }

    private int getNextViewId() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            return View.generateViewId(); // Added in API level 17
        }

        // Re-implement View.generateViewId()for API levels < 17
        // http://stackoverflow.com/a/15442898
        for (;;) {
            final int result = sNextGeneratedId.get();
            // aapt-generated IDs have the high byte nonzero; clamp to the range under that.
            int newValue = result + 1;
            if (newValue > 0x00FFFFFF) newValue = 1; // Roll over to 1, not 0.
            if (sNextGeneratedId.compareAndSet(result, newValue)) {
                return result;
            }
        }
    }
    private static final AtomicInteger sNextGeneratedId = new AtomicInteger(1);

    public static int getScreenWidth(Context context) {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        ((Activity)context).getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        return displayMetrics.widthPixels;
    }
}
