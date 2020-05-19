import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.JsonReader;
import com.splunk.Args;
import com.splunk.Index;
import com.splunk.Job;
import com.splunk.JobArgs;
import com.splunk.JobEventsArgs;
import com.splunk.ResultsReaderXml;
import com.splunk.Role;
import com.splunk.SSLSecurityProtocol;
import com.splunk.SavedSearch;
import com.splunk.SavedSearchDispatchArgs;
import com.splunk.Service;
import com.splunk.ServiceArgs;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SplunkInputConstructor class Connects to local Splunk instance and handles sending commands to
 * instance.
 */
public class SplunkInputConstructor implements SIEM {

  private Service service;

  /**
   * Constructor
   *
   * Connects to Splunk instance using credentials in path "src/main/resources/SplunkCredentials.json".
   * Path adapts to operating system it's running on.
   *
   * @throws RuntimeException if failed to connect
   * @param connect boolean that determines whether to connect to Splunk instance
   */
  public SplunkInputConstructor(boolean connect) throws RuntimeException {

    if (!connect) {
      return;
    }

    String os = System.getProperty("os.name");
    String credentialsPath;
    if (os.contains("Windows")) {
      credentialsPath = "src\\main\\resources\\SplunkCredentials.json";
    } else {
      credentialsPath = "src/main/resources/SplunkCredentials.json";
    }
    try {
      connect(credentialsPath);
    } catch (IOException e) {
      e.printStackTrace();
    } catch (RuntimeException e) {
      throw new RuntimeException(e);
    }
  }


  /**
   * Parses JSON Credentials file and returns key:value pairs
   *
   * @param filePath String of path to the credentials file
   * @return HashMap<String,String> which has the USERNAME, PASSWORD, HOST, and PORT as keys
   */
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
   * Connect to Splunk instance
   *
   * @param credentialsPath path to JSON file that includes Splunk login credentials. This includes keys "USERNAME", "PASSWORD", "HOST", and "PORT"
   * @return true if successfully connected
   * @throws RuntimeException if failed to connect
   * @throws IOException if credentials file cannot be read
   */
  @Override
  public boolean connect(String credentialsPath) throws RuntimeException, IOException {

    // Extract credentials from credentialsPath
    HashMap<String, String> credentials = getCredentials(credentialsPath);
    String username = credentials.get("USERNAME");
    String password = credentials.get("PASSWORD");
    String host = credentials.get("HOST");
    String portStr = credentials.get("PORT");
    int port = Integer.parseInt(portStr);

    // Set up args for connection
    ServiceArgs loginArgs = new ServiceArgs();
    loginArgs.setUsername(username);
    loginArgs.setPassword(password);
    loginArgs.setHost(host);
    loginArgs.setPort(port);
    loginArgs.setSSLSecurityProtocol(SSLSecurityProtocol.TLSv1_2);
    loginArgs.setScheme("https");

    // Connect to Splunk server
    try {
      service = Service.connect(loginArgs);
    } catch (RuntimeException e) {
      throw new RuntimeException(e);
    }

    return true;
  }

  /**
   * Upload log file to Splunk
   *
   * <p>Uploads log file found at passed filePath to Splunk using toolFrom to determine output
   * format and index to upload to. Can use sinkhole argument to delete file after uploading.
   *
   * @param filePath String of path to log file to be uploaded
   * @param format String of log format, e.g. "json"
   * @param toolFrom String of tool name
   * @param sinkhole boolean, true: delete file after uploading, false: don't delete file after
   *     uploading
   * @return true if successfully uploaded log file
   * @throws IOException when filePath does not exist, cannot be read, or cannot be deleted if
   *     sinkhole set to true
   */
  @Override
  public boolean logManagement(String filePath, String format, String toolFrom, boolean sinkhole)
      throws IOException {
    // Ensure filePath exists, throw exception if not found
    File logFile = new File(filePath);
    if (!logFile.exists()) {
      throw new FileNotFoundException(filePath);
    }
    // Ensure filePath is readable
    if (!logFile.canRead()) {
      throw new IOException();
    }

    String indexName = toolFrom + "_index";
    String srcType = '_' + format.toLowerCase();

    // Get index for toolFrom, if it doesn't exist, create one
    Index toolIndex = service.getIndexes().get(indexName);
    if (toolIndex == null) {
      toolIndex = service.getIndexes().create(indexName);
    }

    // Set sourcetype for data
    Args indexArgs = new Args();
    indexArgs.put("sourcetype", srcType);
    toolIndex.upload(filePath, indexArgs);

    // Add new index to list of default searchable indexes for admin role
    Role role = service.getRoles().get("admin");
    String[] defaults = role.getSearchIndexesDefault();
    String[] newDefaults = new String[defaults.length + 1];
    for (int i = 0; i < defaults.length; i++) {
      if (defaults[i].equals(indexName)) {
        return true;
      }
      newDefaults[i] = defaults[i];
    }
    newDefaults[defaults.length] = indexName;
    role.setSearchIndexesDefault(newDefaults);

    if (sinkhole) {
      if (!logFile.delete()) {
        throw new IOException(
            "Failed to delete log file after uploading, file: " + logFile.getPath());
      }
    }

    return true;
  }

  /**
   * Overload logManagement(String, String, false)
   *
   * <p>Overloads method logManagement(String, String, boolean), passing the two String arguments
   * and a default value false for boolean sinkhole argument.
   *
   * @param filePath String of path to file to upload to Splunk
   * @param toolFrom String of tool name the log file came from
   * @return true if successfully uploaded logfile
   * @throws IOException when filePath does not exist or cannot be read
   */
  @Override
  public boolean logManagement(String filePath, String format, String toolFrom) throws IOException {
    try {
      return logManagement(filePath, format, toolFrom, false);
    } catch (IOException e) {
      throw new IOException(e);
    }
  }

  /**
   * Perform one-off search
   *
   * <p>Performs one-off blocking search and returns list of specified fields in the form of a
   * key:value pair
   *
   * @param query String query to search
   * @param fields String[] of fields to return
   * @return List of Maps of specified fields and their values
   */
  public List<Map<String, String>> oneOffSearch(String query, String[] fields) {
    // Run a blocking search
    JobArgs jobargs = new JobArgs();
    jobargs.setExecutionMode(JobArgs.ExecutionMode.BLOCKING);

    // A blocking search returns the job when the search is done
    System.out.println("Wait for the search to finish...");
    Job job = service.getJobs().create(query, jobargs);
    System.out.println("...done!\n");

    // Extract fields from found events
    JobEventsArgs jobEventsArgs = new JobEventsArgs();
    jobEventsArgs.setFieldList(fields);
    InputStream events = job.getEvents(jobEventsArgs);

    // Return list of events with key:value pairs of fields
    List<Map<String, String>> ret = new ArrayList<>();

    // Read returned events, storing fields in ret
    try {
      ResultsReaderXml reader = new ResultsReaderXml(events);
      HashMap<String, String> event;
      while ((event = reader.getNextEvent()) != null) {
        HashMap<String, String> curMap = new HashMap<>();
        for (String key : event.keySet()) {
          curMap.put(key, event.get(key));
        }
        ret.add(curMap);
      }
      reader.close();
    } catch (IOException e) {
      e.printStackTrace();
      return ret;
    }

    return ret;
  }

  /**
   * Dispatches Splunk saved search and waits for it to complete. Does not handle results of saved
   * search and assumes it has appropriate alert actions in place that will trigger when search is
   * run. Saved Searches can be either Splunk reports or alerts, and either can be run.
   *
   * @param searchName String for the name of search to execute. Name of search must match a saved
   *     search that already exists in relevant Splunk instance.
   * @param triggerActions boolean whether to trigger actions linked to search; true to trigger
   *     actions, false to not trigger actions.
   * @return returns true if search was successfully found, dispatched and completed. Returns false
   *     otherwise.
   */
  @Override
  public boolean runReport(String searchName, boolean triggerActions) {
    // Retrieve saved search using searchName
    SavedSearch savedSearch = service.getSavedSearches().get(searchName);

    if (savedSearch == null) {
      System.err.println(searchName + " saved search does not exist");
      return false;
    }

    // Run a saved search and poll for completion
    System.out.println(
        "Run the '" + savedSearch.getName() + "' search (" + savedSearch.getSearch() + ")\n");
    Job jobSavedSearch = null;

    // Run the saved search
    try {
      // Ensure Lookups and Actions are enabled for search job
      SavedSearchDispatchArgs args = new SavedSearchDispatchArgs();
      args.setDispatchLookups(true);
      args.setTriggerActions(triggerActions);
      // Dispatch search job
      jobSavedSearch = savedSearch.dispatch(args);
    } catch (InterruptedException e1) {
      e1.printStackTrace();
    }

    System.out.println("Waiting for the job to finish...\n");

    if (jobSavedSearch != null) {
      // Wait for the job to finish
      while (!jobSavedSearch.isDone()) {
        try {
          Thread.sleep(500);
        } catch (InterruptedException e) {
          e.printStackTrace();
          System.err.println("Search job " + searchName + " interrupted.");
          return false;
        }
      }
    } else {
      System.err.println("Search job " + searchName + " failed to dispatch!");
      return false;
    }

    System.out.println(
        "Search job \"" + searchName + "\" found " + jobSavedSearch.getEventCount() + " results");
    return true;
  }

  /**
   * Set up file integrity monitoring
   *
   * <p>Not yet implemented
   *
   * @param nodeIdentifier identifier for node to set up FIM on
   * @param filePath path to monitor
   * @return false always
   * @throws RuntimeException does not currently throw
   */
  @Override
  public boolean fileIntegrityMonitor(String nodeIdentifier, String filePath)
      throws RuntimeException {
    return false;
  }

  /**
   * Perform correlation on independent variables and dependent variable
   *
   * @param dependent dependent variable to correlate against
   * @param independents list of independent variables to determine correlation coefficient
   * @return null always
   * @throws IllegalArgumentException does not currently throw
   */
  @Override
  public double[] correlate(String dependent, String[] independents)
      throws IllegalArgumentException {
    return null;
  }
}
