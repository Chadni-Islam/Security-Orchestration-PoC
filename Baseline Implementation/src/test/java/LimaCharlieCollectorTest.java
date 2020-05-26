import static java.lang.Thread.sleep;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * JUnit tests class for LimaCharlieCollector class Sets up testing directory called "testDir" in the
 * directory the application is running from. testDir is then deleted at the end of every test.
 *
 * Depending on the environment these tests are run, sleepTime may need to be increased to allow for
 * additional setup and event processing time. Initial value is set to 50 (50ms).
 */
public class LimaCharlieCollectorTest {

  private File testDir;
  private String absPath;
  private StaticInterpreterStub intStub = new StaticInterpreterStub();
  // Time to allow threads to start running and for event processing
  private final int sleepTime = 50;

  // Stub StaticInterpreter class
  // Replaces entry point method to return an empty ArrayList
  private class StaticInterpreterStub extends StaticInterpreter {

    public ArrayList<HashMap<String, String>> interpret(String path, String toolFrom) {
      return new ArrayList<>();
    }
  }


  /**
   * Recursively deletes directory and contents
   *
   * @param file File to delete
   * @throws IOException if file was unable to be deleted
   */
  private void deleteDirectory(File file) throws IOException {
    if (file.isDirectory()) {
      File[] entries = file.listFiles();
      if (entries != null) {
        for (File entry : entries) {
          deleteDirectory(entry);
        }
      }
    }
    if (!file.delete()) {
      throw new IOException("Failed to delete " + file);
    }
  }

  /**
   * Creates directory "testDir" in directory where application is being executed. Asserts that
   * testDir is created and is a directory. This directory is used for testing the LimaCharlieCollector
   * class.
   */
  @Before
  public void setup() {
    Path curPath = Paths.get("");
    absPath = curPath.toAbsolutePath().toString();
    testDir = new File(absPath + "/limacharlie_test_dir");
    testDir.mkdirs();
    absPath = absPath + "/limacharlie_test_dir";
    Assert.assertTrue(testDir.exists());
    Assert.assertTrue(testDir.isDirectory());
  }

  /**
   * Cleanup of testDir directory.
   */
  @After
  public void cleanup() {
    if (testDir.exists()) {
      try {
        deleteDirectory(testDir);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  /**
   * Tests getDir() method of LimaCharlieCollector. getDir() is expected to return the private String
   * dir, which denotes the directory which LimaCharlieCollector is watching for file events. Simply
   * tests returned value against expected value and tests that exception isn't thrown.
   */
  @Test
  public void testGetDir() {
    boolean exceptionThrown = false;
    String expected = testDir.toString();
    try {
      LimaCharlieCollector col = new LimaCharlieCollector(expected, intStub);
      sleep(sleepTime);
      Assert.assertEquals(expected, col.getDir());
    } catch (NotDirectoryException e) {
      e.printStackTrace();
      exceptionThrown = true;
    } catch (InterruptedException e){
      e.printStackTrace();
    }
    Assert.assertFalse(exceptionThrown);
  }

  /**
   * Tests Constructor of LimaCharlieCollector throws exception when passed a path to a missing
   * directory.
   */
  @Test
  public void testInvalidDirException() {
    boolean exceptionThrown = false;

    File missingDir = new File(absPath + "/missingDir");
    Assert.assertFalse(missingDir.exists());

    try {
      LimaCharlieCollector col = new LimaCharlieCollector(missingDir.getPath(), intStub);
    } catch (NotDirectoryException expected) {
      // Exception expected
      exceptionThrown = true;
    }

    Assert.assertTrue(exceptionThrown);
  }

  /**
   * Tests that thread successfully shuts down after SDN shutdown signal is given. Test fails if any
   * exceptions are thrown or if thread does not shutdown in 100ms.
   */
  @Test
  public void testShutdownViaSDN() {
    boolean exceptionThrown = false;
    try {
      LimaCharlieCollector limaCollector = new LimaCharlieCollector(absPath, intStub);
      Thread colThread = limaCollector.getThread();

      // Allow time for thread to start before signalling shutdown
      sleep(sleepTime);

      Assert.assertTrue(colThread.isAlive());

      // Send shutdown signal
      File sdnFile = new File(absPath + "/SDN");
      if (!sdnFile.createNewFile()) {
        Assert.fail();
        return;
      }

      // Allow time for thread to process shutdown signal
      sleep(sleepTime);

      Assert.assertFalse(colThread.isAlive());

    } catch (IOException | InterruptedException e) {
      exceptionThrown = true;
      e.printStackTrace();
    }

    Assert.assertFalse(exceptionThrown);
  }

  /**
   * Tests if LimaCharlieCollector.stopRunning() safely shuts down LimaCharlieCollector thread. Important to
   * note that the expected behaviour of stopRunning() is that it safely shuts down after a timeout
   * of 1000 milliseconds.
   */
  @Test
  public void testStopRunningShutDown() {
    boolean exceptionThrown = false;
    File testFile = new File(absPath + "/testFile");

    try {
      LimaCharlieCollector limaCollector = new LimaCharlieCollector(absPath, intStub);
      Thread colThread = limaCollector.getThread();

      // Give thread time to start
      sleep(sleepTime);

      Assert.assertTrue(colThread.isAlive());

      limaCollector.stopRunning();

      // Give thread time to timeout
      sleep(1000);

      Assert.assertFalse(colThread.isAlive());

    } catch (InterruptedException | IOException e) {
      exceptionThrown = true;
    }

    Assert.assertFalse(exceptionThrown);

    if (testFile.exists()) {
      testFile.delete();
    }
  }

  /**
   * Tests that LimaCharlieCollector does not create and store an OutputHandler on file creation.
   * Requires some waiting to ensure LimaCharlieCollector has time to process events.
   */
  @Test
  public void testNoHandlerCreationOnFileCreate() {
    File testFile = new File(absPath + "/testFile");
    boolean exceptionThrown = false;

    try {
      LimaCharlieCollector limaCollector = new LimaCharlieCollector(absPath, intStub);
      Thread colThread = limaCollector.getThread();

      // Give thread time to start up
      sleep(sleepTime);
      Assert.assertTrue(colThread.isAlive());

      ArrayList<OutputHandler> handlers = limaCollector.getHandlers();
      Assert.assertEquals(0, handlers.size());

      // Generate FILE_CREATE event
      if (!testFile.createNewFile()) {
        Assert.fail();
        return;
      }

      // Give thread time to process new event
      sleep(sleepTime);

      handlers = limaCollector.getHandlers();
      Assert.assertEquals(0, handlers.size());

    } catch (InterruptedException | IOException e) {
      e.printStackTrace();
      exceptionThrown = true;
    }

    if (testFile.exists()) {
      testFile.delete();
    }

    Assert.assertFalse(exceptionThrown);
  }


  /**
   * Tests that LimaCharlieCollector creates and stores an OutputHandler thread on event modification
   * event. Tests single modification on single file. Only tests that list of OutputHandlers is size
   * 0 before any modification and size 1 after a single modification event.
   */
  @Test
  public void testHandlerCreationOnSingleModifySingleFile() {
    File testFile = new File(absPath + "/testFile");
    boolean exceptionThrown = false;

    try {
      LimaCharlieCollector limaCollector = new LimaCharlieCollector(absPath, intStub);
      Thread colThread = limaCollector.getThread();

      // Give thread time to start up
      sleep(sleepTime);
      Assert.assertTrue(colThread.isAlive());

      ArrayList<OutputHandler> handlers = limaCollector.getHandlers();
      Assert.assertEquals(0, handlers.size());

      if (!testFile.createNewFile()) {
        Assert.fail();
        return;
      }

      // Give thread time to process new event
      sleep(sleepTime);

      handlers = limaCollector.getHandlers();
      Assert.assertEquals(0, handlers.size());

      // Create 'MODIFIED' event
      BufferedWriter writer = new BufferedWriter(new FileWriter(testFile, true));
      writer.append("test text");
      writer.close();
      // Give thread time to process new event
      sleep(sleepTime);

      handlers = limaCollector.getHandlers();
      Assert.assertEquals(1, handlers.size());

    } catch (InterruptedException | IOException e) {
      e.printStackTrace();
      exceptionThrown = true;
    }

    if (testFile.exists()) {
      testFile.delete();
    }

    Assert.assertFalse(exceptionThrown);
  }

  /**
   * Tests that LimaCharlieCollector creates and stores an OutputHandler thread for each event
   * modification event. Tests multiple modification events on single file.
   */
  @Test
  public void testHandlerCreationOnMultipleModifySingleFile() {
    File testFile = new File(absPath + "/testFile");
    boolean exceptionThrown = false;

    try {
      LimaCharlieCollector limaCollector = new LimaCharlieCollector(absPath, intStub);
      Thread colThread = limaCollector.getThread();

      // Give thread time to start up
      sleep(sleepTime);
      Assert.assertTrue(colThread.isAlive());

      ArrayList<OutputHandler> handlers = limaCollector.getHandlers();
      Assert.assertEquals(0, handlers.size());

      if (!testFile.createNewFile()) {
        Assert.fail();
        return;
      }

      // Give thread time to process new event
      sleep(sleepTime);

      handlers = limaCollector.getHandlers();
      Assert.assertEquals(0, handlers.size());

      // Create 'MODIFIED' event
      BufferedWriter writer = new BufferedWriter(new FileWriter(testFile, true));
      writer.append("test text");
      writer.close();
      // Give thread time to process new event
      sleep(sleepTime);

      handlers = limaCollector.getHandlers();
      Assert.assertEquals(1, handlers.size());

      // Reset cache of file events to ensure second event is not considered a duplicate
      limaCollector.resetCache();

      // Create second 'MODIFIED' event
      writer = new BufferedWriter(new FileWriter(testFile, true));
      writer.append("second test text");
      writer.close();
      // Give thread time to process new event
      sleep(sleepTime);

      handlers = limaCollector.getHandlers();
      Assert.assertEquals(2, handlers.size());

    } catch (InterruptedException | IOException e) {
      e.printStackTrace();
      exceptionThrown = true;
    }

    if (testFile.exists()) {
      testFile.delete();
    }

    Assert.assertFalse(exceptionThrown);
  }

  /**
   * Tests that LimaCharlieCollector creates and stores an OutputHandler thread for each event
   * modification event. Tests single modification events on multiple files.
   */
  @Test
  public void testHandlerCreationOnSingleModifyMultipleFiles() {
    File testFile = new File(absPath + "/testFile");
    File testFile2 = new File(absPath + "/testFile2");
    boolean exceptionThrown = false;

    try {
      LimaCharlieCollector limaCollector = new LimaCharlieCollector(absPath, intStub);
      Thread colThread = limaCollector.getThread();

      // Give thread time to start up
      sleep(sleepTime);
      Assert.assertTrue(colThread.isAlive());

      ArrayList<OutputHandler> handlers = limaCollector.getHandlers();
      Assert.assertEquals(0, handlers.size());

      if (!testFile.createNewFile() || !testFile2.createNewFile()) {
        Assert.fail();
        return;
      }

      // Give thread time to process new events
      sleep(sleepTime);

      handlers = limaCollector.getHandlers();
      Assert.assertEquals(0, handlers.size());

      // Create 'MODIFIED' event on first file
      BufferedWriter writer = new BufferedWriter(new FileWriter(testFile, true));
      writer.append("test text");
      writer.close();
      // Give thread time to process new event
      sleep(sleepTime);

      handlers = limaCollector.getHandlers();
      Assert.assertEquals(1, handlers.size());

      // Create second 'MODIFIED' event on second file
      writer = new BufferedWriter(new FileWriter(testFile2, true));
      writer.append("second test text");
      writer.close();
      // Give thread time to process new event
      sleep(sleepTime);

      handlers = limaCollector.getHandlers();
      Assert.assertEquals(2, handlers.size());

    } catch (InterruptedException | IOException e) {
      e.printStackTrace();
      exceptionThrown = true;
    }

    if (testFile.exists()) {
      testFile.delete();
    }
    if (testFile2.exists()) {
      testFile2.delete();
    }

    Assert.assertFalse(exceptionThrown);
  }

  /**
   * Tests that LimaCharlieCollector creates and stores an OutputHandler thread for each event
   * modification event. Tests multiple modification events on multiple files.
   */
  @Test
  public void testHandlerCreationOnMultipleModifyMultipleFiles() {
    File testFile = new File(absPath + "/testFile");
    File testFile2 = new File(absPath + "/testFile2");
    boolean exceptionThrown = false;

    try {
      LimaCharlieCollector limaCollector = new LimaCharlieCollector(absPath, intStub);
      Thread colThread = limaCollector.getThread();

      // Give thread time to start up
      sleep(sleepTime);
      Assert.assertTrue(colThread.isAlive());

      ArrayList<OutputHandler> handlers = limaCollector.getHandlers();
      Assert.assertEquals(0, handlers.size());

      if (!testFile.createNewFile() || !testFile2.createNewFile()) {
        Assert.fail();
        return;
      }

      // Give thread time to process new events
      sleep(sleepTime);

      handlers = limaCollector.getHandlers();
      Assert.assertEquals(0, handlers.size());

      // Create 'MODIFIED' event on first file
      BufferedWriter writer = new BufferedWriter(new FileWriter(testFile, true));
      writer.append("test text in file 1");
      writer.close();
      // Give thread time to process new event
      sleep(sleepTime);

      handlers = limaCollector.getHandlers();
      Assert.assertEquals(1, handlers.size());

      // Create 'MODIFIED' event on second file
      writer = new BufferedWriter(new FileWriter(testFile2, true));
      writer.append("test text in file 2");
      writer.close();
      // Give thread time to process new event
      sleep(sleepTime);

      handlers = limaCollector.getHandlers();
      Assert.assertEquals(2, handlers.size());

      // Reset cache of files to ensure second event is not considered a duplicate
      limaCollector.resetCache();

      // Create second 'MODIFIED' event on first file
      writer = new BufferedWriter(new FileWriter(testFile, true));
      writer.append("some more text in file 1");
      writer.close();
      // Give thread time to process new event
      sleep(sleepTime);

      handlers = limaCollector.getHandlers();
      Assert.assertEquals(3, handlers.size());

      // Create second 'MODIFIED' event on second file
      writer = new BufferedWriter(new FileWriter(testFile2, true));
      writer.append("some more text in file 2");
      writer.close();
      // Give thread time to process new event
      sleep(sleepTime);

      handlers = limaCollector.getHandlers();
      Assert.assertEquals(4, handlers.size());

    } catch (InterruptedException | IOException e) {
      e.printStackTrace();
      exceptionThrown = true;
    }

    if (testFile.exists()) {
      testFile.delete();
    }
    if (testFile2.exists()) {
      testFile2.delete();
    }

    Assert.assertFalse(exceptionThrown);
  }
}
