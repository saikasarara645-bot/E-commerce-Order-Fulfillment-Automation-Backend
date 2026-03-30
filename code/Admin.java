import java.io.BufferedReader;
import java.security.MessageDigest;

/** Admin.java – Admin model and authentication logic (password hashing for secure login) */
public class Admin {
    public static final String ANSI_Yellow ="\u001B[38;5;220m";
    public static final String RESET = "\u001B[0m";
    public static final String BOLD  = "\u001B[1m";
    public static final String PINK      = "\u001B[38;5;211m"; // header / highlight
    public static final String MINT      = "\u001B[38;5;156m"; // success/allowed
    public static final String ROSE      = "\u001B[38;5;174m"; // exit/error
    public static final String SOFTGRAY  = "\u001B[38;5;250m"; // normal text
    public String username;
    public String passHash;  // Hashed password
    public Role role;  // User role (e.g., ADMIN, MANAGER, SUPPORT)

    // Constructor to initialize the Admin object
    public Admin(String username, String passHash, Role role) {
        this.username = username;
        this.passHash = passHash;
        this.role = role;
    }
    public Admin(String username, String passHash) {
        this.username = username;
        this.passHash = passHash;
        this.role = Role.ADMIN; // default
    }
      public boolean hasPermission(Role required) {
        return this.role == required;
    }
     private static void printLine() {
    System.out.println(SOFTGRAY + "────────────────────────────────────────" + RESET);
    }

    private static void printTitle(String text) {
    printLine();
    System.out.println(PINK + BOLD + text + RESET);
    printLine();
    }
    public static boolean authenticate(DataPersistence dp, BufferedReader console) throws Exception {
    printTitle("Admin Login");
    int attempts = 0;

    while (attempts < 3) {
        System.out.print(SOFTGRAY+"Username: "+RESET);
        String u = console.readLine();
        if (u == null) u = "";
        u = u.trim();

        String p = readPasswordWithAsterisk(console, SOFTGRAY + "Password: " + RESET);
        boolean authenticated = false;
        int foundIndex = -1;

        for (int i = 0; i < dp.adminCount; i++) {
            Admin adm = dp.admins[i];
            if (adm != null && adm.username.equals(u) && adm.passHash.equals(hashPassword(p))) {
                foundIndex = i;
                authenticated = true;
                break;
            }
        }

          if (authenticated) {
            dp.currentAdminIndex = foundIndex;
            dp.appendLoginAudit("LOGIN_SUCCESS", u);
            Admin admin = dp.admins[foundIndex];
            System.out.println(MINT+"Login successful. Role: " + admin.role +RESET);
            return true;

        } else {
            dp.appendLoginAudit("LOGIN_FAIL", u);
            System.out.print(ROSE+"Invalid credentials. "+RESET);
            attempts++;
            if (attempts < 3) {
                System.out.print(ANSI_Yellow+"Try again.\n"+RESET);
            }
        }
    }

    System.out.print(ROSE+"\nToo many failed attempts.\n"+RESET);
    return false;
    }


    /** Compute SHA-256 hash of a plaintext password and return hex string */
    public static String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");  // hashing algorithm
            byte[] hashBytes = md.digest(password.getBytes());        // compute hash
            // Convert hash bytes to hexadecimal string
            String hexDigits = "0123456789abcdef";
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                int val = b & 0xFF;
                sb.append(hexDigits.charAt(val >>> 4));
                sb.append(hexDigits.charAt(val & 0x0F));
            }
            return sb.toString();
        } catch (Exception e) {
            // In case of error, fall back to plain password (should not happen in normal use)
            return password;
        }
    }
   private static class PasswordMaskingThread extends Thread {
    private volatile boolean running = true;
    private final String prompt;

    public PasswordMaskingThread(String prompt) {
        this.prompt = prompt;
    }

    public void stopMasking() {
        running = false;
    }

    @Override
    public void run() {
        System.out.print(prompt);
        while (running) {
            System.out.print("\010*");
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                break;
            }
        }
    }
}

public static String readPasswordWithAsterisk(BufferedReader console, String prompt) throws Exception {
    PasswordMaskingThread maskingThread = new PasswordMaskingThread(prompt);
    maskingThread.start();

    String password = console.readLine();

    maskingThread.stopMasking();
    System.out.println();

    if (password == null) return "";
    return password.trim();
} 
}
