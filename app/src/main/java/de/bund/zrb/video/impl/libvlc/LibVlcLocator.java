package de.bund.zrb.video.impl.libvlc;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Locates LibVLC/VLC installation on the system and sets up JNA properties.
 * Supports Windows, macOS, and Linux.
 */
public final class LibVlcLocator {
    
    private static final boolean LOG_ENABLED = Boolean.getBoolean("wd4j.log.video");
    
    private LibVlcLocator() {}
    
    /**
     * Attempts to locate VLC installation and configure JNA properties.
     * 
     * @return true if VLC was found and properties were set successfully
     */
    public static boolean locateAndConfigure() {
        String os = System.getProperty("os.name", "").toLowerCase();
        
        if (os.contains("win")) {
            return locateWindows();
        } else if (os.contains("mac")) {
            return locateMacOS();
        } else if (os.contains("linux") || os.contains("nix") || os.contains("nux")) {
            return locateLinux();
        }
        
        log("Unsupported OS: " + os);
        return false;
    }
    
    /**
     * Checks if vlcj is available on the classpath.
     * 
     * @return true if vlcj classes can be loaded
     */
    public static boolean isVlcjAvailable() {
        try {
            Class.forName("uk.co.caprica.vlcj.factory.discovery.NativeDiscovery");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
    
    /**
     * Uses vlcj's NativeDiscovery to find VLC.
     * Only call this if isVlcjAvailable() returns true.
     * 
     * @return true if discovery succeeded
     */
    public static boolean useVlcjDiscovery() {
        try {
            Class<?> discoveryClass = Class.forName("uk.co.caprica.vlcj.factory.discovery.NativeDiscovery");
            Object discovery = discoveryClass.newInstance();
            Boolean found = (Boolean) discoveryClass.getMethod("discover").invoke(discovery);
            log("vlcj NativeDiscovery result: " + found);
            return found != null && found;
        } catch (Exception e) {
            log("vlcj NativeDiscovery failed: " + e.getMessage());
            return false;
        }
    }
    
    private static boolean locateWindows() {
        List<String> candidates = new ArrayList<String>();
        
        // Common installation paths
        candidates.add("C:\\Program Files\\VideoLAN\\VLC");
        candidates.add("C:\\Program Files (x86)\\VideoLAN\\VLC");
        
        // Environment variable
        String vlcPath = System.getenv("VLC_HOME");
        if (vlcPath != null && !vlcPath.trim().isEmpty()) {
            candidates.add(vlcPath.trim());
        }
        
        // Program Files variations
        String programFiles = System.getenv("ProgramFiles");
        if (programFiles != null) {
            candidates.add(programFiles + "\\VideoLAN\\VLC");
        }
        String programFilesX86 = System.getenv("ProgramFiles(x86)");
        if (programFilesX86 != null) {
            candidates.add(programFilesX86 + "\\VideoLAN\\VLC");
        }
        
        for (String candidate : candidates) {
            Path vlcDir = Paths.get(candidate);
            if (Files.isDirectory(vlcDir)) {
                Path libvlcDll = vlcDir.resolve("libvlc.dll");
                Path libvlccoreDll = vlcDir.resolve("libvlccore.dll");
                Path pluginsDir = vlcDir.resolve("plugins");
                
                if (Files.exists(libvlcDll) && Files.exists(libvlccoreDll)) {
                    log("Found VLC at: " + vlcDir);
                    
                    // Set JNA library path
                    String currentPath = System.getProperty("jna.library.path", "");
                    if (!currentPath.isEmpty()) {
                        currentPath = currentPath + File.pathSeparator;
                    }
                    System.setProperty("jna.library.path", currentPath + vlcDir.toString());
                    
                    // Set VLC plugin path
                    if (Files.isDirectory(pluginsDir)) {
                        System.setProperty("VLC_PLUGIN_PATH", pluginsDir.toString());
                        log("Set VLC_PLUGIN_PATH: " + pluginsDir);
                    }
                    
                    return true;
                }
            }
        }
        
        log("VLC not found on Windows");
        return false;
    }
    
    private static boolean locateMacOS() {
        List<String> candidates = new ArrayList<String>();
        
        // Standard macOS application path
        candidates.add("/Applications/VLC.app/Contents/MacOS/lib");
        
        // Homebrew installation
        candidates.add("/usr/local/lib");
        candidates.add("/opt/homebrew/lib");
        
        // User home
        String home = System.getProperty("user.home");
        if (home != null) {
            candidates.add(home + "/Applications/VLC.app/Contents/MacOS/lib");
        }
        
        for (String candidate : candidates) {
            Path vlcDir = Paths.get(candidate);
            if (Files.isDirectory(vlcDir)) {
                // Check for libvlc.dylib or libvlc.so
                Path libvlcDylib = vlcDir.resolve("libvlc.dylib");
                Path libvlcSo = vlcDir.resolve("libvlc.so");
                
                if (Files.exists(libvlcDylib) || Files.exists(libvlcSo)) {
                    log("Found VLC at: " + vlcDir);
                    
                    // Set JNA library path
                    String currentPath = System.getProperty("jna.library.path", "");
                    if (!currentPath.isEmpty()) {
                        currentPath = currentPath + File.pathSeparator;
                    }
                    System.setProperty("jna.library.path", currentPath + vlcDir.toString());
                    
                    // Try to find plugins
                    Path pluginsDir = vlcDir.resolve("vlc").resolve("plugins");
                    if (Files.isDirectory(pluginsDir)) {
                        System.setProperty("VLC_PLUGIN_PATH", pluginsDir.toString());
                        log("Set VLC_PLUGIN_PATH: " + pluginsDir);
                    }
                    
                    return true;
                }
            }
        }
        
        log("VLC not found on macOS");
        return false;
    }
    
    private static boolean locateLinux() {
        List<String> candidates = new ArrayList<String>();
        
        // Standard library paths
        candidates.add("/usr/lib");
        candidates.add("/usr/lib64");
        candidates.add("/usr/local/lib");
        candidates.add("/usr/lib/x86_64-linux-gnu");
        
        // Snap installation
        candidates.add("/snap/vlc/current/usr/lib");
        
        for (String candidate : candidates) {
            Path vlcDir = Paths.get(candidate);
            if (Files.isDirectory(vlcDir)) {
                Path libvlcSo = vlcDir.resolve("libvlc.so");
                Path libvlcSo5 = vlcDir.resolve("libvlc.so.5");
                
                if (Files.exists(libvlcSo) || Files.exists(libvlcSo5)) {
                    log("Found VLC at: " + vlcDir);
                    
                    // Set JNA library path
                    String currentPath = System.getProperty("jna.library.path", "");
                    if (!currentPath.isEmpty()) {
                        currentPath = currentPath + File.pathSeparator;
                    }
                    System.setProperty("jna.library.path", currentPath + vlcDir.toString());
                    
                    // Try to find plugins
                    Path pluginsDir = vlcDir.resolve("vlc").resolve("plugins");
                    if (Files.isDirectory(pluginsDir)) {
                        System.setProperty("VLC_PLUGIN_PATH", pluginsDir.toString());
                        log("Set VLC_PLUGIN_PATH: " + pluginsDir);
                    }
                    
                    return true;
                }
            }
        }
        
        log("VLC not found on Linux");
        return false;
    }
    
    private static void log(String message) {
        if (LOG_ENABLED) {
            System.out.println("[LibVlcLocator] " + message);
        }
    }
}
