/*
 * Copyright 2024 Dan Rollo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.bhaweb.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * Utility class to detect if the application is running on a Raspberry Pi.
 */
public class RaspberryPiDetector {

    /**
     * Path to the CPU info file on Linux systems.
     * This is typically located at /proc/cpuinfo.
     */
    protected static final String DEFAULT_CPU_INFO_PATH = "/proc/cpuinfo";
    // Allow overriding the CPU info path for testing purposes
    protected static String CPU_INFO_PATH = DEFAULT_CPU_INFO_PATH;
    protected static final String MODEL_NAME_PREFIX = "model name";
    protected static final String HARDWARE_PREFIX = "Hardware";
    protected static final String[] RASPBERRY_PI_HARDWARE_MARKERS = {
            "BCM2708", "BCM2709", "BCM2710", "BCM2711", "BCM2835", "BCM2836", "BCM2837", "BCM2838"
    };
    protected static final String MODEL_5_PREFIX = "Model";
    protected static final String[] MODEL_5_MARKERS = {
            "Raspberry", "Pi", "Model", "Rev"
    };
    protected static final String REVISION_PREFIX = "Revision";

    /**
     * Checks if the current system is a Raspberry Pi.
     * 
     * @return true if running on a Raspberry Pi, false otherwise
     */
    public static boolean isRaspberryPi() {
        // Check if we're running on Linux first
        String osName = getOsName();
        if (!osName.contains("linux")) {
            return false;
        }

        // Check for Raspberry Pi specific CPU info
        File cpuInfoFile = getCpuInfoFile();
        if (!cpuInfoFile.exists()) {
            return false;
        }

        final String cpuInfo;
        try {
            cpuInfo = readCpuInfo(cpuInfoFile);
        } catch (IOException e) {
            // If we can't read the file, assume it's not a Raspberry Pi
            return false;
        }

        if (containsRaspberryPi5ModelInfo(cpuInfo)) {
            return true;
        } else {
            // check for old hardware
            return containsRaspberryPiHardware(cpuInfo);
        }
    }

    /**
     * Gets the Raspberry Pi model information if available.
     * 
     * @return a string containing the model information, or an empty string if not running on a Pi
     */
    public static String getRaspberryPiModel() {
        if (!isRaspberryPi()) {
            return "";
        }

        File cpuInfoFile = getCpuInfoFile();
        try {
            String cpuInfo = readCpuInfo(cpuInfoFile);
            return extractModelInfo(cpuInfo);
        } catch (IOException e) {
            // todo: log the error
            return "(model unknown)";
        }
    }

    /**
     * Gets the Raspberry Pi revision information if available.
     *
     * @return a string containing the revision information, or an empty string if not running on a Pi
     */
    public static String getRaspberryPiRevision() {
        if (!isRaspberryPi()) {
            return "";
        }

        File cpuInfoFile = getCpuInfoFile();
        try {
            String cpuInfo = readCpuInfo(cpuInfoFile);
            return extractRevisionInfo(cpuInfo);
        } catch (IOException e) {
            // todo: log the error
            return "(revision unknown)";
        }
    }

    // Protected methods that can be overridden for testing

    /**
     * Gets the OS name.
     * 
     * @return the OS name in lowercase
     */
    protected static String getOsName() {
        return System.getProperty("os.name").toLowerCase();
    }

    /**
     * Gets the CPU info file.
     * 
     * @return the CPU info file
     */
    protected static File getCpuInfoFile() {
        return new File(CPU_INFO_PATH);
    }

    /**
     * Reads the CPU info from the given file.
     * 
     * @param cpuInfoFile the CPU info file
     * @return the CPU info as a string
     * @throws IOException if an I/O error occurs
     */
    protected static String readCpuInfo(File cpuInfoFile) throws IOException {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(cpuInfoFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        return content.toString();
    }

    private static boolean hasPrefixWithMarkers(final String cpuInfo, final String prefix, final String[] markers) {
        final String[] lines = cpuInfo.split("\n");
        for (final String line : lines) {
            // Check for Raspberry Pi hardware markers
            if (line.startsWith(prefix)) {
                final String hardware = line.split(":", 2)[1].trim();
                for (final String marker : markers) {
                    if (hardware.contains(marker)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Checks if the CPU info contains Raspberry Pi hardware markers.
     * 
     * @param cpuInfo the CPU info
     * @return true if the CPU info contains Raspberry Pi hardware markers, false otherwise
     */
    protected static boolean containsRaspberryPiHardware(String cpuInfo) {
        return hasPrefixWithMarkers(cpuInfo, HARDWARE_PREFIX, RASPBERRY_PI_HARDWARE_MARKERS);
    }

    /**
     * Extracts the model information from the CPU info.
     * 
     * @param cpuInfo the CPU info
     * @return the model information, or "(model unknown)" if not found
     */
    protected static String extractModelInfo(String cpuInfo) {
        final String[] lines = cpuInfo.split("\n");
        for (final String line : lines) {
            // on Pi5+, return Model value:
            if (line.startsWith(MODEL_5_PREFIX)) {
                return line.split(":", 2)[1].trim();
            }
            // on old models, return Model name:
            if (line.startsWith(MODEL_NAME_PREFIX)) {
                return line.split(":", 2)[1].trim();
            }
        }
        return "(model unknown)";
    }

    /**
     * Checks if the CPU info contains Raspberry Pi 5+ hardware markers.
     *
     * @param cpuInfo the CPU info
     * @return true if the CPU info contains Raspberry Pi 5+ hardware markers, false otherwise
     */
    protected static boolean containsRaspberryPi5ModelInfo(String cpuInfo) {
        return hasPrefixWithMarkers(cpuInfo, MODEL_5_PREFIX, MODEL_5_MARKERS);
    }

    /**
     * Extracts the revision information from the CPU info.
     * See this table for details about the revision value prior to Pi5:
     * <a href="https://elinux.org/RPi_HardwareHistory">...</a>
     * @param cpuInfo the CPU info
     * @return the revision information, or "(revision unknown)" if not found
     */
    protected static String extractRevisionInfo(String cpuInfo) {
        final String[] lines = cpuInfo.split("\n");
        for (final String line : lines) {
            if (line.startsWith(REVISION_PREFIX)) {
                return line.split(":", 2)[1].trim();
            }
        }
        return "(revision unknown)";
    }

}
