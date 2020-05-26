import com.google.gson.stream.JsonReader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * LimaCharlie Class implements the EDR Interface
 * The class is used to send commands to LimaCharlie using its Python API.
 * This is achieved by executing the python scripts available.
 */
public class LimaCharlie implements EDR {

  private String scriptPath;
  private final String API_KEY;
  private final String OID;

  /**
   * Creates a LimaCharlie object. Reads the credential file to get the
   * API_KEY and OID value and sets the attributes accordingly.
   * @throws IOException if credentials or python script doesn't exist, throws exception
   */
  public LimaCharlie() throws IOException {
    String os = System.getProperty("os.name");
    String credentialsPath;
    if (os.contains("Windows")) {
      credentialsPath = "src\\main\\resources\\LimaCharlieCredentials.json";
      scriptPath = "src\\main\\python\\LimaCharlieConstructor.py";
    } else {
      credentialsPath = "src/main/resources/LimaCharlieCredentials.json";
      scriptPath = "src/main/python/LimaCharlieConstructor.py";
    }

    // check if given path represents a file
    File credentialsFile = new File(credentialsPath);
    if (!credentialsFile.exists() || !credentialsFile.isFile()) {
      throw new FileNotFoundException(credentialsPath);
    }

    File pythonFile = new File(scriptPath);
    if (!pythonFile.exists() || !pythonFile.isFile()) {
      throw new FileNotFoundException(scriptPath);
    }

    HashMap<String, String> credentials;
    try {
      credentials = getCredentials(credentialsPath);
    } catch (IOException e) {
      throw new IOException("Unable to read the credentials file.");
    }

    // Get API_KEY and OID
    if (credentials.size() == 2) {
      OID = credentials.get("OID");
      API_KEY = credentials.get("API_KEY");
    } else {
      OID = "";
      API_KEY = "";
    }
  }

  /**
   * Opens the credential file path to read in the credentials for LimaCharlie
   * The credential file should contain the API_KEY and OID
   * @param filePath String of path to the credentials file
   * @return HashMap<String,String> which has the API_KEY and OID as keys
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
   * Returns the OID attribute of the LimaCharlie instance
   *
   * @return String OID attribute
   */
  public String getOID() {
    return OID;
  }

  /**
   * Returns the API_KEY attribute of the LimaCharlie instance
   * @return String API_KEY attribute
   */
  public String getAPI_KEY() {
    return API_KEY;
  }

  /**
   * Method to delete a file Deletes a file given its path on the sensor
   * Achieves the file deletion by calling the required python script with the
   * correct arguments.
   *
   * @param sensorId SID of the sensor to which command needs to be send
   * @param path path of the file on endpoint which needs to be deleted
   * @throws IOException if python script does not exist, throws exception
   */
  public boolean deleteFile(String sensorId, String path) throws IOException {
    // check if given path represents a file
    File scriptFile = new File(scriptPath);
    if (!scriptFile.exists() || !scriptFile.isFile()) {
      throw new FileNotFoundException(scriptPath);
    }

    // joins the given strings and separates them with the give delimiter
    String args =
        String.join(
            " ",
            "-o " + this.getOID(),
            "-a " + this.getAPI_KEY(),
            "-s " + sensorId,
            "-c file_del",
            "-args file_path=" + path);
    executeScript(args);

    return true;
  }

  /**
   * Method to kill a process kills a process given its PID
   * Achieves the process termination by calling the required python script with the
   * correct arguments.
   *
   * @param sensorId sensorId SID of the sensor to which command needs to be send
   * @param pid process ID on the endpoint which needs to be killed by the sensor
   * @throws IOException if python script does not exist, throw exception
   */
  public boolean killProcess(String sensorId, String pid) throws IOException {
    // check if given path represents a file
    File scriptFile = new File(scriptPath);
    if (!scriptFile.exists() || !scriptFile.isFile()) {
      throw new FileNotFoundException(scriptPath);
    }

    // joins the given strings and separates them with the give delimiter
    String args =
        String.join(
            " ",
            "-o " + this.getOID(),
            "-a " + this.getAPI_KEY(),
            "-s " + sensorId,
            "-c os_kill_process",
            "-args pid=" + pid);
    executeScript(args);

    return true;
  }

  /**
   * Method to execute a script Uses java RunTime to execute the Python script represented by
   * 'scriptPath'
   *
   * @param args contains OID, API_KEY, SID, COMMAND_TYPE, COMMAND_ARGS
   * @return returns an ArrayList of Strings containing the outputs (STD.OUT) from the script
   */
  private ArrayList<String> executeScript(String args) {
    Process p = null;
    ArrayList<String> outputs = new ArrayList<>();
    try {
      p = Runtime.getRuntime().exec("python " + scriptPath + " " + args);
      BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
      String ret;
      while ((ret = in.readLine()) != null) {
        // if the sensor is offline return immediately
        if (ret.contains("sensor is offline")) {
          System.err.println("sensor is offline");
          return outputs;
        }
        outputs.add(ret);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }

    return outputs;
  }

  /**
   * Method that gets a list of processes running from a node
   * The list of processes on a sensor are found by calling the correct
   * python script that sends the command to LimaCharlie and returns the processes
   * as a List of Strings where each String is a JSON representation of the process.
   *
   * @param sensorId identifier of node to get the list of processes from
   * @return the list of processes running on the given node
   * @throws FileNotFoundException if python script does not exist, throw exception
   */
  public List<String> getProcesses(String sensorId) throws FileNotFoundException {
    // Check if given path represents a file
    File scriptFile = new File(scriptPath);
    if (!scriptFile.exists() || !scriptFile.isFile()) {
      throw new FileNotFoundException(scriptPath);
    }

    // joins the given strings and separates them with the give delimiter
    String args =
        String.join(
            " ",
            "-o " + this.getOID(),
            "-a " + this.getAPI_KEY(),
            "-s " + sensorId,
            "-c os_processes");

    return executeScript(args);
  }

  /**
   * Gets information about file
   * It is able to get the file information by using the Python script with the correct arguments
   * The information is then returned as String that is a JSON representation of the file
   * information.
   *
   * @param sensorId used to identify the node to get file information from
   * @param fileName name of file to get the file information from
   * @return string that has information about file, else return "fail"
   * @throws IOException if scriptPath does not exist
   */
  public String getFileInfo(String sensorId, String fileName) throws IOException {
    // Check if given path represents a file
    File scriptFile = new File(scriptPath);
    if (!scriptFile.exists() || !scriptFile.isFile()) {
      throw new FileNotFoundException(scriptPath);
    }

    // joins the given strings and separates them with the give delimiter
    String args =
        String.join(
            " ",
            "-o " + this.getOID(),
            "-a " + this.getAPI_KEY(),
            "-s " + sensorId,
            "-c file_info",
            "-args file_path=" + fileName);

    // File information will be the first line printed and information is only request for one file
    return executeScript(args).get(0);
  }

  /**
   * Method to get a file from a node.
   * Uses the python script to call the correct command to get the file from a sensor.
   *
   * @param sensorId SID of the sensor to which command needs to be send
   * @param filePath path of the file on endpoint which needs to be deleted
   * @param outputFilePath Must be an absolute path to directory where fetched file is saved
   * @return return the File if found, else return null
   * @throws IOException if python script does not exist, throws exception
   */
  public File getFile(String sensorId, String filePath, String outputFilePath)
      throws IOException {

    // Check if given path represents a file
    File scriptFile = new File(scriptPath);
    if (!scriptFile.exists() || !scriptFile.isFile()) {
      throw new FileNotFoundException(scriptPath);
    }

    // joins the given strings and separates them with the give delimiter
    String args =
        String.join(
            " ",
            "-o " + this.getOID(),
            "-a " + this.getAPI_KEY(),
            "-s " + sensorId,
            "-c file_get",
            "-args file_path=" + filePath,
            " output_file_path=" + outputFilePath);

    executeScript(args);

    // check if the retreived file exists, otherwise throw exception
    Path s = Paths.get(filePath);
    File f = new File(outputFilePath + s.getFileName());
    if (!f.exists() || !f.isFile()) {
      throw new FileNotFoundException("Output file not found: " + scriptPath);
    }

    return f;
  }

  /**
   * Method used to isolate a node from the network
   * Tells the sensor to stop all network connectivity on the host except LC comms to the backend.
   * it's network isolation, great to stop lateral movement. Achieves this by calling the correct
   * Python script with the correct arguments.
   *
   * @param sensorId identifier of the node to be isolated from the network
   * @return true if command sent successfully, else return false
   * @throws IOException if python script does not exist, throws exception
   */
  public boolean isolateNode(String sensorId) throws IOException {
    // Check if given path represents a file
    File scriptFile = new File(scriptPath);
    if (!scriptFile.exists() || !scriptFile.isFile()) {
      throw new FileNotFoundException(scriptPath);
    }

    // joins the given strings and separates them with the give delimiter
    String args =
        String.join(
            " ",
            "-o " + this.getOID(),
            "-a " + this.getAPI_KEY(),
            "-s " + sensorId,
            "-c segregate_network");

    ArrayList<String> ret = executeScript(args);
    return ret.contains("True");
  }

  /**
   * Method used to allow node to rejoin the network
   * Tells the sensor to allow network connectivity again (after it was segregated).
   * Achieves this by calling the correct python script.
   *
   * @param sensorId sensorId of the node to be reconnected to the network
   * @return true if command sent successfully, else return false
   * @throws IOException if python script does not exist, throws exception
   */
  public boolean rejoinNode(String sensorId) throws IOException{
    // Check if given path represents a file
    File scriptFile = new File(scriptPath);
    if (!scriptFile.exists() || !scriptFile.isFile()) {
      throw new FileNotFoundException(scriptPath);
    }

    // joins the given strings and separates them with the give delimiter
    String args =
        String.join(
            " ",
            "-o " + this.getOID(),
            "-a " + this.getAPI_KEY(),
            "-s " + sensorId,
            "-c rejoin_network");

    ArrayList<String> ret = executeScript(args);
    return ret.contains("True");
  }

  /**
   * Method used to create a new detection rule
   * CURRENTLY NOT FUNCTIONING
   * @param rule rule is represented as a String
   * @return true if command sent successfully, else returns false
   * @throws IOException if python script cannot be found.
   */
  public boolean newDetectionRule(String rule) throws IOException {
    return false;
  }

}
