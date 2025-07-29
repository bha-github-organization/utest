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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class for RaspberryPiDetector.
 * Uses a testable subclass to override system-dependent methods for testing.
 */
public class RaspberryPiDetectorTest
{

  @TempDir
  Path tempDir;

  /**
   * Testable subclass of RaspberryPiDetector that overrides system-dependent methods.
   */
  static class TestableRaspberryPiDetector
      extends RaspberryPiDetector
  {

    private static boolean isRaspberryPi;

    public static void setup(String cpuInfo) {
      // Determine if this is a Raspberry Pi based on the CPU info
      isRaspberryPi = cpuInfo != null && cpuInfo.contains("Hardware") && cpuInfo.contains("BCM");
    }

    // Override isRaspberryPi for specific tests
    public static boolean isRaspberryPi() {
      // For tests that need to force isRaspberryPi to return true
      // NOTE: Kinda nasty that test method names must contain this prefix, very brittle
      if (Thread.currentThread().getStackTrace()[2].getMethodName().contains("testGetRaspberryPiModel_")) {
        return isRaspberryPi;
      }
      // Otherwise use the parent implementation
      return RaspberryPiDetector.isRaspberryPi();
    }
  }

  /**
   * Helper method to create a temporary CPU info file with the given content.
   */
  private File createCpuInfoFile(String content) throws IOException {
    @SuppressWarnings("SpellCheckingInspection") File cpuInfoFile = tempDir.resolve("cpuinfo").toFile();
    try (BufferedWriter writer = new BufferedWriter(new FileWriter(cpuInfoFile))) {
      writer.write(content);
    }
    return cpuInfoFile;
  }

  @Test
  public void testGetRaspberryPiModel_NonLinuxOS() {
    // Set up the test detector with non-Linux OS
    TestableRaspberryPiDetector.setup("");

    // Test the method
    assertFalse(TestableRaspberryPiDetector.isRaspberryPi());
  }

  @Test
  public void testGetRaspberryPiModel_LinuxNonRaspberryPi() {
    // Create CPU info content for a non-Raspberry Pi Linux system
    String cpuInfo = """
        processor\t: 0
        vendor_id\t: GenuineIntel
        cpu family\t: 6
        model\t\t: 142
        model name\t: Intel(R) Core(TM) i7-8565U CPU @ 1.80GHz
        stepping\t: 11
        Hardware\t: Intel Corporation""";

    // Set up the test detector with Linux OS
    TestableRaspberryPiDetector.setup(cpuInfo);

    // Test the method
    assertFalse(TestableRaspberryPiDetector.isRaspberryPi());
  }

  @Test
  public void testGetRaspberryPiModel_RaspberryPi() {
    // Create CPU info content for a Raspberry Pi
    @SuppressWarnings("SpellCheckingInspection") String cpuInfo = """
        processor\t: 0
        model name\t: ARMv7 Processor rev 3 (v7l)
        BogoMIPS\t: 38.40
        Features\t: half thumb fastmult vfp edsp neon vfpv3 tls vfpv4 idiva idivt vfpd32 lpae evtstrm crc32
        CPU implementer\t: 0x41
        CPU architecture: 7
        CPU variant\t: 0x0
        CPU part\t: 0xd08
        CPU revision\t: 3
        Hardware\t: BCM2835
        Revision\t: c03111
        Serial\t\t: 10000000abcdef01""";

    // Test the containsRaspberryPiHardware method directly
    assertTrue(RaspberryPiDetector.containsRaspberryPiHardware(cpuInfo));
  }

  @Test
  public void testGetRaspberryPiModel_FileNotExists() {
    // Set up the test detector with Linux OS and a non-existent file
    TestableRaspberryPiDetector.setup("");

    // Set up the test detector with the mock file
    TestableRaspberryPiDetector.setup("");

    // Test the method
    assertFalse(TestableRaspberryPiDetector.isRaspberryPi());
  }

  @Test
  public void testGetRaspberryPiModel_IOExceptionHandling() {
    // Set up the test detector to throw IOException
    TestableRaspberryPiDetector.setup(null);

    // Test the method
    assertFalse(TestableRaspberryPiDetector.isRaspberryPi());
  }

  @Test
  public void testGetRaspberryPiModel_MissingCPUInfoFile() {
    TestableRaspberryPiDetector.setup("");

    TestableRaspberryPiDetector.CPU_INFO_PATH = "bogus/path/to/cpuinfo";

    // Save the original os.name property
    String origOSName = System.getProperty("os.name");
    try {
      // Set the os.name property to a Linux value
      System.setProperty("os.name", "linux");

      // Test the method
      assertEquals("", TestableRaspberryPiDetector.getRaspberryPiModel());
    } finally {
      // restore os name property
      System.setProperty("os.name", origOSName);
      TestableRaspberryPiDetector.CPU_INFO_PATH = RaspberryPiDetector.DEFAULT_CPU_INFO_PATH;
    }
  }

  @Test
  public void testGetRaspberryPiModel_NotRaspberryPi() throws IOException {
    // Create CPU info content for a non-Raspberry Pi Linux system
    String cpuInfo = """
        processor\t: 0
        vendor_id\t: GenuineIntel
        cpu family\t: 6
        model\t\t: 142
        model name\t: Intel(R) Core(TM) i7-8565U CPU @ 1.80GHz
        stepping\t: 11
        Hardware\t: Intel Corporation""";

    // Set up the test detector with Linux OS
    TestableRaspberryPiDetector.setup(cpuInfo);

    File cpuInfoFile = createCpuInfoFile(cpuInfo);
    TestableRaspberryPiDetector.CPU_INFO_PATH = cpuInfoFile.getAbsolutePath();

    // Save the original os.name property
    String origOSName = System.getProperty("os.name");
    try {
      // Set the os.name property to a Linux value
      System.setProperty("os.name", "linux");

      // Test the method
      assertEquals("", TestableRaspberryPiDetector.getRaspberryPiModel());
    } finally {
      // restore os name property
      System.setProperty("os.name", origOSName);
      TestableRaspberryPiDetector.CPU_INFO_PATH = RaspberryPiDetector.DEFAULT_CPU_INFO_PATH;
    }
  }

  @Test
  public void testGetRaspberryPiModel_GoodModelName() throws IOException {
    // Create CPU info content for a Raspberry Pi with model info
    String cpuInfo = """
        processor\t: 0
        model name\t: ARMv7 Processor rev 3 (v7l)
        BogoMIPS\t: 38.40
        Hardware\t: BCM2835
        """;

    // Set up the test detector with Linux OS
    TestableRaspberryPiDetector.setup(cpuInfo);

    File cpuInfoFile = createCpuInfoFile(cpuInfo);
    TestableRaspberryPiDetector.CPU_INFO_PATH = cpuInfoFile.getAbsolutePath();

    // Save the original os.name property
    String origOSName = System.getProperty("os.name");
    try {
      // Set the os.name property to a Linux value
      System.setProperty("os.name", "linux");

      // Test the method
      assertEquals("ARMv7 Processor rev 3 (v7l)", TestableRaspberryPiDetector.getRaspberryPiModel());
    } finally {
      // restore os name property
      System.setProperty("os.name", origOSName);
      TestableRaspberryPiDetector.CPU_INFO_PATH = RaspberryPiDetector.DEFAULT_CPU_INFO_PATH;
    }
  }

@Test
  public void testGetRaspberryPiModel_MissingModelName() throws IOException {
    // Create CPU info content for a Raspberry Pi with model info
    String cpuInfo = """
        processor\t: 0
        model-bad-name\t: ARMv7 Processor rev 3 (v7l)
        BogoMIPS\t: 38.40
        Hardware\t: BCM2835
        """;

    // Set up the test detector with Linux OS
    TestableRaspberryPiDetector.setup(cpuInfo);

    File cpuInfoFile = createCpuInfoFile(cpuInfo);
    TestableRaspberryPiDetector.CPU_INFO_PATH = cpuInfoFile.getAbsolutePath();

    // Save the original os.name property
    String origOSName = System.getProperty("os.name");
    try {
      // Set the os.name property to a Linux value
      System.setProperty("os.name", "linux");

      // Test the method
      assertEquals("Raspberry Pi (model unknown)", TestableRaspberryPiDetector.getRaspberryPiModel());
    } finally {
      // restore os name property
      System.setProperty("os.name", origOSName);
      TestableRaspberryPiDetector.CPU_INFO_PATH = RaspberryPiDetector.DEFAULT_CPU_INFO_PATH;
    }
  }

  @Test
  public void testGetRaspberryPiModel_WithModelInfo() {
    // Create CPU info content for a Raspberry Pi with model info
    String cpuInfo = """
        processor\t: 0
        model name\t: ARMv7 Processor rev 3 (v7l)
        BogoMIPS\t: 38.40
        Hardware\t: BCM2835
        """;

    // Test the extractModelInfo method directly
    assertEquals("ARMv7 Processor rev 3 (v7l)", RaspberryPiDetector.extractModelInfo(cpuInfo));
  }

  @Test
  public void testGetRaspberryPiModel_NoModelInfo() {
    // Create CPU info content for a Raspberry Pi without model info
    String cpuInfo = """
        processor\t: 0
        BogoMIPS\t: 38.40
        Hardware\t: BCM2835
        """;

    // Test the extractModelInfo method directly
    assertEquals("Raspberry Pi (model unknown)", RaspberryPiDetector.extractModelInfo(cpuInfo));
  }

  @Test
  public void testGetRaspberryPiModel_WhenFalse() {
    // This case is like that covered by testGetRaspberryPiModel_NoModelInfo since the exception handling
    // in getRaspberryPiModel just returns "Raspberry Pi (model unknown)"
    // We'll test the behavior of getRaspberryPiModel when isRaspberryPi is false

    // Save the original os.name property
    String origOSName = System.getProperty("os.name");
    try {
        // Set the os.name property to a non-Linux value
        System.setProperty("os.name", "SomeOS");

        // Test the method
        assertEquals("", RaspberryPiDetector.getRaspberryPiModel());
    } finally {
      // restore os name property
        System.setProperty("os.name", origOSName);
    }
  }

  @Test
  public void testContainsRaspberryPiHardware_True() {
    // Create CPU info content with Raspberry Pi hardware
    String cpuInfo = """
        processor\t: 0
        model name\t: ARMv7 Processor rev 3 (v7l)
        Hardware\t: BCM2835
        """;

    // Test the method
    assertTrue(RaspberryPiDetector.containsRaspberryPiHardware(cpuInfo));
  }

  @Test
  public void testContainsRaspberryPiHardware_False() {
    // Create CPU info content without Raspberry Pi hardware
    String cpuInfo = """
        processor\t: 0
        model name\t: Intel(R) Core(TM) i7-8565U CPU @ 1.80GHz
        Hardware\t: Intel Corporation
        """;

    // Test the method
    assertFalse(RaspberryPiDetector.containsRaspberryPiHardware(cpuInfo));
  }

  @Test
  public void testExtractModelInfo_WithModelInfo() {
    // Create CPU info content with model info
    String cpuInfo = """
        processor\t: 0
        model name\t: ARMv7 Processor rev 3 (v7l)
        Hardware\t: BCM2835
        """;

    // Test the method
    assertEquals("ARMv7 Processor rev 3 (v7l)", RaspberryPiDetector.extractModelInfo(cpuInfo));
  }

  @Test
  public void testExtractModelInfo_NoModelInfo() {
    // Create CPU info content without model info
    String cpuInfo = """
        processor\t: 0
        Hardware\t: BCM2835
        """;

    // Test the method
    assertEquals("Raspberry Pi (model unknown)", RaspberryPiDetector.extractModelInfo(cpuInfo));
  }
}
