package com.example.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

public class CropOverlayView extends View {

    private final Paint scrimPaint = new Paint();
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint handlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final RectF cropRect = new RectF();
    private final RectF imageBounds = new RectF();

    private float aspectRatio = 0f; // 0 = Free aspect ratio

    private static final int TOUCH_NONE = 0;
    private static final int TOUCH_MOVE = 1;
    private static final int TOUCH_TOP_LEFT = 2;
    private static final int TOUCH_TOP_RIGHT = 3;
    private static final int TOUCH_BOTTOM_LEFT = 4;
    private static final int TOUCH_BOTTOM_RIGHT = 5;

    private int activeTouchMode = TOUCH_NONE;
    private float lastTouchX;
    private float lastTouchY;

    private static final float HANDLE_RADIUS = 48f;
    private static final float MIN_CROP_SIZE = 120f;

    public CropOverlayView(Context context) {
        super(context);
        init();
    }

    public CropOverlayView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CropOverlayView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        scrimPaint.setColor(0x99000000);
        scrimPaint.setStyle(Paint.Style.FILL);

        borderPaint.setColor(0xFFFFFFFF);
        borderPaint.setStrokeWidth(3f);
        borderPaint.setStyle(Paint.Style.STROKE);

        gridPaint.setColor(0x80FFFFFF);
        gridPaint.setStrokeWidth(1.5f);
        gridPaint.setStyle(Paint.Style.STROKE);

        handlePaint.setColor(0xFF6366F1); // Modern indigo accent
        handlePaint.setStrokeWidth(6f);
        handlePaint.setStyle(Paint.Style.STROKE);
        handlePaint.setStrokeCap(Paint.Cap.ROUND);
    }

    public void setImageBounds(RectF bounds) {
        imageBounds.set(bounds);
        resetCropToImageBounds();
    }

    public void resetCropToImageBounds() {
        if (imageBounds.isEmpty()) {
            cropRect.set(getPaddingLeft(), getPaddingTop(), getWidth() - getPaddingRight(), getHeight() - getPaddingBottom());
        } else {
            // Margin 5%
            float insetX = imageBounds.width() * 0.05f;
            float insetY = imageBounds.height() * 0.05f;
            cropRect.set(imageBounds.left + insetX, imageBounds.top + insetY,
                    imageBounds.right - insetX, imageBounds.bottom - insetY);
        }
        applyAspectRatio();
        invalidate();
    }

    public void setAspectRatio(float ratio) {
        this.aspectRatio = ratio;
        applyAspectRatio();
        invalidate();
    }

    private void applyAspectRatio() {
        if (aspectRatio <= 0f || cropRect.isEmpty()) return;

        float currentW = cropRect.width();
        float currentH = cropRect.height();
        float centerX = cropRect.centerX();
        float centerY = cropRect.centerY();

        float newW, newH;
        if (currentW / currentH > aspectRatio) {
            newH = currentH;
            newW = newH * aspectRatio;
        } else {
            newW = currentW;
            newH = newW / aspectRatio;
        }

        // Check image bounds
        if (!imageBounds.isEmpty()) {
            if (newW > imageBounds.width()) {
                newW = imageBounds.width();
                newH = newW / aspectRatio;
            }
            if (newH > imageBounds.height()) {
                newH = imageBounds.height();
                newW = newH * aspectRatio;
            }
        }

        cropRect.set(centerX - newW / 2f, centerY - newH / 2f, centerX + newW / 2f, centerY + newH / 2f);
        clampCropRect();
    }

    private void clampCropRect() {
        RectF bounds = imageBounds.isEmpty() ? new RectF(0, 0, getWidth(), getHeight()) : imageBounds;

        if (cropRect.width() > bounds.width()) {
            cropRect.left = bounds.left;
            cropRect.right = bounds.right;
        }
        if (cropRect.height() > bounds.height()) {
            cropRect.top = bounds.top;
            cropRect.bottom = bounds.bottom;
        }

        if (cropRect.left < bounds.left) {
            cropRect.offset(bounds.left - cropRect.left, 0);
        }
        if (cropRect.right > bounds.right) {
            cropRect.offset(bounds.right - cropRect.right, 0);
        }
        if (cropRect.top < bounds.top) {
            cropRect.offset(0, bounds.top - cropRect.top);
        }
        if (cropRect.bottom > bounds.bottom) {
            cropRect.offset(0, bounds.bottom - cropRect.bottom);
        }
    }

    public RectF getCropRect() {
        return new RectF(cropRect);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (imageBounds.isEmpty()) {
            resetCropToImageBounds();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (cropRect.isEmpty()) return;

        int width = getWidth();
        int height = getHeight();

        // 1. Draw dim scrim outside crop box
        canvas.drawRect(0, 0, width, cropRect.top, scrimPaint);
        canvas.drawRect(0, cropRect.bottom, width, height, scrimPaint);
        canvas.drawRect(0, cropRect.top, cropRect.left, cropRect.bottom, scrimPaint);
        canvas.drawRect(cropRect.right, cropRect.top, width, cropRect.bottom, scrimPaint);

        // 2. Draw border
        canvas.drawRect(cropRect, borderPaint);

        // 3. Draw Rule of Thirds grid
        float oneThirdW = cropRect.width() / 3f;
        float oneThirdH = cropRect.height() / 3f;

        canvas.drawLine(cropRect.left + oneThirdW, cropRect.top, cropRect.left + oneThirdW, cropRect.bottom, gridPaint);
        canvas.drawLine(cropRect.left + oneThirdW * 2f, cropRect.top, cropRect.left + oneThirdW * 2f, cropRect.bottom, gridPaint);
        canvas.drawLine(cropRect.left, cropRect.top + oneThirdH, cropRect.right, cropRect.top + oneThirdH, gridPaint);
        canvas.drawLine(cropRect.left, cropRect.top + oneThirdH * 2f, cropRect.right, cropRect.top + oneThirdH * 2f, gridPaint);

        // 4. Draw corner handles
        float len = Math.min(36f, Math.min(cropRect.width(), cropRect.height()) / 4f);

        // Top-left
        canvas.drawLine(cropRect.left, cropRect.top, cropRect.left + len, cropRect.top, handlePaint);
        canvas.drawLine(cropRect.left, cropRect.top, cropRect.left, cropRect.top + len, handlePaint);

        // Top-right
        canvas.drawLine(cropRect.right, cropRect.top, cropRect.right - len, cropRect.top, handlePaint);
        canvas.drawLine(cropRect.right, cropRect.top, cropRect.right, cropRect.top + len, handlePaint);

        // Bottom-left
        canvas.drawLine(cropRect.left, cropRect.bottom, cropRect.left + len, cropRect.bottom, handlePaint);
        canvas.drawLine(cropRect.left, cropRect.bottom, cropRect.left, cropRect.bottom - len, handlePaint);

        // Bottom-right
        canvas.drawLine(cropRect.right, cropRect.bottom, cropRect.right - len, cropRect.bottom, handlePaint);
        canvas.drawLine(cropRect.right, cropRect.bottom, cropRect.right, cropRect.bottom - len, handlePaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                lastTouchX = x;
                lastTouchY = y;
                activeTouchMode = getTouchMode(x, y);
                return activeTouchMode != TOUCH_NONE;

            case MotionEvent.ACTION_MOVE:
                if (activeTouchMode == TOUCH_NONE) return false;

                float dx = x - lastTouchX;
                float dy = y - lastTouchY;

                RectF bounds = imageBounds.isEmpty() ? new RectF(0, 0, getWidth(), getHeight()) : imageBounds;

                if (activeTouchMode == TOUCH_MOVE) {
                    // Restrict movement within bounds
                    if (cropRect.left + dx < bounds.left) dx = bounds.left - cropRect.left;
                    if (cropRect.right + dx > bounds.right) dx = bounds.right - cropRect.right;
                    if (cropRect.top + dy < bounds.top) dy = bounds.top - cropRect.top;
                    if (cropRect.bottom + dy > bounds.bottom) dy = bounds.bottom - cropRect.bottom;

                    cropRect.offset(dx, dy);
                } else {
                    // Resizing
                    resizeCropRect(dx, dy, bounds);
                }

                lastTouchX = x;
                lastTouchY = y;
                invalidate();
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                activeTouchMode = TOUCH_NONE;
                return true;
        }

        return super.onTouchEvent(event);
    }

    private void resizeCropRect(float dx, float dy, RectF bounds) {
        switch (activeTouchMode) {
            case TOUCH_TOP_LEFT:
                cropRect.left = Math.max(bounds.left, Math.min(cropRect.left + dx, cropRect.right - MIN_CROP_SIZE));
                if (aspectRatio > 0f) {
                    cropRect.top = cropRect.bottom - cropRect.width() / aspectRatio;
                } else {
                    cropRect.top = Math.max(bounds.top, Math.min(cropRect.top + dy, cropRect.bottom - MIN_CROP_SIZE));
                }
                break;

            case TOUCH_TOP_RIGHT:
                cropRect.right = Math.min(bounds.right, Math.max(cropRect.right + dx, cropRect.left + MIN_CROP_SIZE));
                if (aspectRatio > 0f) {
                    cropRect.top = cropRect.bottom - cropRect.width() / aspectRatio;
                } else {
                    cropRect.top = Math.max(bounds.top, Math.min(cropRect.top + dy, cropRect.bottom - MIN_CROP_SIZE));
                }
                break;

            case TOUCH_BOTTOM_LEFT:
                cropRect.left = Math.max(bounds.left, Math.min(cropRect.left + dx, cropRect.right - MIN_CROP_SIZE));
                if (aspectRatio > 0f) {
                    cropRect.bottom = cropRect.top + cropRect.width() / aspectRatio;
                } else {
                    cropRect.bottom = Math.min(bounds.bottom, Math.max(cropRect.bottom + dy, cropRect.top + MIN_CROP_SIZE));
                }
                break;

            case TOUCH_BOTTOM_RIGHT:
                cropRect.right = Math.min(bounds.right, Math.max(cropRect.right + dx, cropRect.left + MIN_CROP_SIZE));
                if (aspectRatio > 0f) {
                    cropRect.bottom = cropRect.top + cropRect.width() / aspectRatio;
                } else {
                    cropRect.bottom = Math.min(bounds.bottom, Math.max(cropRect.bottom + dy, cropRect.top + MIN_CROP_SIZE));
                }
                break;
        }

        clampCropRect();
    }

    private int getTouchMode(float x, float y) {
        if (isNear(x, y, cropRect.left, cropRect.top)) return TOUCH_TOP_LEFT;
        if (isNear(x, y, cropRect.right, cropRect.top)) return TOUCH_TOP_RIGHT;
        if (isNear(x, y, cropRect.left, cropRect.bottom)) return TOUCH_BOTTOM_LEFT;
        if (isNear(x, y, cropRect.right, cropRect.bottom)) return TOUCH_BOTTOM_RIGHT;
        if (cropRect.contains(x, y)) return TOUCH_MOVE;
        return TOUCH_NONE;
    }

    private boolean isNear(float x1, float y1, float x2, float y2) {
        return Math.hypot(x1 - x2, y1 - y2) <= HANDLE_RADIUS;
    }
}
