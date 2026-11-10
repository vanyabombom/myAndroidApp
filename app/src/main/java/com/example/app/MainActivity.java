package com.example.app;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.slider.Slider;

import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    // Views
    private ImageView ivPhoto;
    private CropOverlayView cropOverlayView;
    private LinearLayout layoutPlaceholder;
    private LinearLayout layoutCropActions;
    private MaterialButton btnSave;
    private MaterialButton btnReset;
    private ImageButton btnUndo;
    private ImageButton btnRedo;
    private ImageButton btnShare;

    // Categories
    private MaterialButtonToggleGroup toggleGroupCategories;
    private View panelFilters;
    private View panelCrop;
    private View panelTransform;
    private View panelAdjust;

    // Filter controls
    private MaterialButtonToggleGroup toggleGroupFilters;
    private MaterialButtonToggleGroup toggleGroupCropRatios;

    // Adjustments controls
    private Slider sliderBrightness;
    private Slider sliderContrast;
    private Slider sliderSaturation;
    private TextView tvBrightnessVal;
    private TextView tvContrastVal;
    private TextView tvSaturationVal;

    // State Bitmaps
    private Bitmap initialLoadedBitmap = null;
    private Bitmap transformedBaseBitmap = null;
    private Bitmap displayedFilteredBitmap = null;

    // Undo / Redo history
    private static final int MAX_HISTORY_STEPS = 5;
    private final Deque<Bitmap> undoStack = new ArrayDeque<>();
    private final Deque<Bitmap> redoStack = new ArrayDeque<>();

    private PhotoFilterUtil.FilterType currentFilter = PhotoFilterUtil.FilterType.ORIGINAL;
    private Uri tempCameraUri = null;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    // Camera permission launcher
    private final ActivityResultLauncher<String> requestCameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    launchCamera();
                } else {
                    Toast.makeText(
                            this,
                            "Для знімків з камери потрібен дозвіл на використання камери!",
                            Toast.LENGTH_LONG
                    ).show();
                }
            });

    // Camera capture launcher
    private final ActivityResultLauncher<Uri> takePictureLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
                if (success && tempCameraUri != null) {
                    loadBitmapFromUri(tempCameraUri);
                }
            });

    // Gallery picker launcher
    private final ActivityResultLauncher<String> pickGalleryLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    loadBitmapFromUri(uri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initViews();
        setupListeners();
        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(android.content.Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(android.content.Intent intent) {
        if (intent == null) return;
        Uri uri = intent.getData();
        if (uri == null && android.content.Intent.ACTION_SEND.equals(intent.getAction())) {
            uri = intent.getParcelableExtra(android.content.Intent.EXTRA_STREAM);
        }
        if (uri != null) {
            loadBitmapFromUri(uri);
        }
    }

    private void initViews() {
        ivPhoto = findViewById(R.id.ivPhoto);
        cropOverlayView = findViewById(R.id.cropOverlayView);
        layoutPlaceholder = findViewById(R.id.layoutPlaceholder);
        layoutCropActions = findViewById(R.id.layoutCropActions);
        btnSave = findViewById(R.id.btnSave);
        btnReset = findViewById(R.id.btnReset);
        btnUndo = findViewById(R.id.btnUndo);
        btnRedo = findViewById(R.id.btnRedo);
        btnShare = findViewById(R.id.btnShare);

        toggleGroupCategories = findViewById(R.id.toggleGroupCategories);
        panelFilters = findViewById(R.id.panelFilters);
        panelCrop = findViewById(R.id.panelCrop);
        panelTransform = findViewById(R.id.panelTransform);
        panelAdjust = findViewById(R.id.panelAdjust);

        toggleGroupFilters = findViewById(R.id.toggleGroupFilters);
        toggleGroupCropRatios = findViewById(R.id.toggleGroupCropRatios);

        sliderBrightness = findViewById(R.id.sliderBrightness);
        sliderContrast = findViewById(R.id.sliderContrast);
        sliderSaturation = findViewById(R.id.sliderSaturation);
        tvBrightnessVal = findViewById(R.id.tvBrightnessVal);
        tvContrastVal = findViewById(R.id.tvContrastVal);
        tvSaturationVal = findViewById(R.id.tvSaturationVal);

        toggleGroupCategories.check(R.id.tabFilters);
        toggleGroupFilters.check(R.id.filterOriginal);
        toggleGroupCropRatios.check(R.id.cropFree);

        updateUiState(false);
        updateHistoryButtons();
    }

    private void setupListeners() {
        // Camera & Gallery
        findViewById(R.id.btnCamera).setOnClickListener(v -> checkCameraPermissionAndLaunch());
        findViewById(R.id.btnGallery).setOnClickListener(v -> pickGalleryLauncher.launch("image/*"));

        // Save & Share & Reset
        btnSave.setOnClickListener(v -> saveCurrentPhoto());
        btnShare.setOnClickListener(v -> {
            if (displayedFilteredBitmap != null) {
                PhotoFilterUtil.shareBitmap(this, displayedFilteredBitmap);
            }
        });
        btnReset.setOnClickListener(v -> resetToOriginal());

        // Undo & Redo
        btnUndo.setOnClickListener(v -> performUndo());
        btnRedo.setOnClickListener(v -> performRedo());

        // Category Tab switching
        toggleGroupCategories.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;

            // Hide crop overlay if switching away from Crop tab
            if (checkedId != R.id.tabCrop) {
                exitCropMode();
            }

            panelFilters.setVisibility(checkedId == R.id.tabFilters ? View.VISIBLE : View.GONE);
            panelCrop.setVisibility(checkedId == R.id.tabCrop ? View.VISIBLE : View.GONE);
            panelTransform.setVisibility(checkedId == R.id.tabTransform ? View.VISIBLE : View.GONE);
            panelAdjust.setVisibility(checkedId == R.id.tabAdjust ? View.VISIBLE : View.GONE);

            if (checkedId == R.id.tabCrop) {
                enterCropMode();
            }
        });

        // Filter Selection
        setupFilterListeners();

        // Transform (Rotate & Flip)
        setupTransformListeners();

        // Crop Ratios & Actions
        setupCropListeners();

        // Adjustment Sliders
        setupAdjustmentListeners();
    }

    private void setupFilterListeners() {
        toggleGroupFilters.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;

            if (checkedId == R.id.filterOriginal) {
                currentFilter = PhotoFilterUtil.FilterType.ORIGINAL;
            } else if (checkedId == R.id.filterGrayscale) {
                currentFilter = PhotoFilterUtil.FilterType.GRAYSCALE;
            } else if (checkedId == R.id.filterSepia) {
                currentFilter = PhotoFilterUtil.FilterType.SEPIA;
            } else if (checkedId == R.id.filterCinematic) {
                currentFilter = PhotoFilterUtil.FilterType.CINEMATIC;
            } else if (checkedId == R.id.filterWarm) {
                currentFilter = PhotoFilterUtil.FilterType.WARM;
            } else if (checkedId == R.id.filterCool) {
                currentFilter = PhotoFilterUtil.FilterType.COOL;
            } else if (checkedId == R.id.filterVivid) {
                currentFilter = PhotoFilterUtil.FilterType.VIVID;
            } else if (checkedId == R.id.filterVintage) {
                currentFilter = PhotoFilterUtil.FilterType.VINTAGE;
            } else if (checkedId == R.id.filterNoir) {
                currentFilter = PhotoFilterUtil.FilterType.NOIR;
            } else if (checkedId == R.id.filterCyberpunk) {
                currentFilter = PhotoFilterUtil.FilterType.CYBERPUNK;
            } else if (checkedId == R.id.filterDramatic) {
                currentFilter = PhotoFilterUtil.FilterType.DRAMATIC;
            } else if (checkedId == R.id.filterVignette) {
                currentFilter = PhotoFilterUtil.FilterType.VIGNETTE;
            } else if (checkedId == R.id.filterBlur) {
                currentFilter = PhotoFilterUtil.FilterType.BLUR;
            } else if (checkedId == R.id.filterInvert) {
                currentFilter = PhotoFilterUtil.FilterType.INVERT;
            }

            applyCurrentFilter();
        });
    }

    private void setupTransformListeners() {
        findViewById(R.id.btnRotateRight).setOnClickListener(v -> applyTransformation(
                PhotoFilterUtil.rotateBitmap(transformedBaseBitmap, 90f)
        ));

        findViewById(R.id.btnRotateLeft).setOnClickListener(v -> applyTransformation(
                PhotoFilterUtil.rotateBitmap(transformedBaseBitmap, -90f)
        ));

        findViewById(R.id.btnFlipH).setOnClickListener(v -> applyTransformation(
                PhotoFilterUtil.flipHorizontal(transformedBaseBitmap)
        ));

        findViewById(R.id.btnFlipV).setOnClickListener(v -> applyTransformation(
                PhotoFilterUtil.flipVertical(transformedBaseBitmap)
        ));
    }

    private void setupCropListeners() {
        toggleGroupCropRatios.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            if (checkedId == R.id.cropFree) {
                cropOverlayView.setAspectRatio(0f);
            } else if (checkedId == R.id.crop1_1) {
                cropOverlayView.setAspectRatio(1.0f);
            } else if (checkedId == R.id.crop4_3) {
                cropOverlayView.setAspectRatio(4.0f / 3.0f);
            } else if (checkedId == R.id.crop16_9) {
                cropOverlayView.setAspectRatio(16.0f / 9.0f);
            } else if (checkedId == R.id.crop9_16) {
                cropOverlayView.setAspectRatio(9.0f / 16.0f);
            } else if (checkedId == R.id.crop3_4) {
                cropOverlayView.setAspectRatio(3.0f / 4.0f);
            }
        });

        findViewById(R.id.btnCancelCrop).setOnClickListener(v -> {
            exitCropMode();
            toggleGroupCategories.check(R.id.tabFilters);
        });

        findViewById(R.id.btnApplyCrop).setOnClickListener(v -> applyCrop());
    }

    private void setupAdjustmentListeners() {
        sliderBrightness.addOnChangeListener((slider, value, fromUser) -> {
            tvBrightnessVal.setText(String.format(Locale.US, "%+d", (int) value));
            if (fromUser) livePreviewAdjustments();
        });

        sliderContrast.addOnChangeListener((slider, value, fromUser) -> {
            tvContrastVal.setText(String.format(Locale.US, "%.1fx", value));
            if (fromUser) livePreviewAdjustments();
        });

        sliderSaturation.addOnChangeListener((slider, value, fromUser) -> {
            tvSaturationVal.setText(String.format(Locale.US, "%.1fx", value));
            if (fromUser) livePreviewAdjustments();
        });

        findViewById(R.id.btnResetAdjust).setOnClickListener(v -> {
            sliderBrightness.setValue(0f);
            sliderContrast.setValue(1.0f);
            sliderSaturation.setValue(1.0f);
            applyCurrentFilter();
        });

        findViewById(R.id.btnApplyAdjust).setOnClickListener(v -> {
            if (transformedBaseBitmap == null) return;
            float b = sliderBrightness.getValue();
            float c = sliderContrast.getValue();
            float s = sliderSaturation.getValue();

            recordUndoState();
            executorService.execute(() -> {
                Bitmap adjusted = PhotoFilterUtil.applyAdjustments(transformedBaseBitmap, b, c, s);
                runOnUiThread(() -> {
                    transformedBaseBitmap = adjusted;
                    sliderBrightness.setValue(0f);
                    sliderContrast.setValue(1.0f);
                    sliderSaturation.setValue(1.0f);
                    applyCurrentFilter();
                    Toast.makeText(this, "Корекцію успішно застосовано!", Toast.LENGTH_SHORT).show();
                });
            });
        });
    }

    private void livePreviewAdjustments() {
        if (transformedBaseBitmap == null) return;
        float b = sliderBrightness.getValue();
        float c = sliderContrast.getValue();
        float s = sliderSaturation.getValue();

        executorService.execute(() -> {
            Bitmap adjusted = PhotoFilterUtil.applyAdjustments(transformedBaseBitmap, b, c, s);
            Bitmap finalDisplay = PhotoFilterUtil.applyFilter(adjusted, currentFilter);
            runOnUiThread(() -> {
                displayedFilteredBitmap = finalDisplay;
                ivPhoto.setImageBitmap(displayedFilteredBitmap);
            });
        });
    }

    private void enterCropMode() {
        if (transformedBaseBitmap == null) return;
        cropOverlayView.setVisibility(View.VISIBLE);
        layoutCropActions.setVisibility(View.VISIBLE);

        ivPhoto.post(() -> {
            RectF displayedRect = getDisplayedImageRect();
            if (displayedRect != null && !displayedRect.isEmpty()) {
                cropOverlayView.setImageBounds(displayedRect);
            }
        });
    }

    private void exitCropMode() {
        cropOverlayView.setVisibility(View.GONE);
        layoutCropActions.setVisibility(View.GONE);
    }

    private RectF getDisplayedImageRect() {
        if (ivPhoto.getDrawable() == null) return null;
        Matrix matrix = ivPhoto.getImageMatrix();
        RectF rect = new RectF(0, 0,
                ivPhoto.getDrawable().getIntrinsicWidth(),
                ivPhoto.getDrawable().getIntrinsicHeight());
        matrix.mapRect(rect);
        return rect;
    }

    private void applyCrop() {
        if (transformedBaseBitmap == null) return;

        RectF cropRectOnView = cropOverlayView.getCropRect();
        RectF imageOnView = getDisplayedImageRect();

        if (imageOnView == null || imageOnView.isEmpty() || cropRectOnView.isEmpty()) {
            exitCropMode();
            return;
        }

        // Map View cropRect to Bitmap coordinates
        float scaleX = (float) transformedBaseBitmap.getWidth() / imageOnView.width();
        float scaleY = (float) transformedBaseBitmap.getHeight() / imageOnView.height();

        int cropX = Math.max(0, (int) ((cropRectOnView.left - imageOnView.left) * scaleX));
        int cropY = Math.max(0, (int) ((cropRectOnView.top - imageOnView.top) * scaleY));
        int cropW = Math.min(transformedBaseBitmap.getWidth() - cropX, (int) (cropRectOnView.width() * scaleX));
        int cropH = Math.min(transformedBaseBitmap.getHeight() - cropY, (int) (cropRectOnView.height() * scaleY));

        if (cropW > 10 && cropH > 10) {
            recordUndoState();
            Rect cropBounds = new Rect(cropX, cropY, cropX + cropW, cropY + cropH);
            transformedBaseBitmap = PhotoFilterUtil.cropBitmap(transformedBaseBitmap, cropBounds);
            applyCurrentFilter();
            Toast.makeText(this, "Фото обрізано", Toast.LENGTH_SHORT).show();
        }

        exitCropMode();
        toggleGroupCategories.check(R.id.tabFilters);
    }

    private void applyTransformation(Bitmap newBitmap) {
        if (newBitmap == null) return;
        recordUndoState();
        transformedBaseBitmap = newBitmap;
        applyCurrentFilter();
    }

    private void recordUndoState() {
        if (transformedBaseBitmap == null) return;
        if (undoStack.size() >= MAX_HISTORY_STEPS) {
            undoStack.pollFirst();
        }
        undoStack.addLast(transformedBaseBitmap.copy(Bitmap.Config.ARGB_8888, true));
        redoStack.clear();
        updateHistoryButtons();
    }

    private void performUndo() {
        if (undoStack.isEmpty() || transformedBaseBitmap == null) return;
        redoStack.addLast(transformedBaseBitmap.copy(Bitmap.Config.ARGB_8888, true));
        transformedBaseBitmap = undoStack.pollLast();
        applyCurrentFilter();
        updateHistoryButtons();
        Toast.makeText(this, "Дію відмінено", Toast.LENGTH_SHORT).show();
    }

    private void performRedo() {
        if (redoStack.isEmpty() || transformedBaseBitmap == null) return;
        undoStack.addLast(transformedBaseBitmap.copy(Bitmap.Config.ARGB_8888, true));
        transformedBaseBitmap = redoStack.pollLast();
        applyCurrentFilter();
        updateHistoryButtons();
        Toast.makeText(this, "Дію повернуто", Toast.LENGTH_SHORT).show();
    }

    private void updateHistoryButtons() {
        boolean canUndo = !undoStack.isEmpty();
        boolean canRedo = !redoStack.isEmpty();

        btnUndo.setEnabled(canUndo);
        btnUndo.setAlpha(canUndo ? 1.0f : 0.35f);

        btnRedo.setEnabled(canRedo);
        btnRedo.setAlpha(canRedo ? 1.0f : 0.35f);
    }

    private void resetToOriginal() {
        if (initialLoadedBitmap != null) {
            recordUndoState();
            transformedBaseBitmap = initialLoadedBitmap.copy(Bitmap.Config.ARGB_8888, true);
            currentFilter = PhotoFilterUtil.FilterType.ORIGINAL;
            toggleGroupFilters.check(R.id.filterOriginal);
            sliderBrightness.setValue(0f);
            sliderContrast.setValue(1.0f);
            sliderSaturation.setValue(1.0f);
            applyCurrentFilter();
            Toast.makeText(this, "Скинуто до початкового стану", Toast.LENGTH_SHORT).show();
        }
    }

    private void checkCameraPermissionAndLaunch() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            launchCamera();
        } else {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void launchCamera() {
        try {
            File photoFile = File.createTempFile("cam_capture_", ".jpg", getCacheDir());
            tempCameraUri = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".fileprovider",
                    photoFile
            );
            takePictureLauncher.launch(tempCameraUri);
        } catch (IOException e) {
            Toast.makeText(this, "Помилка камери: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void loadBitmapFromUri(Uri uri) {
        executorService.execute(() -> {
            Bitmap loaded = PhotoFilterUtil.loadSampledBitmapFromUri(this, uri, 2048);
            runOnUiThread(() -> {
                if (loaded != null) {
                    undoStack.clear();
                    redoStack.clear();
                    updateHistoryButtons();

                    initialLoadedBitmap = loaded;
                    transformedBaseBitmap = loaded.copy(Bitmap.Config.ARGB_8888, true);

                    currentFilter = PhotoFilterUtil.FilterType.ORIGINAL;
                    toggleGroupFilters.check(R.id.filterOriginal);
                    sliderBrightness.setValue(0f);
                    sliderContrast.setValue(1.0f);
                    sliderSaturation.setValue(1.0f);

                    applyCurrentFilter();
                    updateUiState(true);
                    Toast.makeText(this, "Фото завантажено!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Не вдалося відкрити фото", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void applyCurrentFilter() {
        if (transformedBaseBitmap == null) return;

        executorService.execute(() -> {
            Bitmap filtered = PhotoFilterUtil.applyFilter(transformedBaseBitmap, currentFilter);
            runOnUiThread(() -> {
                displayedFilteredBitmap = filtered;
                ivPhoto.setImageBitmap(displayedFilteredBitmap);
            });
        });
    }

    private void saveCurrentPhoto() {
        if (displayedFilteredBitmap == null) {
            Toast.makeText(this, "Немає зображення для збереження", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSave.setEnabled(false);
        executorService.execute(() -> {
            Uri savedUri = PhotoFilterUtil.saveBitmapToGallery(this, displayedFilteredBitmap);
            runOnUiThread(() -> {
                btnSave.setEnabled(true);
                if (savedUri != null) {
                    Toast.makeText(
                            this,
                            "Збережено в галерею (Pictures/Photoshop2.0)!",
                            Toast.LENGTH_LONG
                    ).show();
                } else {
                    Toast.makeText(this, "Помилка збереження фото", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void updateUiState(boolean hasPhoto) {
        if (hasPhoto) {
            ivPhoto.setVisibility(View.VISIBLE);
            layoutPlaceholder.setVisibility(View.GONE);
            btnSave.setEnabled(true);
            btnReset.setEnabled(true);
            btnShare.setEnabled(true);
            btnShare.setAlpha(1.0f);
            btnReset.setAlpha(1.0f);
            toggleGroupCategories.setEnabled(true);
        } else {
            ivPhoto.setVisibility(View.GONE);
            layoutPlaceholder.setVisibility(View.VISIBLE);
            exitCropMode();
            btnSave.setEnabled(false);
            btnReset.setEnabled(false);
            btnShare.setEnabled(false);
            btnShare.setAlpha(0.35f);
            btnReset.setAlpha(0.35f);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }
}
