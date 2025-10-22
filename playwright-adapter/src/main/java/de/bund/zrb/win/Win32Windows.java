package de.bund.zrb.win;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.*;
import com.sun.jna.ptr.IntByReference;

import java.time.Duration;

public final class Win32Windows {
    private Win32Windows() {}

    /**
     * Wartet bis zum Timeout auf ein Top-Level-/Hauptfenster des Prozesses.
     */
    public static WinDef.HWND waitForTopLevelWindowOfPid(int pid, Duration timeout) {
        final long deadline = System.currentTimeMillis() + timeout.toMillis();
        WinDef.HWND found;
        do {
            found = findTopLevelWindowOfPid(pid);
            if (found != null) return found;
            try { Thread.sleep(50); } catch (InterruptedException ignored) {}
        } while (System.currentTimeMillis() < deadline);
        return null;
    }

    /**
     * Sucht ein Top-Level-/Hauptfenster (kein Owner, sichtbar, GA_ROOT) eines Prozesses.
     */
    public static WinDef.HWND findTopLevelWindowOfPid(int pid) {
        final WinDef.HWND[] match = new WinDef.HWND[1];

        User32.INSTANCE.EnumWindows((h, data) -> {
            if (!User32.INSTANCE.IsWindowVisible(h)) return true;

            // Prozess-ID ermitteln
            IntByReference outPid = new IntByReference();
            User32.INSTANCE.GetWindowThreadProcessId(h, outPid);
            if (outPid.getValue() != pid) return true;

            if (isMainWindow(h)) {
                match[0] = h;
                return false; // Abbruch der Enumeration
            }
            return true;
        }, Pointer.NULL);

        return match[0];
    }

    /**
     * Fenstertitel lesen.
     */
    public static String getWindowText(WinDef.HWND h) {
        char[] buf = new char[1024];
        User32.INSTANCE.GetWindowText(h, buf, buf.length);
        return Native.toString(buf);
    }

    /**
     * Heuristik für „Main Window“ mit ausschließlich vorhandenen Konstanten:
     *  - kein Owner (GW_OWNER == null)
     *  - sichtbar
     *  - GetAncestor(GA_ROOT) == self
     */
    private static boolean isMainWindow(WinDef.HWND h) {
        // kein Owner?
        WinDef.HWND owner = User32.INSTANCE.GetWindow(h, new WinDef.DWORD(WinUser.GW_OWNER));
        if (owner != null) return false;

        // sichtbar?
        if (!User32.INSTANCE.IsWindowVisible(h)) return false;

        // Root?
        WinDef.HWND root = User32.INSTANCE.GetAncestor(h, WinUser.GA_ROOT);
        return root != null && root.equals(h);
    }
}
