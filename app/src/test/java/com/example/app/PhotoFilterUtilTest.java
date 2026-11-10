package com.example.app;

import org.junit.Test;

import static org.junit.Assert.*;

public class PhotoFilterUtilTest {

    @Test
    public void testFilterTypesCompleteness() {
        PhotoFilterUtil.FilterType[] filters = PhotoFilterUtil.FilterType.values();
        assertTrue("Should have at least 10 filters", filters.length >= 10);

        for (PhotoFilterUtil.FilterType filter : filters) {
            assertNotNull(filter.getDisplayName());
            assertFalse(filter.getDisplayName().trim().isEmpty());
        }
    }

    @Test
    public void testSpecificFiltersExist() {
        assertNotNull(PhotoFilterUtil.FilterType.valueOf("ORIGINAL"));
        assertNotNull(PhotoFilterUtil.FilterType.valueOf("GRAYSCALE"));
        assertNotNull(PhotoFilterUtil.FilterType.valueOf("SEPIA"));
        assertNotNull(PhotoFilterUtil.FilterType.valueOf("CINEMATIC"));
        assertNotNull(PhotoFilterUtil.FilterType.valueOf("WARM"));
        assertNotNull(PhotoFilterUtil.FilterType.valueOf("COOL"));
        assertNotNull(PhotoFilterUtil.FilterType.valueOf("VIVID"));
        assertNotNull(PhotoFilterUtil.FilterType.valueOf("VINTAGE"));
        assertNotNull(PhotoFilterUtil.FilterType.valueOf("NOIR"));
        assertNotNull(PhotoFilterUtil.FilterType.valueOf("CYBERPUNK"));
        assertNotNull(PhotoFilterUtil.FilterType.valueOf("DRAMATIC"));
        assertNotNull(PhotoFilterUtil.FilterType.valueOf("VIGNETTE"));
        assertNotNull(PhotoFilterUtil.FilterType.valueOf("BLUR"));
        assertNotNull(PhotoFilterUtil.FilterType.valueOf("INVERT"));
    }

    @Test
    public void testNullSafety() {
        assertNull(PhotoFilterUtil.applyFilter(null, PhotoFilterUtil.FilterType.GRAYSCALE));
        assertNull(PhotoFilterUtil.rotateBitmap(null, 90f));
        assertNull(PhotoFilterUtil.flipHorizontal(null));
        assertNull(PhotoFilterUtil.flipVertical(null));
        assertNull(PhotoFilterUtil.cropBitmap(null, null));
        assertNull(PhotoFilterUtil.applyVignette(null));
        assertNull(PhotoFilterUtil.applyFastBlur(null, 5));
        assertNull(PhotoFilterUtil.applyAdjustments(null, 0f, 1f, 1f));
    }
}
