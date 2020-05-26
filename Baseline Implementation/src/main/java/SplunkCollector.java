import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * SplunkCollector class. Implements Runnable and is intended to be used as a thread that watches a
 * directory specified in its constructor. Creates OutputHandler for each file modification
 * detected. Thread can be stopped by either calling SplunkCollector.stopRunning() (shuts down after
 * timeout period of 1000 milliseconds) or by creating/modifying/deleting a file called "SDN" in the
 * path specified in the constructor.
 */
public class SplunkCollector implements Runnable {

  private String dir;
  private ArrayList<OutputHandler> handlers;
  private final AtomicBoolean running = new AtomicBoolean(false);
  private Thread thread;
  private StaticInterpreter staticInterpreter;
  /**
   * Constructor for SplunkCollector.
   *
   * @param path path of directory to watch. Expects path to point to a valid directory
   * @throws NotDirectoryException if passed path does not exist or is not a directory
   */
  public SplunkCollector(String path, StaticInterpreter staticInterpreter)
      throws NotDirectoryException {
    // Check passed directory exists and is a directory
    File directoryToWatch = new File(path);
    if (!directoryToWatch.exists() || !directoryToWatch.isDirectory()) {
      throw new NotDirectoryException(path);
    }

    this.staticInterpreter = staticInterpreter;
    dir = path;
    handlers = new ArrayList<OutputHandler>();
    thread = new Thread(this);
    thread.start();
  }

  /**
   * Returns copy of watched directory string. Copies string for safety.
   *
   * @return String of watched directory
   */
  public String getDir() {
    String ret = dir;
    return ret;
  }

  /**
   * Gets copy of current OutputHandler list
   *
   * @return ArrayList<OutputHandler> of OutputHandler threads.
   */
  public ArrayList<OutputHandler> getHandlers() {
    ArrayList<OutputHandler> ret = new ArrayList<OutputHandler>(handlers);
    return ret;
  }

  /**
   * Get thread for this SplunkCollector
   *
   * @return Thread for this SplunkCollector
   */
  public Thread getThread() {
    return thread;
  }

  /**
   * Safely stops execution of thread using AtomicBoolean running. Exits after timeout of 1000
   * milliseconds. To interrupt without event, Thread.interrupt() needs to be called.
   */
  public void stopRunning() {
    running.set(false);
  }

  /**
   * Watches directory passed in constructor. When a file modification is detected, creates and
   * stores OutputHandler thread in ArrayList handlers, passing the path to the file to the
   * OutputHandler. Watches directory indefinitely until either SplunkCollector.stopRunning() is
   * called (where it will exit after a maximum of 1000 milliseconds), Thread.interrupt() is called,
   * or shutdown signal is given in the form of a file being created/modified/deleted with the name
   * "SDN" (Shut Down Now) in the specified path. For example, if specified path is
   * "/home/user/Documents" and the file "SDN" (absolute path of "/home/user/Documents/SDN") is
   * created, modified or deleted, the SplunkCollector thread will return.
   */
  @Override
  public void run() {
    running.set(true);
    WatchService watcher;
    try {
      watcher = FileSystems.getDefault().newWatchService();

      Path watched = Paths.get(dir);
      WatchKey key = watched.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);

      HashSet<String> fileNames = new HashSet<>();
      long startTime = System.currentTimeMillis();

      // Watch directory until stopRunning() is called
      while (running.get()) {
        // Poll the watcher for events every second
        key = watcher.poll(1000, TimeUnit.MILLISECONDS);

        // Invalidate timeCache
        if ((System.currentTimeMillis() - startTime) > 1000) {
          startTime = System.currentTimeMillis();
          fileNames.clear();
        }

        if (key != null) {
          for (WatchEvent<?> event : key.pollEvents()) {
            WatchEvent.Kind<?> kind = event.kind();
            WatchEvent<Path> ev = (WatchEvent<Path>) event;
            String filename = ev.context().toString();

            if (kind == OVERFLOW) {
              continue;
            }

            // Shutdown if shutdown signal received
            if (filename.equals("SDN")) {
              File sdnFile = new File(dir + "/" + filename);
              // Cleanup shutdown signal
              if (sdnFile.exists()) {
                sdnFile.delete();
              }
              return;
            }

            File curFile = new File(dir + "/" + filename.toString());

            // Create and store OutputHandler when modification detected
            if (kind == ENTRY_MODIFY && curFile.exists()) {

              // If file event has been cached in the last second, skip it
              if (fileNames.contains(filename)) {
                continue;
              }
              // Add current file to cache with time of event
              fileNames.add(filename);

              OutputHandler handler =
                  new OutputHandler(dir + "/" + filename, "splunk", this.staticInterpreter);
              handlers.add(handler);
            }
          }

          // Ensure key has reset
          boolean valid = key.reset();
          if (!valid) {
            break;
          }
        }
      }
    } catch (IOException | InterruptedException e) {
      e.printStackTrace();
    }
  }
}
