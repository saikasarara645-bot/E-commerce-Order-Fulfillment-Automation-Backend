import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;

import javax.management.relation.Role;
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
 // If file not found or format error, skip (will use defaults if any)
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
//OrderID|Date|Address|PaymentMode|Status|Total|ItemList|CancelReason|TrackingId|IsSimulationOrder|SimulationItemName|SimulationItemPrice
String[] parts = line.split("\\|");
if (parts.length < 5) continue;
Order o = new Order();

// Basic fields
o.orderId = normalizeOrderId(parts[0].trim());
o.date = (parts.length > 1 ? parts[1].trim() : "");
o.address = (parts.length > 2 ? parts[2].trim() : "");
o.paymentMode = (parts.length > 3 ? parts[3].trim() : "");
o.status = (parts.length > 4 ? parts[4].trim() : "PENDING");

// Total amount (index 5)
if (parts.length > 5) {
o.totalAmount = toInt(parts[5].trim());
} else {
o.totalAmount = 0;
}

// Items list (index 6)
String itemsPart = "";
if (parts.length > 6) {
itemsPart = parts[6].trim();
parseItemsIntoOrder(o, itemsPart);
}
// Cancel reason (index 7)
if (parts.length > 7) {
o.cancelReason = parts[7].trim();
} else {
o.cancelReason = "";
}

// Tracking ID (index 8)
if (parts.length > 8) {
o.trackingId = parts[8].trim();
} else {
o.trackingId = "";
}
// Simulation flag (index 9)
if (parts.length > 9) {
o.isSimulationOrder = parts[9].trim().equalsIgnoreCase("true");
} else {
o.isSimulationOrder = false;
}
// Simulation item name (index 10)
if (parts.length > 10) {
o.simulationItemName = parts[10].trim();
} else {
o.simulationItemName = "";
}
// Simulation item price (index 11)
if (parts.length > 11) {
o.simulationItemPrice = toInt(parts[11].trim());
} else {
o.simulationItemPrice = 0;
}

// Recalculate total if missing/zero
if (o.totalAmount <= 0 && o.itemCount > 0) {
int total = 0;

if (o.isSimulationOrder) {
for (int i = 0; i < o.itemCount; i++) {
Item it = o.items[i];
if (it == null) continue;
total += o.simulationItemPrice * it.quantity;
}
} else {
for (int i = 0; i < o.itemCount; i++) {
Item it = o.items[i];
if (it == null) continue;

Product p = findProductById(it.productId);
if (p != null) {
total += p.price * it.quantity;
}
}
}
o.totalAmount = total;
}
orders[orderCount++] = o;
}

} catch (Exception e) {
// If orders.txt doesn't exist, it's fine

} finally {
if (br != null) br.close();
}
}


//.....................................

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

 } finally {
 if (br != null) br.close();
 }
 // Debug print AFTER loading
 for (int i = 0; i < adminCount; i++) {
 if (admins[i] != null) {
 System.out.print(LAVENDER+"Loaded admin: " +
admins[i].username + " (" + admins[i].role + ")\n"+RESET);
 }
 }
 // Default admin if none found
 if (adminCount == 0) {
 String defaultUser = "admin";
 String defaultPassHash = Admin.hashPassword("admin123");
 admins[adminCount++] = new Admin(defaultUser, defaultPassHash,
Role.ADMIN);
 }
}
 /** Save all data back to text files */
 public void saveAll() throws Exception {
 saveProducts();
 saveOrders();
 saveAdmins();
 }
 public void saveProducts() throws Exception {
 FileWriter fw = new FileWriter(path("products.txt"), false);
// overwrite file
 for (int i = 0; i < productCount; i++) {
 Product p = products[i];
 if (p == null) continue;
 // Format: ProductID|Category|Brand|Name|Price|Stock
 fw.write(p.productId + "|" + p.category + "|" + p.brand +
"|" + p.name + "|" + p.price + "|" + p.stock + "\n");
 }
 fw.close();
 }
public void addAdmin(Admin newAdmin) {
 if (adminCount < admins.length) {
 admins[adminCount++] = newAdmin; // Add new admin to the list
 } else {
 System.out.println(ROSE+"Unable to add new admin. Admin list is full."+RESET);
 }
}

//------------------------

public void saveOrders() throws Exception {
// Overwrite orders.txt
FileWriter fw = new FileWriter(path("orders.txt"), false);

for (int i = 0; i < orderCount; i++) {
Order o = orders[i];
if (o == null) continue;

// Build item list: ProductIDxQty,ProductIDxQty,...
StringBuilder itemList = new StringBuilder();


for (int j = 0; j < o.itemCount; j++) {
Item item = o.items[j];
if (item == null) continue;
itemList.append(item.productId).append("x").append(item.quantity);


if (j < o.itemCount - 1) {
itemList.append(",");
}
}
// Safe values
String orderIdPart = (o.orderId == null) ? "" : o.orderId;

String datePart = (o.date == null) ? "" : o.date;
String addressPart = (o.address == null) ? "" : o.address;
String paymentPart = (o.paymentMode == null) ? "" : o.paymentMode;
String statusPart = (o.status == null) ? "" : o.status;
String cancelReasonPart = (o.cancelReason == null) ? "" : o.cancelReason;




//........................................
 private void saveAdmins() throws Exception {
 FileWriter fw = new FileWriter(path("admins.txt"), false);
 for (int i = 0; i < adminCount; i++) {
 Admin a = admins[i];
 if (a == null) continue;
 fw.write(a.username + "|" + a.passHash +"|"+a.role.name()+
"\n");
 }
 fw.close();
 }
 //Convert a string to integer without using library parse methods

 public static int toInt(String s) {
 if (s == null) return 0;
 s = s.trim();
 if (s.length() == 0) return 0;
 boolean neg = false;
 if (s.charAt(0) == '-') {
 neg = true;
 s = s.substring(1);
 }
 int value = 0;
 for (int i = 0; i < s.length(); i++) {
 char c = s.charAt(i);
 if (c >= '0' && c <= '9') {
 value = value * 10 + (c - '0');
 } else {
 // ignore any non-digit characters

 }
 }
 return neg ? -value : value;
 }
public int computeOrderTotal(Order o) {
 if (o == null) return 0;
 int total = 0;
 for (int i = 0; i < o.itemCount; i++) {
 Item it = o.items[i];
 if (it == null) continue;
 Product p = findProductById(it.productId);
 if (p != null) {
 total += p.price * it.quantity;
 }
 }
 return total;
}
 /** Determine nextOrderNumber by finding the max numeric part of loaded order IDs */
 private void computeNextOrderNumber() {
 int maxNum = 0;
 for (int i = 0; i < orderCount; i++) {
 Order o = orders[i];
 if (o == null || o.orderId == null) continue;
 String id = o.orderId;
 // extract numeric part from ID (works for O1019, 01019, 1019)
 String numPart = "";
 for (int k = 0; k < id.length(); k++) {
 char ch = id.charAt(k);
 if (ch >= '0' && ch <= '9') {
 numPart += ch;
 }
 }
 int num = toInt(numPart);
 if (num > maxNum) {
 maxNum = num;
 }
 }
 // set next order number after loop
 nextOrderNumber = maxNum + 1;
}
 // Generate a new unique Order ID (e.g., "O1001", "O1002", ...) 
 public String generateOrderId() {
 int n = nextOrderNumber;
 nextOrderNumber++;
 String s = "" + n;
 while (s.length() < 5) s = "0" + s; // 1001 -> 01001
 return s;
}
/** Parse an "ItemList" string into Item objects added to Order
* Supported formats:
* - "P01x2,P03x1"
* - "P01:2,P03:1"
*/
public void parseItemsIntoOrder(Order o, String itemsPart) {
 if (o == null || itemsPart == null) return;
 String part = itemsPart.trim();
 if (part.length() == 0) return;
 String[] itemTokens = part.split(",");
 for (int i = 0; i < itemTokens.length; i++) {
 String token = itemTokens[i].trim();
 if (token.length() == 0) continue;
 String pid = "";
 int qty = 0;
 // Support "PIDxQTY"
 if (token.contains("x")) {
 String[] kv = token.split("x");
 if (kv.length == 2) {
 pid = kv[0].trim();
 qty = toInt(kv[1].trim());
 }
 }
 // Support "PID:QTY"
 else if (token.contains(":")) {
 String[] kv = token.split(":");
 if (kv.length == 2) {
 pid = kv[0].trim();
 qty = toInt(kv[1].trim());
 }
 }
 if (pid.length() > 0 && qty > 0) {
 Item item = new Item(pid, qty);
 o.addItem(item);
 }
 }
}

//Find a Product by its ID (case-sensitive match). Returns null if not found. */

 public Product findProductById(String productId) {
 if (productId == null) return null;
 String key = productId.trim();
 for (int i = 0; i < productCount; i++) {
 Product p = products[i];
 if (p != null && p.productId != null &&
p.productId.trim().equalsIgnoreCase(key)) {
 return p;
 }
 }
 return null;
}
public void loadTestDataFromFile(String filename) {
 int productLoaded = 0, adminLoaded = 0, orderLoaded = 0;


 String mode = "";
 try (BufferedReader br = new BufferedReader(new
FileReader(path(filename)))) {
 String line;
 while ((line = br.readLine()) != null) {
 line = line.trim();
 if (line.equals("")) continue;
 if (line.startsWith("#")) {
 mode = line.trim().toUpperCase();
 continue;
 }
 String[] parts = line.split("\\|");
 switch (mode) {
 case "#PRODUCTS":
 if (parts.length == 6) {
 String pid = parts[0].trim();
 String cat = parts[1].trim();
 String brand = parts[2].trim();
 String name = parts[3].trim();
 int price = toInt(parts[4].trim());
 int stock = toInt(parts[5].trim());
 products[productCount++] = new Product(pid,
cat, brand, name, price, stock);
 productLoaded++;
 }
 break;
 case "#ADMINS":
 if (parts.length == 2) {
 String username = parts[0].trim();
 String passHash = parts[1].trim();
 admins[adminCount++] = new Admin(username,
passHash);
 adminLoaded++;
 }
 break;
 case "#ORDERS":
 if (parts.length >= 7) {
 Order o = new Order();
 o.orderId = parts[0].trim();
 o.date = parts[1].trim();
 o.address = parts[2].trim();
 o.paymentMode = parts[3].trim();
 o.status = parts[4].trim();
 parseItemsIntoOrder(o, parts[5].trim());
 o.totalAmount = toInt(parts[6].trim());
 if (parts.length >= 8) o.cancelReason =
parts[7].trim();
 orders[orderCount++] = o;
 orderLoaded++;
 }
 break;
 }
 }
 System.out.println(PINK+"Test data loaded from: " +
filename+RESET);
 System.out.println(LAVENDER+"- Products added: " +
productLoaded+RESET);
 System.out.println(LAVENDER+"- Orders added: " +
orderLoaded+RESET);
 System.out.println(LAVENDER+"- Admins added: " +
adminLoaded+RESET);
 } catch (Exception e) {
 System.out.println(ROSE+" Failed to load test data from " +
filename + ": " + e.getMessage()+RESET);
 }
}

public void appendLoginAudit(String action, String username) {
 FileWriter fw = null;
 try {
 fw = new FileWriter(path("login_audit.txt"), true);
 fw.write(action + "|" + username + "|" +
currentDateTimeString() + "\n");
 } catch (Exception e) {
 // ignore to avoid crash
 } finally {
 try { if (fw != null) fw.close(); } catch (Exception ex) {}
 }
}

public String currentDateTimeString() {
 java.time.LocalDateTime dt = java.time.LocalDateTime.now();
 return dt.toString(); // 2026-02-07T12:30:00
}
// Auto-generate Product ID based on category using your existing pattern (M/L/H/A/P)
public String generateProductIdByCategory(String category) {
 // Map category -> prefix
 char prefix = 'X';
 if (category != null) {
 String c = category.trim().toUpperCase();
 if (c.equals("SMARTPHONE")) prefix = 'M';
 else if (c.equals("LAPTOP")) prefix = 'L';
 else if (c.equals("HOME APPLIANCE")) prefix = 'H';
 else if (c.equals("ACCESSORIES")) prefix = 'A';
 else if (c.equals("POWER BANK")) prefix = 'P';
 }

// If unknown category, default prefix
 if (prefix == 'X') prefix = 'G'; // General

 // Find current max number for that prefix
 int max = 0;
 for (int i = 0; i < productCount; i++) {
 Product p = products[i];
 if (p == null || p.productId == null) continue;

 String pid = p.productId.trim().toUpperCase();
 if (pid.length() < 2) continue;
 if (pid.charAt(0) != prefix) continue;


 // extract digits
 String digits = "";
 for (int k = 0; k < pid.length(); k++) {
 char ch = pid.charAt(k);
 if (ch >= '0' && ch <= '9') digits += ch;
 }

 int n = toInt(digits);
 if (n > max) max = n;
  }

  // Next number
 int next = max + 1;
 // keep your style: M101, L201, H301, A401, P501
 return "" + prefix + next;
}
}
