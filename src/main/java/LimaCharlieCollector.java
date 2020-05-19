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
 * LimaCharlie Collector class. Implements Runnable and is intended to be used as a thread that
 * watches a directory specified in its constructor. Creates OutputHandler for each file
 * modification detected. Thread can be stopped by either calling LimaCharlieCollector.stopRunning()
 * (shuts down after timeout period of 1000 milliseconds) or by creating/modifying/deleting a file
 * called "SDN" in the path specified in the constructor.
 */
public class LimaCharlieCollector implements Runnable {
  private String dir;
  private Path path;
  private ArrayList<OutputHandler> handlers;
  private final AtomicBoolean running = new AtomicBoolean(false);
  private Thread thread;
  private StaticInterpreter staticInterpreter;
  // Cache and start of last cache reset
  private HashSet<String> fileNamesCache = new HashSet<>();
  private long cacheStartTime;
  private final int cacheResetInterval = 60000;

  /**
   * Constructor for LimaCharlieCollector.
   *
   * @param dir path of directory to watch. Expects path to point to a valid directory
   * @throws NotDirectoryException if passed path does not exist or is not a directory
   */
  public LimaCharlieCollector(String dir, StaticInterpreter staticInterpreter)
      throws NotDirectoryException {
    // Check passed directory exists and is a directory
    File directoryToWatch = new File(dir);
    if (!directoryToWatch.exists() || !directoryToWatch.isDirectory()) {
      throw new NotDirectoryException(dir);
    }

    this.staticInterpreter = staticInterpreter;
    this.dir = dir;
    path = Paths.get(dir);
    handlers = new ArrayList<>();
    cacheStartTime = System.currentTimeMillis();
    thread = new Thread(this);
    thread.start();
  }

  /**
   * Invalidate cache of file names used to eliminate duplicates
   */
  public void resetCache() {
    cacheStartTime = System.currentTimeMillis();
    fileNamesCache.clear();
  }

  /**
   * Watches directory passed in constructor. When a file modification is detected, creates and
   * stores OutputHandler thread in ArrayList handlers, passing the path to the file to the
   * OutputHandler. Watches directory indefinitely until either LimaCharlieCollector.stopRunning()
   * is called (where it will exit after a maximum of 1000 milliseconds), Thread.interrupt() is
   * called, or shutdown signal is given in the form of a file being created/modified/deleted with
   * the name "SDN" (Shut Down Now) in the specified path. For example, if specified path is
   * "/home/user/Documents" and the file "SDN" (absolute path of "/home/user/Documents/SDN") is
   * created, modified or deleted, the LimaCharlieCollector thread will return.
   */
  @Override
  public void run() {
    running.set(true);
    try {
      WatchService watcher = FileSystems.getDefault().newWatchService();
      WatchKey key = path.register(watcher, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE);

      // Set time of last cache clear
      cacheStartTime = System.currentTimeMillis();
      while (running.get()) {
        // Poll the watcher for events every second
        key = watcher.poll(1000, TimeUnit.MILLISECONDS);

        // Invalidate timeCache every $cacheResetInterval
        if ((System.currentTimeMillis() - cacheStartTime) > cacheResetInterval) {
          cacheStartTime = System.currentTimeMillis();
          fileNamesCache.clear();
        }

        if (key != null) {
          for (WatchEvent<?> event : key.pollEvents()) {

            WatchEvent.Kind<?> kind = event.kind();
            WatchEvent<Path> ev = (WatchEvent<Path>) event;
            String fileName = ev.context().toString();
            File currentFile = new File(dir + "/" + fileName);

            // if EventType == OVERFLOW, then jump
            if (kind == OVERFLOW) {
              continue;
            }

            // Shutdown thread if SDN file created
            if (fileName.equals("SDN")) {
              File sdnFile = new File(dir + "/" + fileName);
              // Cleanup shutdown signal
              if (sdnFile.exists()) {
                sdnFile.delete();
              }
              running.set(false);
              continue;
            }

            if (kind == ENTRY_MODIFY && currentFile.exists()) {
              // If file event has been cached in the last second, skip it
              if (fileNamesCache.contains(fileName)) {
                continue;
              }
              // Add current file to cache with time of event
              fileNamesCache.add(fileName);

              OutputHandler handler =
                  new OutputHandler(dir + "/" + fileName, "limacharlie", this.staticInterpreter);
              handlers.add(handler);
            }
          }
          if (!key.reset()) {
            running.set(false);
          }
        }
      }
    } catch (IOException | InterruptedException e) {
      e.printStackTrace();
    }
  }

  /** Is used to kill a thread safely */
  public void stopRunning() {
    running.set(false);
  }

  /**
   * Returns copy of watched directory string. Copies string for safety.
   *
   * @return String of watched directory
   */
  public String getDir() {
    return dir;
  }

  /**
   * Get thread for this LimaCharlieCollector
   *
   * @return Thread for this LimaCharlieCollector
   */
  public Thread getThread() {
    return thread;
  }

  /**
   * Gets copy of current OutputHandler lists
   *
   * @return ArrayList<OutputHandler> of OutputHandler threads.
   */
  public ArrayList<OutputHandler> getHandlers() {
    ArrayList<OutputHandler> result = new ArrayList<>(handlers);
    return result;
  }
}
