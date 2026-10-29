package com.genymobile.scrcpy;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.system.Os;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class PrivilegedCapture {

    private static final int SHELL_UID = 2000;
    private static Handler handler;

    interface UidSetter {
        void set(int uid) throws Exception;
    }

    private PrivilegedCapture() {
        // not instantiable
    }

    @SuppressWarnings("deprecation")
    @SuppressLint("DiscouragedPrivateApi")
    public static void init() throws Exception {
        if (Os.getuid() != 0) {
            throw new IOException("Secure capture requires root; authorize Shell in your root manager or use adb root");
        }
        // Keep shell file access and UHID input while retaining the real root UID.
        Process.class.getDeclaredMethod("setGroups", int[].class).invoke(null, (Object) new int[] {SHELL_UID, 3011});
        Os.setgid(SHELL_UID);
        Os.seteuid(SHELL_UID);
        handler = new Handler(Looper.getMainLooper());
    }

    static int getCaptureUid(int sdk) {
        // Android 12-13 SurfaceFlinger explicitly requires system or graphics, even for root.
        return sdk < AndroidVersions.API_34_ANDROID_14 ? 1000 : 0;
    }

    static <T> T runWithUid(int uid, Callable<T> action, UidSetter setter) throws Exception {
        setter.set(0);
        try {
            if (uid != 0) {
                setter.set(uid);
            }
            return action.call();
        } finally {
            // Regain CAP_SETUID before switching from system back to shell.
            setter.set(0);
            setter.set(SHELL_UID);
        }
    }

    @SuppressWarnings("deprecation")
    public static <T> T call(Callable<T> action) throws IOException {
        if (handler == null) {
            throw new IOException("Secure capture has not been initialized");
        }
        // Binder observes the main thread's effective UID. Serialize identity changes there.
        FutureTask<T> task = new FutureTask<>(() -> runWithUid(getCaptureUid(Build.VERSION.SDK_INT), action, Os::seteuid));
        if (Looper.myLooper() == handler.getLooper()) {
            task.run();
        } else if (!handler.post(task)) {
            throw new IOException("Secure capture main thread has stopped");
        }
        try {
            return task.get(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            task.cancel(false);
            Thread.currentThread().interrupt();
            throw new IOException("Secure capture interrupted", e);
        } catch (TimeoutException e) {
            task.cancel(false);
            throw new IOException("Secure capture timed out", e);
        } catch (ExecutionException e) {
            throw new IOException("Could not create secure display", e.getCause());
        }
    }
}
