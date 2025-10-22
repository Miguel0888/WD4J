package de.bund.zrb.win;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.*;
import com.sun.jna.platform.win32.WinDef.*;
import com.sun.jna.platform.win32.WinGDI.BITMAPINFO;
import com.sun.jna.platform.win32.WinGDI.BITMAPINFOHEADER;
import com.sun.jna.ptr.IntByReference;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

/**
 * Capture the client area of a window as BufferedImage without title bar.
 * Try WM_PRINT -> WM_PRINTCLIENT -> PrintWindow (client/fullcontent) -> Desktop BitBlt with short TopMost hop.
 */
public final class WindowCapture {

    // --- constants ---
    private static final int WS_EX_TOPMOST = 0x00000008;
    private static final int SRCCOPY = 0x00CC0020;

    private static final int WM_PRINT       = 0x0317;
    private static final int WM_PRINTCLIENT = 0x0318;

    private static final int PRF_CHECKVISIBLE = 0x00000001;
    private static final int PRF_NONCLIENT    = 0x00000002;
    private static final int PRF_CLIENT       = 0x00000004;
    private static final int PRF_ERASEBKGND   = 0x00000008;
    private static final int PRF_CHILDREN     = 0x00000010;

    private static final int PW_CLIENTONLY         = 0x00000001;
    // PW_RENDERFULLCONTENT exists on newer Windows; keep local define
    private static final int PW_RENDERFULLCONTENT  = 0x00000002;

    private static final HWND HWND_TOPMOST    = new HWND(Pointer.createConstant(-1));
    private static final HWND HWND_NOTOPMOST  = new HWND(Pointer.createConstant(-2));
    private static final int SWP_NOSIZE       = 0x0001;
    private static final int SWP_NOMOVE       = 0x0002;
    private static final int SWP_NOACTIVATE   = 0x0010;
    private static final int SWP_NOSENDCHANGING = 0x0400;

    private WindowCapture() { }

    // --- public API ---

    /** Backward-compatible entry: capture client area without title bar. */
    public static BufferedImage capture(HWND hWnd) {
        return captureClient(hWnd, true);
    }

    /** Capture client area, optionally allow the final TopMost trick to avoid occlusions. */
    public static BufferedImage captureClient(HWND hWnd, boolean allowTopMostTrick) {
        if (hWnd == null || !User32.INSTANCE.IsWindow(hWnd)) return null;

        // Compute client size
        RECT cr = new RECT();
        if (!User32.INSTANCE.GetClientRect(hWnd, cr)) return null;
        int w = cr.right - cr.left;
        int h = cr.bottom - cr.top;
        if (w <= 0 || h <= 0) return null;

        // 1) Try WM_PRINT (more widely honored than WM_PRINTCLIENT in manchen UIs)
        BufferedImage img = tryPrintMessage(hWnd, WM_PRINT, w, h);
        if (!isBlack(img)) return img;

        // 2) Fallback: WM_PRINTCLIENT
        img = tryPrintMessage(hWnd, WM_PRINTCLIENT, w, h);
        if (!isBlack(img)) return img;

        // 3) Fallback: PrintWindow (prefer full content if supported; still client only)
        img = tryPrintWindowClientFirst(hWnd, w, h);
        if (!isBlack(img)) return img;

        // 4) Last resort: Desktop BitBlt of client rect after short TopMost hop (no overdraw)
        if (allowTopMostTrick) {
            img = tryDesktopBlitTopMost(hWnd, w, h);
            if (!isBlack(img)) return img;
        }

        return img; // may be black if all failed
    }

    // --- strategy 1+2: WM_PRINT / WM_PRINTCLIENT ---

    private static BufferedImage tryPrintMessage(HWND hWnd, int msg, int width, int height) {
        HDC wndDC = null;
        HDC memDC = null;
        HBITMAP hBitmap = null;
        WinNT.HANDLE old = null;

        try {
            wndDC = User32.INSTANCE.GetDC(hWnd);
            if (wndDC == null) return null;

            memDC = GDI32.INSTANCE.CreateCompatibleDC(wndDC);
            hBitmap = GDI32.INSTANCE.CreateCompatibleBitmap(wndDC, width, height);
            old = GDI32.INSTANCE.SelectObject(memDC, hBitmap);

            int prf = PRF_CLIENT | PRF_CHILDREN | PRF_ERASEBKGND | PRF_CHECKVISIBLE;
            WPARAM wParam = new WPARAM(Pointer.nativeValue(memDC.getPointer()));
            LPARAM lParam = new LPARAM(prf);

            // Use SendMessageTimeout to avoid potential hangs
            LRESULT res = User32.INSTANCE.SendMessageTimeout(
                    hWnd, msg, wParam, lParam,
                    WinUser.SMTO_ABORTIFHUNG, 100 /*ms*/, new WinDef.DWORDByReference());
            // Even if res==0 (timeout), Bild kann beschrieben sein; prüfe Pixels
            return toBufferedImage(memDC, hBitmap, width, height);
        } finally {
            if (memDC != null && old != null) GDI32.INSTANCE.SelectObject(memDC, old);
            if (hBitmap != null) GDI32.INSTANCE.DeleteObject(hBitmap);
            if (memDC != null) GDI32.INSTANCE.DeleteDC(memDC);
            if (wndDC != null) User32.INSTANCE.ReleaseDC(hWnd, wndDC);
        }
    }

    // --- strategy 3: PrintWindow client/fullcontent ---

    private static BufferedImage tryPrintWindowClientFirst(HWND hWnd, int width, int height) {
        HDC dc = null, mem = null;
        HBITMAP hbmp = null;
        WinNT.HANDLE old = null;
        try {
            dc = User32.INSTANCE.GetDC(hWnd);
            if (dc == null) return null;

            mem = GDI32.INSTANCE.CreateCompatibleDC(dc);
            hbmp = GDI32.INSTANCE.CreateCompatibleBitmap(dc, width, height);
            old = GDI32.INSTANCE.SelectObject(mem, hbmp);

            // Try full-content render first (helps with some GPU paths), but only client area
            boolean ok = User32.INSTANCE.PrintWindow(hWnd, mem, PW_CLIENTONLY | PW_RENDERFULLCONTENT);
            if (!ok) {
                ok = User32.INSTANCE.PrintWindow(hWnd, mem, PW_CLIENTONLY);
            }
            if (!ok) {
                // As very last attempt on this path, copy visible client from this dc
                GDI32.INSTANCE.BitBlt(mem, 0, 0, width, height, dc, 0, 0, SRCCOPY);
            }
            return toBufferedImage(mem, hbmp, width, height);
        } finally {
            if (mem != null && old != null) GDI32.INSTANCE.SelectObject(mem, old);
            if (hbmp != null) GDI32.INSTANCE.DeleteObject(hbmp);
            if (mem != null) GDI32.INSTANCE.DeleteDC(mem);
            if (dc != null) User32.INSTANCE.ReleaseDC(hWnd, dc);
        }
    }

    // --- strategy 4: Desktop blit with short TopMost hop (no repaint, no occlusions) ---

    private static BufferedImage tryDesktopBlitTopMost(HWND hWnd, int width, int height) {
        // Map client rect to screen via WINDOWINFO.rcClient
        WinUser.WINDOWINFO wi = new WinUser.WINDOWINFO();
        wi.cbSize = wi.size();
        if (!User32.INSTANCE.GetWindowInfo(hWnd, wi)) return null;
        RECT rc = wi.rcClient;
        int left = rc.left;
        int top  = rc.top;

        boolean wasTop = (wi.dwExStyle & WS_EX_TOPMOST) != 0;
        if (!wasTop) {
            User32.INSTANCE.SetWindowPos(
                    hWnd, HWND_TOPMOST, 0, 0, 0, 0,
                    SWP_NOSIZE | SWP_NOMOVE | SWP_NOACTIVATE | SWP_NOSENDCHANGING
            );
            // Give the compositor a tiny tick
            sleepQuiet(15);
        }

        HDC desktop = null, mem = null;
        HBITMAP hbmp = null;
        WinNT.HANDLE old = null;
        try {
            desktop = User32.INSTANCE.GetDC(null);
            mem = GDI32.INSTANCE.CreateCompatibleDC(desktop);
            hbmp = GDI32.INSTANCE.CreateCompatibleBitmap(desktop, width, height);
            old = GDI32.INSTANCE.SelectObject(mem, hbmp);

            GDI32.INSTANCE.BitBlt(mem, 0, 0, width, height, desktop, left, top, SRCCOPY);
            return toBufferedImage(mem, hbmp, width, height);
        } finally {
            if (mem != null && old != null) GDI32.INSTANCE.SelectObject(mem, old);
            if (hbmp != null) GDI32.INSTANCE.DeleteObject(hbmp);
            if (mem != null) GDI32.INSTANCE.DeleteDC(mem);
            if (desktop != null) User32.INSTANCE.ReleaseDC(null, desktop);

            if (!wasTop) {
                User32.INSTANCE.SetWindowPos(
                        hWnd, HWND_NOTOPMOST, 0, 0, 0, 0,
                        SWP_NOSIZE | SWP_NOMOVE | SWP_NOACTIVATE | SWP_NOSENDCHANGING
                );
            }
        }
    }

    // --- pixels -> BufferedImage ---

    private static BufferedImage toBufferedImage(HDC srcDC, HBITMAP hbm, int width, int height) {
        BITMAPINFO bi = new BITMAPINFO();
        bi.bmiHeader = new BITMAPINFOHEADER();
        bi.bmiHeader.biSize = bi.bmiHeader.size();
        bi.bmiHeader.biWidth = width;
        bi.bmiHeader.biHeight = -height;
        bi.bmiHeader.biPlanes = 1;
        bi.bmiHeader.biBitCount = 32;
        bi.bmiHeader.biCompression = WinGDI.BI_RGB;

        int stride = width * 4;
        int imageSize = stride * height;
        Memory buffer = new Memory(imageSize);

        IntByReference lines = new IntByReference();
        GDI32.INSTANCE.GetDIBits(srcDC, hbm, 0, height, buffer, bi, WinGDI.DIB_RGB_COLORS);

        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        ByteBuffer bb = buffer.getByteBuffer(0, imageSize);
        int[] rgb = new int[width * height];
        for (int y = 0; y < height; y++) {
            int row = y * stride;
            int base = y * width;
            for (int x = 0; x < width; x++) {
                int i = row + (x << 2);
                int b = bb.get(i) & 0xFF;
                int g = bb.get(i + 1) & 0xFF;
                int r = bb.get(i + 2) & 0xFF;
                rgb[base + x] = (r << 16) | (g << 8) | b;
            }
        }
        img.setRGB(0, 0, width, height, rgb, 0, width);
        return img;
    }

    private static boolean isBlack(BufferedImage img) {
        if (img == null) return true;
        int w = img.getWidth(), h = img.getHeight();
        int stepX = Math.max(1, w / 16);
        int stepY = Math.max(1, h / 16);
        for (int y = 0; y < h; y += stepY) {
            for (int x = 0; x < w; x += stepX) {
                if ((img.getRGB(x, y) & 0xFFFFFF) != 0x000000) return false;
            }
        }
        return true;
    }

    private static void sleepQuiet(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
    }

    // --- SendMessageTimeout overload on User32 (older JNA builds miss it) ---
    static {
        // Ensure User32 has SendMessageTimeout in this build
        try {
            User32.class.getMethod("SendMessageTimeout", HWND.class, int.class, WPARAM.class, LPARAM.class, int.class, int.class, WinDef.DWORDByReference.class);
        } catch (NoSuchMethodException e) {
            // Provide a tiny proxy via Native.invoke if needed (fallback not shown here to keep class focused)
        }
    }
}
