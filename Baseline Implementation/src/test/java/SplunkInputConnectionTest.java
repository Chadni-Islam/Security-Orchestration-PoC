import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.JsonReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import org.junit.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * JUnit test class for connecting to Splunk using the SplunkInputConstructor class. As this class
 * interfaces with a Splunk server instance, details about that instance are required for these
 * tests to run. These details are expected to be included in the following files in the directory
 * "src/test/resources/cybersecurity2/":
 *
 * "SplunkCredentialsGood.json", "SplunkCredentialsBadHost.json", "SplunkCredentialsBadPort.json",
 * "SplunkCredentialsBadUsername.json", "SplunkCredentialsBadPassword.json"
 */
public class SplunkInputConnectionTest {

  private String goodCredentialsPath = "src/test/resources/cybersecurity2/SplunkCredentialsGood.json";
  private String badHostPath = "src/test/resources/cybersecurity2/SplunkCredentialsBadHost.json";
  private String badPortPath = "src/test/resources/cybersecurity2/SplunkCredentialsBadPort.json";
  private String badUsernamePath = "src/test/resources/cybersecurity2/SplunkCredentialsBadUsername.json";
  private String badPasswordPath = "src/test/resources/cybersecurity2/SplunkCredentialsBadPassword.json";


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


  @Before
  public void setUp() {
    HashMap<String, String> creds;
    try {
      creds = getCredentials(goodCredentialsPath);
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

    // If any left blank, fail test
    if (username == null || password == null || host == null || portStr == null) {
      Assert.fail();
      return;
    }

    // Convert port argument to int
    try {
      int port = Integer.parseInt(portStr);
    } catch (NumberFormatException e) {
      System.err.println("Invalid port number");
      Assert.fail();
    }
  }

  @After
  public void tearDown() throws Exception {
  }

  /**
   * Tests that SplunkInputConstructor constructor connects to Splunk server successfully when given
   * valid connection parameters and doesn't throw exception.
   */
  @Test
  public void testConnectSuccessWithValidCredentials() {
    // Create Input Constructor without connecting
    SplunkInputConstructor splunkIn = new SplunkInputConstructor(false);

    // Connect using valid test data
    try {
      splunkIn.connect(goodCredentialsPath);
    } catch (RuntimeException | IOException e) {
      e.printStackTrace();
      Assert.fail();
    }
  }

  /**
   * Tests that SplunkInputConstructor throws exception when invalid host is passed.
   */
  @Test
  public void testConnectFailWithInvalidHost() {
    boolean exceptionThrown = false;
    try {
      SplunkInputConstructor splunkIn = new SplunkInputConstructor(false);
      splunkIn.connect(badHostPath);
      exceptionThrown = false;
    } catch (RuntimeException expected) {
      exceptionThrown = true;
    } catch (IOException e) {
      e.printStackTrace();
      exceptionThrown = true;
    }
    Assert.assertTrue(exceptionThrown);
  }

  /**
   * Tests that SplunkInputConstructor throws exception when invalid port is passed.
   */
  @Test
  public void testConnectFailWithInvalidPort() {
    boolean exceptionThrown = false;
    try {
      SplunkInputConstructor splunkIn = new SplunkInputConstructor(false);
      splunkIn.connect(badPortPath);
      exceptionThrown = false;
    } catch (RuntimeException expected) {
      exceptionThrown = true;
    } catch (IOException e) {
      e.printStackTrace();
      exceptionThrown = true;
    }
    Assert.assertTrue(exceptionThrown);
  }

  /**
   * Tests that SplunkInputConstructor throws exception when incorrect username login is passed.
   */
  @Test
  public void testConnectFailWithInvalidUsername() {
    boolean exceptionThrown = false;
    try {
      SplunkInputConstructor splunkIn = new SplunkInputConstructor(false);
      splunkIn.connect(badUsernamePath);
      exceptionThrown = false;
    } catch (RuntimeException expected) {
      exceptionThrown = true;
    } catch (IOException e) {
      e.printStackTrace();
      exceptionThrown = true;
    }
    Assert.assertTrue(exceptionThrown);
  }

  /**
   * Tests that SplunkInputConstructor throws exception when incorrect login password is passed.
   */
  @Test
  public void testConnectFailWithInvalidPassword() {
    boolean exceptionThrown = false;
    try {
      SplunkInputConstructor splunkIn = new SplunkInputConstructor(false);
      splunkIn.connect(badPasswordPath);
      exceptionThrown = false;
    } catch (RuntimeException expected) {
      exceptionThrown = true;
    } catch (IOException e) {
      e.printStackTrace();
      exceptionThrown = true;
    }
    Assert.assertTrue(exceptionThrown);
  }
}