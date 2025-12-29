package com.example.smartim.im.jna;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;
import com.sun.jna.Pointer;
import com.sun.jna.platform.mac.CoreFoundation;
import com.sun.jna.ptr.PointerByReference;

public interface CarbonWrapper extends Library {
    CarbonWrapper INSTANCE = Native.load("Carbon", CarbonWrapper.class);

    // TIS functions
    Pointer TISCreateInputSourceList(Pointer properties, boolean includeAllInstalled);

    int TISSelectInputSource(Pointer inputSource); // Standard return type is OSStatus (int)

    Pointer TISGetInputSourceProperty(Pointer inputSource, Pointer propertyKey);

    // Helper to load global constants (CFStringRef keys)
    class Keys {
        private static Pointer load(String name) {
            return NativeLibrary.getInstance("Carbon").getGlobalVariableAddress(name).getPointer(0);
        }

        public static final Pointer kTISPropertyInputSourceID = load("kTISPropertyInputSourceID");
        public static final Pointer kTISPropertyInputSourceCategory = load("kTISPropertyInputSourceCategory");
        public static final Pointer kTISCategoryKeyboardInputSource = load("kTISCategoryKeyboardInputSource");
        public static final Pointer kTISPropertyInputSourceIsSelectCapable = load(
                "kTISPropertyInputSourceIsSelectCapable");
        // Add others if needed
    }

    // CoreFoundation functions needed for processing the results
    long CFArrayGetCount(Pointer theArray);

    Pointer CFArrayGetValueAtIndex(Pointer theArray, long idx);

    long CFStringGetLength(Pointer theString);

    boolean CFStringGetCString(Pointer theString, byte[] buffer, long bufferSize, int encoding);

    int kCFStringEncodingUTF8 = 0x08000100;

    // Helper to convert CFString to Java String
    default String toJavaString(Pointer cfString) {
        if (cfString == null)
            return null;
        long length = CFStringGetLength(cfString);
        if (length == 0)
            return "";
        // Rough estimate for buffer size (UTF8 can be up to 4 bytes per char)
        long bufferSize = length * 4 + 1;
        byte[] buffer = new byte[(int) bufferSize];
        if (CFStringGetCString(cfString, buffer, bufferSize, kCFStringEncodingUTF8)) {
            // Find null terminator
            int actualLength = 0;
            while (actualLength < buffer.length && buffer[actualLength] != 0) {
                actualLength++;
            }
            return new String(buffer, 0, actualLength, java.nio.charset.StandardCharsets.UTF_8);
        }
        return null;
    }
}
