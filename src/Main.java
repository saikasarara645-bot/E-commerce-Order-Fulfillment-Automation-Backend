import java.io.BufferedReader;
import java.io.InputStreamReader;

/** Main.java – Entry point that initializes data and starts the CLI application */
public class Main {
    public static final String RESET = "\u001B[0m";
    public static final String MINT      = "\u001B[38;5;156m"; // success/allowed
    public static final String ROSE      = "\u001B[38;5;174m"; // exit/error
  
    public static void main(String[] args) {
        try {
            // Prepare console reader for CLI input
            BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
            // Initialize data persistence (loads data from text files)
            DataPersistence dp = new DataPersistence("data");  // base directory "" = current directory
            dp.loadAll();
            // Initialize logging system
            Log log = new Log(dp);
            // Create Workflow orchestrator
            Workflow wf = new Workflow(dp, log);
            // Secure Admin Login
            if (!wf.adminLogin(console)) {
                System.out.print(ROSE+"Exiting...\n"+RESET);
                return;
            }
            // Show Admin Dashboard menu (interactive loop)
            wf.adminDashboard(console);
            // On exit, save all data back to files
            dp.saveAll();
            System.out.print(MINT+"Saved. Bye.\n"+RESET);
        } catch (Exception e) {
            System.out.print(ROSE+"Fatal Error: " + e.getMessage() + "\n"+RESET);
        }
    }
}