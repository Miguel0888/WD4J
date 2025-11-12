package de.bund.zrb.win;

import com.sun.jna.ptr.IntByReference;
import com.sun.jna.platform.win32.*;
import com.sun.jna.Native;

import java.util.Locale;

/** Windows-spezifische Prozess-Erkennung für Browser-Exe. */
public class WindowsBrowserProcessService implements BrowserProcessService {
    @Override
    public BrowserInstanceState detectBrowserInstanceState(String expectedExecutablePath) {
        if (expectedExecutablePath == null) {
            throw new IllegalArgumentException("Executable path must not be null.");
        }
        String normalizedExpected = normalizePath(expectedExecutablePath);
        String expectedFileName = extractFileName(normalizedExpected);

        WinNT.HANDLE snapshot = Kernel32.INSTANCE.CreateToolhelp32Snapshot(
                Tlhelp32.TH32CS_SNAPPROCESS,
                new WinDef.DWORD(0)
        );
        if (WinBase.INVALID_HANDLE_VALUE.equals(snapshot)) {
            return BrowserInstanceState.UNKNOWN;
        }
        try {
            Tlhelp32.PROCESSENTRY32.ByReference entry = new Tlhelp32.PROCESSENTRY32.ByReference();
            if (!Kernel32.INSTANCE.Process32First(snapshot, entry)) {
                return BrowserInstanceState.UNKNOWN;
            }
            do {
                int pid = entry.th32ProcessID.intValue();
                if (pid == 0) continue;
                String imagePath = queryProcessImagePath(pid);
                if (imagePath != null) {
                    String normalizedActual = normalizePath(imagePath);
                    // 1) Vollständiger Pfad identisch
                    if (normalizedActual.equals(normalizedExpected)) {
                        return BrowserInstanceState.RUNNING;
                    }
                    // 2) Dateiname identisch (Pfad darf abweichen)
                    String actualFileName = extractFileName(normalizedActual);
                    if (actualFileName != null && actualFileName.equals(expectedFileName)) {
                        return BrowserInstanceState.RUNNING;
                    }
                } else {
                    // Fallback: Nur Dateiname vergleichen wenn Pfad nicht ermittelbar
                    String procExeName = extractExeName(entry.szExeFile);
                    if (procExeName != null && procExeName.equals(expectedFileName)) {
                        return BrowserInstanceState.RUNNING;
                    }
                }
            } while (Kernel32.INSTANCE.Process32Next(snapshot, entry));
            return BrowserInstanceState.NOT_RUNNING;
        } catch (Exception e) {
            return BrowserInstanceState.UNKNOWN;
        } finally {
            Kernel32.INSTANCE.CloseHandle(snapshot);
        }
    }

    @Override
    public BrowserTerminationResult terminateBrowserInstances(String expectedExecutablePath) {
        if (expectedExecutablePath == null) {
            throw new IllegalArgumentException("Executable path must not be null.");
        }
        String normalizedExpected = normalizePath(expectedExecutablePath);
        String expectedFileName = extractFileName(normalizedExpected);
        WinNT.HANDLE snapshot = Kernel32.INSTANCE.CreateToolhelp32Snapshot(
                Tlhelp32.TH32CS_SNAPPROCESS,
                new WinDef.DWORD(0)
        );
        if (WinBase.INVALID_HANDLE_VALUE.equals(snapshot)) {
            return new BrowserTerminationResult(0, 0, true);
        }
        int terminatedCount = 0;
        int failureCount = 0;
        boolean detectionFailed = false;
        try {
            Tlhelp32.PROCESSENTRY32.ByReference entry = new Tlhelp32.PROCESSENTRY32.ByReference();
            if (!Kernel32.INSTANCE.Process32First(snapshot, entry)) {
                return new BrowserTerminationResult(0, 0, true);
            }
            do {
                int pid = entry.th32ProcessID.intValue();
                if (pid == 0) continue;
                String imagePath = queryProcessImagePath(pid);
                boolean match = false;
                if (imagePath != null) {
                    String normalizedActual = normalizePath(imagePath);
                    if (normalizedActual.equals(normalizedExpected)) {
                        match = true;
                    } else {
                        String actualFileName = extractFileName(normalizedActual);
                        match = actualFileName != null && actualFileName.equals(expectedFileName);
                    }
                } else if (entry.szExeFile != null) {
                    String procExeName = extractExeName(entry.szExeFile);
                    match = procExeName != null && procExeName.equals(expectedFileName);
                }
                if (!match) continue;
                boolean terminated = terminateProcessByPid(pid);
                if (terminated) terminatedCount++; else failureCount++;
            } while (Kernel32.INSTANCE.Process32Next(snapshot, entry));
        } catch (Exception e) {
            detectionFailed = true;
        } finally {
            Kernel32.INSTANCE.CloseHandle(snapshot);
        }
        return new BrowserTerminationResult(terminatedCount, failureCount, detectionFailed);
    }

    private String queryProcessImagePath(int pid) {
        WinNT.HANDLE processHandle = Kernel32.INSTANCE.OpenProcess(
                WinNT.PROCESS_QUERY_LIMITED_INFORMATION,
                false,
                pid
        );
        if (processHandle == null) return null;
        try {
            char[] buffer = new char[WinDef.MAX_PATH * 2];
            IntByReference size = new IntByReference(buffer.length);
            boolean success = Kernel32.INSTANCE.QueryFullProcessImageName(
                    processHandle,
                    0,
                    buffer,
                    size
            );
            if (!success) return null;
            return new String(buffer, 0, size.getValue());
        } finally {
            Kernel32.INSTANCE.CloseHandle(processHandle);
        }
    }

    private boolean terminateProcessByPid(int pid) {
        WinNT.HANDLE processHandle = Kernel32.INSTANCE.OpenProcess(
                WinNT.PROCESS_TERMINATE,
                false,
                pid
        );
        if (processHandle == null) return false;
        try {
            return Kernel32.INSTANCE.TerminateProcess(processHandle, 1);
        } finally {
            Kernel32.INSTANCE.CloseHandle(processHandle);
        }
    }

    private String normalizePath(String path) {
        return path.replace('/', '\\').toLowerCase(Locale.ROOT).trim();
    }

    private String extractFileName(String normalizedPath) {
        if (normalizedPath == null) return null;
        int idx = normalizedPath.lastIndexOf('\\');
        return idx >= 0 ? normalizedPath.substring(idx + 1) : normalizedPath;
    }

    private String extractExeName(char[] nameChars) {
        if (nameChars == null) return null;
        String s = Native.toString(nameChars);
        if (s == null) return null;
        return s.toLowerCase(Locale.ROOT).trim();
    }
}
