import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.JsonReader;
import com.splunk.Index;
import com.splunk.SSLSecurityProtocol;
import com.splunk.Service;
import com.splunk.ServiceArgs;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * JUnit test class for testing logManagement() method of SplunkInputConstructor class. Because
 * these tests use Splunk indexes, the test index needs to be cleaned between tests. Unfortunately,
 * this process can be lengthy and therefore the test may take a few minutes.
 *
 * <p>As this method interfaces with a Splunk server instance, details about that instance are
 * required for these tests to run. These details are expected to be included in the file
 * "src/test/resources/cybersecurity2/SplunkCredentialsGood.json". These tests require this file to
 * contain a valid username, password, host, and port.
 */
public class SplunkInputLogManagementTest {

  private static final int NUM_TESTS = 3;

  private static boolean onceOffSetupDone;
  private static int testsRun;

  // Connection details
  private String splunkCredentialsPath = "src/test/resources/cybersecurity2/SplunkCredentialsGood.json";

  private Index index;
  private Service service;
  private int origCount;

  private File testFile;
  private String testPath;
  private String format;

  /**
   * Behavior class that contains placeholder for method to check if behavior is true, number of
   * tries before failing and time to pause between attempts.
   */
  public static abstract class EventuallyTrueBehavior {

    public int tries;
    public int pauseTime;

    {
      tries = 60;
      pauseTime = 1000;
    }

    public String timeoutMessage = "Test timed out before true.";

    public abstract boolean predicate();
  }

  /**
   * Test that passed behavior will eventually be true after a certain number of attempts. This is
   * used to check a behavior that may take some time to see the result is correct. When calling,
   * the passed behavior needs to be defined, including the predicate() behavior to check against.
   *
   * @param behavior EventuallyTrueBehavior object that defines number of attempts to make, time
   * between attempts and implementation of predicate() method that should return true when behavior
   * is correct.
   * @return true if passed behavior is true, false if not or after a timeout.
   */
  public static boolean assertEventuallyTrue(EventuallyTrueBehavior behavior) {
    int remainingTries = behavior.tries;
    while (remainingTries > 0) {
      boolean succeeded = behavior.predicate();
      if (succeeded) {
        return true;
      } else {
        remainingTries -= 1;
        try {
          Thread.sleep(behavior.pauseTime);
        } catch (InterruptedException e) {
          Assert.fail("Test interrupted");
          return false;
        }
      }
    }
    Assert.fail(behavior.timeoutMessage);
    return false;
  }

  // Extract credentials from filePath
  private HashMap<String, String> getCredentials(String filePath)
      throws IOException, JsonSyntaxException {
    Gson gson = new Gson();
    HashMap<String, String> ret = new HashMap<>();
    JsonReader reader = new JsonReader(new FileReader(filePath));
    JsonElement elem;

    try {
      elem = gson.fromJson(reader, JsonElement.class);
    } catch (JsonSyntaxException e) {
      throw new JsonSyntaxException(e);
    }

    JsonObject json = elem.getAsJsonObject();
    for (String s : json.keySet()) {
      ret.put(s, json.get(s).getAsString());
    }
    return ret;
  }


  /**
   * Pre-test setup
   *
   * <p>Extracts connection variables from credentials file, asserts that they are not
   * null and that passed port is an integer. Connects to Splunk instance and stores service.
   * Creates clean testtool index in Splunk and testFile with test data to be uploaded.
   *
   * <p>Fails test if any argument is null, port is not integer, cannot connect to Splunk server,
   * testFile already exists, cannot create the testFile, or any IOException occurs while attempting
   * to write to testFile.
   */
  @Before
  public void setUp() {
    // Ensure credentials file contains valid fields
    HashMap<String, String> creds;
    try {
      creds = getCredentials(splunkCredentialsPath);
    } catch (IOException e) {
      e.printStackTrace();
      Assert.fail();
      return;
    }
    // Extract connection details from credentials
    String username = creds.get("USERNAME");
    String password = creds.get("PASSWORD");
    String host = creds.get("HOST");
    String portStr = creds.get("PORT");
    int port;

    // If any left blank, fail test
    if (username == null || password == null || host == null || portStr == null) {
      Assert.fail();
      return;
    }

    // Convert port argument to int
    try {
      port = Integer.parseInt(portStr);
    } catch (NumberFormatException e) {
      System.err.println("Invalid port number");
      Assert.fail();
      return;
    }

    // Connect to Splunk
    service = null;
    ServiceArgs loginArgs = new ServiceArgs();
    loginArgs.setUsername(username);
    loginArgs.setPassword(password);
    loginArgs.setHost(host);
    loginArgs.setPort(port);
    loginArgs.setSSLSecurityProtocol(SSLSecurityProtocol.TLSv1_2);
    loginArgs.setScheme("https");

    try {
      service = Service.connect(loginArgs);
    } catch (RuntimeException e) {
      System.err.println("Failed to connect");
      Assert.fail();
      return;
    }

    // Create empty test index, if exists, save event count
    index = service.getIndexes().get("testtool_index");
    if (index == null) {
      index = service.getIndexes().create("testtool_index");
      origCount = 0;
    } else {
      if (!onceOffSetupDone) {
        index.clean(180);
        onceOffSetupDone = true;
        origCount = 0;
        testsRun = 0;
      } else {
        origCount = index.getTotalEventCount();
      }
    }

    // Create log file to upload
    String testData = "{\"routing\": {\"this\": \"6227d6a1b140e182ca0525ba87f801d2\", \"hostname\": \"HOST-PC\", \"event_type\": \"DNS_REQUEST\", \"parent\": \"c889af0876d08ca3f3c6d664b112875f\", \"tags\": [\"workstation\"], \"event_id\": \"63c3a9cb-2294-4ac8-b791-aeb3db9557bf\", \"oid\": \"e1c68fed-0825-4561-a6df-3e31303166b7\", \"iid\": \"16ce081f-b30c-40ed-9e95-5803878caf66\", \"int_ip\": \"169.253.201.23\", \"ext_ip\": \"58.174.197.102\", \"sid\": \"f96fea93-bc47-48ba-ab85-2ae69dc7b79e\", \"event_time\": 1535196081381, \"plat\": 268435456, \"arch\": 2, \"moduleid\": 2}, \"event\": {\"DNS_TYPE\": 1, \"PROCESS_ID\": 0, \"IP_ADDRESS\": \"172.217.167.74\", \"DOMAIN_NAME\": \"cloudusersettings-pa.clients6.google.com\", \"MESSAGE_ID\": 2459}}";
    Path curPath = Paths.get("");
    testPath = curPath.toAbsolutePath().toString();
    testPath = testPath + "/testFile";
    testFile = new File(testPath);
    format = "json";

    if (testFile.exists()) {
      System.err.println("Test file already exists");
      Assert.fail();
    }

    try {
      if (!testFile.createNewFile()) {
        System.err.println("Cannot create test log file");
        Assert.fail();
        return;
      }
      BufferedWriter writer = new BufferedWriter(new FileWriter(testFile, true));
      writer.append(testData);
      writer.close();

    } catch (IOException e) {
      e.printStackTrace();
      Assert.fail();
    }
  }

  /**
   * Post-test tear down.
   *
   * <p>Deletes testFile, and cleans and removes testtool index in Splunk. Fails test if testFile
   * cannot be deleted.
   */
  @After
  public void tearDown() {
    if (testFile.exists()) {
      if (!testFile.delete()) {
        System.err.println("Failed to delete testFile");
        Assert.fail();
      }
    }

    // Count number of tests run, on last one, clean and remove index
    testsRun++;
    if (testsRun >= NUM_TESTS) {
      index.clean(180);
      index.remove();
      index.update();
    }
  }

  /**
   * Tests that logManagement() method successfully uploads data from log file to index in Splunk
   * server. Fails if logManagement() throws exception, or expected behavior is not seen within the
   * timeout period.
   */
  @Test
  public void testLogManagement() {
    // Upload test data
    SplunkInputConstructor splunkIn = new SplunkInputConstructor(false);

    try {
      splunkIn.connect(splunkCredentialsPath);
      splunkIn.logManagement(testPath, format, "testtool");
    } catch (IOException e) {
      e.printStackTrace();
      Assert.fail();
    }

    // Test that index eventually contains test data from testFile
    boolean result = assertEventuallyTrue(new EventuallyTrueBehavior() {
      @Override
      public boolean predicate() {
        index = service.getIndexes().get("testtool_index");
        index.refresh();
        index.update();
        return index.getTotalEventCount() == (origCount + 1);
      }
    });

    Assert.assertTrue(result);
  }

  /**
   * Tests that logManagement() method successfully deletes the uploaded log file after uploading if
   * the sinkhole variable is set to true. Fails if logManagement() throws exception, or expected
   * behavior is not seen within the timeout period.
   */
  @Test
  public void testLogManagementSinkhole() {
    // Upload test data
    SplunkInputConstructor splunkIn = new SplunkInputConstructor(false);

    try {
      splunkIn.connect(splunkCredentialsPath);
      splunkIn.logManagement(testPath, format, "testtool", true);
    } catch (IOException e) {
      e.printStackTrace();
      Assert.fail();
    }

    // Test that index eventually contains test data from testFile
    boolean result = assertEventuallyTrue(new EventuallyTrueBehavior() {
      @Override
      public boolean predicate() {
        index = service.getIndexes().get("testtool_index");
        index.refresh();
        index.update();
        return index.getTotalEventCount() == (origCount + 1);
      }
    });

    Assert.assertTrue(result);

    // Test that testfile was successfully deleted during the logManagement() call
    Assert.assertFalse(testFile.exists());
  }

  /**
   * Tests that logManagement() throws exception if passed a testFile that doesn't exist. Fails if
   * no IOException is thrown.
   */
  @Test
  public void testLogManagementMissingFile() {
    boolean exceptionThrown = false;
    String fakePath = Paths.get("").toAbsolutePath().toString() + "/fakefile";
    File fakeFile = new File(fakePath);
    Assert.assertFalse(fakeFile.exists());

    // Upload test data
    SplunkInputConstructor splunkIn = new SplunkInputConstructor(false);

    try {
      splunkIn.connect(splunkCredentialsPath);
      splunkIn.logManagement(fakePath, format, "testtool");
    } catch (IOException expected) {
      exceptionThrown = true;
    }

    Assert.assertTrue(exceptionThrown);
  }
}
