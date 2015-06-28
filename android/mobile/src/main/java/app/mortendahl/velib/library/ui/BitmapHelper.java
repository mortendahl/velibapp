package app.mortendahl.velib.library.ui;

import java.io.IOException;

import android.util.DisplayMetrics;
import android.util.TypedValue;

import android.database.Cursor;
import android.graphics.*;
import android.media.ExifInterface;
import android.net.Uri;
import android.provider.MediaStore;

import app.mortendahl.velib.VelibApplication;

public abstract class BitmapHelper {

    private BitmapHelper() {}

    public static Bitmap createSolidBitmap(int bitmapSize, int color) {

        int bitmapWidth = bitmapSize;
        int bitmapHeight = bitmapSize;

        Bitmap output = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(output);
        canvas.drawColor(color);

        return output;
    }

    public static Bitmap createTextBitmap(int bitmapSize, int color, String text) {

        if (text == null) { text = ""; }
        text = text.trim();  // remove any whitespace before the name
        text = text.substring(0, Math.min(text.length(), 4));
        text = text.trim();  // remove any whitespace after the cutout

        final float TEXT_TO_SIZE_RATIO = 0.55f;

        final int bitmapWidth = bitmapSize;
        final int bitmapHeight = bitmapSize;

        final Bitmap output = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888);

        final Canvas canvas = new Canvas(output);
        canvas.drawColor(color);

        final float textSize = bitmapSize * TEXT_TO_SIZE_RATIO;
        final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(0xffffffff);
        paint.setTextSize(textSize);

        // make it horizontally centred
        paint.setTextAlign(Paint.Align.CENTER);

        // Compute current width with an arbitatry size. Tips: Add upper I as padding
        final float tmpTextWidth = paint.measureText(String.format("I%1$sI", text));
        //paint.setTextSize(bitmapWidth * textSize / tmpTextWidth);

        final float x = bitmapWidth / 2f;
        // make it vertically centred
        final Rect textRect = new Rect();
        paint.getTextBounds(text, 0, text.length(), textRect);
        final float y = (bitmapHeight + textRect.height()) / 2f;

        canvas.drawText(text, x, y, paint);

        return output;
    }

    public static Bitmap putOnDot(Bitmap input, int color, int edgeEnlargement) {

        final int width = input.getWidth() + edgeEnlargement;
        final int height = input.getHeight() + edgeEnlargement;

        Bitmap output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        // draw edge circle
        final Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(color);
        canvas.drawCircle(width/2, height/2, width/2, paint);

        // draw input bitmap on top
        canvas.drawBitmap(input, edgeEnlargement/2, edgeEnlargement/2, paint);

        return output;

    }

    public static Bitmap drawHalo(int innerRadius, int outerRadius, int color) {

        Bitmap bigCircle = drawCircle(outerRadius, color);

        Canvas canvas = new Canvas(bigCircle);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(0xff424242);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_OUT));
        canvas.drawCircle(outerRadius, outerRadius, innerRadius, paint);
        //Rect rect = new Rect(0, 0, input.getWidth(), input.getHeight());
        //canvas.drawBitmap(smallCircle, rect, rect, paint);

        return bigCircle;

    }

    public static Bitmap cropSquare(Bitmap inputBitmap) {

        int imageWidth = inputBitmap.getWidth();
        int imageHeight = inputBitmap.getHeight();

        // crop picture
        int imageOutputSize = Math.min(imageWidth, imageHeight);
        Bitmap outputBitmap = Bitmap.createBitmap(imageOutputSize, imageOutputSize, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(outputBitmap);
        canvas.drawARGB(255, 255, 0, 0);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        //final int cx = imageWidth/2;
        //final int cy = imageHeight/2;
        int left = 0;//cx - cy;
        int top = imageHeight/2 - imageOutputSize/2;//cy - cx;//0;
        int right = imageOutputSize; //cx + cy;
        int bottom = top + imageOutputSize;//imageHeight;
        Rect inputRect = new Rect(left, top, right, bottom);
        Rect outputRect = new Rect(0, 0, imageOutputSize, imageOutputSize);
        canvas.drawBitmap(inputBitmap, inputRect, outputRect, paint);

        return outputBitmap;

    }

    public static Bitmap cropBitmapToCircle(Bitmap input) {

        Bitmap output = Bitmap.createBitmap(input.getWidth(), input.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        canvas.drawARGB(0, 0, 0, 0);

        final int color = 0xff424242;

        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(color);
        canvas.drawCircle(input.getWidth() / 2, input.getHeight() / 2, input.getWidth() / 2, paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        Rect rect = new Rect(0, 0, input.getWidth(), input.getHeight());
        canvas.drawBitmap(input, rect, rect, paint);

        return output;

    }

    public static Bitmap drawCircle(int radius, int color) {

        int width = radius * 2;
        int height = radius * 2;

        int xPos = radius;
        int yPos = radius;

        Bitmap output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(color);

        canvas.drawCircle(xPos, yPos, radius, paint);

        return output;

    }

    public static Bitmap greyscale(Bitmap input) {

        ColorMatrix matrix = new ColorMatrix();
        matrix.setSaturation(0f);
        ColorMatrixColorFilter filter = new ColorMatrixColorFilter(matrix);

        Paint paint = new Paint();
        paint.setColorFilter(filter);

        int width = input.getWidth();
        int height = input.getHeight();
        Bitmap output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        canvas.drawBitmap(input, 0, 0, paint);

        return output;

    }

    public static int calculateInSampleSize(int width, int height, int reqWidth, int reqHeight) {
        // from http://developer.android.com/training/displaying-bitmaps/load-bitmap.html
        //  - a power of two value is calculated because the decoder uses a final value by rounding down to the nearest power of two
        int inSampleSize = 1;
        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;
            // calculate largest inSampleSize value that is a power of 2 and keeps
            // both height and width larger than the requested height and width
            while (		(halfHeight / inSampleSize) > reqHeight
                    &&	(halfWidth / inSampleSize) > reqWidth ) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    private static DisplayMetrics cachedMetrics = null;

    public static int dipsToPixels(float dipValue) {
        if (cachedMetrics == null) { cachedMetrics = VelibApplication.getCachedAppContext().getResources().getDisplayMetrics(); }
        float pxValue = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dipValue, cachedMetrics);
        return (int) pxValue;
    }

    public static final float[] TRANSFORM_NONE = null;

    // rotate 90 (counterclockwise)
    public static final float[] TRANSFORM_90 = new float[] {
            0, -1,  0,
            1,  0,  0,
            0,  0,  1  };

    public static final float[] TRANSFORM_180 = BitmapHelper.chainTransformations(
            BitmapHelper.TRANSFORM_90,
            BitmapHelper.TRANSFORM_90);

    public static final float[] TRANSFORM_270 = BitmapHelper.chainTransformations(BitmapHelper.chainTransformations(
            BitmapHelper.TRANSFORM_90,
            BitmapHelper.TRANSFORM_90),
            BitmapHelper.TRANSFORM_90);

    // flip vertically
    public static final float[] TRANSFORM_FLIPVER =	new float[] {
            -1,  0,  0,
             0,  1,  0,
             0,  0,  1  };

    // flip horizontally
    public static final float[] TRANSFORM_FLIPHOR =	new float[] {
            1,  0,  0,
            0, -1,  0,
            0,  0,  1  };

    public static float[] chainTransformations(float[] a, float[] b) {
        boolean wellformed = a.length == 3*3 && b.length == 3*3;
        if (!wellformed) { throw new IllegalArgumentException(); }

        float[] result = new float[9];
        for (int row=0; row<3; row++) {
            for (int col=0; col<3; col++) {
                result[row * 3 + col] = 0;
                for (int i=0; i<3; i++) {
                    result[row * 3 + col] += a[row * 3 + i] * b[i * 3 + col];
                }
            }
        }
        return result;
    }

    public static float[] extractPortraitTransformation(Uri pictureUri) {

        Cursor cursor = VelibApplication.getCachedAppContext().getContentResolver()
                .query(pictureUri, new String[] { MediaStore.Images.ImageColumns.ORIENTATION }, null, null, null);
        cursor.moveToFirst();
        int orientation = cursor.getInt(0);

        float[] transformationRequired;
        switch (orientation) {
            case 0:
                transformationRequired = TRANSFORM_NONE;
                break;

            case 90:
                transformationRequired = TRANSFORM_90;
                break;

            case 180:
                transformationRequired = TRANSFORM_180;
                break;

            case 270:
                transformationRequired = TRANSFORM_270;
                break;

            default:
                transformationRequired = TRANSFORM_NONE;
                break;
        }

        return transformationRequired;

    }

    public static float[] extractPortraitTransformation(String pictureFilename) throws IOException {

        ExifInterface exif = new ExifInterface(pictureFilename);
        int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);

        float[] transformationRequired;
        switch (orientation) {
            case ExifInterface.ORIENTATION_NORMAL:
                transformationRequired = TRANSFORM_NONE;
                break;

            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                transformationRequired = TRANSFORM_FLIPHOR;
                break;

            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                transformationRequired = TRANSFORM_FLIPVER;
                break;

            case ExifInterface.ORIENTATION_TRANSPOSE:
                transformationRequired = chainTransformations(TRANSFORM_90, TRANSFORM_FLIPVER);
                break;

            case ExifInterface.ORIENTATION_TRANSVERSE:
                transformationRequired = null;
                break;

            case ExifInterface.ORIENTATION_ROTATE_90:
                transformationRequired = TRANSFORM_90;
                break;

            case ExifInterface.ORIENTATION_ROTATE_180:
                transformationRequired = TRANSFORM_180;
                break;

            case ExifInterface.ORIENTATION_ROTATE_270:
                transformationRequired = TRANSFORM_270;
                break;

            default:
                transformationRequired = TRANSFORM_NONE;
                break;
        }

        return transformationRequired;

    }

    public static Bitmap transform(Bitmap inputBitmap, float[] transformation) {

        if (transformation == null) { return inputBitmap; }

        int imageWidth = inputBitmap.getWidth();
        int imageHeight = inputBitmap.getHeight();

        Matrix transformMatrix = new Matrix();
        transformMatrix.setValues(transformation);

        Bitmap outputBitmap = Bitmap.createBitmap(inputBitmap, 0, 0, imageWidth, imageHeight, transformMatrix, true);
        return outputBitmap;

    }

}
