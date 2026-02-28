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

 try {
 br = new BufferedReader(new
FileReader(path("products.txt")));
 String line;
 while ((line = br.readLine()) != null) {
 line = line.trim();
 if (line.length() == 0) continue;
 // Format: ProductID|Category|Brand|Name|Price|Stock
 String[] parts = line.split("\\|");
 if (parts.length < 6) continue;
 String pid = parts[0].trim();
 String category = parts[1].trim();
 String brand = parts[2].trim();
 String name = parts[3].trim();
 String priceStr = parts[4].trim().replace(",", "");
 String stockStr = parts[5].trim();
 int price = toInt(priceStr);
 int stock = toInt(stockStr);
 products[productCount++] = new Product(pid, category,
brand, name, price, stock);
 }
} catch (Exception e) {
 // If file not found or format error, skip (will use
defaults if any)
 } finally {
 if (br != null) br.close();
 }
 }
 private String normalizeOrderId(String raw) {
 if (raw == null) return "";
 raw = raw.trim().toUpperCase();

 // remove leading 'O' if present
 if (raw.startsWith("O")) raw = raw.substring(1);
 // keep only digits
 String digits = "";
 for (int i = 0; i < raw.length(); i++) {
 char c = raw.charAt(i);
 if (c >= '0' && c <= '9') digits += c;
 }
 int num = toInt(digits);
 if (num <= 0) return "";
 // pad to 4 digits: 1019 -> 1019, 19 -> 0019
 String s = "" + num;
 while (s.length() < 4) s = "0" + s;
 return "O" + s;
}
private void loadOrders() throws Exception {
 orderCount = 0;
 BufferedReader br = null;
 try {
 br = new BufferedReader(new FileReader(path("orders.txt")));
 String line;
 while ((line = br.readLine()) != null) {
 line = line.trim();
 if (line.length() == 0) continue;
 // Format:
 //OrderID|Date|Address|PaymentMode|Status|Total|ItemList|CancelReason|TrackingId(optional)
 String[] parts = line.split("\\|");
 if (parts.length < 5) continue;
 Order o = new Order();

 //  normalize to STORAGE format (keep O + 4 digits)
 o.orderId = normalizeOrderId(parts[0].trim());
 o.date = (parts.length > 1 ? parts[1].trim() : "");
 o.address = (parts.length > 2 ? parts[2].trim() : "");
 o.paymentMode = (parts.length > 3 ? parts[3].trim() : "");
 o.status = (parts.length > 4 ? parts[4].trim() :
"PENDING");
 // Items list (index 6)
 String itemsPart = "";
 if (parts.length > 6) {
 itemsPart = parts[6].trim();
 parseItemsIntoOrder(o, itemsPart);
 }
 // Total amount (index 5)
 if (parts.length > 5) {
 o.totalAmount = toInt(parts[5].trim());
 } else {
 o.totalAmount = 0;
 }
 // FIX: if total is 0 but items exist → recalculate from products
 if (o.totalAmount <= 0 && o.itemCount > 0) {
 int total = 0;
 for (int i = 0; i < o.itemCount; i++) {
 Item it = o.items[i];
 if (it == null) continue;
 Product p = findProductById(it.productId);
 if (p != null) {
 total += p.price * it.quantity;
 }
 }
 o.totalAmount = total;
 }


 // Cancel reason (index 7)
 if (parts.length > 7) {
 o.cancelReason = parts[7].trim();
 }
 // Tracking ID (index 8)
 if (parts.length > 8) {
 o.trackingId = parts[8].trim();
 }
 orders[orderCount++] = o;
 }
 } catch (Exception e) {
 // If orders.txt doesn't exist, it's fine
 } finally {
 if (br != null) br.close();
 }
}

private void loadAdmins() throws Exception {
 adminCount = 0;
 BufferedReader br = null;
 try {
 br = new BufferedReader(new FileReader(path("admins.txt")));
 String line;
 while ((line = br.readLine()) != null) {
 line = line.trim();
 if (line.length() == 0) continue;
 // username|passHash|role
 String[] parts = line.split("\\|");
 if (parts.length < 3) continue;
 String username = parts[0].trim();
 String passHash = parts[1].trim();
 //  use YOUR enum Role (NOT javax.management.relation.Role)
 Role role;
 try {
 role = Role.valueOf(parts[2].trim().toUpperCase());
 } catch (Exception ex) {
 System.out.println(ROSE+"Invalid role for user " +
username + ". Using ADMIN by default."+RESET);
 role = Role.ADMIN; // fallback instead of skipping
 }
 Admin a = new Admin(username, passHash, role);
 admins[adminCount++] = a;
 }
 } catch (Exception e) {
 // It's okay if file doesn't exist yet.