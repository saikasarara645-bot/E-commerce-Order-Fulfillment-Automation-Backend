import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;

/** Log.java – Handles workflow logging to logs.txt and viewing order timelines */
public class Log {
 
    public static final String RESET = "\u001B[0m";
    public static final String ROSE      = "\u001B[38;5;174m"; // exit/error
   
    private DataPersistence dp;

    public Log(DataPersistence dp) {
        this.dp = dp;
    }
   
    /** Append a log entry to logs.txt */
    public void write(String orderId, String message) {
        try {
            FileWriter fw = new FileWriter(dp.path("logs.txt"), true);  // open in append mode
            fw.write("Order " + orderId + " - " + message + "\n");
            fw.close();
        } catch (Exception e) {
            // ignore logging errors to avoid disrupting main flow
        }
    }