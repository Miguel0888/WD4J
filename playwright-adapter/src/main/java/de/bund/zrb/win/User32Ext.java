package de.bund.zrb.win;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.win32.W32APIOptions;

public interface User32Ext extends com.sun.jna.platform.win32.User32 {
    User32Ext INSTANCE = Native.load("user32", User32Ext.class, W32APIOptions.DEFAULT_OPTIONS);

    // BOOL PrintWindow(HWND hwnd, HDC hdcBlt, UINT nFlags);
    boolean PrintWindow(WinDef.HWND hWnd, WinDef.HDC hDC, int nFlags);
}
