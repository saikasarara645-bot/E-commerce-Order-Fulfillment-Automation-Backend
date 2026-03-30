import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;

/** DataPersistence.java – Handles loading and saving of data from JSON files */
public class DataPersistence {
 public static final String RESET = "\u001B[0m";
 public static final String PINK = "\u001B[38;5;211m"; 
 public static final String LAVENDER = "\u001B[38;5;183m";
 public static final String MINT = "\u001B[38;5;156m"; 
public static final String PEACH = "\u001B[38;5;216m";
 public static final String ROSE = "\u001B[38;5;174m";
 public static final String SOFTGRAY = "\u001B[38;5;250m";
   private String baseDir;
 public Product[] products = new Product[200];
 public int productCount = 0;
 public Order[] orders = new Order[200];
 public int orderCount = 0;
 public Admin[] admins = new Admin[50];
 public int adminCount = 0;
 public int currentAdminIndex = -1; 
 private int nextOrderNumber = 1001; 
public DataPersistence(String baseDir) {
this.baseDir = (baseDir == null ? "" : baseDir);
 }
 public String path(String filename) {
 if (baseDir.equals("")) {
 return filename; 
 }
 return baseDir + "/" + filename; 
}
private String normalizeOrderId(String raw) {
    if (raw == null) return "";

    raw = raw.trim().toUpperCase();

    // Remove leading O if present
    if (raw.startsWith("O")) {
        raw = raw.substring(1);
    }

    // Keep only digits
    String digits = "";
    for (int i = 0; i < raw.length(); i++) {
        char c = raw.charAt(i);
        if (c >= '0' && c <= '9') {
            digits += c;
        }
    }

    if (digits.equals("")) return "";

    int num = toInt(digits);
    if (num <= 0) return "";

    String s = String.valueOf(num);
    while (s.length() < 5) {
        s = "0" + s;
    }

    return "O" + s;
}
private String twoDigits(int n) {
    if (n < 10) {
        return "0" + n;
    }
    return "" + n;
}

private boolean isLeapYear(int year) {
    if (year % 400 == 0) return true;
    if (year % 100 == 0) return false;
    return year % 4 == 0;
}

private int[] getCurrentDateTimeParts() {
    long millis = System.currentTimeMillis();
    long totalSeconds = millis / 1000L;
    long totalDays = totalSeconds / 86400L;

    int secondsInDay = (int) (totalSeconds % 86400L);
    if (secondsInDay < 0) {
        secondsInDay += 86400;
        totalDays--;
    }

    int hour = secondsInDay / 3600;
    int minute = (secondsInDay % 3600) / 60;
    int second = secondsInDay % 60;

    int year = 1970;
    while (true) {
        int daysInYear = isLeapYear(year) ? 366 : 365;
        if (totalDays >= daysInYear) {
            totalDays -= daysInYear;
            year++;
        } else {
            break;
        }
    }

    int[] monthDays = {31,28,31,30,31,30,31,31,30,31,30,31};
    if (isLeapYear(year)) monthDays[1] = 29;

    int month = 1;
    while (month <= 12) {
        if (totalDays >= monthDays[month - 1]) {
            totalDays -= monthDays[month - 1];
            month++;
        } else {
            break;
        }
    }

    int day = (int) totalDays + 1;
    return new int[] { year, month, day, hour, minute, second };
}

private String readWholeFile(String filename) throws Exception {
    BufferedReader br = null;
    StringBuilder sb = new StringBuilder();

    try {
        br = new BufferedReader(new FileReader(path(filename)));
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line).append("\n");
        }
    } catch (Exception e) {
        return "";
    } finally {
        if (br != null) br.close();
    }

    return sb.toString().trim();
}

private void writeWholeFile(String filename, String content) throws Exception {
    FileWriter fw = new FileWriter(path(filename), false);
    fw.write(content);
    fw.close();
}

private String jsonEscape(String s) {
    if (s == null) return "";
    StringBuilder out = new StringBuilder();

    for (int i = 0; i < s.length(); i++) {
        char c = s.charAt(i);

        if (c == '\\') out.append("\\\\");
        else if (c == '"') out.append("\\\"");
        else if (c == '\n') out.append("\\n");
        else if (c == '\r') out.append("\\r");
        else if (c == '\t') out.append("\\t");
        else out.append(c);
    }

    return out.toString();
}

private String q(String s) {
    return "\"" + jsonEscape(s) + "\"";
}

private int skipSpaces(String s, int i) {
    while (i < s.length()) {
        char c = s.charAt(i);
        if (c == ' ' || c == '\n' || c == '\r' || c == '\t') i++;
        else break;
    }
    return i;
}

private int findValueStart(String obj, String key) {
    String token = "\"" + key + "\"";
    int k = obj.indexOf(token);
    if (k < 0) return -1;

    int colon = obj.indexOf(":", k + token.length());
    if (colon < 0) return -1;

    return skipSpaces(obj, colon + 1);
}

private String extractJsonString(String obj, String key) {
    int i = findValueStart(obj, key);
    if (i < 0 || i >= obj.length()) return "";
    if (obj.charAt(i) != '"') return "";

    i++; // skip opening quote
    StringBuilder sb = new StringBuilder();
    boolean escape = false;

    while (i < obj.length()) {
        char c = obj.charAt(i);

        if (escape) {
            if (c == 'n') sb.append('\n');
            else if (c == 'r') sb.append('\r');
            else if (c == 't') sb.append('\t');
            else sb.append(c);
            escape = false;
        } else {
            if (c == '\\') {
                escape = true;
            } else if (c == '"') {
                break;
            } else {
                sb.append(c);
            }
        }
        i++;
    }

    return sb.toString();
}

private int extractJsonInt(String obj, String key) {
    int i = findValueStart(obj, key);
    if (i < 0 || i >= obj.length()) return 0;

    String num = "";
    if (obj.charAt(i) == '-') {
        num += "-";
        i++;
    }

    while (i < obj.length()) {
        char c = obj.charAt(i);
        if (c >= '0' && c <= '9') {
            num += c;
            i++;
        } else {
            break;
        }
    }

    return toInt(num);
}

private boolean extractJsonBoolean(String obj, String key) {
    int i = findValueStart(obj, key);
    if (i < 0 || i >= obj.length()) return false;

    if (i + 4 <= obj.length() && obj.substring(i, i + 4).equals("true")) {
        return true;
    }

    return false;
}

private String extractJsonArray(String obj, String key) {
    int i = findValueStart(obj, key);
    if (i < 0 || i >= obj.length()) return "[]";
    if (obj.charAt(i) != '[') return "[]";

    int start = i;
    int depth = 0;
    boolean inString = false;
    boolean escape = false;

    while (i < obj.length()) {
        char c = obj.charAt(i);

        if (inString) {
            if (escape) {
                escape = false;
            } else if (c == '\\') {
                escape = true;
            } else if (c == '"') {
                inString = false;
            }
        } else {
            if (c == '"') {
                inString = true;
            } else if (c == '[') {
                depth++;
            } else if (c == ']') {
                depth--;
                if (depth == 0) {
                    return obj.substring(start, i + 1);
                }
            }
        }

        i++;
    }

    return "[]";
}

private String[] splitTopLevelObjects(String jsonArrayText) {
    String text = jsonArrayText == null ? "" : jsonArrayText.trim();

    if (text.startsWith("[")) text = text.substring(1);
    if (text.endsWith("]")) text = text.substring(0, text.length() - 1);

    String[] temp = new String[500];
    int count = 0;

    int start = -1;
    int depth = 0;
    boolean inString = false;
    boolean escape = false;

    for (int i = 0; i < text.length(); i++) {
        char c = text.charAt(i);

        if (inString) {
            if (escape) {
                escape = false;
            } else if (c == '\\') {
                escape = true;
            } else if (c == '"') {
                inString = false;
            }
        } else {
            if (c == '"') {
                inString = true;
            } else if (c == '{') {
                if (depth == 0) start = i;
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0 && start != -1) {
                    temp[count++] = text.substring(start, i + 1);
                    start = -1;
                }
            }
        }
    }

    String[] result = new String[count];
    for (int i = 0; i < count; i++) {
        result[i] = temp[i];
    }

    return result;
}
/** Load all data from files: products, orders, admins */
public void loadAll() throws Exception {
    loadProducts();
    loadOrders();
    loadAdmins();
    refreshNextOrderNumber();
}
 private void loadProducts() throws Exception {
    productCount = 0;

    String json = readWholeFile("products.json");
    if (json.equals("")) return;

    String[] objects = splitTopLevelObjects(json);

    for (int i = 0; i < objects.length; i++) {
        String obj = objects[i];

        String pid = extractJsonString(obj, "productId");
        String category = extractJsonString(obj, "category");
        String brand = extractJsonString(obj, "brand");
        String name = extractJsonString(obj, "name");
        int price = extractJsonInt(obj, "price");
        int stock = extractJsonInt(obj, "stock");

        products[productCount++] = new Product(pid, category, brand, name, price, stock);
    }
}
public void addAdmin(Admin newAdmin) {
     if (adminCount < admins.length) { 
        admins[adminCount++] = newAdmin; // Add new admin to the list 
     } else {
     System.out.println(ROSE+"Unable to add new admin. Admin list is full."+RESET);
     }
     }
private void loadOrders() throws Exception {
    orderCount = 0;

    String json = readWholeFile("orders.json");
    if (json.equals("")) return;

    String[] objects = splitTopLevelObjects(json);

    for (int x = 0; x < objects.length; x++) {
        String obj = objects[x];

        Order o = new Order();

        o.orderId = normalizeOrderId(extractJsonString(obj, "orderId"));
        o.date = extractJsonString(obj, "date");
        o.address = extractJsonString(obj, "address");
        o.paymentMode = extractJsonString(obj, "paymentMode");
        o.status = extractJsonString(obj, "status");
        o.totalAmount = extractJsonInt(obj, "totalAmount");
        o.cancelReason = extractJsonString(obj, "cancelReason");
        o.trackingId = extractJsonString(obj, "trackingId");
        o.isSimulationOrder = extractJsonBoolean(obj, "isSimulationOrder");
        o.simulationItemName = extractJsonString(obj, "simulationItemName");
        o.simulationItemPrice = extractJsonInt(obj, "simulationItemPrice");

        String itemsJson = extractJsonArray(obj, "items");
        String[] itemObjects = splitTopLevelObjects(itemsJson);

        for (int i = 0; i < itemObjects.length; i++) {
            String itemObj = itemObjects[i];
            String pid = extractJsonString(itemObj, "productId");
            int qty = extractJsonInt(itemObj, "quantity");

            if (pid.length() > 0 && qty > 0) {
                Item item = new Item(pid, qty);
                o.addItem(item);
            }
        }

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
}


public void saveAll() throws Exception {
    saveProducts();
    saveOrders();
    saveAdmins();
}

private void loadAdmins() throws Exception {
    adminCount = 0;

    String json = readWholeFile("admins.json");
    if (json.equals("")) {
        String defaultUser = "admin";
        String defaultPassHash = Admin.hashPassword("admin123");
        admins[adminCount++] = new Admin(defaultUser, defaultPassHash, Role.ADMIN);
        return;
    }

    String[] objects = splitTopLevelObjects(json);

    for (int i = 0; i < objects.length; i++) {
        String obj = objects[i];

        String username = extractJsonString(obj, "username");
        String passHash = extractJsonString(obj, "passHash");
        String roleText = extractJsonString(obj, "role");

        Role role;
        try {
            role = Role.valueOf(roleText.toUpperCase());
        } catch (Exception ex) {
            System.out.println(ROSE + "Invalid role for user " +
                username + ". Using ADMIN by default." + RESET);
            role = Role.ADMIN;
        }

        admins[adminCount++] = new Admin(username, passHash, role);
    }

    for (int i = 0; i < adminCount; i++) {
        if (admins[i] != null) {
            System.out.print(LAVENDER + "Loaded admin: " +
                admins[i].username + " (" + admins[i].role + ")\n" + RESET);
        }
    }

    if (adminCount == 0) {
        String defaultUser = "admin";
        String defaultPassHash = Admin.hashPassword("admin123");
        admins[adminCount++] = new Admin(defaultUser, defaultPassHash, Role.ADMIN);
    }
}

public void saveProducts() throws Exception {
    StringBuilder json = new StringBuilder();
    json.append("[\n");

    int written = 0;

    for (int i = 0; i < productCount; i++) {
        Product p = products[i];
        if (p == null) continue;

        if (written > 0) json.append(",\n");

        json.append("  {\n");
        json.append("    \"productId\": ").append(q(p.productId)).append(",\n");
        json.append("    \"category\": ").append(q(p.category)).append(",\n");
        json.append("    \"brand\": ").append(q(p.brand)).append(",\n");
        json.append("    \"name\": ").append(q(p.name)).append(",\n");
        json.append("    \"price\": ").append(p.price).append(",\n");
        json.append("    \"stock\": ").append(p.stock).append("\n");
        json.append("  }");

        written++;
    }

    json.append("\n]");
    writeWholeFile("products.json", json.toString());
}
public void saveOrders() throws Exception {
    StringBuilder json = new StringBuilder();
    json.append("[\n");

    int writtenOrders = 0;

    for (int i = 0; i < orderCount; i++) {
        Order o = orders[i];
        if (o == null) continue;

        if (writtenOrders > 0) json.append(",\n");

        String orderIdPart = (o.orderId == null) ? "" : o.orderId;
        String datePart = (o.date == null) ? "" : o.date;
        String addressPart = (o.address == null) ? "" : o.address;
        String paymentPart = (o.paymentMode == null) ? "" : o.paymentMode;
        String statusPart = (o.status == null) ? "" : o.status;
        String cancelReasonPart = (o.cancelReason == null) ? "" : o.cancelReason;
        String trackingPart = (o.trackingId == null) ? "" : o.trackingId;
        String simNamePart = (o.simulationItemName == null) ? "" : o.simulationItemName;

        json.append("  {\n");
        json.append("    \"orderId\": ").append(q(orderIdPart)).append(",\n");
        json.append("    \"date\": ").append(q(datePart)).append(",\n");
        json.append("    \"address\": ").append(q(addressPart)).append(",\n");
        json.append("    \"paymentMode\": ").append(q(paymentPart)).append(",\n");
        json.append("    \"status\": ").append(q(statusPart)).append(",\n");
        json.append("    \"totalAmount\": ").append(o.totalAmount).append(",\n");
        json.append("    \"cancelReason\": ").append(q(cancelReasonPart)).append(",\n");
        json.append("    \"trackingId\": ").append(q(trackingPart)).append(",\n");
        json.append("    \"isSimulationOrder\": ").append(o.isSimulationOrder).append(",\n");
        json.append("    \"simulationItemName\": ").append(q(simNamePart)).append(",\n");
        json.append("    \"simulationItemPrice\": ").append(o.simulationItemPrice).append(",\n");
        json.append("    \"items\": [\n");

        int writtenItems = 0;
        for (int j = 0; j < o.itemCount; j++) {
            Item item = o.items[j];
            if (item == null) continue;

            if (writtenItems > 0) json.append(",\n");

            json.append("      {\n");
            json.append("        \"productId\": ").append(q(item.productId)).append(",\n");
            json.append("        \"quantity\": ").append(item.quantity).append("\n");
            json.append("      ");

            json.append("}");

            writtenItems++;
        }

        json.append("\n    ]\n");
        json.append("  }");

        writtenOrders++;
    }

    json.append("\n]");
    writeWholeFile("orders.json", json.toString());
}

 private void saveAdmins() throws Exception {
    StringBuilder json = new StringBuilder();
    json.append("[\n");

    int written = 0;

    for (int i = 0; i < adminCount; i++) {
        Admin a = admins[i];
        if (a == null) continue;

        if (written > 0) json.append(",\n");

        json.append("  {\n");
        json.append("    \"username\": ").append(q(a.username)).append(",\n");
        json.append("    \"passHash\": ").append(q(a.passHash)).append(",\n");
        json.append("    \"role\": ").append(q(a.role.name())).append("\n");
        json.append("  }");

        written++;
    }

    json.append("\n]");
    writeWholeFile("admins.json", json.toString());
}
 public void sortProductsById() {
    for (int i = 0; i < productCount - 1; i++) {
        for (int j = i + 1; j < productCount; j++) {

            Product a = products[i];
            Product b = products[j];

            if (a == null || b == null) continue;

            if (a.productId.compareToIgnoreCase(b.productId) > 0) {
                Product temp = products[i];
                products[i] = products[j];
                products[j] = temp;
            }
        }
    }
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
private int computeNextOrderNumber() {
    int max = 1000;   // so first generated id becomes O01001

    for (int i = 0; i < orderCount; i++) {
        Order o = orders[i];
        if (o == null || o.orderId == null) continue;

        String normalized = normalizeOrderId(o.orderId);
        if (normalized.equals("")) continue;

        // Remove leading O
        String digits = normalized.substring(1);

        int n = toInt(digits);
        if (n > max) {
            max = n;
        }
    }

    return max + 1;
}

public void refreshNextOrderNumber() {
    nextOrderNumber = computeNextOrderNumber();
}

 // Generate a new unique Order ID (e.g., "O1001", "O1002", ...) 
public String generateOrderId() {
    // Safety check in case data changed before generation
    if (nextOrderNumber <= 0) {
        refreshNextOrderNumber();
    }

    String s = String.valueOf(nextOrderNumber);
    while (s.length() < 5) {
        s = "0" + s;
    }

    String id = "O" + s;
    nextOrderNumber++;

    return id;
}
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
            // ignore non-digit characters
        }
    }

    return neg ? -value : value;
}

public void appendLoginAudit(String action, String username) {
    try {
        String existing = readWholeFile("login_audit.json");
        if (existing.equals("")) existing = "[]";

        String[] oldObjects = splitTopLevelObjects(existing);

        StringBuilder json = new StringBuilder();
        json.append("[\n");

        int written = 0;

        for (int i = 0; i < oldObjects.length; i++) {
            if (oldObjects[i] == null || oldObjects[i].trim().equals("")) continue;

            if (written > 0) json.append(",\n");
            json.append(oldObjects[i]);
            written++;
        }

        if (written > 0) json.append(",\n");

        json.append("  {\n");
        json.append("    \"action\": ").append(q(action)).append(",\n");
        json.append("    \"username\": ").append(q(username)).append(",\n");
        json.append("    \"timestamp\": ").append(q(currentDateTimeString())).append("\n");
        json.append("  }\n");

        json.append("]");

        writeWholeFile("login_audit.json", json.toString());

    } catch (Exception e) {
        // ignore to avoid crash
    }
}

public String currentDateTimeString() {
    int[] p = getCurrentDateTimeParts();
    return p[0] + "-" + twoDigits(p[1]) + "-" + twoDigits(p[2])
         + "T" + twoDigits(p[3]) + ":" + twoDigits(p[4]) + ":" + twoDigits(p[5]);
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
