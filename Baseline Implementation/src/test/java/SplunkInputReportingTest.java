import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.JsonReader;
import com.splunk.Args;
import com.splunk.Index;
import com.splunk.SSLSecurityProtocol;
import com.splunk.SavedSearch;
import com.splunk.Service;
import com.splunk.ServiceArgs;
import com.splunk.Settings;
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
 * JUnit test class for testing runReport() method of SplunkInputConstructor class. Because these
 * tests use Splunk indexes, the test index needs to be cleaned between tests. Unfortunately, this
 * process can be lengthy and therefore the test may take a few minutes. Tests also require root
 * access as it requires reading and deleting of files in privileged directories.
 *
 * <p>As this method interfaces with a Splunk server instance, details about that instance are
 * required for these tests to run. These details are expected to be included in the file
 * "src/test/resources/cybersecurity2/SplunkCredentialsGood.json". These tests require this file to
 * contain a valid username, password, host, and port.
 */
public class SplunkInputReportingTest {

  private Index index;
  private Service service;
  private String searchName;
  private SavedSearch testSearch;

  private File testFile;
  private String testPath;
  private File outFile;
  private String lookupPath;

  private SplunkInputConstructor splunkIn;
  private String splunkCredentialsPath = "src/test/resources/cybersecurity2/SplunkCredentialsGood.json";

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
   * Uploads test data to Splunk index. Creates test search to execute
   * and path to output of search.
   *
   * <p>Fails test if any credential field is null, port is not integer, cannot connect to Splunk server,
   * testFile already exists, cannot create the testFile, any IOException occurs while attempting to
   * write to testFile or fails to upload test data.
   */
  @Before
  public void setUp() {
    HashMap<String, String> creds;
    try {
      creds = getCredentials(splunkCredentialsPath);
    } catch (IOException e) {
      e.printStackTrace();
      Assert.fail();
      return;
    }
    // Extract connection details from arguments
    String username = creds.get("USERNAME");
    String password = creds.get("PASSWORD");
    String host = creds.get("HOST");
    String portStr = creds.get("PORT");
    int port = 0;

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

    // Create empty test index, if exists, clean it
    index = service.getIndexes().get("testtool_index");
    if (index == null) {
      index = service.getIndexes().create("testtool_index");
    } else {
      index.clean(180);
    }
    Assert.assertTrue(index.getTotalEventCount() == 0);

    // Create log file to upload
    String testEvent1 = "{\"routing\": {\"this\": \"6227d6a1b140e182ca0525ba87f801d2\", \"hostname\": \"HOST-PC\", \"event_type\": \"DNS_REQUEST\", \"parent\": \"c889af0876d08ca3f3c6d664b112875f\", \"tags\": [\"workstation\"], \"event_id\": \"63c3a9cb-2294-4ac8-b791-aeb3db9557bf\", \"oid\": \"e1c68fed-0825-4561-a6df-3e31303166b7\", \"iid\": \"16ce081f-b30c-40ed-9e95-5803878caf66\", \"int_ip\": \"169.253.201.23\", \"ext_ip\": \"58.174.197.102\", \"sid\": \"f96fea93-bc47-48ba-ab85-2ae69dc7b79e\", \"event_time\": 1535196081381, \"plat\": 268435456, \"arch\": 2, \"moduleid\": 2}, \"event\": {\"DNS_TYPE\": 1, \"PROCESS_ID\": 0, \"IP_ADDRESS\": \"172.217.167.74\", \"DOMAIN_NAME\": \"cloudusersettings-pa.clients6.google.com\", \"MESSAGE_ID\": 2459}}";
    String testEvent2 = "{\"routing\": {\"this\": \"c4de6aad7121c80ecd8b6f2f1dc002b1\", \"hostname\": \"HOST-PC\", \"event_type\": \"NEW_PROCESS\", \"parent\": \"a9a48552e83253af6f528d1c55c4d7da\", \"tags\": [\"workstation\"], \"event_id\": \"1f85ef80-bb65-474f-a686-08118f8d8fbe\", \"oid\": \"e1c68fed-0825-4561-a6df-3e31303166b7\", \"iid\": \"16ce081f-b30c-40ed-9e95-5803878caf66\", \"int_ip\": \"169.253.201.23\", \"ext_ip\": \"58.174.197.102\", \"sid\": \"f96fea93-bc47-48ba-ab85-2ae69dc7b79e\", \"event_time\": 1535195364012, \"plat\": 268435456, \"arch\": 2, \"moduleid\": 2}, \"event\": {\"MEMORY_USAGE\": 20049920, \"PARENT\": {\"MEMORY_USAGE\": 17158144, \"COMMAND_LINE\": \"C:\\\\WINDOWS\\\\system32\\\\svchost.exe -k DcomLaunch -p\", \"PROCESS_ID\": 1324, \"THREADS\": 23, \"USER_NAME\": \"NT AUTHORITY\\\\SYSTEM\", \"FILE_PATH\": \"C:\\\\WINDOWS\\\\system32\\\\svchost.exe\", \"BASE_ADDRESS\": 140702368989184, \"PARENT_PROCESS_ID\": 1140}, \"COMMAND_LINE\": \"\\\"C:\\\\WINDOWS\\\\system32\\\\backgroundTaskHost.exe\\\" -ServerName:App.AppXemn3t55segp7q92mwd35v2a5rk5mvwyz.mca\", \"PROCESS_ID\": 15388, \"THREADS\": 12, \"USER_NAME\": \"HOST-PC\\\\USER\", \"FILE_PATH\": \"C:\\\\WINDOWS\\\\system32\\\\backgroundTaskHost.exe\", \"BASE_ADDRESS\": 140701829300224, \"PARENT_PROCESS_ID\": 1324}}";
    Path curPath = Paths.get("");
    testPath = curPath.toAbsolutePath().toString();
    testPath = testPath + "/testFile";
    testFile = new File(testPath);

    // Ensure testFile doesn't exist before trying to create new one
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
      writer.append(testEvent1);
      writer.append(testEvent2);
      writer.close();
    } catch (IOException e) {
      e.printStackTrace();
      Assert.fail();
    }

    // Connect to Splunk server
    try {
      splunkIn = new SplunkInputConstructor(false);
      splunkIn.connect(splunkCredentialsPath);
    } catch (RuntimeException | IOException e) {
      e.printStackTrace();
      Assert.fail();
    }

    // Upload test data
    try {
      splunkIn.logManagement(testPath, "json", "testtool");
    } catch (IOException e) {
      e.printStackTrace();
      Assert.fail();
    }

    // Ensure that index eventually contains test data from testFile
    boolean result = SplunkInputLogManagementTest
        .assertEventuallyTrue(new SplunkInputLogManagementTest.EventuallyTrueBehavior() {
          @Override
          public boolean predicate() {
            index = service.getIndexes().get("testtool_index");
            index.refresh();
            index.update();
            return index.getTotalEventCount() == 2;
          }
        });
    Assert.assertTrue(result);

    // Create Saved Search to run
    String query = "index=\"testtool_index\" routing.event_type=\"NEW_PROCESS\" earliest=1 latest=now";
    searchName = "Test Search";
    testSearch = service.getSavedSearches().get(searchName);
    if (testSearch != null) {
      testSearch.remove();
    }
    Args args = new Args();
    args.put("actions", "lookup");
    args.put("action.lookup.filename", "testOut.csv");
    testSearch = service.getSavedSearches().create(searchName, query, args);

    // Get path of csv lookup output file
    char s;
    Settings settings = service.getSettings();
    String os = service.getInfo().getOsName();
    if (os.equals("Windows")) {
      s = '\\';
    } else {
      s = '/';
    }
    String homePath = settings.getSplunkHome();
    lookupPath =
        homePath + s + "etc" + s + "apps" + s + "search" + s + "lookups" + s + "testOut.csv";
    outFile = new File(lookupPath);
  }

  /**
   * Post test cleanup
   *
   * <p>Fails if testFile or outFile cannot be deleted
   */
  @After
  public void tearDown() {
    if (testFile.exists()) {
      if (!testFile.delete()) {
        System.err.println("Failed to delete test file");
        Assert.fail();
      }
    }

    if (testSearch != null) {
      testSearch.remove();
    }

    if (outFile != null) {
      if (outFile.exists()) {
        if (!outFile.delete()) {
          System.err.println("Failed to delete output file");
          Assert.fail();
        }
      }
    }
  }

  /**
   * Tests that SplunkInputConstructor.runReport() executes specified search.
   */
  @Test
  public void testRunReport() {
    // Run search
    splunkIn.runReport(searchName, true);

    // Test expected output file exists
    outFile = new File(lookupPath);
    Assert.assertTrue(outFile.exists());
  }
}
