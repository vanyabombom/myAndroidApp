package com.example.app;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class PhotoFilterUtil {

    public enum FilterType {
        ORIGINAL("Оригінал"),
        GRAYSCALE("Ч/Б"),
        SEPIA("Сепія"),
        INVERT("Негатив"),
        VINTAGE("Вінтаж"),
        VIVID("Яскравий"),
        COOL("Холодний"),
        WARM("Теплий"),
        CINEMATIC("Кіно"),
        NOIR("Нуар"),
        CYBERPUNK("Кіберпанк"),
        DRAMATIC("Драматичний"),
        VIGNETTE("Віньєтка"),
        BLUR("Розмиття");

        private final String displayName;

        FilterType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public static Bitmap applyFilter(Bitmap source, FilterType filterType) {
        if (source == null) return null;

        switch (filterType) {
            case ORIGINAL:
                return source.copy(Bitmap.Config.ARGB_8888, true);

            case VIGNETTE:
                return applyVignette(source);

            case BLUR:
                return applyFastBlur(source, 10);

            case GRAYSCALE: {
                ColorMatrix cm = new ColorMatrix();
                cm.setSaturation(0f);
                return applyColorMatrix(source, cm);
            }

            case SEPIA: {
                ColorMatrix cm = new ColorMatrix(new float[]{
                        0.393f, 0.769f, 0.189f, 0f, 0f,
                        0.349f, 0.686f, 0.168f, 0f, 0f,
                        0.272f, 0.534f, 0.131f, 0f, 0f,
                        0f,     0f,     0f,     1f, 0f
                });
                return applyColorMatrix(source, cm);
            }

            case INVERT: {
                ColorMatrix cm = new ColorMatrix(new float[]{
                        -1f,  0f,  0f, 0f, 255f,
                         0f, -1f,  0f, 0f, 255f,
                         0f,  0f, -1f, 0f, 255f,
                         0f,  0f,  0f, 1f,   0f
                });
                return applyColorMatrix(source, cm);
            }

            case VINTAGE: {
                ColorMatrix cm = new ColorMatrix(new float[]{
                        1.10f, 0.10f, 0.05f, 0f,  18f,
                        0.05f, 1.00f, 0.05f, 0f,  12f,
                        0.05f, 0.05f, 0.80f, 0f, -20f,
                        0f,    0f,    0f,    1f,   0f
                });
                return applyColorMatrix(source, cm);
            }

            case VIVID: {
                ColorMatrix cm = new ColorMatrix(new float[]{
                        1.35f, 0f,    0f,    0f, -25f,
                        0f,    1.35f, 0f,    0f, -25f,
                        0f,    0f,    1.35f, 0f, -25f,
                        0f,    0f,    0f,    1f,   0f
                });
                ColorMatrix sat = new ColorMatrix();
                sat.setSaturation(1.4f);
                cm.postConcat(sat);
                return applyColorMatrix(source, cm);
            }

            case COOL: {
                ColorMatrix cm = new ColorMatrix(new float[]{
                        0.85f, 0f,    0f,    0f,   0f,
                        0f,    0.95f, 0f,    0f,   0f,
                        0f,    0f,    1.30f, 0f,  25f,
                        0f,    0f,    0f,    1f,   0f
                });
                return applyColorMatrix(source, cm);
            }

            case WARM: {
                ColorMatrix cm = new ColorMatrix(new float[]{
                        1.25f, 0f,    0f,    0f,  20f,
                        0f,    1.05f, 0f,    0f,  10f,
                        0f,    0f,    0.80f, 0f, -15f,
                        0f,    0f,    0f,    1f,   0f
                });
                return applyColorMatrix(source, cm);
            }

            case CINEMATIC: {
                // Teal & Orange tone
                ColorMatrix cm = new ColorMatrix(new float[]{
                        1.20f, 0.10f, 0.00f, 0f,  10f,
                        0.05f, 1.10f, 0.10f, 0f,   0f,
                        0.00f, 0.15f, 1.25f, 0f,  15f,
                        0f,    0f,    0f,    1f,   0f
                });
                return applyColorMatrix(source, cm);
            }

            case NOIR: {
                // High contrast dramatic monochrome
                ColorMatrix cm = new ColorMatrix();
                cm.setSaturation(0f);
                ColorMatrix contrast = new ColorMatrix(new float[]{
                        1.6f, 0f,   0f,   0f, -60f,
                        0f,   1.6f, 0f,   0f, -60f,
                        0f,   0f,   1.6f, 0f, -60f,
                        0f,   0f,   0f,   1f,   0f
                });
                cm.postConcat(contrast);
                return applyColorMatrix(source, cm);
            }

            case CYBERPUNK: {
                // Deep violet/cyan futuristic matrix
                ColorMatrix cm = new ColorMatrix(new float[]{
                        1.2f, -0.1f, 0.3f, 0f,  25f,
                        0.0f,  1.1f, 0.2f, 0f, -10f,
                        0.2f,  0.0f, 1.4f, 0f,  40f,
                        0f,    0f,   0f,   1f,   0f
                });
                return applyColorMatrix(source, cm);
            }

            case DRAMATIC: {
                // Punchy contrast with rich shadows
                ColorMatrix cm = new ColorMatrix(new float[]{
                        1.4f, 0f,   0f,   0f, -40f,
                        0f,   1.4f, 0f,   0f, -40f,
                        0f,   0f,   1.4f, 0f, -40f,
                        0f,   0f,   0f,   1f,   0f
                });
                ColorMatrix sat = new ColorMatrix();
                sat.setSaturation(1.25f);
                cm.postConcat(sat);
                return applyColorMatrix(source, cm);
            }

            default:
                return source.copy(Bitmap.Config.ARGB_8888, true);
        }
    }

    public static Bitmap applyAdjustments(Bitmap source, float brightness, float contrast, float saturation) {
        if (source == null) return null;

        // Contrast & brightness matrix
        float scale = contrast;
        float translate = (-0.5f * scale + 0.5f) * 255.0f + brightness;

        ColorMatrix cm = new ColorMatrix(new float[]{
                scale, 0f,    0f,    0f, translate,
                0f,    scale, 0f,    0f, translate,
                0f,    0f,    scale, 0f, translate,
                0f,    0f,    0f,    1f, 0f
        });

        ColorMatrix satMatrix = new ColorMatrix();
        satMatrix.setSaturation(saturation);
        cm.postConcat(satMatrix);

        return applyColorMatrix(source, cm);
    }

    private static Bitmap applyColorMatrix(Bitmap source, ColorMatrix colorMatrix) {
        Bitmap output = Bitmap.createBitmap(source.getWidth(), source.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        paint.setColorFilter(new ColorMatrixColorFilter(colorMatrix));
        canvas.drawBitmap(source, 0, 0, paint);
        return output;
    }

    public static Bitmap applyVignette(Bitmap source) {
        if (source == null) return null;
        Bitmap output = source.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(output);

        float cx = output.getWidth() / 2f;
        float cy = output.getHeight() / 2f;
        float radius = (float) Math.hypot(cx, cy);

        RadialGradient gradient = new RadialGradient(
                cx, cy, radius,
                new int[]{0x00000000, 0x33000000, 0xD9000000},
                new float[]{0.0f, 0.55f, 1.0f},
                Shader.TileMode.CLAMP
        );

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setShader(gradient);
        canvas.drawRect(0, 0, output.getWidth(), output.getHeight(), paint);
        return output;
    }

    public static Bitmap applyFastBlur(Bitmap source, int radius) {
        if (source == null) return null;
        int downscale = 6;
        int w = Math.max(8, source.getWidth() / downscale);
        int h = Math.max(8, source.getHeight() / downscale);

        Bitmap small = Bitmap.createScaledBitmap(source, w, h, true);
        Bitmap blurred = Bitmap.createScaledBitmap(small, source.getWidth(), source.getHeight(), true);
        if (small != source) small.recycle();
        return blurred;
    }

    public static Bitmap rotateBitmap(Bitmap source, float degrees) {
        if (source == null) return null;
        Matrix matrix = new Matrix();
        matrix.postRotate(degrees);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    public static Bitmap flipHorizontal(Bitmap source) {
        if (source == null) return null;
        Matrix matrix = new Matrix();
        matrix.postScale(-1f, 1f, source.getWidth() / 2f, source.getHeight() / 2f);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    public static Bitmap flipVertical(Bitmap source) {
        if (source == null) return null;
        Matrix matrix = new Matrix();
        matrix.postScale(1f, -1f, source.getWidth() / 2f, source.getHeight() / 2f);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    public static Bitmap cropBitmap(Bitmap source, Rect cropRect) {
        if (source == null || cropRect == null) return source;

        int left = Math.max(0, cropRect.left);
        int top = Math.max(0, cropRect.top);
        int width = Math.min(cropRect.width(), source.getWidth() - left);
        int height = Math.min(cropRect.height(), source.getHeight() - top);

        if (width <= 0 || height <= 0) return source;
        return Bitmap.createBitmap(source, left, top, width, height);
    }

    public static Bitmap loadSampledBitmapFromUri(Context context, Uri uri, int maxDim) {
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;

            try (InputStream input = context.getContentResolver().openInputStream(uri)) {
                if (input == null) return null;
                BitmapFactory.decodeStream(input, null, options);
            }

            int inSampleSize = 1;
            while (options.outWidth / inSampleSize > maxDim || options.outHeight / inSampleSize > maxDim) {
                inSampleSize *= 2;
            }

            options.inJustDecodeBounds = false;
            options.inSampleSize = inSampleSize;

            Bitmap bitmap;
            try (InputStream input = context.getContentResolver().openInputStream(uri)) {
                if (input == null) return null;
                bitmap = BitmapFactory.decodeStream(input, null, options);
            }

            if (bitmap == null) return null;

            // Fix EXIF orientation
            try (InputStream exifInput = context.getContentResolver().openInputStream(uri)) {
                if (exifInput != null) {
                    ExifInterface exif = new ExifInterface(exifInput);
                    int orientation = exif.getAttributeInt(
                            ExifInterface.TAG_ORIENTATION,
                            ExifInterface.ORIENTATION_NORMAL
                    );
                    bitmap = rotateByExif(bitmap, orientation);
                }
            } catch (Exception ignored) {
            }

            return bitmap;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static Bitmap rotateByExif(Bitmap bitmap, int orientation) {
        Matrix matrix = new Matrix();
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.postRotate(90);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.postRotate(180);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.postRotate(270);
                break;
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                matrix.postScale(1, -1);
                break;
            default:
                return bitmap;
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    public static Uri saveBitmapToGallery(Context context, Bitmap bitmap) {
        if (bitmap == null) return null;

        String filename = "Photoshop2_" + System.currentTimeMillis() + ".jpg";
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, filename);
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Photoshop2.0");

        Uri uri = context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        if (uri != null) {
            try (OutputStream out = context.getContentResolver().openOutputStream(uri)) {
                if (out != null) {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out);
                    return uri;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public static void shareBitmap(Context context, Bitmap bitmap) {
        if (bitmap == null) return;
        try {
            File cachePath = new File(context.getCacheDir(), "images");
            if (!cachePath.exists()) cachePath.mkdirs();
            File file = new File(cachePath, "shared_photo.jpg");
            try (FileOutputStream stream = new FileOutputStream(file)) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream);
            }

            Uri contentUri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", file);
            if (contentUri != null) {
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                shareIntent.setDataAndType(contentUri, context.getContentResolver().getType(contentUri));
                shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
                shareIntent.setType("image/jpeg");
                context.startActivity(Intent.createChooser(shareIntent, "Поділитися фото"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
