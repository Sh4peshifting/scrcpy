package com.genymobile.scrcpy;

import org.junit.Assert;
import org.junit.Test;

public class OptionsTest {
    @Test
    public void testSecureCaptureIsOptIn() {
        Assert.assertFalse(Options.parse(BuildConfig.VERSION_NAME).getCaptureSecure());
        Assert.assertTrue(Options.parse(BuildConfig.VERSION_NAME, "capture_secure=true").getCaptureSecure());
        Assert.assertFalse(Options.parse(BuildConfig.VERSION_NAME, "capture_secure=false").getCaptureSecure());
    }
}
