import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;

/**
 * JUnit test class for StaticInterpreter class
 */
public class StaticInterpreterTest {

  private StaticInterpreter staticInterpreter;
  private final String goodLimaCharlieLogDataPath = "src/test/resources/cybersecurity2/LimaCharlieLogData.log";
  private final String badLimaCharlieLogDataPath = "src/test/resources/cybersecurity2/NotLimaCharlieLogData.log";
  private final String goodLimaCharlieDRPath = "src/test/resources/cybersecurity2/LimaCharlieDRData.log";
  private final String badLimaCharlieDRPath = "src/test/resources/cybersecurity2/BadLimaCharlieDRData.log";
  private final String lcDRFileCreatePath = "src/test/resources/cybersecurity2/LimaCharlieDRFileCreateData.log";
  private final String lcDRNewProcessPath = "src/test/resources/cybersecurity2/LimaCharlieDRNewProcessData.log";
  private final String lcFileCreateNewProcessPath = "src/test/resources/cybersecurity2/LimaCharlieDRFileCreateNewProcessData.log";
  private final String splunkHarmfulFilesPath = "src/test/resources/cybersecurity2/harmfulFiles.csv";
  private final String splunkHarmfulProcessesPath = "src/test/resources/cybersecurity2/harmfulProcesses.csv";
  private final String splunkBadFileName = "src/test/resources/cybersecurity2/SplunkBadFileName.csv";
  private final String splunkBadHarmfulFilesPath = "src/test/resources/cybersecurity2/harmfulFilesBad.csv";
  private final String splunkHarmfulFilesBackupPath = "src/test/resources/cybersecurity2/harmfulFilesBackup.csv";
  private final String splunkBadHarmfulProcessesPath = "src/test/resources/cybersecurity2/harmfulProcessesBad.csv";
  private final String splunkHarmfulProcessesBackupPath = "src/test/resources/cybersecurity2/harmfulProcessesBackup.csv";

  @Before
  public void setup() {
    staticInterpreter = new StaticInterpreter();
  }

  /**
   * Test to make sure the StaticInterpreter class exists and we can create an object. Passes if no
   * exception thrown
   */
  @Test
  public void StaticInterpreterExists() {
    StaticInterpreter staticInterpreter = new StaticInterpreter();
    Assert.assertTrue(true);
  }

  /**
   * Test to make sure it has a function that can be called to interpret LimaCharlie output and
   * takes in a file path as a string
   */
  @Test
  public void hasFunctionInterpretLimaCharlieOutput() {
    Class<?> staticInterpreterClass = StaticInterpreter.class;
    try {
      // If not exception is thrown then it contains the method
      staticInterpreterClass.getMethod("interpretLimaCharlieOutput", String.class);
      Assert.assertTrue(true);
    } catch (NoSuchMethodException e) {
      e.printStackTrace();
      Assert.fail();
    }
  }

  /**
   * Test to make sure that the StaticInterpreter can interpret LimaCharlie log data as log data and
   * know to send it to Splunk
   */
  @Test
  public void canInterpretLimaCharlieLogData() {
    HashMap<String, String> expectedOutput = new HashMap<>();
    expectedOutput.put("action", "Log Management");
    expectedOutput.put("toolTo", "splunk");
    expectedOutput.put("type", "json");
    expectedOutput.put("toolFrom", "limacharlie");
    expectedOutput.put("message", "success");
    expectedOutput.put("filePath", goodLimaCharlieLogDataPath);
    ArrayList<HashMap<String, String>> actions = staticInterpreter
        .interpretLimaCharlieOutput(goodLimaCharlieLogDataPath);
    HashMap<String, String> actual = actions.get(0);
    Assert.assertEquals(expectedOutput, actual);
  }

  /**
   * Test to make sure it has a function that can be called to interpret LimaCharlie output and
   * takes in a file path as a string
   */
  @Test
  public void hasFunctionIsLimaCharlieLog() {
    Class<?> staticInterpreterClass = StaticInterpreter.class;
    try {
      // If not exception is thrown then it contains the method
      staticInterpreterClass.getMethod("isLimaCharlieLog", String.class);
      Assert.assertTrue(true);
    } catch (NoSuchMethodException e) {
      e.printStackTrace();
      Assert.fail();
    }
  }

  /**
   * Tests to make sure the isLimaCharlieLog returns true when given real LimaCharlie Log data
   */
  @Test
  public void isLimaCharlieLogGoodCase() {
    boolean actual = staticInterpreter.isLimaCharlieLog(goodLimaCharlieLogDataPath);
    Assert.assertTrue(actual);
  }

  /**
   * Tests to make sure it returns false when not given LimaCharlieLog data Note that the
   * interpreter will throw an Exception because it will fail to parse the invalid file as a JSON
   * object
   */
  @Test
  public void isLimaCharlieLogBadCase() {
    Boolean actual = staticInterpreter.isLimaCharlieLog(badLimaCharlieLogDataPath);
    Assert.assertFalse(actual);
  }

  /**
   * Test to check if StaticInterpreter is able to interpret LimaCharlieDR Data
   */
  @Test
  public void isLimaCharlieDRGoodCase() {
    Boolean actual = staticInterpreter.isLimaCharlieDR(goodLimaCharlieDRPath);
    Assert.assertTrue(actual);
  }

  /**
   * Tests to make sure that it does not parse bad LimaCharlie DR data
   */
  @Test
  public void isLimaCharlieDRBadCase() {
    Boolean actual = staticInterpreter.isLimaCharlieDR(badLimaCharlieDRPath);
    Assert.assertFalse(actual);
  }

  /**
   * Test to make sure the interpret method is able to correctly interpret LimaCharlieDR Data that
   * includes FILE_CREATE events
   */
  @Test
  public void interpretLimaCharlieDRDataFileCreate() {
    HashMap<String, String> fileCreateExpected = new HashMap<>();
    fileCreateExpected.put("action", "Reporting");
    fileCreateExpected.put("reportName", "harmfulFiles");
    fileCreateExpected.put("toolTo", "splunk");
    fileCreateExpected.put("type", "json");
    fileCreateExpected.put("toolFrom", "limacharlie");
    fileCreateExpected.put("message", "success");
    fileCreateExpected.put("filePath", lcDRFileCreatePath);

    ArrayList<HashMap<String, String>> expected = new ArrayList<>();
    expected.add(fileCreateExpected);

    ArrayList<HashMap<String, String>> actual = null;
    try {
      actual = staticInterpreter
          .interpret(lcDRFileCreatePath, "limacharlie");
    } catch (IOException e) {
      e.printStackTrace();
      Assert.fail();
    }
    Assert.assertEquals(1, actual.size());
    Assert.assertEquals(expected, actual);
  }

  /**
   * Test to make sure the interpret method is able to correctly interpret LimaCharlieDR Data that
   * includes NEW_PROCESS events
   */
  @Test
  public void interpretLimaCharlieDRDataNewProcess() {
    HashMap<String, String> newProcessExpected = new HashMap<>();
    newProcessExpected.put("action", "Reporting");
    newProcessExpected.put("reportName", "harmfulProcesses");
    newProcessExpected.put("toolTo", "splunk");
    newProcessExpected.put("type", "json");
    newProcessExpected.put("toolFrom", "limacharlie");
    newProcessExpected.put("message", "success");
    newProcessExpected.put("filePath", lcDRNewProcessPath);

    ArrayList<HashMap<String, String>> expected = new ArrayList<>();
    expected.add(newProcessExpected);

    ArrayList<HashMap<String, String>> actual = null;
    try {
      actual = staticInterpreter
          .interpret(lcDRNewProcessPath, "limacharlie");
    } catch (IOException e) {
      e.printStackTrace();
      Assert.fail();
    }
    Assert.assertEquals(1, actual.size());
    Assert.assertEquals(expected, actual);
  }

  /**
   * Test to make sure the interpret method is able to correctly interpret LimaCharlieDR Data that
   * includes both FILE_CREATE and NEW_PROCESS events
   */
  @Test
  public void interpretLimaCharlieDRDataFileCreateAndNewProcess() {
    HashMap<String, String> fileExpected = new HashMap<>();
    fileExpected.put("action", "Reporting");
    fileExpected.put("reportName", "harmfulFiles");
    fileExpected.put("toolTo", "splunk");
    fileExpected.put("type", "json");
    fileExpected.put("toolFrom", "limacharlie");
    fileExpected.put("message", "success");
    fileExpected.put("filePath", lcFileCreateNewProcessPath);

    HashMap<String, String> processExpected = new HashMap<>();
    processExpected.put("action", "Reporting");
    processExpected.put("reportName", "harmfulProcesses");
    processExpected.put("toolTo", "splunk");
    processExpected.put("type", "json");
    processExpected.put("toolFrom", "limacharlie");
    processExpected.put("message", "success");
    processExpected.put("filePath", lcFileCreateNewProcessPath);

    ArrayList<HashMap<String, String>> actual = null;
    try {
      actual = staticInterpreter
          .interpret(lcFileCreateNewProcessPath, "limacharlie");
    } catch (IOException e) {
      e.printStackTrace();
      Assert.fail();
    }
    // Ensure actual list has exactly 2 elements as expected
    if (actual.size() != 2) {
      Assert.fail();
      return;
    }

    // Tracks if expected FILE_CREATE result has been found in actual returned list
    boolean fileExpectedFound = false;
    // Tracks if expected NEW_PROCESS result has been found in actual returned list
    boolean processExpectedFound = false;
    for (int i = 0; i < actual.size(); i++) {
      if (actual.get(i).equals(fileExpected)) {
        fileExpectedFound = true;
      } else if (actual.get(i).equals(processExpected)) {
        processExpectedFound = true;
      }
    }

    Assert.assertTrue(fileExpectedFound);
    Assert.assertTrue(processExpectedFound);
  }

  /**
   * Test to make sure that the bad lima charlie data does not parse correctly
   */
  @Test
  public void failInterpretBadDRData() {
    HashMap<String, String> expected = new HashMap<>();
    expected.put("message", "fail");
    try {
      ArrayList<HashMap<String, String>> actions = staticInterpreter
          .interpret(badLimaCharlieDRPath, "limacharlie");
      HashMap<String, String> actual = actions.get(0);
      Assert.assertEquals(expected, actual);
    } catch (IOException e) {
      e.printStackTrace();
      Assert.fail();
    }
  }

  /**
   * Test to ensure interpret() outputs ArrayList with a single HashMap with fail message
   */
  @Test
  public void failInterpretInvalidToolFrom() {
    try {
      HashMap<String, String> expHash = new HashMap<>();
      expHash.put("message", "fail");
      ArrayList<HashMap<String, String>> expected = new ArrayList<>();
      expected.add(expHash);

      ArrayList actual = staticInterpreter.interpret("irrelevantFilePath", "incorrectTool");
      Assert.assertEquals(expected, actual);
    } catch (IOException e) {
      e.printStackTrace();
      Assert.fail();
    }
  }


  /**
   * Test to make sure it has a function that can be called to interpret Splunk output and takes in
   * a file path as a string
   */
  @Test
  public void hasFunctionInterpretSplunkOutput() {
    Class<?> staticInterpreterClass = StaticInterpreter.class;
    try {
      // If not exception is thrown then it contains the method
      staticInterpreterClass.getMethod("interpretSplunkOutput", String.class);
      Assert.assertTrue(true);
    } catch (NoSuchMethodException e) {
      e.printStackTrace();
      Assert.fail();
    }
  }

  /**
   * Test to make sure interpret method correctly interprets Splunk harmfulFiles.csv file. Important
   * to note that interpreting of harmfulFiles.csv assumes the file is named as such.
   */
  @Test
  public void interpretSplunkHarmfulFiles() {
    try {
      ArrayList<HashMap<String, String>> actual = staticInterpreter
          .interpret(splunkHarmfulFilesPath, "splunk");

      // Expected HashMap values
      HashMap<String, String> expHash = new HashMap<>();
      expHash.put("toolTo", "limacharlie");
      expHash.put("hostname", "DESKTOP-66J0G0K");
      expHash.put("tags{}", "create-file\nprocess\nrhys-vm\nvm-w10");
      expHash.put("iid", "22bd1700-f87b-4b87-bcdf-c759007b7bfa");
      expHash.put("filePath", "C:\\Users\\midso\\Documents\\Ingenuity\\hack.txt");
      expHash.put("action", "file_del");
      expHash.put("toolFrom", "splunk");
      expHash.put("oid", "5aa237f6-0bec-449f-a5a2-24b5533c165a");
      expHash.put("message", "success");
      expHash.put("sid", "76ba344d-ae92-4227-a13b-0fcbdde332ea");
      expHash.put("ext_ip", "129.127.36.82");

      // File contains 3 identical actions, so duplicate expected and compare
      ArrayList<HashMap<String, String>> expected = new ArrayList<>();
      expected.add(expHash);
      expected.add(expHash);
      expected.add(expHash);

      Assert.assertEquals(3, actual.size());
      Assert.assertEquals(expected, actual);

    } catch (IOException e) {
      e.printStackTrace();
      Assert.fail();
    }
  }

  /**
   * Test to make sure interpret method correctly interprets Splunk harmfulProcesses.csv file.
   * Important to note that interpreting of harmfulProcesses.csv assumes the file is named as such.
   */
  @Test
  public void interpretSplunkHarmfulProcesses() {
    try {
      ArrayList<HashMap<String, String>> actual = staticInterpreter
          .interpret(splunkHarmfulProcessesPath, "splunk");

      // Expected HashMap values
      HashMap<String, String> expHash1 = new HashMap<>();
      expHash1.put("toolTo", "limacharlie");
      expHash1.put("hostname", "DESKTOP-66J0G0K");
      expHash1.put("tags{}", "create-file\nprocess\nrhys-vm\nvm-w10");
      expHash1.put("iid", "22bd1700-f87b-4b87-bcdf-c759007b7bfa");
      expHash1.put("action", "os_kill_process");
      expHash1.put("process_id", "2996");
      expHash1.put("toolFrom", "splunk");
      expHash1.put("oid", "5aa237f6-0bec-449f-a5a2-24b5533c165a");
      expHash1.put("message", "success");
      expHash1.put("sid", "76ba344d-ae92-4227-a13b-0fcbdde332ea");
      expHash1.put("ext_ip", "129.127.36.82");

      // File contains 3 events that are identical except process id
      HashMap<String, String> expHash2 = new HashMap<>(expHash1);
      expHash2.replace("process_id", "3000");
      HashMap<String, String> expHash3 = new HashMap<>(expHash1);
      expHash3.replace("process_id", "3804");

      // Add expected HashMaps to list and compare
      ArrayList<HashMap<String, String>> expected = new ArrayList<>();
      expected.add(expHash1);
      expected.add(expHash2);
      expected.add(expHash3);

      Assert.assertEquals(3, actual.size());
      Assert.assertEquals(expected, actual);

    } catch (IOException e) {
      e.printStackTrace();
      Assert.fail();
    }
  }

  /**
   * Test to make sure interpret method correctly interprets returns an empty list if passed an
   * unsupported filename.
   */
  @Test
  public void failInterpretSplunkBadFileName() {
    try {
      ArrayList<HashMap<String, String>> actual = staticInterpreter
          .interpret(splunkBadFileName, "splunk");
      ArrayList<HashMap<String, String>> expected = new ArrayList<>();

      Assert.assertEquals(expected, actual);
    } catch (IOException e) {
      e.printStackTrace();
      Assert.fail();
    }
  }

  /**
   * Test to make sure interpret method correctly fails to interpret a bad harmfulFiles event and
   * returns a fail message for that event, but returns correct actions for good events. Important
   * to note that because interpreting Splunk outputs expects specific file names,
   * harmfulFilesBad.csv is renamed to harmfulFiles.csv to be interpreted, and the original good
   * harmfulFiles.csv file is renamed to harmfulFilesBackup.csv. These files are reverted to
   * harmfulFiles.csv and harmfulFilesBad.csv respectively at the end of the test.
   */
  @Test
  public void failInterpretSplunkBadHarmfulFiles() {
    File badFile = new File(splunkBadHarmfulFilesPath);
    File goodFile = new File(splunkHarmfulFilesPath);
    File goodFileBackup = new File(splunkHarmfulFilesBackupPath);

    // Ensure correct files exist and can setup correctly
    if (badFile.exists() && goodFile.exists()) {
      if (!goodFile.renameTo(goodFileBackup) || !badFile.renameTo(goodFile)) {
        Assert.fail();
        return;
      }
    } else {
      Assert.fail();
      return;
    }

    try {
      ArrayList<HashMap<String, String>> actual = staticInterpreter
          .interpret(splunkHarmfulFilesPath, "splunk");

      // Expected HashMap values
      HashMap<String, String> expHash = new HashMap<>();
      expHash.put("toolTo", "limacharlie");
      expHash.put("hostname", "DESKTOP-66J0G0K");
      expHash.put("tags{}", "create-file\nprocess\nrhys-vm\nvm-w10");
      expHash.put("iid", "22bd1700-f87b-4b87-bcdf-c759007b7bfa");
      expHash.put("filePath", "C:\\Users\\midso\\Documents\\Ingenuity\\hack.txt");
      expHash.put("action", "file_del");
      expHash.put("toolFrom", "splunk");
      expHash.put("oid", "5aa237f6-0bec-449f-a5a2-24b5533c165a");
      expHash.put("message", "success");
      expHash.put("sid", "76ba344d-ae92-4227-a13b-0fcbdde332ea");
      expHash.put("ext_ip", "129.127.36.82");

      // File contains 2 identical actions and 1 failed action, so duplicate expected and compare
      ArrayList<HashMap<String, String>> expected = new ArrayList<>();
      HashMap<String, String> failedHash = new HashMap<>();
      failedHash.put("message", "fail");
      expected.add(failedHash);
      expected.add(expHash);
      expected.add(expHash);

      Assert.assertEquals(3, actual.size());
      Assert.assertEquals(expected, actual);
    } catch (IOException e) {
      e.printStackTrace();
    }

    // Revert file names to original structure
    if (!goodFile.renameTo(badFile) || !goodFileBackup.renameTo(goodFile)) {
      Assert.fail();
    }

    Assert.assertTrue(goodFile.exists());
    Assert.assertTrue(badFile.exists());
    Assert.assertFalse(goodFileBackup.exists());
  }

  /**
   * Test to make sure interpret method correctly fails to interpret a bad harmfulProcesses event
   * and returns a fail message for that event, but returns correct actions for good events.
   * Important to note that because interpreting Splunk outputs expects specific file names,
   * harmfulProcessesBad.csv is renamed to harmfulProcesses.csv to be interpreted, and the original
   * good harmfulProcesses.csv file is renamed to harmfulProcessesBackup.csv. These files are
   * reverted to harmfulProcesses.csv and harmfulProcessesBad.csv respectively at the end of the
   * test.
   */
  @Test
  public void failInterpretSplunkBadHarmfulProcesses() {
    File badFile = new File(splunkBadHarmfulProcessesPath);
    File goodFile = new File(splunkHarmfulProcessesPath);
    File goodFileBackup = new File(splunkHarmfulProcessesBackupPath);

    // Ensure correct files exist and can setup correctly
    if (badFile.exists() && goodFile.exists()) {
      if (!goodFile.renameTo(goodFileBackup) || !badFile.renameTo(goodFile)) {
        Assert.fail();
        return;
      }
    } else {
      Assert.fail();
      return;
    }

    try {
      ArrayList<HashMap<String, String>> actual = staticInterpreter
          .interpret(splunkHarmfulProcessesPath, "splunk");

      // Expected HashMap values
      HashMap<String, String> failedHash = new HashMap<>();
      failedHash.put("message", "fail");

      HashMap<String, String> expHash2 = new HashMap<>();
      expHash2.put("toolTo", "limacharlie");
      expHash2.put("hostname", "DESKTOP-66J0G0K");
      expHash2.put("tags{}", "create-file\nprocess\nrhys-vm\nvm-w10");
      expHash2.put("iid", "22bd1700-f87b-4b87-bcdf-c759007b7bfa");
      expHash2.put("action", "os_kill_process");
      expHash2.put("process_id", "3000");
      expHash2.put("toolFrom", "splunk");
      expHash2.put("oid", "5aa237f6-0bec-449f-a5a2-24b5533c165a");
      expHash2.put("message", "success");
      expHash2.put("sid", "76ba344d-ae92-4227-a13b-0fcbdde332ea");
      expHash2.put("ext_ip", "129.127.36.82");

      HashMap<String, String> expHash3 = new HashMap<>(expHash2);
      expHash3.replace("process_id", "3804");

      // File contains 2 identical actions and 1 failed action, so duplicate expected and compare
      ArrayList<HashMap<String, String>> expected = new ArrayList<>();
      expected.add(failedHash);
      expected.add(expHash2);
      expected.add(expHash3);

      Assert.assertEquals(3, actual.size());
      Assert.assertEquals(expected, actual);
    } catch (IOException e) {
      e.printStackTrace();
    }

    // Revert file names to original structure
    if (!goodFile.renameTo(badFile) || !goodFileBackup.renameTo(goodFile)) {
      Assert.fail();
    }

    Assert.assertTrue(goodFile.exists());
    Assert.assertTrue(badFile.exists());
    Assert.assertFalse(goodFileBackup.exists());
  }
}
