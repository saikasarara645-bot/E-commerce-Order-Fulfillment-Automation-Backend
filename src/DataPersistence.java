import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
/** DataPersistence.java – Handles loading and saving of data from text
files */
public class DataPersistence {
 public static final String RESET = "\u001B[0m";
 // Soft pastel colors
 public static final String PINK = "\u001B[38;5;211m"; //header / highlight
 public static final String LAVENDER = "\u001B[38;5;183m"; // menu numbers
 public static final String MINT = "\u001B[38;5;156m"; //success/allowed
public static final String PEACH = "\u001B[38;5;216m";
//warnings/restricted
 public static final String ROSE = "\u001B[38;5;174m";
  //exit/error
 public static final String SOFTGRAY = "\u001B[38;5;250m";
  //normal text
   private String baseDir;
 // Data stores in memory
 public Product[] products = new Product[200];
 public int productCount = 0;
 public Order[] orders = new Order[200];
 public int orderCount = 0;
 public Admin[] admins = new Admin[50];
 public int adminCount = 0;
 public int currentAdminIndex = -1; // index of the currently logged-in admin
 private int nextOrderNumber = 1001; // next numeric ID for new orders (starting from O1001)

public DataPersistence(String baseDir) {
this.baseDir = (baseDir == null ? "" : baseDir);
 }
 public String path(String filename) {
 if (baseDir.equals("")) {
 return filename; // e.g. "admins.txt"
 }
 return baseDir + "/" + filename; // e.g. "data/admins.txt"
}
private String normalizeOrderIdForUI(String id) {
 if (id == null) return "";
 id = id.trim();
 if (id.length() == 0) return "";
 // If starts with O -> remove it
 if (id.startsWith("O") || id.startsWith("o")) id = id.substring(1);

 // Keep only digits
 String num = "";
 for (int i = 0; i < id.length(); i++) {
 char c = id.charAt(i);
 if (c >= '0' && c <= '9') num += c;
 }
 if (num.length() == 0) return "";
 // Pad to 5 digits
 while (num.length() < 5) num = "0" + num;
 return num;
}

/** Load all data from files: products, orders, admins */
 public void loadAll() throws Exception {
 loadProducts();
 loadOrders();
 loadAdmins();
 // Compute initial next order number based on loaded orders
 computeNextOrderNumber();
 }
 private void loadProducts() throws Exception {
 productCount = 0;
 BufferedReader br = null;