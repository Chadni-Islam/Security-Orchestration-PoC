import java.io.IOException;

/**
 * Interface for connecting to and sending commands to SIEM tools
 *
 * <p>Each tool has their own specific requirements for connecting and sending commands, but this
 * interface defines a generic set of capabilities for all tools.
 */
public interface SIEM {

  /**
   * Connect to SIEM tool
   *
   * @param credentialsPath path to file that includes login credentials
   * @return true if successfully connected
   * @throws RuntimeException if fails to connect
   * @throws IOException if credentials file cannot be read
   */
  boolean connect(String credentialsPath) throws RuntimeException, IOException;

  /**
   * Perform log management capability with sinkhole functionality
   *
   * @param logPath String of path to log file to upload for log management
   * @param toolFrom String of tool name where the data came from, used for labelling
   * @param dataType String of data format the log file is in
   * @param sinkhole boolean of whether to delete file after uploading; true to delete, false to
   * not
   * @return true if successfully uploaded log file
   * @throws IllegalArgumentException if toolFrom is not valid EDR tool, or dataType is not valid
   * data format
   * @throws IOException if file specified by logPath does not exist or cannot be read/deleted
   */
  boolean logManagement(String logPath, String toolFrom, String dataType, boolean sinkhole)
      throws IllegalArgumentException, IOException;

  /**
   * Perform log management capability
   *
   * @param logPath String of path to log file to upload for log management
   * @param toolFrom String of tool name where the data came from, used for labelling
   * @param dataType String of data format the log file is in
   * @return true if successfully uploaded log file
   * @throws IllegalArgumentException if toolFrom is not valid EDR tool, or dataType is not valid
   * data format
   * @throws IOException if file specified by logPath does not exist or cannot be read/deleted
   */
  boolean logManagement(String logPath, String toolFrom, String dataType)
      throws IllegalArgumentException, IOException;

  /**
   * Execute report
   *
   * @param reportName Name of report to run
   * @param triggerActions boolean of whether to trigger any actions linked to report
   * @return true if successfully performed; false otherwise
   * @throws IllegalArgumentException if no report with reportName found
   */
  boolean runReport(String reportName, boolean triggerActions) throws IllegalArgumentException;

  /**
   * Establishes file integrity monitoring
   *
   * @param nodeIdentifier String identifier for node to set up FIM on
   * @param filePath String path to monitor
   * @return true if node and filePath found and monitoring is established successfully; false
   * otherwise
   * @throws RuntimeException if node specified by nodeIdentifier cannot be found
   */
  boolean fileIntegrityMonitor(String nodeIdentifier, String filePath) throws RuntimeException;

  /**
   * Perform correlative analysis on data
   *
   * @param dependent dependent variable to perform correlation on
   * @param independents list of independent variables to correlate against
   * @return list of correlation coefficients of passed independent variables in same order as
   * independents
   * @throws IllegalArgumentException if any independent field not found in data set
   */
  double[] correlate(String dependent, String[] independents) throws IllegalArgumentException;
}
