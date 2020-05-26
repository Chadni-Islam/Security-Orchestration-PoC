import de.siegmar.fastcsv.reader.CsvContainer;
import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.CsvRow;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import org.apache.commons.io.FileUtils;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;

/**
 * StaticInterpreter class
 *
 * <p>StaticInterpreter class Used to interpret output from Splunk and LimaCharlie and determine
 * which action to take based on the interpretation
 */
public class StaticInterpreter {

  /*
   LimaCharlie log data takes the form
   {"routing": {...}, "event": {...}}
   where routing is a hash with keys:
   hostname, event_type, tags, event_id, oid, iid, int_ip, ext_ip, sid, event_time, plat, arch, moduleid
  */
  private static final String[] LC_ROUTING_HASH_KEYS = {
    "iid",
    "int_ip",
    "oid",
    "tags",
    "ext_ip",
    "sid",
    "hostname",
    "event_type",
    "event_id",
    "plat",
    "arch",
    "moduleid",
    "event_time"
  };

  // container for fields which needs to be fetched from FILE_CREATE data
  // Keys are exactly as CSV headers, Values are normalized versions
  private static final HashMap<String, String> FILES_SEARCH_FIELDS = new HashMap<>();

  // container for fields which needs to be fetched from NEW_PROCESS data
  // Keys are exactly as CSV headers, Values are normalized versions
  private static final HashMap<String, String> PROCESS_SEARCH_FIELDS = new HashMap<>();

  /**
   * Contructor for StaticInterpreter
   *
   * <p>Populates the FILES_SEARCH_FIELDS & PROCESS_SEARCH_FIELDS hashmaps
   */
  public StaticInterpreter() {
    populateHashMaps(FILES_SEARCH_FIELDS, "harmfulFile");
    populateHashMaps(PROCESS_SEARCH_FIELDS, "harmfulProcess");
  }

  private void populateHashMaps(HashMap<String, String> headers, String source) {
    headers.put("detect.routing.hostname", "hostname");
    headers.put("detect.routing.iid", "iid");
    headers.put("detect.routing.ext_ip", "ext_ip");
    headers.put("detect.routing.oid", "oid");
    headers.put("detect.routing.sid", "sid");
    headers.put("detect.routing.tags{}", "tags{}");

    if (source == "harmfulFile") {
      headers.put("detect.event.FILE_PATH", "filePath");
    } else if (source == "harmfulProcess") {
      headers.put("detect.event.PROCESS_ID", "process_id");
    }
  }

  /**
   * Interprets the file given to it and returns information about file
   *
   * @param filePath path to file to interpret
   * @param toolFrom tool that produced file
   * @return HashMap that contains information about file {action, type, toolTo, toolFrom, message,
   *     filePath} else return HashMap with fail message.
   */
  public ArrayList<HashMap<String, String>> interpret(String filePath, String toolFrom)
      throws IOException {
    if (toolFrom.equals("limacharlie")) {
      return interpretLimaCharlieOutput(filePath);
    } else if (toolFrom.equals("splunk")) {
      return interpretSplunkOutput(filePath);
    }
    ArrayList<HashMap<String, String>> ret = new ArrayList<>();
    HashMap<String, String> temp = new HashMap<>();
    temp.put("message", "fail");
    ret.add(temp);
    return ret;
  }

  /**
   * Interpret Splunk Output
   *
   * <p>Returns list of actions with relevant information
   *
   * @param filePath Path to Splunk output file, expects a CSV file
   * @return An array list of hashmaps, where evey item of list is an action to perform
   * @throws IOException when file paths can not be found or isn't a file
   */
  public ArrayList<HashMap<String, String>> interpretSplunkOutput(String filePath)
      throws IOException {
    ArrayList<HashMap<String, String>> ret = new ArrayList<>();

    // Parse CSV file
    File file = new File(filePath);
    if (!file.exists() || !file.isFile()) {
      throw new IOException(filePath + " file does not exist");
    }
    CsvReader csvReader = new CsvReader();
    csvReader.setContainsHeader(true);
    CsvContainer csv = csvReader.read(file, StandardCharsets.UTF_8);

    // make a hashmap for each row in the CSV file
    for (CsvRow row : csv.getRows()) {
      HashMap<String, String> event = new HashMap<>();
      event.put("toolTo", "limacharlie");
      event.put("toolFrom", "splunk");
      event.put("message", "success");
      if (file.getName().equals("harmfulFiles.csv")) {
        event.put("action", "file_del");

        // from each row get event information,
        for (String s : FILES_SEARCH_FIELDS.keySet()) {
          // If any necessary fields are empty, return fail message
          if (row.getField(s).equals("")) {
            event.clear();
            event.put("message", "fail");
            break;
          }
          event.put(FILES_SEARCH_FIELDS.get(s), row.getField(s));
        }
        ret.add(event);
      } else if (file.getName().equals("harmfulProcesses.csv")) {
        event.put("action", "os_kill_process");

        // from each row get event information,
        for (String s : PROCESS_SEARCH_FIELDS.keySet()) {
          // If any necessary fields are empty, return fail message
          if (row.getField(s).equals("")) {
            event.clear();
            event.put("message", "fail");
            break;
          }
          event.put(PROCESS_SEARCH_FIELDS.get(s), row.getField(s));
        }
        ret.add(event);
      }
    }

    // return array of hashmaps
    return ret;
  }

  /**
   * LimaCharlie output interpreter
   *
   * <p>Returns an arraylist of actions with relevant information
   *
   * @param filePath Expects LC output in JSON format
   * @return An arraylist of hashmaps, where each item in the arraylist is an action to be
   *     performed.
   */
  public ArrayList<HashMap<String, String>> interpretLimaCharlieOutput(String filePath) {
    // Initialise to fixed values
    ArrayList<HashMap<String, String>> ret = new ArrayList<>();
    HashMap<String, String> curMap = new HashMap<>();

    if (isLimaCharlieLog(filePath)) {
      curMap.put("action", "Log Management");
      curMap.put("toolTo", "splunk");
      curMap.put("type", "json");
      curMap.put("toolFrom", "limacharlie");
      curMap.put("message", "success");
      curMap.put("filePath", filePath);
      ret.add(curMap);

    } else if (isLimaCharlieDR(filePath)) {
      boolean newProcessFound = false;
      boolean fileCreateFound = false;

      // Parse DR data and return list of events
      ArrayList<JsonElement> events = parseLimaCharlieDR(filePath);
      for (JsonElement obj : events) {
        // DR Data can only contain FILE_CREATE and NEW_PROCESS detections
        // If both have been found, stop parsing
        if (fileCreateFound && newProcessFound) {
          break;
        }
        JsonObject json = obj.getAsJsonObject();
        JsonObject routingJson = json.get("routing").getAsJsonObject();

        // If the event is for NEW_PROCESS, flag it as found
        if (routingJson.get("event_type").getAsString().equals("NEW_PROCESS")) {
          newProcessFound = true;
        }
        // If the event is for FILE_CREATE, flag it as found
        else if (routingJson.get("event_type").getAsString().equals("FILE_CREATE")) {
          fileCreateFound = true;
        }
      }

      if (newProcessFound) {
        // add NEW_PROCESS fields to map
        HashMap<String, String> processAction = new HashMap<>();
        processAction.put("action", "Reporting");
        processAction.put("toolTo", "splunk");
        processAction.put("type", "json");
        processAction.put("toolFrom", "limacharlie");
        processAction.put("message", "success");
        processAction.put("filePath", filePath);
        processAction.put("reportName", "harmfulProcesses");

        // Add action to returned list
        ret.add(processAction);
      }
      if (fileCreateFound) {
        // add FILE_CREATE fields to map
        HashMap<String, String> fileAction = new HashMap<>();
        fileAction.put("action", "Reporting");
        fileAction.put("toolTo", "splunk");
        fileAction.put("type", "json");
        fileAction.put("toolFrom", "limacharlie");
        fileAction.put("message", "success");
        fileAction.put("filePath", filePath);
        fileAction.put("reportName", "harmfulFiles");

        ret.add(fileAction);
      }
    } else {
      curMap.put("message", "fail");
      ret.add(curMap);
    }

    // Return list of action hashmaps
    return ret;
  }

  private ArrayList<JsonElement> parseLimaCharlieDR(String filePath) {
    ArrayList<JsonElement> ret = new ArrayList<>();

    File file = new File(filePath);
    try {
      Gson json1 = new Gson();
      // Each line must be a valid JSON object and have the routing hash
      for (String line : FileUtils.readLines(file, "UTF-8")) {
        JsonElement elem;
        // Google JSON will throw exception if it cannot parse the line
        try {
          elem = json1.fromJson(line, JsonElement.class);
          ret.add(elem);
        } catch (JsonSyntaxException e) {
          return null;
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
    return ret;
  }

  /**
   * Interprets the file as if it was LimaCharlie log data Returns true if it is log data, else
   * returns false
   *
   * @param filePath path to the LimaCharlie output
   * @return returns true if file is log data, else returns false
   */
  public Boolean isLimaCharlieLog(String filePath) {
    File file = new File(filePath);
    try {
      Gson json1 = new Gson();
      // Each line must be a valid JSON object and have the routing hash
      for (String line : FileUtils.readLines(file, "UTF-8")) {
        JsonElement elem;
        // Google JSON will throw exception if it cannot parse the line
        try {
          elem = json1.fromJson(line, JsonElement.class);
        } catch (JsonSyntaxException e) {
          return false;
        }

        JsonObject json = elem.getAsJsonObject();
        JsonObject routingJson = json.get("routing").getAsJsonObject();

        // No routing hash, therefore return false since all logs contain a routing hash
        if (routingJson == null) {
          return false;
        }

        // Checking to make sure the routing hash contains all expected keys for a LimaCharlie log
        for (String key : LC_ROUTING_HASH_KEYS) {
          if (routingJson.get(key) == null) {
            return false;
          }
        }

        // Must have an event hash
        // Event hash is different per event, therefore will not check internal structure
        // Top-level event hash is exclusive to LimaCharlieLog Data
        if (json.get("event") == null) {
          return false;
        }
      }
    } catch (IOException e) {
      return false;
    }

    return true;
  }

  /**
   * Attempts to interpret file as LimaCharlieDR Data If it can, then return true, else returns
   * false
   *
   * @param filePath path to file to interpret
   * @return true if it is LimaCharlieDR Data else return false
   */
  public Boolean isLimaCharlieDR(String filePath) {
    File file = new File(filePath);
    try {
      // Each line must have relevant hashes
      for (String line : FileUtils.readLines(file, "UTF-8")) {
        if (!line.isEmpty()) {

          // sometimes carriage returns result in parse errors
          line = line.replaceAll("\\n", "");
          line = line.replaceAll("\\r", "");
          Gson json1 = new Gson();
          JsonElement elem;
          // Google JSON will throw exception if it cannot parse the line
          try {
            elem = json1.fromJson(line, JsonElement.class);
          } catch (JsonSyntaxException e) {
            return false;
          }
          JsonObject json = elem.getAsJsonObject();

          if (json.get("source") == null) {
            return false;
          }
          if (json.get("detect") == null) {
            return false;
          }
          if (json.get("routing") == null) {
            return false;
          }
          if (json.get("detect_id") == null) {
            return false;
          }
          if (json.get("cat") == null) {
            return false;
          }
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
      return false;
    }
    return true;
  }
}
