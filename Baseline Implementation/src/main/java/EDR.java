import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

/**
 * EDR Interface
 *
 * Describes the methods that EDR tools should implement if they are to be used apart of Orchestrator
 */
public interface EDR {

  /**
   * Method used to isolate a node from the network
   *
   * @param identifier identifier of the node to be isolated from the network
   * @return true if command sent successfully, else return false
   * @throws IOException if file for certain tools cannot be found
   */
  boolean isolateNode(String identifier) throws IOException;

  /**
   * Method used to allow node to rejoin the network
   * Tells the sensor to allow network connectivity again (after it was segregated).
   *
   * @param identifier identifier of the node to be reconnected to the network
   * @return true if command sent successfully, else return false
   * @throws IOException if file for certain tools cannot be found
   */
  boolean rejoinNode(String identifier) throws IOException;

  /**
   * Method used to create a new detection rule
   *
   * @param rule rule is represented as a String
   * @return true if command sent successfully, else returns false
   * @throws IOException if file for certain tools cannot be found
   */
  boolean newDetectionRule(String rule) throws IOException;

  /**
   * Deletes a file from node
   *
   * @param identifier used to identify the node to delete the file from
   * @param fileName name of file to be deleted
   * @return true if command successfully sent, else return false
   * @throws IOException if file for certain tools cannot be found
   */
  boolean deleteFile(String identifier, String fileName) throws IOException;

  /**
   * Method to get a file from a node
   *
   * @param identifier identifier of node from where to retrieve file
   * @param fileName name of file to be retrieved
   * @param outPutFile name of the output file where content of fileName will be saved
   * @return return the File if found, else return
   * @throws IOException if file for certain tools cannot be found
   */
  File getFile(String identifier, String fileName, String outPutFile) throws IOException;

  /**
   * Gets information about file
   *
   * @param identifier used to identify the node to get file information from
   * @param fileName name of file to get the file information from
   * @return string that has information about file, else return "fail"
   * @throws IOException if file for certain tools cannot be found
   */
  String getFileInfo(String identifier, String fileName) throws IOException;

  /**
   * Kills a process on a node
   *
   * @param identifier identifier of node that a process kill command will be sent to
   * @param pid the process id of the process to be killed
   * @return true if command sent successfully, else return false
   * @throws IOException if file for certain tools cannot be found
   */
  boolean killProcess(String identifier, String pid) throws IOException;

  /**
   * Method that gets a list of processes running from a node
   *
   * @param identifier identifier of node to get the list of processes from
   * @return the list of processes running on the given node
   * @throws IOException if file for certain tools cannot be found
   */
  List<String> getProcesses(String identifier) throws IOException;
}
