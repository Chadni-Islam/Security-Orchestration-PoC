import static java.lang.Thread.sleep;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class OutputHandler implements Runnable {

  private String outPath;
  private String toolFrom;
  private Thread thread;
  private StaticInterpreter staticInterpreter;

  /**
   * Constructor for OutputHandler
   *
   * @param outPath path to output file to read and handle
   */
  public OutputHandler(String outPath, String toolFrom, StaticInterpreter staticInterpreter) {
    this.toolFrom = toolFrom;
    this.outPath = outPath;
    this.staticInterpreter = staticInterpreter;
    thread = new Thread(this);
    thread.start();
  }

  /**
   *  LimaCharlie action handler
   *
   * @param action A dictionary which contains information about an action to be taken on a sensor
   */
  private void toLimaCharlie(HashMap<String, String> action) {
    // Create LimaCharlie Input Constructor
    LimaCharlie lcInput;
    try {
      lcInput = new LimaCharlie();
      String sid = action.get("sid");

      // Get action to perform, extract info from fileInfo and execute
      if (action.get("action").equals("file_del")) {
        String filePath = action.get("filePath");
        lcInput.deleteFile(sid, filePath);

      } else if (action.get("action").equals("os_kill_process")) {
        String pid = action.get("process_id");
        lcInput.killProcess(sid, pid);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * Splunk reporting action handler
   *
   * @param action A Dictionary which contains information about the Splunk reporting action
   * @param actionIndex Index of current action from list of actions, required for log management
   * @param actionsLength size of total actions, required for deleting the file after last action
   */
   private void splunkReporting(HashMap<String, String> action, int actionIndex, int actionsLength) {
     // Extract common arguments from fileInfo
     String filePath = action.get("filePath");
     String type = action.get("type");
     String toolFrom = action.get("toolFrom");
     String reportName = action.get("reportName");

     // Connect to Splunk
     SplunkInputConstructor splunkInput;
     try {
       splunkInput = new SplunkInputConstructor(true);
     } catch (RuntimeException e) {
       e.printStackTrace();
       return;
     }

     try {
       // Log detection output on first action
       if (actionIndex <= 0) {
         splunkInput.logManagement(filePath, type, toolFrom, false);
       }
       // Wait for detection output to finish uploading
       sleep(2000);

       splunkInput.runReport(reportName, true);

       // Delete detection output file after last action
       if (actionIndex >= (actionsLength - 1)) {
         File logFile = new File(filePath);
         if (logFile.exists()) {
           if (!logFile.delete()) {
             System.err.println("Failed to delete detection log file: " + filePath);
           }
         }
       }
     } catch (IOException | InterruptedException e) {
       e.printStackTrace();
     }
   }

  /**
   * Splunk log management action handler
   *
   * @param action A Dictionary contains information about the Splunk log management action
   */
   private void splunkLogManagement(HashMap<String, String> action) {
     // Extract common arguments from fileInfo
     String filePath = action.get("filePath");
     String type = action.get("type");
     String toolFrom = action.get("toolFrom");

     // Connect to Splunk
     SplunkInputConstructor splunkInput;
     try {
       splunkInput = new SplunkInputConstructor(true);
     } catch (RuntimeException e) {
       e.printStackTrace();
       return;
     }

     // perform log management
     try {
       splunkInput.logManagement(filePath, type, toolFrom, true);
     } catch (IOException e) {
       e.printStackTrace();
       return;
     }
   }

  /**
   * Creates a thread for each arraylist of action, uses the toolTo and toolFrom to execute the
   * appropriate methods for each actions in the arraylist.
   */
  @Override
  public void run() {
    // wait for stream to finish writing to the file
    try {
      sleep(2000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    // Get list of actions from output
    ArrayList<HashMap<String, String>> actions = new ArrayList<>();
    try {
      actions = staticInterpreter.interpret(this.outPath, this.toolFrom);
    } catch (IOException e) {
      e.printStackTrace();
    }

    // Ensure actions has content to avoid out of bounds exceptions
    if (actions.size() <= 0) {
      return;
    }

    // Return if interpret failed
    if (actions.get(0).get("message").equals("fail")) {
      return;
    }

    // if output is from Splunk, delete after parsing
    if (actions.get(0).get("toolFrom").equals("splunk")) {
      // delete out file
      File splunkOutFile = new File(this.outPath);
      if (splunkOutFile.exists()) {
        if (!splunkOutFile.delete()) {
          System.err.println("Failed to delete Splunk Output file " + this.outPath);
        }
      }
    }

    for (int i = 0; i < actions.size(); i++) {
      HashMap<String, String> curAction = actions.get(i);
      if (curAction.get("message").equals("fail")) {
        continue;
      }

      // Execute action based on information returned from interpreter
      if ( curAction.get("toolTo").equals("splunk") ) {
        if (curAction.get("action").equals("Log Management")) {
          splunkLogManagement(curAction);
        } else if (curAction.get("action").equals("Reporting")) {
          splunkReporting(curAction, i, actions.size());
        }
      } else if (curAction.get("toolTo").equals("limacharlie")) {
        toLimaCharlie(curAction);
      }
    }
  }
}
