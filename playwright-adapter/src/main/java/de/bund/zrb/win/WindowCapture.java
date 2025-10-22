package de.bund.zrb.win;

import com.sun.jna.Memory;
import com.sun.jna.platform.win32.*;
import com.sun.jna.platform.win32.WinGDI.BITMAPINFO;
import com.sun.jna.platform.win32.WinGDI.BITMAPINFOHEADER;
import com.sun.jna.ptr.IntByReference;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

public final class WindowCapture {
    private WindowCapture(){}

    public static BufferedImage capture(WinDef.HWND hWnd) {
        WinDef.RECT rect = new WinDef.RECT();
        User32.INSTANCE.GetWindowRect(hWnd, rect);
        int w = rect.right - rect.left;
        int h = rect.bottom - rect.top;
        if (w <= 0 || h <= 0) return null;

        WinDef.HDC hdcWindow = User32.INSTANCE.GetDC(hWnd);
        WinDef.HDC hdcMem    = GDI32.INSTANCE.CreateCompatibleDC(hdcWindow);
        WinDef.HBITMAP hbm   = GDI32.INSTANCE.CreateCompatibleBitmap(hdcWindow, w, h);
        WinNT.HANDLE old     = GDI32.INSTANCE.SelectObject(hdcMem, hbm);

        // Flags: 0 = Standard, 2 rendert non-client Bereich in vielen Fällen besser
        User32Ext.INSTANCE.PrintWindow(hWnd, hdcMem, 0);

        // Bitmap -> BufferedImage
        BITMAPINFO bmi = new BITMAPINFO();
        bmi.bmiHeader = new BITMAPINFOHEADER();
        bmi.bmiHeader.biSize        = bmi.bmiHeader.size();
        bmi.bmiHeader.biWidth       = w;
        bmi.bmiHeader.biHeight      = -h; // top-down
        bmi.bmiHeader.biPlanes      = 1;
        bmi.bmiHeader.biBitCount    = 32;
        bmi.bmiHeader.biCompression = WinGDI.BI_RGB;

        int stride = w * 4;
        int imageSize = stride * h;
        Memory buffer = new Memory(imageSize);

        IntByReference lines = new IntByReference();
        GDI32.INSTANCE.GetDIBits(hdcMem, hbm, 0, h, buffer, bmi, WinGDI.DIB_RGB_COLORS);

        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        ByteBuffer bb = buffer.getByteBuffer(0, imageSize);
        int[] rgb = new int[w * h];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int i = y * stride + x * 4;
                int b = bb.get(i)   & 0xFF;
                int g = bb.get(i+1) & 0xFF;
                int r = bb.get(i+2) & 0xFF;
                rgb[y * w + x] = (r << 16) | (g << 8) | b;
            }
        }
        img.setRGB(0, 0, w, h, rgb, 0, w);

        // Cleanup
        GDI32.INSTANCE.SelectObject(hdcMem, old);
        GDI32.INSTANCE.DeleteObject(hbm);
        GDI32.INSTANCE.DeleteDC(hdcMem);
        User32.INSTANCE.ReleaseDC(hWnd, hdcWindow);

        return img;
    }
}
