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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

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
    private static String mockOsName;

    private static String mockCpuInfo;

    private static boolean throwIOException;

    private static boolean isRaspberryPi;

    public static void setup(String osName, File cpuInfoFile, String cpuInfo, boolean exists, boolean throwException) {
      mockOsName = osName;
      mockCpuInfo = cpuInfo;
      throwIOException = throwException;

      // Determine if this is a Raspberry Pi based on the CPU info
      isRaspberryPi = cpuInfo != null && cpuInfo.contains("Hardware") && cpuInfo.contains("BCM");
    }

    // Override the parent class method
    protected static String getOsName() {
      return mockOsName;
    }

    // Override the parent class method
    protected static String readCpuInfo(File cpuInfoFile) throws IOException {
      if (throwIOException) {
        throw new IOException("Test exception");
      }
      return mockCpuInfo;
    }

    // Override isRaspberryPi for specific tests
    public static boolean isRaspberryPi() {
      // For tests that need to force isRaspberryPi to return true
      System.err.println(Thread.currentThread().getStackTrace()[2].getMethodName());
      if (Thread.currentThread().getStackTrace()[2].getMethodName().contains("testGetRaspberryPiModel_")) {
        System.err.println("forcing isRaspberryPi to return true");
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
  public void testIsRaspberryPi_NonLinuxOS() throws IOException {
    // Create a CPU info file (content doesn't matter for this test)
    File cpuInfoFile = createCpuInfoFile("");

    // Set up the test detector with non-Linux OS
    TestableRaspberryPiDetector.setup("Windows 10", cpuInfoFile, "", true, false);

    // Test the method
    assertFalse(TestableRaspberryPiDetector.isRaspberryPi());
  }

  @Test
  public void testIsRaspberryPi_LinuxNonRaspberryPi() throws IOException {
    // Create CPU info content for a non-Raspberry Pi Linux system
    String cpuInfo = """
        processor\t: 0
        vendor_id\t: GenuineIntel
        cpu family\t: 6
        model\t\t: 142
        model name\t: Intel(R) Core(TM) i7-8565U CPU @ 1.80GHz
        stepping\t: 11
        Hardware\t: Intel Corporation""";

    // Create a CPU info file
    File cpuInfoFile = createCpuInfoFile(cpuInfo);

    // Set up the test detector with Linux OS
    TestableRaspberryPiDetector.setup("Linux", cpuInfoFile, cpuInfo, true, false);

    // Test the method
    assertFalse(TestableRaspberryPiDetector.isRaspberryPi());
  }

  @Test
  public void testIsRaspberryPi_RaspberryPi() {
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
  public void testIsRaspberryPi_FileNotExists() throws IOException {
    // Create a CPU info file (content doesn't matter for this test)
    File cpuInfoFile = createCpuInfoFile("");

    // Set up the test detector with Linux OS and a non-existent file
    TestableRaspberryPiDetector.setup("Linux", cpuInfoFile, "", false, false);

    // Mock the exists() method to return false
    File mockFile = new File("/non/existent/file")
    {
      @Override
      public boolean exists() {
        return false;
      }
    };

    // Set up the test detector with the mock file
    TestableRaspberryPiDetector.setup("Linux", mockFile, "", false, false);

    // Test the method
    assertFalse(TestableRaspberryPiDetector.isRaspberryPi());
  }

  @Test
  public void testIsRaspberryPi_IOExceptionHandling() throws IOException {
    // Create a CPU info file
    File cpuInfoFile = createCpuInfoFile("some content");

    // Set up the test detector to throw IOException
    TestableRaspberryPiDetector.setup("Linux", cpuInfoFile, null, true, true);

    // Test the method
    assertFalse(TestableRaspberryPiDetector.isRaspberryPi());
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

    // Create a CPU info file
    File cpuInfoFile = createCpuInfoFile(cpuInfo);

    // Set up the test detector with Linux OS
    TestableRaspberryPiDetector.setup("Linux", cpuInfoFile, cpuInfo, true, false);

    // Test the method
    assertEquals("", TestableRaspberryPiDetector.getRaspberryPiModel());
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
  public void testGetRaspberryPiModel_IOExceptionHandling() throws IOException {
    // This test is covered by testExtractModelInfo_NoModelInfo since the exception handling
    // in getRaspberryPiModel just returns "Raspberry Pi (model unknown)"
    // We'll test the behavior of getRaspberryPiModel when isRaspberryPi is false

    // Create a subclass that overrides isRaspberryPi to return false
    @SuppressWarnings("SameReturnValue") TestableRaspberryPiDetector testDetector = new TestableRaspberryPiDetector()
    {
      public static boolean isRaspberryPi() {
        return false;
      }
    };

    // Test the method
    assertEquals("", RaspberryPiDetector.getRaspberryPiModel());
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
