package com.genymobile.scrcpy;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PrivilegedCaptureTest {
    @Test
    public void testVersionPolicy() {
        Assert.assertEquals(1000, PrivilegedCapture.getCaptureUid(31));
        Assert.assertEquals(1000, PrivilegedCapture.getCaptureUid(32));
        Assert.assertEquals(1000, PrivilegedCapture.getCaptureUid(33));
        Assert.assertEquals(0, PrivilegedCapture.getCaptureUid(34));
        Assert.assertEquals(0, PrivilegedCapture.getCaptureUid(35));
        Assert.assertEquals(0, PrivilegedCapture.getCaptureUid(36));
    }

    @Test
    public void testSystemCaptureRestoresShell() throws Exception {
        List<Integer> uids = new ArrayList<>();
        String result = PrivilegedCapture.runWithUid(1000, () -> {
            Assert.assertEquals(Arrays.asList(0, 1000), uids);
            return "display";
        }, uids::add);
        Assert.assertEquals("display", result);
        Assert.assertEquals(Arrays.asList(0, 1000, 0, 2000), uids);
    }

    @Test
    public void testRootCaptureRestoresShellAfterFailure() throws Exception {
        List<Integer> uids = new ArrayList<>();
        IOException failure = new IOException("denied");
        try {
            PrivilegedCapture.runWithUid(0, () -> {
                throw failure;
            }, uids::add);
            Assert.fail("Capture must fail");
        } catch (IOException e) {
            Assert.assertSame(failure, e);
        }
        Assert.assertEquals(Arrays.asList(0, 0, 2000), uids);
    }

    @Test
    public void testSystemSwitchFailureRestoresShell() throws Exception {
        List<Integer> uids = new ArrayList<>();
        try {
            PrivilegedCapture.runWithUid(1000, () -> {
                Assert.fail("Capture must not run after a failed UID switch");
                return null;
            }, uid -> {
                uids.add(uid);
                if (uid == 1000) {
                    throw new IOException("denied");
                }
            });
            Assert.fail("UID switch must fail");
        } catch (IOException e) {
            Assert.assertEquals("denied", e.getMessage());
        }
        Assert.assertEquals(Arrays.asList(0, 1000, 0, 2000), uids);
    }

    @Test
    public void testRestoreFailureIsNotIgnored() throws Exception {
        try {
            PrivilegedCapture.runWithUid(0, () -> "display", uid -> {
                if (uid == 2000) {
                    throw new IOException("restore denied");
                }
            });
            Assert.fail("A failed identity restore must abort capture");
        } catch (IOException e) {
            Assert.assertEquals("restore denied", e.getMessage());
        }
    }
}
