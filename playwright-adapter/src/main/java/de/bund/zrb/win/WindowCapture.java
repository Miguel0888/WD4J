package de.bund.zrb.win;

import com.sun.jna.Memory;
import com.sun.jna.platform.win32.*;
import com.sun.jna.platform.win32.WinDef.HBITMAP;
import com.sun.jna.platform.win32.WinDef.HDC;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinDef.RECT;
import com.sun.jna.platform.win32.WinGDI.BITMAPINFO;
import com.sun.jna.platform.win32.WinGDI.BITMAPINFOHEADER;
import com.sun.jna.ptr.IntByReference;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

public final class WindowCapture {

    // Avoid JNA version dependency
    private static final int SRCCOPY = 0x00CC0020;
    private static final int PW_CLIENTONLY = 0x00000001;

    public enum Strategy {
        NON_INTRUSIVE,  // BitBlt from desktop (no repaint, no flicker; requires visible area)
        PRINTWINDOW     // PrintWindow PW_CLIENTONLY (works when occluded; may flicker)
    }

    private WindowCapture() { }

    /**
     * Backwards compatible entry point.
     * Capture only the client area using non-intrusive desktop copy (no flicker).
     */
    public static BufferedImage capture(WinDef.HWND hWnd) {
        return capture(hWnd, Strategy.NON_INTRUSIVE);
    }

    /**
     * Capture client area with explicit strategy.
     */
    public static BufferedImage capture(WinDef.HWND hWnd, Strategy strategy) {
        if (hWnd == null) return null;
        if (strategy == Strategy.PRINTWINDOW) {
            return captureWithPrintWindow(hWnd);
        }
        return captureFromDesktop(hWnd);
    }

    // ---------- Strategy 1: Non-intrusive desktop copy (no repaint, no flicker) ----------

    private static BufferedImage captureFromDesktop(HWND hWnd) {
        ClientOnScreen client = getClientOnScreen(hWnd);
        if (!client.valid()) return null;

        HDC desktopDC = null;
        HDC memDC = null;
        HBITMAP hBitmap = null;
        WinNT.HANDLE oldObj = null;

        try {
            // Grab desktop DC (entire virtual screen for NULL hwnd)
            desktopDC = User32.INSTANCE.GetDC(null);

            // Create target mem DC/Bitmap
            memDC = GDI32.INSTANCE.CreateCompatibleDC(desktopDC);
            hBitmap = GDI32.INSTANCE.CreateCompatibleBitmap(desktopDC, client.width, client.height);
            oldObj = GDI32.INSTANCE.SelectObject(memDC, hBitmap);

            // Copy pixels from desktop at client's screen rectangle
            GDI32.INSTANCE.BitBlt(
                    memDC, 0, 0, client.width, client.height,
                    desktopDC, client.left, client.top,
                    SRCCOPY
            );

            return toBufferedImage(memDC, hBitmap, client.width, client.height);
        } finally {
            if (memDC != null && oldObj != null) {
                GDI32.INSTANCE.SelectObject(memDC, oldObj);
            }
            if (hBitmap != null) {
                GDI32.INSTANCE.DeleteObject(hBitmap);
            }
            if (memDC != null) {
                GDI32.INSTANCE.DeleteDC(memDC);
            }
            if (desktopDC != null) {
                User32.INSTANCE.ReleaseDC(null, desktopDC);
            }
        }
    }

    // ---------- Strategy 2: PrintWindow (works when occluded; can flicker) ----------

    private static BufferedImage captureWithPrintWindow(HWND hWnd) {
        RECT r = new RECT();
        User32.INSTANCE.GetClientRect(hWnd, r);
        int width = r.right - r.left;
        int height = r.bottom - r.top;
        if (width <= 0 || height <= 0) return null;

        HDC windowDC = null;
        HDC memDC = null;
        HBITMAP hBitmap = null;
        WinNT.HANDLE oldObj = null;

        try {
            windowDC = User32.INSTANCE.GetDC(hWnd);
            if (windowDC == null) return null;

            memDC = GDI32.INSTANCE.CreateCompatibleDC(windowDC);
            hBitmap = GDI32.INSTANCE.CreateCompatibleBitmap(windowDC, width, height);
            oldObj = GDI32.INSTANCE.SelectObject(memDC, hBitmap);

            boolean ok = User32.INSTANCE.PrintWindow(hWnd, memDC, PW_CLIENTONLY);
            if (!ok) {
                // Fallback: copy from this window's DC (still intrusive)
                GDI32.INSTANCE.BitBlt(memDC, 0, 0, width, height, windowDC, 0, 0, SRCCOPY);
            }

            return toBufferedImage(memDC, hBitmap, width, height);
        } finally {
            if (memDC != null && oldObj != null) {
                GDI32.INSTANCE.SelectObject(memDC, oldObj);
            }
            if (hBitmap != null) {
                GDI32.INSTANCE.DeleteObject(hBitmap);
            }
            if (memDC != null) {
                GDI32.INSTANCE.DeleteDC(memDC);
            }
            if (windowDC != null) {
                User32.INSTANCE.ReleaseDC(hWnd, windowDC);
            }
        }
    }

    // ---------- Helpers ----------

    /**
     * Determine client rectangle in screen coordinates using GetWindowInfo.
     */
    private static ClientOnScreen getClientOnScreen(HWND hWnd) {
        WinUser.WINDOWINFO wi = new WinUser.WINDOWINFO();
        wi.cbSize = wi.size();
        boolean ok = User32.INSTANCE.GetWindowInfo(hWnd, wi);
        if (!ok) return ClientOnScreen.invalid();

        RECT rc = wi.rcClient; // already in screen coordinates
        int w = rc.right - rc.left;
        int h = rc.bottom - rc.top;
        if (w <= 0 || h <= 0) return ClientOnScreen.invalid();

        return new ClientOnScreen(rc.left, rc.top, w, h);
    }

    private static BufferedImage toBufferedImage(HDC srcDC, HBITMAP hbm, int width, int height) {
        BITMAPINFO bmi = new BITMAPINFO();
        bmi.bmiHeader = new BITMAPINFOHEADER();
        bmi.bmiHeader.biSize = bmi.bmiHeader.size();
        bmi.bmiHeader.biWidth = width;
        bmi.bmiHeader.biHeight = -height; // top-down
        bmi.bmiHeader.biPlanes = 1;
        bmi.bmiHeader.biBitCount = 32;
        bmi.bmiHeader.biCompression = WinGDI.BI_RGB;

        int stride = width * 4;
        int imageSize = stride * height;
        Memory buffer = new Memory(imageSize);

        IntByReference lines = new IntByReference();
        GDI32.INSTANCE.GetDIBits(srcDC, hbm, 0, height, buffer, bmi, WinGDI.DIB_RGB_COLORS);

        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        ByteBuffer bb = buffer.getByteBuffer(0, imageSize);
        int[] rgb = new int[width * height];

        for (int y = 0; y < height; y++) {
            int rowOffset = y * stride;
            int p = y * width;
            for (int x = 0; x < width; x++) {
                int i = rowOffset + (x << 2);
                int b = bb.get(i) & 0xFF;
                int g = bb.get(i + 1) & 0xFF;
                int r = bb.get(i + 2) & 0xFF;
                rgb[p + x] = (r << 16) | (g << 8) | b;
            }
        }

        img.setRGB(0, 0, width, height, rgb, 0, width);
        return img;
    }

    // Small value object to express intent
    private static final class ClientOnScreen {
        final int left;
        final int top;
        final int width;
        final int height;

        ClientOnScreen(int left, int top, int width, int height) {
            this.left = left;
            this.top = top;
            this.width = width;
            this.height = height;
        }

        static ClientOnScreen invalid() {
            return new ClientOnScreen(0, 0, -1, -1);
        }

        boolean valid() {
            return width > 0 && height > 0;
        }
    }
}
