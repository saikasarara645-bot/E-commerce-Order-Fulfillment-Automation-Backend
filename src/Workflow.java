import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;



/** Workflow.java – Orchestrates order processing and provides the Admin Dashboard menu */
public class Workflow {
    public static final String ANSI_SOFT_CORAL = "\u001B[38;5;209m";
    public static final String ANSI_MUTED_PEACH = "\u001B[38;5;216m";
    public static final String ANSI_Yellow ="\u001B[38;5;220m";
    // ===== Pastel Theme (Girlish + Professional) =====
    public static final String RESET = "\u001B[0m";
    public static final String BOLD  = "\u001B[1m";
    // Soft pastel colors
    public static final String PINK      = "\u001B[38;5;211m"; // header / highlight
    public static final String LAVENDER  = "\u001B[38;5;183m"; // menu numbers
    public static final String MINT      = "\u001B[38;5;156m"; // success/allowed
    //public static final String PEACH     = "\u001B[38;5;216m"; // warnings/restricted
    public static final String ROSE      = "\u001B[38;5;174m"; // exit/error
    public static final String SOFTGRAY  = "\u001B[38;5;250m"; // normal text
    // Background (optional)
    public static final String BG_WHITE  = "\u001B[48;5;231m";


    private DataPersistence dp;
    private Log log;
    private PaymentService paymentService;

    public Workflow(DataPersistence dp, Log log) {
        this.dp = dp;
        this.log = log;
        this.paymentService = new PaymentService(log);
    }
    private String readWholeFile(String filename) {
    BufferedReader br = null;
    StringBuilder sb = new StringBuilder();

    try {
        br = new BufferedReader(new FileReader(dp.path(filename)));
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line).append("\n");
        }
    } catch (Exception e) {
        return "";
    } finally {
        try { if (br != null) br.close(); } catch (Exception ex) {}
    }

    return sb.toString().trim();
}

private void writeWholeFile(String filename, String content) throws Exception {
    FileWriter fw = new FileWriter(dp.path(filename), false);
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

    i++;
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

    return DataPersistence.toInt(num);
}

private boolean extractJsonBoolean(String obj, String key) {
    int i = findValueStart(obj, key);
    if (i < 0 || i >= obj.length()) return false;

    return obj.startsWith("true", i);
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
    String text = (jsonArrayText == null ? "" : jsonArrayText.trim());

    if (text.startsWith("[")) text = text.substring(1);
    if (text.endsWith("]")) text = text.substring(0, text.length() - 1);

    String[] temp = new String[1000];
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

private String orderToJson(Order o, String indent) {
    if (indent == null) indent = "";

    String in1 = indent + "  ";
    String in2 = indent + "    ";
    String in3 = indent + "      ";

    StringBuilder json = new StringBuilder();

    json.append(indent).append("{\n");
    json.append(in1).append("\"orderId\": ").append(q(o.orderId == null ? "" : o.orderId)).append(",\n");
    json.append(in1).append("\"date\": ").append(q(o.date == null ? "" : o.date)).append(",\n");
    json.append(in1).append("\"address\": ").append(q(o.address == null ? "" : o.address)).append(",\n");
    json.append(in1).append("\"paymentMode\": ").append(q(o.paymentMode == null ? "" : o.paymentMode)).append(",\n");
    json.append(in1).append("\"status\": ").append(q(o.status == null ? "" : o.status)).append(",\n");
    json.append(in1).append("\"totalAmount\": ").append(o.totalAmount).append(",\n");
    json.append(in1).append("\"cancelReason\": ").append(q(o.cancelReason == null ? "" : o.cancelReason)).append(",\n");
    json.append(in1).append("\"trackingId\": ").append(q(o.trackingId == null ? "" : o.trackingId)).append(",\n");
    json.append(in1).append("\"isSimulationOrder\": ").append(o.isSimulationOrder).append(",\n");
    json.append(in1).append("\"simulationItemName\": ").append(q(o.simulationItemName == null ? "" : o.simulationItemName)).append(",\n");
    json.append(in1).append("\"simulationItemPrice\": ").append(o.simulationItemPrice).append(",\n");
    json.append(in1).append("\"items\": [\n");

    int writtenItems = 0;
    for (int i = 0; i < o.itemCount; i++) {
        Item it = o.items[i];
        if (it == null) continue;

        if (writtenItems > 0) json.append(",\n");

        json.append(in2).append("{\n");
        json.append(in3).append("\"productId\": ").append(q(it.productId == null ? "" : it.productId)).append(",\n");
        json.append(in3).append("\"quantity\": ").append(it.quantity).append("\n");
        json.append(in2).append("}");

        writtenItems++;
    }

    json.append("\n").append(in1).append("]\n");
    json.append(indent).append("}");

    return json.toString();
}

private String ordersToJson(Order[] list, int count, String indent) {
    if (indent == null) indent = "";

    StringBuilder json = new StringBuilder();
    json.append("[\n");

    int written = 0;
    for (int i = 0; i < count; i++) {
        Order o = list[i];
        if (o == null) continue;

        if (written > 0) json.append(",\n");
        json.append(orderToJson(o, indent + "  "));
        written++;
    }

    json.append("\n").append(indent).append("]");
    return json.toString();
}

private Order buildOrderFromJson(String obj) {
    if (obj == null || obj.trim().equals("")) return null;

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

        if (!pid.equals("") && qty > 0) {
            o.addItem(new Item(pid, qty));
        }
    }

    if (o.totalAmount <= 0 && o.itemCount > 0) {
        o.totalAmount = computeOrderTotal(o);
    }

    return o;
}

private int appendOrdersFromJsonArray(String ordersJson) {
    int restored = 0;

    String[] orderObjects = splitTopLevelObjects(ordersJson);

    for (int i = 0; i < orderObjects.length; i++) {
        if (dp.orderCount >= dp.orders.length) break;

        Order o = buildOrderFromJson(orderObjects[i]);
        if (o == null) continue;

        o.orderId = normalizeOrderId(o.orderId);

        if (o.orderId.equals("")) continue;

        if (findOrderById(o.orderId) != null) {
            continue; // skip duplicates
        }

        dp.orders[dp.orderCount++] = o;
        restored++;
    }

    dp.refreshNextOrderNumber();
    return restored;
}

private void replaceOrdersFromJsonArray(String ordersJson) {
    for (int i = 0; i < dp.orders.length; i++) {
        dp.orders[i] = null;
    }
    dp.orderCount = 0;

    appendOrdersFromJsonArray(ordersJson);
}

private String readLastAuditSummary() {
    String json = readWholeFile("login_audit.json");
    if (json.equals("")) return "";

    String[] objects = splitTopLevelObjects(json);
    if (objects.length == 0) return "";

    String last = objects[objects.length - 1];

    String action = extractJsonString(last, "action");
    String username = extractJsonString(last, "username");
    String timestamp = extractJsonString(last, "timestamp");

    String out = "";
    if (!action.equals("")) out += action;
    if (!username.equals("")) out += (out.equals("") ? "" : " | ") + username;
    if (!timestamp.equals("")) out += (out.equals("") ? "" : " | ") + timestamp;

    return out;
}

    /** Wrapper for Admin authentication */
    public boolean adminLogin(BufferedReader console) throws Exception {
        return Admin.authenticate(dp, console);
    }
    
   private void printLine() {
    System.out.println(SOFTGRAY + "════════════════════════════════════════════════════════════" + RESET);
}

    private void printTitle(String text) {
    printLine();
    System.out.println(PINK + BOLD + text + RESET);
    printLine();
    }
    private void printDashboardBox(Admin admin) {

    int width = 60; // inner width of box

    String username = (admin != null && admin.username != null)
            ? admin.username : "Unknown";

    String role = (admin != null && admin.role != null)
            ? admin.role.name() : "Unknown";

    String top    = "╔════════════════════════════════════════════════════════════╗";
    String mid    = "║                                                            ║";
    String sep    = "╠════════════════════════════════════════════════════════════╣";
    String bottom = "╚════════════════════════════════════════════════════════════╝";

    System.out.println(SOFTGRAY + top + RESET);

    System.out.println(SOFTGRAY + "║" + RESET
            + PINK + BOLD + centerText("** ADMIN DASHBOARD **", width) + RESET
            + SOFTGRAY + "║" + RESET);

    System.out.println(SOFTGRAY + "║" + RESET
            + LAVENDER + centerText("E-Commerce Order Fulfillment Automation System", width) + RESET
            + SOFTGRAY + "║" + RESET);

    System.out.println(SOFTGRAY + sep + RESET);

    System.out.println(SOFTGRAY + "║" + RESET
            + MINT + centerText("Logged in as: " + username + " (" + role + ")", width) + RESET
            + SOFTGRAY + "║" + RESET);

    System.out.println(SOFTGRAY + mid + RESET);
    System.out.println(SOFTGRAY + bottom + RESET);
}
private void printSection(String text) {
    System.out.println();
    System.out.println(PINK + BOLD + ">>" + text + RESET);
    printLine();
}
private void printMenuOption(int num, String text, boolean enabled) {
    String label = "[" + num + "] ";

    if (enabled) {
        System.out.println(LAVENDER + label + RESET + MINT + text + RESET);
    } else {
        System.out.println(LAVENDER + label + RESET + ROSE + text + " (Restricted)" + RESET);
    }
}
private void printFooter(String message) {
    if (message == null) message = "";
    System.out.println();
    printLine();
    System.out.println(MINT + centerText(message, 60) + RESET);
}
private String centerText(String text, int width) {

    if (text == null) text = "";

    text = text.trim();              // 1

    int len = text.length();         // 2

    if (len >= width) {
        return text.substring(0, width);   // 3
    }

    int left = (width - len) / 2;
    int right = width - len - left;

    StringBuilder sb = new StringBuilder();   // 4

    for (int i = 0; i < left; i++) {
        sb.append(" ");
    }

    sb.append(text);                          // 5

    for (int i = 0; i < right; i++) {
        sb.append(" ");
    }

    return sb.toString();                    
}
private void printRoleSummary(Admin admin) {
    printTitle("Quick Summary");
   int pending = 0;
int packed = 0;
int shipped = 0;
int outForDelivery = 0;
int delivered = 0;
int cancelled = 0;

for (int i = 0; i < dp.orderCount; i++) {
    Order o = dp.orders[i];
    if (o == null || o.status == null) continue;

    switch (o.status) {
        case "PENDING":
            pending++;
            break;
        case "PACKED":
            packed++;
            break;
        case "SHIPPED":
            shipped++;
            break;
        case "OUT_FOR_DELIVERY":
            outForDelivery++;
            break;
        case "DELIVERED":
            delivered++;
            break;
        case "CANCELLED":
            cancelled++;
            break;
    }
}
    int activeOrders = packed + shipped + outForDelivery;
       System.out.println(SOFTGRAY + "Total Orders: " + RESET + MINT + dp.orderCount + RESET);
       System.out.println(SOFTGRAY + "Active Orders: " + RESET + MINT + activeOrders + RESET);
       System.out.println(SOFTGRAY + "Pending: " + RESET + MINT+ pending + RESET);
       System.out.println(SOFTGRAY + "Packed: " + RESET + MINT + packed + RESET);
       System.out.println(SOFTGRAY + "Shipped: " + RESET + MINT + shipped + RESET);
       System.out.println(SOFTGRAY + "Out for Delivery: " + RESET + MINT + outForDelivery + RESET);
       System.out.println(SOFTGRAY + "Delivered: " + RESET + MINT + delivered + RESET);
       System.out.println(SOFTGRAY + "Cancelled: " + RESET +ROSE+ cancelled + RESET);
       printLine();
       printProductSummary();  
}
private String twoDigits(int n) {
    return (n < 10 ? "0" : "") + n;
}

private boolean isLeapYear(int year) {
    if (year % 400 == 0) return true;
    if (year % 100 == 0) return false;
    return year % 4 == 0;
}

private int[] getCurrentDateParts() {
    long millis = System.currentTimeMillis();
    long totalDays = millis / 86400000L;

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
    return new int[] { year, month, day };
}

private int countLowStock(int threshold) {
    int c = 0;
    for (int i = 0; i < dp.productCount; i++) {
        Product p = dp.products[i];
        if (p != null && p.stock <= threshold) c++;
    }
    return c;
}



 public void adminDashboard(BufferedReader console) throws Exception {

    while (true) {

        Admin currentAdmin = dp.admins[dp.currentAdminIndex];

        printDashboardBox(currentAdmin);
        printRoleSummary(currentAdmin);
        printLine();

        printAdminMenu(currentAdmin);

        printFooter("System Ready . Awaiting Command");
        System.out.print(SOFTGRAY + "Please select an option: " + RESET);

        String choice = console.readLine();
        if (choice == null) choice = "";
        choice = choice.trim();

        System.out.println();

        if (!choice.equals("")) {
            System.out.println(MINT + "You selected option: " + choice + RESET);
            printLine();
        }

        if (choice.equals("0")) {
            System.out.println(LAVENDER + "Exiting Admin Dashboard..." + RESET);
            System.out.println(LAVENDER + "Thank you for using E-commerce Order Fulfillment Automation System" + RESET);
            return;
        }

        handleDashboardChoice(choice, console, currentAdmin);

        printLine();
    }
}
private void printAdminMenu(Admin currentAdmin) {

    boolean isAdmin = currentAdmin.role == Role.ADMIN;
    boolean isManager = currentAdmin.role == Role.MANAGER;
    boolean canManageStock = isAdmin || isManager;
    System.out.println();
    System.out.println(LAVENDER + BOLD + "==================== MENU ====================" + RESET);

    // ORDER MANAGEMENT
    printSection("ORDER MANAGEMENT");
    printMenuOption(1,"Accept New Order",true);
    printMenuOption(2,"Update Order Status",true);
    printMenuOption(3,"View Order Logs",true);
    printMenuOption(4,"Search/Filter Orders",true);
    printMenuOption(5,"Generate Receipt",true);

    // PRODUCT
    printSection("PRODUCT & STOCK");
    printMenuOption(6,"Advanced Product Filter",true);
    printMenuOption(7,"Manage Products",canManageStock);
    printMenuOption(8,"Low Stock Alerts",canManageStock);
    printMenuOption(9,"Restock Product",canManageStock);
    printMenuOption(10,"Export Stock Report",canManageStock);

    // OPERATIONS
    printSection("OPERATIONS");
    printMenuOption(11,"Reorder Previous Order",true);
    printMenuOption(12,"Retry Failed Order",true);
    printMenuOption(13,"Simulation Mode",true);
    printMenuOption(14,"Load Test Data",true);
    printMenuOption(15,"System Health Check",true);
    printMenuOption(16,"Show Order Timeline",true);
    printMenuOption(17,"Auto Cancel Stale Orders",true);
    printMenuOption(18,"Show Recently Auto-Cancelled Orders",true);
    // ADMIN ONLY
    printSection("SYSTEM");
    printMenuOption(19,"Bulk Import Orders",isAdmin);
    printMenuOption(20,"Archive Delivered Orders",isAdmin);
    printMenuOption(21,"Clear Logs",isAdmin);
    printMenuOption(22,"Add New Admin",isAdmin);
    printMenuOption(23,"Change Password",true);
    printMenuOption(24,"Generate Report",isAdmin);
    printMenuOption(25,"Delete ALL Order History",isAdmin);
    printMenuOption(26,"Restore Order History",isAdmin);
    printMenuOption(27,"Undo Last Restore",isAdmin);

    System.out.println();
    System.out.println(LAVENDER + "[0] Exit" + RESET);
}
private void handleDashboardChoice(String choice, BufferedReader console, Admin admin) throws Exception {

    boolean isAdmin = admin.role == Role.ADMIN;
    boolean canManageStock = admin.role == Role.ADMIN || admin.role == Role.MANAGER;

    switch(choice) {

        case "1": acceptNewOrder(console); break;
        case "2": handleStatusUpdate(console); break;

        case "3":
            showOrderLogsMenu(console);
            break;

        case "4": handleOrderSearch(console); break;
        case "5": generateReceipt(console); break;
        case "6": handleAdvancedFilter(console); break;

        case "7":
            if(canManageStock) handleProductManagement(console);
            else System.out.println(ROSE+"Restricted: Admin/Manager only."+RESET);
            break;

        case "8":
            if(canManageStock) showLowStockAlerts();
            else System.out.println(ROSE+"Restricted: Admin/Manager only."+RESET);
            break;

        case "9":
            if(canManageStock) handleRestock(console);
            else System.out.println(ROSE+"Restricted: Admin/Manager only."+RESET);
            break;

        case "10":
            if(canManageStock) exportStockReport();
            else System.out.println(ROSE+"Restricted: Admin/Manager only."+RESET);
            break;

        case "11": handleReorder(console); break;
        case "12": retryCancelledOrder(console); break;
        case "13": runSimulation(console); break;
        case "14":
           System.out.print(LAVENDER + "Enter JSON filename (e.g. orders_import.json): " + RESET);
                String file = console.readLine();
                if (file == null) file = "";
                file = file.trim();

                if (!file.equals("")) {
                    dp.loadTestDataFromFile(file);
                    dp.saveAll();

                    System.out.print(MINT + "Loaded test data successfully\n" + RESET);
                    System.out.print(SOFTGRAY + "-> " + dp.productCount + " products loaded.\n" + RESET);
                    System.out.print(SOFTGRAY + "-> " + dp.orderCount + " orders loaded.\n" + RESET);
                    System.out.print(SOFTGRAY + "-> " + dp.adminCount + " admins loaded.\n" + RESET);
                }
                break;
        case "15": systemHealthCheck(); break;
        case "16": showOrderTimeline(console); break;
        case "17":
        System.out.print(SOFTGRAY + "Cancel orders pending for how many days?: " + RESET);
        String daysStr = console.readLine();
         if (daysStr == null) daysStr = "";
        daysStr = daysStr.trim();

        int days = DataPersistence.toInt(daysStr);
        if (days <= 0) {
        System.out.print(ROSE + "Invalid number of days.\n" + RESET);
        break;
    }

    autoCancelStaleOrders(days);
    break;
        case "18":
        showRecentlyAutoCancelledOrders();
        break;

        case "19":
        if (isAdmin) {
          importOrdersFromFile(console);
        } else {
        System.out.println(ROSE + "Restricted: Admin only." + RESET);
        }
        break;

        case "20":
            if(isAdmin) archiveDeliveredOrders(console);
            else System.out.println(ROSE+"Restricted: Admin only."+RESET);
            break;

        case "21":
            if(isAdmin) clearLogs(console);
            else System.out.println(ROSE+"Restricted: Admin only."+RESET);
            break;

        case "22":
            if(isAdmin) addNewAdmin(console);
            else System.out.println(ROSE+"Restricted: Admin only."+RESET);
            break;

        case "23":
            changeAdminPassword(console);
            break;

        case "24":
            if(isAdmin) generateReport();
            else System.out.println(ROSE+"Restricted: Admin only."+RESET);
            break;

        case "25":
            if(isAdmin) deleteAllOrderHistory(console);
            else System.out.println(ROSE+"Restricted: Admin only."+RESET);
            break;

        case "26":
            if(isAdmin) restoreOrdersFromArchive(console);
            else System.out.println(ROSE+"Restricted: Admin only."+RESET);
            break;

        case "27":
            if(isAdmin) undoLastRestore(console);
            else System.out.println(ROSE+"Restricted: Admin only."+RESET);
            break;

        default:
            System.out.println(ROSE+"Invalid option."+RESET);
    }
}
private void showOrderLogsMenu(BufferedReader console) throws Exception {
    printTitle("Available Orders (Sorted by Date)");

    // Manual copy instead of Arrays.copyOf(...)
    Order[] sorted = new Order[dp.orderCount];
    for (int i = 0; i < dp.orderCount; i++) {
        sorted[i] = dp.orders[i];
    }

    // Manual sort instead of Arrays.sort(..., Comparator.comparing(...))
    for (int i = 0; i < sorted.length - 1; i++) {
        for (int j = i + 1; j < sorted.length; j++) {
            if (sorted[i] == null || sorted[j] == null) {
                continue;
            }

            String date1 = (sorted[i].date == null) ? "" : sorted[i].date.trim();
            String date2 = (sorted[j].date == null) ? "" : sorted[j].date.trim();

            if (date1.compareTo(date2) > 0) {
                Order temp = sorted[i];
                sorted[i] = sorted[j];
                sorted[j] = temp;
            }
        }
    }

    for (int i = 0; i < sorted.length; i++) {
        Order o = sorted[i];
        if (o != null) {
            System.out.println(
                SOFTGRAY + o.orderId + RESET +
                SOFTGRAY + " | Date: " + RESET + MINT + o.date + RESET +
                SOFTGRAY + " | Status: " + RESET + LAVENDER + o.status + RESET
            );
        }
    }

    System.out.print(LAVENDER + "Enter Order ID to view logs: " + RESET);

    String id = console.readLine();
    if (id == null) return;

    id = id.trim();
    if (id.equals("")) return;

    Order order = findOrderById(id);

    if (order != null)
        log.viewLogsByOrder(order.orderId);
    else
        System.out.println(ROSE + "Order not found." + RESET);
}
private void showOrderTimeline(BufferedReader console) throws Exception {
    showTimelinePreview();

    System.out.print(SOFTGRAY + "Enter Order ID for timeline: " + RESET);
    String id = console.readLine();
    if (id == null) id = "";
    id = id.trim();

    if (id.equals("")) {
        System.out.print(ROSE + "Order ID cannot be empty.\n" + RESET);
        return;
    }

    Order order = findOrderById(id);

    if (order == null) {
        System.out.print(ROSE + "Order " + normalizeOrderId(id) + " not found.\n" + RESET);
        return;
    }

    String realOrderId = order.orderId;

    System.out.print(PINK + BOLD + "Timeline for " + realOrderId + "\n" + RESET);
    printLine();

    log.viewLogsByOrder(realOrderId);

    printLine();
}

private void addNewAdmin(BufferedReader console) throws Exception {
    // Only allow current admin to add new admin if they have the ADMIN role
    Admin currentAdmin = dp.admins[dp.currentAdminIndex];
    if (currentAdmin == null || !currentAdmin.hasPermission(Role.ADMIN)) {
        System.out.print(ROSE+"Permission denied. Only admins can add new admins."+RESET+"\n");
        return;
    }

    // Proceed with adding the new admin
    System.out.print(SOFTGRAY+"Enter new admin username: "+RESET);
    String username = console.readLine().trim();

    System.out.print(SOFTGRAY+"Enter new admin password: "+RESET);
    String password = console.readLine().trim();

    System.out.print(SOFTGRAY+"Enter role (ADMIN, MANAGER, SUPPORT): "+RESET);
    String roleStr = console.readLine().trim().toUpperCase();
    Role role = Role.valueOf(roleStr);

    // Hash the password before saving
    String hashedPassword = Admin.hashPassword(password);

    // Create new admin object
    Admin newAdmin = new Admin(username, hashedPassword, role);

    // Add new admin to the list
    dp.addAdmin(newAdmin);

    // Save the updated admin list to file
    dp.saveAll();
    printLine();
    System.out.println(MINT+"New admin added successfully."+RESET+"\n");
}

private void handleOrderSearch(BufferedReader console) throws Exception {
    showOrdersPreview();

    System.out.print(SOFTGRAY + "Enter Order ID or Status to search (or press Enter for advanced filter): " + RESET);
    String query = console.readLine();
    if (query == null) query = "";
    query = query.trim();

    // ===========================
    // ADVANCED FILTER MODE
    // ===========================
    if (query.equals("")) {
        System.out.print(SOFTGRAY + "Enter Status to filter (or press Enter for any): " + RESET);
        String statusFilter = console.readLine();
        if (statusFilter == null) statusFilter = "";
        statusFilter = statusFilter.trim();

        System.out.print(SOFTGRAY + "Enter Payment Mode to filter (or press Enter for any): " + RESET);
        String paymentFilter = console.readLine();
        if (paymentFilter == null) paymentFilter = "";
        paymentFilter = paymentFilter.trim();

        System.out.print(SOFTGRAY + "Enter Date to filter (YYYY-MM-DD, or press Enter for any): " + RESET);
        String dateFilter = console.readLine();
        if (dateFilter == null) dateFilter = "";
        dateFilter = dateFilter.trim();

        String statusFilterUC = statusFilter.toUpperCase();
        String paymentFilterUC = paymentFilter.toUpperCase();

        Order[] results = new Order[dp.orderCount];
        int count = 0;
        printLine();

        for (int i = 0; i < dp.orderCount; i++) {
            Order o = dp.orders[i];
            if (o == null) continue;

            String status = (o.status == null) ? "" : o.status.trim().toUpperCase();
            String payment = (o.paymentMode == null) ? "" : o.paymentMode.trim().toUpperCase();
            String date = (o.date == null) ? "" : o.date.trim();

            if (!statusFilterUC.equals("") && !status.equals(statusFilterUC)) {
                continue;
            }
            if (!paymentFilterUC.equals("") && !payment.equals(paymentFilterUC)) {
                continue;
            }
            if (!dateFilter.equals("") && !date.equals(dateFilter)) {
                continue;
            }

            results[count++] = o;
        }

        if (count == 0) {
            System.out.print(ROSE + "No orders found matching the given criteria.\n" + RESET);
        } else {
            String statusCrit = statusFilter.equals("") ? "Any" : statusFilter;
            String payCrit = paymentFilter.equals("") ? "Any" : paymentFilter;
            String dateCrit = dateFilter.equals("") ? "Any" : dateFilter;

            System.out.print(SOFTGRAY + "Orders matching filters - Status: " + RESET + statusCrit
                    + SOFTGRAY + ", Payment: " + RESET + payCrit
                    + SOFTGRAY + ", Date: " + RESET + dateCrit + ":\n" + RESET);

            for (int i = 0; i < count; i++) {
                Order o = results[i];

                String rawStatus = (o.status == null) ? "" : o.status.trim().toUpperCase();
                String statusStr = rawStatus;

                if ("DELIVERED".equals(rawStatus)) statusStr = LAVENDER + rawStatus + RESET;
                else if ("CANCELLED".equals(rawStatus)) statusStr = ROSE + rawStatus + RESET;
                else if ("PENDING".equals(rawStatus)) statusStr = MINT + rawStatus + RESET;
                else if ("SHIPPED".equals(rawStatus)) statusStr = MINT + rawStatus + RESET;
                else if ("PACKED".equals(rawStatus)) statusStr = MINT + rawStatus + RESET;
                else if ("OUT_FOR_DELIVERY".equals(rawStatus)) statusStr = MINT + rawStatus + RESET;

                String orderId = (o.orderId == null) ? "(Unknown)" : o.orderId;
                String date = (o.date == null || o.date.trim().equals("")) ? "(N/A)" : o.date;
                String payment = (o.paymentMode == null || o.paymentMode.trim().equals("")) ? "(N/A)" : o.paymentMode;
                int total = computeOrderTotal(o);

                System.out.print("- " + orderId
                        + " | Date: " + date
                        + " | Payment: " + payment
                        + " | Status: " + statusStr
                        + " | Total: BDT " + total);

                if ("CANCELLED".equals(rawStatus) &&
                        o.cancelReason != null &&
                        !o.cancelReason.trim().equals("")) {
                    System.out.print(ROSE + " | CancelReason: " + o.cancelReason.trim() + RESET);
                }

                System.out.print("\n");
            }

            printLine();
            System.out.print(SOFTGRAY + "Enter Order ID to view details (or press Enter to skip): " + RESET);
            String selId = console.readLine();
            if (selId == null) selId = "";
            selId = selId.trim();

            if (!selId.equals("")) {
                Order target = findOrderById(selId);

                if (target != null) viewOrderDetails(target);
                else System.out.print(ROSE + "Order " + normalizeOrderId(selId) + " not found.\n" + RESET);
            }
        }
        return;
    }

    // ===========================
    // STANDARD SEARCH MODE
    // ===========================
    Order found = findOrderById(query);

    if (found != null) {
        viewOrderDetails(found);
        return;
    }

    // Otherwise treat input as status query
    String statusQuery = query.trim().toUpperCase();
    Order[] results = new Order[dp.orderCount];
    int count = 0;

    for (int i = 0; i < dp.orderCount; i++) {
        Order o = dp.orders[i];
        if (o == null || o.status == null) continue;

        String status = o.status.trim().toUpperCase();
        if (status.contains(statusQuery)) {
            results[count++] = o;
        }
    }

    if (count == 0) {
        System.out.print(ROSE + "No orders found matching \"" + query + "\".\n" + RESET);
    } else {
        System.out.print(SOFTGRAY + "Orders with status containing \"" + query + "\":\n" + RESET);

        for (int i = 0; i < count; i++) {
            Order o = results[i];

            String rawStatus = (o.status == null) ? "" : o.status.trim().toUpperCase();
            String statusStr = rawStatus;

            if ("DELIVERED".equals(rawStatus)) statusStr = LAVENDER + rawStatus + RESET;
            else if ("CANCELLED".equals(rawStatus)) statusStr = ROSE + rawStatus + RESET;
            else if ("PENDING".equals(rawStatus)) statusStr = MINT + rawStatus + RESET;
            else if ("SHIPPED".equals(rawStatus)) statusStr = MINT + rawStatus + RESET;
            else if ("PACKED".equals(rawStatus)) statusStr = MINT + rawStatus + RESET;
            else if ("OUT_FOR_DELIVERY".equals(rawStatus)) statusStr = MINT + rawStatus + RESET;

            int total = computeOrderTotal(o);
            String orderId = (o.orderId == null) ? "(Unknown)" : o.orderId;

            System.out.print(SOFTGRAY + "- " + orderId + " | Status: " + statusStr + " | Total: BDT " + total + RESET);

            if (o.cancelReason != null && !o.cancelReason.trim().equals("")) {
                System.out.print(ROSE + " | CancelReason: " + o.cancelReason.trim() + RESET);
            }
            System.out.print("\n");
        }

        System.out.print(SOFTGRAY + "Enter Order ID to view details (or press Enter to skip): " + RESET);
        String selId = console.readLine();
        if (selId == null) selId = "";
        selId = selId.trim();

        if (!selId.equals("")) {
            Order target = findOrderById(selId);

            if (target != null) viewOrderDetails(target);
            else System.out.print(ROSE + "Order " + normalizeOrderId(selId) + " not found.\n" + RESET);
        }
    }
}

    /** Feature 6: Manually progress an order status through the workflow (PENDING -> PACKED -> SHIPPED -> OUT_FOR_DELIVERY -> DELIVERED) */
/** Feature 6: Manually progress an order status through the workflow
 *  (PENDING -> PACKED -> SHIPPED -> OUT_FOR_DELIVERY -> DELIVERED)
 */
private void handleStatusUpdate(BufferedReader console) throws Exception {
    showOrdersForStatusUpdate();

    System.out.print(SOFTGRAY + "Enter Order ID to update status: " + RESET);
    String id = console.readLine();
    if (id == null) id = "";
    id = id.trim();

    if (id.equals("")) {
        System.out.print(ROSE + "Order ID cannot be empty.\n" + RESET);
        return;
    }

    Order order = findOrderById(id);

    if (order == null) {
        System.out.print(ROSE + "Order " + normalizeOrderId(id) + " not found.\n" + RESET);
        return;
    }

    String currentStatus = (order.status == null) ? "" : order.status.trim().toUpperCase();

    // Already finished / blocked orders
    if ("DELIVERED".equals(currentStatus) || "CANCELLED".equals(currentStatus)) {
        System.out.print(ROSE + "Order " + order.orderId + " is " + currentStatus + "; status cannot be changed.\n" + RESET);
        return;
    }

    // =========================
    // CASE 1: PENDING -> process acceptance
    // =========================
    if ("PENDING".equals(currentStatus)) {
        boolean accepted = processPendingOrder(order, console);

        dp.saveOrders();

        if (!accepted) {
            System.out.print(ROSE + "Order processing failed. Status updated to CANCELLED ("
                    + order.cancelReason + ").\n" + RESET);
            return;
        }

        // If accepted, order is now PACKED and should stop here
        log.write(order.orderId, "Order accepted and moved to PACKED");
        System.out.print(MINT + "Order " + order.orderId + " accepted successfully. Status updated to PACKED.\n" + RESET);
        return;
    }

    // =========================
    // CASE 2: Normal manual transitions
    // =========================
    String nextStatus = null;

    if ("PACKED".equals(currentStatus)) {
        nextStatus = "SHIPPED";
    } else if ("SHIPPED".equals(currentStatus)) {
        nextStatus = "OUT_FOR_DELIVERY";
    } else if ("OUT_FOR_DELIVERY".equals(currentStatus)) {
        nextStatus = "DELIVERED";
    }

    if (nextStatus == null) {
        System.out.print(ROSE + "No further status transition available for " + currentStatus + ".\n" + RESET);
        return;
    }

    order.status = nextStatus;

    if ("SHIPPED".equals(nextStatus)) {
        String normalizedId = normalizeOrderId(order.orderId);
        order.trackingId = "TRK" + normalizedId;
    }

    dp.saveOrders();
    log.write(order.orderId, "Status changed to " + nextStatus);

    System.out.print(MINT + "Order " + order.orderId + " status updated to " + nextStatus + ".\n" + RESET);
}
private Order findOrderById(String inputId) {
    if (inputId == null) return null;

    inputId = normalizeOrderId(inputId.trim());

    for (int i = 0; i < dp.orderCount; i++) {
        Order o = dp.orders[i];
        if (o == null || o.orderId == null) continue;

        String storedId = normalizeOrderId(o.orderId.trim());
        if (storedId.equalsIgnoreCase(inputId)) {
            return o;
        }
    }
    return null;
}

    /** Feature 5 & 8: Reorder a previous order (copy its items into a new order and process it) */
private void handleReorder(BufferedReader console) throws Exception {
    showReorderPreview();

    System.out.print(SOFTGRAY + "Enter Order ID to reorder: " + RESET);
    String oldId = console.readLine();
    if (oldId == null) oldId = "";
    oldId = normalizeOrderId(oldId);

    if (oldId.equals("")) {
        System.out.print(ROSE + "Order ID cannot be empty.\n" + RESET);
        return;
    }

    if (dp.orderCount >= dp.orders.length) {
        System.out.print(ROSE + "Order storage is full. Cannot create reorder.\n" + RESET);
        return;
    }

    Order original = findOrderById(oldId);

    if (original == null) {
        System.out.print(ROSE + "Order " + oldId + " not found.\n" + RESET);
        return;
    }

    // Create order object first, but DO NOT generate ID yet
    Order newOrder = new Order();
    newOrder.date = currentDateString();
    newOrder.address = original.address;
    newOrder.paymentMode = (original.paymentMode == null || original.paymentMode.trim().equals(""))
            ? "COD"
            : original.paymentMode.trim();

    // preserve simulation metadata
    newOrder.isSimulationOrder = original.isSimulationOrder;
    newOrder.simulationItemName = original.simulationItemName;
    newOrder.simulationItemPrice = original.simulationItemPrice;

    // copy items first; if anything fails, no order ID is lost
    for (int j = 0; j < original.itemCount; j++) {
        Item it = original.items[j];
        if (it == null) continue;

        if (!newOrder.addItem(new Item(it.productId, it.quantity))) {
            System.out.print(ROSE + "Failed to copy item " + it.productId + " for reorder.\n" + RESET);
            return;
        }
    }

    if (newOrder.itemCount <= 0) {
        System.out.print(ROSE + "Original order has no valid items to reorder.\n" + RESET);
        return;
    }

    // keep reorder as pending for later processing
    newOrder.status = "PENDING";
    newOrder.cancelReason = "";
    newOrder.trackingId = "";

    // calculate total now
    newOrder.totalAmount = computeOrderTotal(newOrder);

    // NOW generate ID only after everything is valid and ready
    newOrder.orderId = dp.generateOrderId();

    // save new pending order
    dp.orders[dp.orderCount++] = newOrder;
    dp.saveOrders();

    log.write(newOrder.orderId, "Reorder created from " + oldId + " (Status: PENDING)");

    System.out.print(MINT + "Reorder created successfully! New Order ID: "
            + newOrder.orderId + " (Status: PENDING).\n" + RESET);

    if (newOrder.isSimulationOrder) {
        System.out.print(ANSI_Yellow + "This is a simulation reorder. Simulation item name and fixed price were preserved.\n" + RESET);
    }
}
    /** Feature 6 (continued): View or filter products by brand or category */
    private void handleAdvancedFilter(BufferedReader console) throws Exception {
        showProductsPreview();
        System.out.print(SOFTGRAY+"Filter by Brand or Category? (B/C): "+RESET);
        String choice = console.readLine();
        if (choice == null) choice = "";
        choice = choice.trim().toUpperCase();
        if (!choice.equals("B") && !choice.equals("C")) {
            System.out.print(ROSE+"Invalid choice."+RESET+SOFTGRAY+" Enter 'B' for Brand or 'C' for Category.\n"+RESET);
            return;
        }
        System.out.print("Enter " + (choice.equals("B") ? "Brand" : "Category") + " name: ");
        String keyword = console.readLine();
        if (keyword == null) keyword = "";
        keyword = keyword.trim();
        if (keyword.equals("")) {
            System.out.print(ROSE+"Input cannot be empty.\n"+RESET);
            return;
        }
        // Filter products by brand or category (case-insensitive substring match)
        Product[] filtered = new Product[dp.productCount];
        int count = 0;
        for (int i = 0; i < dp.productCount; i++) {
            Product p = dp.products[i];
            if (p == null) continue;
            String field = choice.equals("B") ? p.brand : p.category;
            if (field.toLowerCase().contains(keyword.toLowerCase())) {
                filtered[count++] = p;
            }
        }
        if (count == 0) {
            System.out.print(ROSE+"No products found for \"" + keyword + "\".\n"+RESET);
        }  else {
    String title =
        "Filtered Products (" +
        (choice.equals("B") ? "Brand" : "Category") +
        " contains \"" + keyword + "\")";

    printProductTable(filtered, count, title);
}
    
    }

    /** Feature 13: Display low stock items (stock < 5) highlighted in color */
    private void showLowStockAlerts() {
        boolean anyLow = false;
        System.out.print(ANSI_Yellow+"Low Stock Items (stock <=5):\n"+RESET);
        for (int i = 0; i < dp.productCount; i++) {
            Product p = dp.products[i];
            if (p == null) continue;
            if (p.stock <=5) {
                anyLow = true;
                // Highlight low stock product in yellow
                System.out.print(ANSI_Yellow+ p.productId + " | " + p.name + " | Stock: " + p.stock + RESET + "\n");
            }
        }
        if (!anyLow) {
            System.out.print(ROSE+"None (all products have sufficient stock).\n"+RESET);
        }
    }

   
   private void exportStockReport() throws Exception {
    StringBuilder json = new StringBuilder();
    json.append("[\n");

    int written = 0;
    for (int i = 0; i < dp.productCount; i++) {
        Product p = dp.products[i];
        if (p == null) continue;

        if (written > 0) json.append(",\n");

        json.append("  {\n");
        json.append("    \"productId\": ").append(q(p.productId)).append(",\n");
        json.append("    \"name\": ").append(q(p.name)).append(",\n");
        json.append("    \"price\": ").append(p.price).append(",\n");
        json.append("    \"stock\": ").append(p.stock).append("\n");
        json.append("  }");

        written++;
    }

    json.append("\n]");

    writeWholeFile("stock_report.json", json.toString());
    System.out.print(MINT + "Stock report generated in stock_report.json\n" + RESET);
}
     
private void importOrdersFromFile(BufferedReader console) throws Exception {
    printTitle("Bulk Import Orders");

    System.out.print(SOFTGRAY + "Enter import filename (orders_import.json): " + RESET);
    String fileName = console.readLine();
    if (fileName == null) fileName = "";
    fileName = fileName.trim();

    if (fileName.equals("")) {
        System.out.print(ROSE + "Filename cannot be empty.\n" + RESET);
        return;
    }

    if (!fileName.toLowerCase().endsWith(".json")) {
        System.out.print(ROSE + "Unsupported file type. Use .json only.\n" + RESET);
        return;
    }

    int importedCount = 0;
    int skippedCount = 0;
    int invalidCount = 0;

    try {
        String json = readWholeFile(fileName);

        if (json.equals("")) {
            System.out.print(ROSE + "File is empty or not found: " + fileName + "\n" + RESET);
            return;
        }

        String[] objects = splitTopLevelObjects(json);

        for (int x = 0; x < objects.length; x++) {
            try {
                if (dp.orderCount >= dp.orders.length) {
                    System.out.print(ROSE + "Order storage is full. Import stopped.\n" + RESET);
                    break;
                }

                String obj = objects[x];

                Order o = buildOrderFromJson(obj);

                if (o == null) {
                    invalidCount++;
                    continue;
                }

                // defaults if missing
                if (o.date == null || o.date.trim().equals("")) {
                    o.date = currentDateString();
                }

                if (o.address == null) o.address = "";
                o.address = capitalizeWords(o.address.trim());

                String pm = normalizePaymentMode(o.paymentMode);
                o.paymentMode = pm.equals("") ? "COD" : pm;

                if (o.status == null || o.status.trim().equals("")) {
                    o.status = "PENDING";
                }

                if (o.cancelReason == null) o.cancelReason = "";
                if (o.trackingId == null) o.trackingId = "";

                if (o.itemCount <= 0) {
                    invalidCount++;
                    continue;
                }

                // recalculate total if needed
                if (o.totalAmount <= 0) {
                    o.totalAmount = computeOrderTotal(o);
                }

                // finalize / validate order id
                String finalOrderId = normalizeOrderId(o.orderId);

                if (finalOrderId.equals("")) {
                    finalOrderId = dp.generateOrderId();
                } else {
                    if (findOrderById(finalOrderId) != null) {
                        skippedCount++;
                        continue;
                    }
                }

                o.orderId = finalOrderId;

                dp.orders[dp.orderCount++] = o;
                importedCount++;

                System.out.print(MINT + "Imported: " + o.orderId + "\n" + RESET);
                log.write("SYSTEM", "Bulk imported order " + o.orderId);

            } catch (Exception ex) {
                invalidCount++;
            }
        }

        dp.saveOrders();
        dp.refreshNextOrderNumber();

    } catch (Exception e) {
        System.out.print(ROSE + "Error reading file: " + fileName + "\n" + RESET);
    }

    printLine();
    System.out.print(MINT + importedCount + " order(s) imported from " + fileName + ".\n" + RESET);

    if (skippedCount > 0) {
        System.out.print(ANSI_Yellow + skippedCount + " duplicate order(s) skipped.\n" + RESET);
    }

    if (invalidCount > 0) {
        System.out.print(ROSE + invalidCount + " invalid record(s) skipped.\n" + RESET);
    }
}

    /** Feature 11: Simulation mode to generate and process orders in various scenarios */
 private void runSimulation(BufferedReader console) throws Exception {
    printTitle("Simulation Mode");

    System.out.print(SOFTGRAY + "Simulation scenarios:\n" + RESET);
    System.out.print(LAVENDER + "1. " + RESET + "Successful order\n");
    System.out.print(LAVENDER + "2. " + RESET + "Payment failure scenario\n");
    System.out.print(LAVENDER + "3. " + RESET + "Inventory shortage scenario\n");
    System.out.print(LAVENDER + "4. " + RESET + "Random order scenario\n");
    System.out.print(SOFTGRAY + "Choose scenario (1-4): " + RESET);

    String opt = console.readLine();
    if (opt == null) opt = "";
    opt = opt.trim();

    if (!opt.equals("1") && !opt.equals("2") && !opt.equals("3") && !opt.equals("4")) {
        System.out.print(ROSE + "Invalid scenario selection.\n" + RESET);
        return;
    }

    if (dp.orderCount >= dp.orders.length) {
        System.out.print(ROSE + "Order storage is full.\n" + RESET);
        return;
    }

    Order simOrder = new Order();
    simOrder.orderId = dp.generateOrderId();
    simOrder.date = currentDateString();
    simOrder.address = "Simulation Address";
    simOrder.status = "PENDING";

    // mark as simulation order
    simOrder.isSimulationOrder = true;
    simOrder.simulationItemName = "Simulation Item";
    simOrder.simulationItemPrice = 9999;

    // use fake item ids for simulation orders
    if (opt.equals("1")) {
        simOrder.addItem(new Item("SIM-ITEM-1", 1));
        simOrder.paymentMode = "COD";
        simOrder.totalAmount = simOrder.simulationItemPrice * 1;
        simOrder.status = "DELIVERED";
    }
    else if (opt.equals("2")) {
        simOrder.addItem(new Item("SIM-ITEM-1", 1));
        simOrder.paymentMode = "MockCard";
        simOrder.totalAmount = simOrder.simulationItemPrice * 1;
        simOrder.status = "CANCELLED";
        simOrder.cancelReason = "Payment Failure (Simulation)";
    }
    else if (opt.equals("3")) {
        simOrder.addItem(new Item("SIM-ITEM-1", 3));
        simOrder.paymentMode = "COD";
        simOrder.totalAmount = simOrder.simulationItemPrice * 3;
        simOrder.status = "CANCELLED";
        simOrder.cancelReason = "Inventory Shortage (Simulation)";
    }
    else if (opt.equals("4")) {
        simOrder.addItem(new Item("SIM-ITEM-1", 2));
        simOrder.paymentMode = "COD";
        simOrder.totalAmount = simOrder.simulationItemPrice * 2;
        simOrder.status = "PACKED";
    }

    dp.orders[dp.orderCount++] = simOrder;
    dp.saveOrders();

    log.write(simOrder.orderId, "Simulation order created with status: " + simOrder.status);

    System.out.print(MINT + "Simulation Order " + simOrder.orderId
            + " created (Status: " + simOrder.status + ").\n" + RESET);
}
    /** Feature 8: Retry processing a failed (cancelled) order by creating a fresh attempt */
 private void retryCancelledOrder(BufferedReader console) throws Exception {
    printTitle("Cancelled Orders");
    boolean found = false;

    for (int i = 0; i < dp.orderCount; i++) {
        Order o = dp.orders[i];
        if (o == null) continue;

        String status = (o.status == null) ? "" : o.status.trim();
        if ("CANCELLED".equalsIgnoreCase(status)) {
            String reason = (o.cancelReason == null || o.cancelReason.trim().equals(""))
                    ? "(No reason recorded)"
                    : o.cancelReason.trim();

            System.out.print("- " + o.orderId + " | Reason: " + reason + "\n");
            found = true;
        }
    }

    if (!found) {
        System.out.print(ROSE + "No cancelled orders to retry.\n" + RESET);
        return;
    }

    if (dp.orderCount >= dp.orders.length) {
        System.out.print(ROSE + "Order storage is full. Cannot create retry order.\n" + RESET);
        return;
    }

    System.out.print(SOFTGRAY + "Enter Cancelled Order ID to retry: " + RESET);
    String cid = console.readLine();
    if (cid == null) cid = "";
    cid = normalizeOrderId(cid);

    if (cid.equals("")) {
        System.out.print(ROSE + "Order ID cannot be empty.\n" + RESET);
        return;
    }

    Order original = findOrderById(cid);

    if (original == null) {
        System.out.print(ROSE + "Order " + cid + " not found.\n" + RESET);
        return;
    }

    String originalStatus = (original.status == null) ? "" : original.status.trim();
    if (!"CANCELLED".equalsIgnoreCase(originalStatus)) {
        System.out.print(ROSE + "Order " + cid + " is not in cancelled list.\n" + RESET);
        return;
    }

    // Create retry order first, but DO NOT generate ID yet
    Order retryOrder = new Order();
    retryOrder.date = currentDateString();
    retryOrder.address = original.address;
    retryOrder.paymentMode = (original.paymentMode == null || original.paymentMode.trim().equals(""))
            ? "COD"
            : original.paymentMode.trim();

    // preserve simulation metadata
    retryOrder.isSimulationOrder = original.isSimulationOrder;
    retryOrder.simulationItemName = original.simulationItemName;
    retryOrder.simulationItemPrice = original.simulationItemPrice;

    // copy items first; if copying fails, no order ID is lost
    for (int j = 0; j < original.itemCount; j++) {
        Item it = original.items[j];
        if (it == null) continue;

        if (!retryOrder.addItem(new Item(it.productId, it.quantity))) {
            System.out.print(ROSE + "Failed to copy item " + it.productId + " for retry order.\n" + RESET);
            return;
        }
    }

    if (retryOrder.itemCount <= 0) {
        System.out.print(ROSE + "Cancelled order has no valid items to retry.\n" + RESET);
        return;
    }

    // keep retry order pending for later processing
    retryOrder.status = "PENDING";
    retryOrder.cancelReason = "";
    retryOrder.trackingId = "";

    // compute total now
    retryOrder.totalAmount = computeOrderTotal(retryOrder);

    // NOW generate ID only after everything is valid
    retryOrder.orderId = dp.generateOrderId();

    // save retry order
    dp.orders[dp.orderCount++] = retryOrder;
    dp.saveOrders();

    log.write(retryOrder.orderId, "Retry order created from " + cid + " (Status: PENDING)");

    System.out.print(MINT + "Retry order created successfully! New Order ID: "
            + retryOrder.orderId + " (Status: PENDING).\n" + RESET);

    if (retryOrder.isSimulationOrder) {
        System.out.print(ANSI_Yellow + "This is a simulation retry order. Simulation item name and fixed price were preserved.\n" + RESET);
    }
}
    
   private void archiveDeliveredOrders(BufferedReader console) throws Exception {
    System.out.print(SOFTGRAY + "Archive delivered orders older than how many days? " + RESET);
    String daysStr = console.readLine();
    if (daysStr == null) daysStr = "";
    daysStr = daysStr.trim();

    int N = DataPersistence.toInt(daysStr);
    if (N <= 0) {
        System.out.print(ROSE + "Invalid number of days.\n" + RESET);
        return;
    }

    String todayStr = currentDateString();
    int todayCount = dateToDayCount(todayStr);

    String existing = readWholeFile("archive_orders.json");
    if (existing.equals("")) existing = "[]";
    String[] oldArchived = splitTopLevelObjects(existing);

    StringBuilder archive = new StringBuilder();
    archive.append("[\n");

    int written = 0;
    for (int i = 0; i < oldArchived.length; i++) {
        if (oldArchived[i] == null || oldArchived[i].trim().equals("")) continue;

        if (written > 0) archive.append(",\n");
        archive.append(oldArchived[i]);
        written++;
    }

    int archivedCount = 0;
    Order[] remaining = new Order[dp.orders.length];
    int remCount = 0;

    for (int i = 0; i < dp.orderCount; i++) {
        Order o = dp.orders[i];
        if (o == null) continue;

        String status = (o.status == null) ? "" : o.status.trim();

        if ("DELIVERED".equalsIgnoreCase(status)) {
            int orderDayCount = dateToDayCount(o.date);
            int age = todayCount - orderDayCount;

            if (age > N) {
                if (written > 0) archive.append(",\n");
                archive.append(orderToJson(o, "  "));
                written++;

                archivedCount++;
                log.write(o.orderId, "Archived after delivery (age " + age + " days)");
                continue;
            }
        }

        remaining[remCount++] = o;
    }

    archive.append("\n]");
    writeWholeFile("archive_orders.json", archive.toString());

    dp.orders = remaining;
    dp.orderCount = remCount;
    dp.saveOrders();

    System.out.print(MINT + "Archived " + archivedCount + " delivered orders (older than " + N + " days).\n" + RESET);
}
    /** Feature 20: Change password for the currently logged-in admin account */
    private void changeAdminPassword(BufferedReader console) throws Exception {
        System.out.print(SOFTGRAY+"Enter current password: "+RESET);
        String currentPass = console.readLine();
        if (currentPass == null) currentPass = "";
        currentPass = currentPass.trim();
        Admin admin = dp.admins[dp.currentAdminIndex];
        if (!admin.passHash.equals(Admin.hashPassword(currentPass))) {
            System.out.print(ROSE+"Current password is incorrect.\n"+RESET);
            return;
        }
        System.out.print(SOFTGRAY+"Enter new password: "+RESET);
        String newPass1 = console.readLine();
        if (newPass1 == null) newPass1 = "";
        newPass1 = newPass1.trim();
        System.out.print(SOFTGRAY+"Confirm new password: "+RESET);
        String newPass2 = console.readLine();
        if (newPass2 == null) newPass2 = "";
        newPass2 = newPass2.trim();
        if (!newPass1.equals(newPass2) || newPass1.equals("")) {
            System.out.print(ROSE+"Password mismatch or empty. Password not changed.\n"+RESET);
            return;
        }
        // Update password hash and save to file immediately
        admin.passHash = Admin.hashPassword(newPass1);
        dp.saveAll();
        log.write("ADMIN", "Password changed");
        System.out.print(MINT+"Admin password changed successfully.\n"+RESET);
    }

    
   private void clearLogs(BufferedReader console) throws Exception {
    System.out.print(ANSI_Yellow + "Are you sure you want to clear all logs? (Y/N): " + RESET);
    String confirm = console.readLine();
    if (confirm == null) confirm = "";
    confirm = confirm.trim();

    if (!confirm.equalsIgnoreCase("Y") && !confirm.equalsIgnoreCase("YES")) {
        System.out.print(ROSE + "Log clearance cancelled.\n" + RESET);
        return;
    }

    writeWholeFile("logs.json", "[]");
    System.out.print(MINT + "All logs cleared.\n" + RESET);
}

 private void generateReceipt(BufferedReader console) throws Exception {
    showOrdersPreview();

    System.out.print(SOFTGRAY + "Enter Order ID for receipt: " + RESET);
    String rid = console.readLine();
    if (rid == null) rid = "";
    rid = rid.trim();

    if (rid.equals("")) {
        System.out.print(ROSE + "Order ID cannot be empty.\n" + RESET);
        return;
    }

    Order order = findOrderById(rid);

    if (order == null) {
        System.out.print(ROSE + "Order " + normalizeOrderId(rid) + " not found.\n" + RESET);
        return;
    }

    if (order.status == null || !"DELIVERED".equalsIgnoreCase(order.status.trim())) {
        System.out.print(ROSE + "Receipt can only be generated for delivered orders.\n" + RESET);
        return;
    }

    String filename = "receipt_" + order.orderId + ".json";
    StringBuilder json = new StringBuilder();

    String addr = (order.address == null || order.address.trim().equals(""))
            ? "(Not Provided)"
            : order.address.trim();

    String tracking = (order.trackingId == null || order.trackingId.trim().equals(""))
            ? "(Not assigned)"
            : order.trackingId.trim();

    json.append("{\n");
    json.append("  \"orderId\": ").append(q(order.orderId)).append(",\n");
    json.append("  \"address\": ").append(q(addr)).append(",\n");
    json.append("  \"status\": ").append(q(order.status)).append(",\n");
    json.append("  \"trackingId\": ").append(q(tracking)).append(",\n");
    json.append("  \"paymentMode\": ").append(q(order.paymentMode == null ? "" : order.paymentMode)).append(",\n");
    json.append("  \"isSimulationOrder\": ").append(order.isSimulationOrder).append(",\n");
    json.append("  \"items\": [\n");

    int writtenItems = 0;
    for (int j = 0; j < order.itemCount; j++) {
        Item it = order.items[j];
        if (it == null) continue;

        String itemName;
        int priceEach;

        if (order.isSimulationOrder) {
            itemName = (order.simulationItemName == null || order.simulationItemName.trim().equals(""))
                    ? "Simulation Item"
                    : order.simulationItemName.trim();
            priceEach = order.simulationItemPrice;
        } else {
            Product p = dp.findProductById(it.productId);
            itemName = (p != null && p.name != null && !p.name.trim().equals(""))
                    ? p.name.trim()
                    : it.productId;
            priceEach = (p != null) ? p.price : 0;
        }

        if (writtenItems > 0) json.append(",\n");

        json.append("    {\n");
        json.append("      \"itemName\": ").append(q(itemName)).append(",\n");
        json.append("      \"quantity\": ").append(it.quantity).append(",\n");
        json.append("      \"priceEach\": ").append(priceEach).append("\n");
        json.append("    }");

        writtenItems++;
    }

    json.append("\n  ],\n");
    json.append("  \"totalPaid\": ").append(computeOrderTotal(order)).append(",\n");
    json.append("  \"message\": ").append(q("Thank you for your purchase!")).append("\n");
    json.append("}");

    writeWholeFile(filename, json.toString());

    System.out.print(MINT + "Receipt generated: " + filename + "\n" + RESET);
}
    /** Feature 14: Increase stock of an existing product (restock) */
    private void handleRestock(BufferedReader console) throws Exception {
        showProductsPreview();
        showRestockPreview();
        System.out.print(SOFTGRAY+"Enter Product ID to restock: "+RESET);
        String pid = console.readLine();
        if (pid == null) pid = "";
        pid = pid.trim();
        if (pid.equals("")) {
            System.out.print(ROSE+"Product ID cannot be empty.\n"+RESET);
            return;
        }
        Product product = dp.findProductById(pid);
        if (product == null) {
            System.out.print(ROSE+"Product " + pid + " not found.\n"+RESET);
            return;
        }
        System.out.print(SOFTGRAY+"Enter quantity to add: "+RESET);
        String qtyStr = console.readLine();
        if (qtyStr == null) qtyStr = "";
        qtyStr = qtyStr.trim();
        int addQty = DataPersistence.toInt(qtyStr);
        if (addQty <= 0) {
            System.out.print(ROSE+"Invalid quantity.\n"+RESET);
            return;
        }
        product.stock += addQty;
        System.out.print(MINT+"Product " + product.productId + " restocked. New stock: " + product.stock + "\n"+RESET);
        dp.saveProducts();
        log.write("ADMIN", "Restocked " + product.productId + " (+" + addQty + ")");
    }
    
    private String padRight(String s, int width) {
    if (s == null) s = "";
    if (s.length() >= width) return s.substring(0, width - 1) + "…";
    String out = s;
    while (out.length() < width) out += " ";
    return out;
}

private String formatMoney(int n) {
    // simple (no commas). If you want commas, tell me.
    return "BDT " + n;
}

private void printProductTable(Product[] list, int count, String title) {
    System.out.println(PINK + BOLD + "\n" + title + RESET);
    printLine();

    if (count == 0) {
        System.out.println(ROSE + "No products found." + RESET);
        printLine();
        return;
    }

    // Header
    System.out.print(LAVENDER
            + padRight("ID", 8)
            + padRight("Name", 26)
            + padRight("Brand", 14)
            + padRight("Category", 16)
            + padRight("Price", 12)
            + padRight("Stock", 8)
            + RESET + "\n");

    System.out.println(SOFTGRAY
            + "----------------------------------------------------------------------------------------"
            + RESET);

    // Rows
    for (int i = 0; i < count; i++) {
        Product p = list[i];
        if (p == null) continue;

        String stockColor = (p.stock <= 5) ? ROSE : MINT;

        System.out.print(
                SOFTGRAY + padRight(p.productId, 8) + RESET +
                padRight(p.name, 26) +
                padRight(p.brand, 14) +
                padRight(p.category, 16) +
                padRight(formatMoney(p.price), 12) +
                stockColor + padRight(String.valueOf(p.stock), 8) + RESET +
                "\n"
        );
    }

    printLine();
}
private String capitalizeWords(String text) {
    if (text == null) return "";

    text = text.trim().toLowerCase();

    String[] parts = text.split("\\s+");   // changed from " "

    StringBuilder result = new StringBuilder();

    for (int i = 0; i < parts.length; i++) {
        String word = parts[i];

        String cap = word.substring(0,1).toUpperCase() + word.substring(1);

        if (result.length() > 0) result.append(" ");
        result.append(cap);
    }

    return result.toString();
}
  /** Feature 23: Manage products (Add, Edit, Delete products) */
private void handleProductManagement(BufferedReader console) throws Exception {
    System.out.print(SOFTGRAY + "Choose action - [A]dd, [E]dit, [D]elete: " + RESET);
    String action = console.readLine();
    if (action == null) action = "";
    action = action.trim().toUpperCase();

    if (action.equals("A")) {
        // Add new product
        if (dp.productCount >= dp.products.length) {
            System.out.print(ROSE + "Product list is full, cannot add more products.\n" + RESET);
            return;
        }

        // Ask category first
        System.out.print(SOFTGRAY + "Enter Category (Smartphone/Laptop/Home Appliance/Accessories/Power Bank): " + RESET);
        String category = console.readLine();
        category = capitalizeWords(category);
        if (category == null) category = "";
        category = category.trim();

        if (category.equals("")) {
            System.out.print(ROSE + "Category cannot be empty.\n" + RESET);
            return;
        }

        // Auto-generate Product ID based on category
        String newId = dp.generateProductIdByCategory(category);

        while (dp.findProductById(newId) != null) {
            newId = dp.generateProductIdByCategory(category);
        }

        System.out.print(MINT + "Auto Generated Product ID: " + newId + "\n" + RESET);

        System.out.print(SOFTGRAY + "Enter Brand: " + RESET);
        String brand = console.readLine();
        brand = capitalizeWords(brand);
        if (brand == null) brand = "";
        brand = brand.trim();

        System.out.print(SOFTGRAY + "Enter Product Name: " + RESET);
        String name = console.readLine();
        name = capitalizeWords(name);
        if (name == null) name = "";
        name = name.trim();

        System.out.print(SOFTGRAY + "Enter Price: " + RESET);
        String priceStr = console.readLine();
        if (priceStr == null) priceStr = "";
        priceStr = priceStr.trim();

        System.out.print(SOFTGRAY + "Enter Initial Stock: " + RESET);
        String stockStr = console.readLine();
        if (stockStr == null) stockStr = "";
        stockStr = stockStr.trim();

        if (category.equals("") || brand.equals("") || name.equals("")) {
            System.out.print(ROSE + "Fields cannot be empty. Product not added.\n" + RESET);
            return;
        }

        int price = DataPersistence.toInt(priceStr);
        int stock = DataPersistence.toInt(stockStr);

        if (price <= 0) {
            System.out.print(ROSE + "Invalid price. Product not added.\n" + RESET);
            return;
        }

        if (stock < 0) {
            System.out.print(ROSE + "Invalid stock. Product not added.\n" + RESET);
            return;
        }

        dp.products[dp.productCount++] = new Product(newId, category, brand, name, price, stock);
        dp.sortProductsById();
        dp.saveProducts();

        System.out.print(MINT + "Product " + newId + " added successfully.\n" + RESET);
        log.write("ADMIN", "Added product " + newId);

    } else if (action.equals("E")) {
        // Edit existing product
        showProductsPreview();

        System.out.print(SOFTGRAY + "Enter Product ID to edit: " + RESET);
        String editId = console.readLine();
        if (editId == null) editId = "";
        editId = editId.trim();

        if (editId.equals("")) {
            System.out.print(ROSE + "Product ID cannot be empty.\n" + RESET);
            return;
        }

        Product product = dp.findProductById(editId);
        if (product == null) {
            System.out.print(ROSE + "Product " + editId + " not found.\n" + RESET);
            return;
        }

        System.out.print(SOFTGRAY + "Edit field - [N]ame, [P]rice, [S]tock: " + RESET);
        String field = console.readLine();
        if (field == null) field = "";
        field = field.trim().toUpperCase();

        if (field.equals("N")) {
            System.out.print(SOFTGRAY + "Enter new Name: " + RESET);
            String newName = console.readLine();
            newName = capitalizeWords(newName);
            if (newName == null) newName = "";
            newName = newName.trim();

            if (!newName.equals("")) {
                product.name = newName;
                dp.saveProducts();
                System.out.print(MINT + "Product " + product.productId + " name updated.\n" + RESET);
                log.write("ADMIN", "Edited product " + product.productId + " (Name changed)");
            } else {
                System.out.print(ROSE + "Name cannot be empty.\n" + RESET);
            }

        } else if (field.equals("P")) {
            System.out.print(SOFTGRAY + "Enter new Price: " + RESET);
            String newPriceStr = console.readLine();
            if (newPriceStr == null) newPriceStr = "";
            newPriceStr = newPriceStr.trim();

            int newPrice = DataPersistence.toInt(newPriceStr);
            if (newPrice > 0) {
                product.price = newPrice;
                dp.saveProducts();
                System.out.print(MINT + "Product " + product.productId + " price updated.\n" + RESET);
                log.write("ADMIN", "Edited product " + product.productId + " (Price changed)");
            } else {
                System.out.print(ROSE + "Invalid price.\n" + RESET);
            }

        } else if (field.equals("S")) {
            System.out.print(SOFTGRAY + "Enter new Stock value: " + RESET);
            String newStockStr = console.readLine();
            if (newStockStr == null) newStockStr = "";
            newStockStr = newStockStr.trim();

            int newStock = DataPersistence.toInt(newStockStr);
            if (newStock >= 0) {
                product.stock = newStock;
                dp.saveProducts();
                System.out.print(MINT + "Product " + product.productId + " stock updated.\n" + RESET);
                log.write("ADMIN", "Edited product " + product.productId + " (Stock adjusted)");
            } else {
                System.out.print(ROSE + "Invalid stock value.\n" + RESET);
            }

        } else {
            System.out.print(ROSE + "Invalid field selection.\n" + RESET);
        }

    } else if (action.equals("D")) {
        showProductsPreview();

        System.out.print(SOFTGRAY + "Enter Product ID to delete: " + RESET);
        String delId = console.readLine();
        if (delId == null) delId = "";
        delId = delId.trim();

        if (delId.equals("")) {
            System.out.print(ROSE + "Product ID cannot be empty.\n" + RESET);
            return;
        }

        int idx = -1;
        for (int i = 0; i < dp.productCount; i++) {
            if (dp.products[i] != null && dp.products[i].productId.equalsIgnoreCase(delId)) {
                idx = i;
                break;
            }
        }

        if (idx == -1) {
            System.out.print(ROSE + "Product " + delId + " not found.\n" + RESET);
            return;
        }

        System.out.print(ANSI_Yellow + "Are you sure you want to delete " + delId + "? (Y/N): " + RESET);
        String conf = console.readLine();
        if (conf == null) conf = "";
        conf = conf.trim().toUpperCase();

        if (!conf.equals("Y") && !conf.equals("YES")) {
            System.out.print(ROSE + "Deletion cancelled.\n" + RESET);
            return;
        }

        for (int j = idx; j < dp.productCount - 1; j++) {
            dp.products[j] = dp.products[j + 1];
        }

        dp.products[dp.productCount - 1] = null;
        dp.productCount--;

        dp.saveProducts();
        System.out.print(MINT + "Product " + delId + " deleted.\n" + RESET);
        log.write("ADMIN", "Deleted product " + delId);

    } else {
        System.out.print(ROSE + "Invalid action.\n" + RESET);
    }
}


 /** Process a PENDING order through inventory check, reservation, invoice generation, and payment simulation */
private boolean processPendingOrder(Order order, BufferedReader console) throws Exception {
    if (order == null || !order.status.equals("PENDING")) return false;

    boolean inventoryOK = true;

    // Step 1: Pre-check all items without modifying stock
    for (int i = 0; i < order.itemCount; i++) {
        Item it = order.items[i];
        if (it == null) continue;
        Product prod = dp.findProductById(it.productId);
        if (prod == null) {
            order.status = "CANCELLED";
            order.cancelReason = "Invalid product " + it.productId;
            log.write(order.orderId, "Order cancelled - " + order.cancelReason);
            return false;
        }
        if (prod.stock < it.quantity) {
            inventoryOK = false;
            order.cancelReason = "Inventory Shortage: " + it.productId;
            break;
        }
    }

    if (!inventoryOK) {
        order.status = "CANCELLED";
        log.write(order.orderId, "Order cancelled -" + order.cancelReason);
        return false;
    }

    // Step 2: Reserve stock
    for (int i = 0; i < order.itemCount; i++) {
        Item it = order.items[i];
        Product prod = dp.findProductById(it.productId);
        if (prod != null) {
            prod.stock -= it.quantity;
        }
    }
    log.write(order.orderId, "Inventory OK – stock reserved");

    // Step 3: Calculate total price
    int total = 0;
    for (int i = 0; i < order.itemCount; i++) {
        Item it = order.items[i];
        Product prod = dp.findProductById(it.productId);
        int price = (prod != null ? prod.price : 0);
        total += price * it.quantity;
    }
    order.totalAmount = total;

    // Step 4: Simulate payment
    boolean paymentSuccess;
    if (console == null) {
        paymentSuccess = order.paymentMode.equalsIgnoreCase("COD");
        if (order.paymentMode.equalsIgnoreCase("COD")) {
        log.write(order.orderId, "PAYMENT OK (COD)");
       } else {
        log.write(order.orderId, "PAYMENT FAIL (Auto decline for simulation)");
        paymentSuccess = false;
    }
    } else {
        paymentSuccess = paymentService.processPayment(order, console);
    }

    // Step 5: Rollback stock if payment fails
    if (!paymentSuccess) {
        for (int i = 0; i < order.itemCount; i++) {
            Item it = order.items[i];
            Product prod = dp.findProductById(it.productId);
            if (prod != null) {
                prod.stock += it.quantity;
            }
        }
        order.status = "CANCELLED";
        order.cancelReason = "Payment Declined";
        log.write(order.orderId, "Order cancelled - " + order.cancelReason);
        return false;
    }

    // Step 6: Mark as PACKED and generate properly formatted invoice
    order.status = "PACKED";
    log.write(order.orderId, "Status changed to PACKED");

       try {
        String ym = order.date.substring(0, 7).replace("-", "");
        String orderNum = order.orderId;
        if (orderNum != null && orderNum.startsWith("O")) {
            orderNum = orderNum.substring(1);
        }

        String invoiceId = "INV-" + ym + "-" + orderNum;

        String existing = readWholeFile("invoices.json");
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
        json.append("    \"invoiceId\": ").append(q(invoiceId)).append(",\n");
        json.append("    \"orderId\": ").append(q(order.orderId)).append(",\n");
        json.append("    \"totalAmount\": ").append(order.totalAmount).append(",\n");
        json.append("    \"generatedAt\": ").append(q(dp.currentDateTimeString())).append("\n");
        json.append("  }\n");

        json.append("]");

        writeWholeFile("invoices.json", json.toString());

        System.out.print("Invoice generated: " + invoiceId + "\n");
    } catch (Exception e) {
        // Ignore invoice errors silently
    }

    return true;
}

    /** View detailed information of an order (internal helper) */
  private void viewOrderDetails(Order order) {
    if (order == null) {
        System.out.print(ROSE + "Order details not available.\n" + RESET);
        return;
    }

    String orderId = (order.orderId == null || order.orderId.trim().equals("")) ? "(Unknown)" : order.orderId.trim();
    String date = (order.date == null || order.date.trim().equals("")) ? "(Unknown)" : order.date.trim();
    String status = (order.status == null || order.status.trim().equals("")) ? "(Unknown)" : order.status.trim();
    String cancelReason = (order.cancelReason == null || order.cancelReason.trim().equals("")) ? "(None)" : order.cancelReason.trim();
    String trackingId = (order.trackingId == null || order.trackingId.trim().equals("")) ? "" : order.trackingId.trim();
    String address = (order.address == null || order.address.trim().equals("")) ? "(Not provided)" : order.address.trim();
    String paymentMode = (order.paymentMode == null || order.paymentMode.trim().equals("")) ? "(N/A)" : order.paymentMode.trim();

    System.out.print(LAVENDER + "Order ID: " + orderId + "\n" + RESET);
    System.out.print(LAVENDER + "Date: " + date + "\n" + RESET);
    System.out.print(LAVENDER + "Status: " + status + "\n" + RESET);

    if ("CANCELLED".equalsIgnoreCase(status)) {
        System.out.print(ROSE + "Cancel Reason: " + cancelReason + "\n" + RESET);
    }

    if (!trackingId.equals("")) {
        System.out.print(LAVENDER + "Tracking ID: " + trackingId + "\n" + RESET);
    }

    if (order.isSimulationOrder) {
        System.out.print(ANSI_Yellow + "Order Type: Simulation Order\n" + RESET);
    }

    System.out.print(LAVENDER + "Address: " + address + "\n" + RESET);
    System.out.print(LAVENDER + "Payment Mode: " + paymentMode + "\n" + RESET);
    System.out.print(LAVENDER + "Total Amount: BDT " + computeOrderTotal(order) + "\n" + RESET);
    System.out.print("Items:\n");

    boolean hasItems = false;
    for (int i = 0; i < order.itemCount; i++) {
        Item it = order.items[i];
        if (it == null) continue;

        hasItems = true;

        String itemName;
        int itemPrice;

        if (order.isSimulationOrder) {
            itemName = (order.simulationItemName == null || order.simulationItemName.trim().equals(""))
                    ? "Simulation Item"
                    : order.simulationItemName.trim();
            itemPrice = order.simulationItemPrice;
        } else {
            Product p = dp.findProductById(it.productId);
            itemName = (p != null && p.name != null && !p.name.trim().equals(""))
                    ? p.name.trim()
                    : it.productId;
            itemPrice = (p != null) ? p.price : 0;
        }

        System.out.print(LAVENDER + "- " + itemName + " (x" + it.quantity + ", BDT " + itemPrice + " each)\n" + RESET);
    }

    if (!hasItems) {
        System.out.print(ROSE + "- No items found\n" + RESET);
    }
}

  
    private void generateReport() throws Exception {
    int totalOrders = dp.orderCount;
    int completedCount = 0;
    int cancelledCount = 0;
    int revenueSum = 0;

    String[] reasons = new String[totalOrders];
    int[] reasonCounts = new int[totalOrders];
    int reasonTypes = 0;

    for (int i = 0; i < dp.orderCount; i++) {
        Order o = dp.orders[i];
        if (o == null) continue;

        if (o.status.equalsIgnoreCase("DELIVERED")) {
            completedCount++;
            revenueSum += o.totalAmount;
        }

        if (o.status.equalsIgnoreCase("CANCELLED")) {
            cancelledCount++;
            String reason = (o.cancelReason == null || o.cancelReason.equals("") ? "Unknown" : o.cancelReason);

            boolean found = false;
            for (int r = 0; r < reasonTypes; r++) {
                if (reasons[r].equalsIgnoreCase(reason)) {
                    reasonCounts[r]++;
                    found = true;
                    break;
                }
            }

            if (!found) {
                reasons[reasonTypes] = reason;
                reasonCounts[reasonTypes] = 1;
                reasonTypes++;
            }
        }
    }

    for (int i = 0; i < reasonTypes - 1; i++) {
        int maxIndex = i;
        for (int j = i + 1; j < reasonTypes; j++) {
            if (reasonCounts[j] > reasonCounts[maxIndex]) {
                maxIndex = j;
            }
        }

        String tempReason = reasons[i];
        reasons[i] = reasons[maxIndex];
        reasons[maxIndex] = tempReason;

        int tempCount = reasonCounts[i];
        reasonCounts[i] = reasonCounts[maxIndex];
        reasonCounts[maxIndex] = tempCount;
    }

    StringBuilder json = new StringBuilder();
    json.append("{\n");
    json.append("  \"totalOrders\": ").append(totalOrders).append(",\n");
    json.append("  \"completedOrders\": ").append(completedCount).append(",\n");
    json.append("  \"cancelledOrders\": ").append(cancelledCount).append(",\n");
    json.append("  \"totalRevenue\": ").append(revenueSum).append(",\n");
    json.append("  \"topCancellationReasons\": [\n");

    int top = (reasonTypes < 3) ? reasonTypes : 3;
    for (int k = 0; k < top; k++) {
        if (k > 0) json.append(",\n");

        json.append("    {\n");
        json.append("      \"reason\": ").append(q(reasons[k])).append(",\n");
        json.append("      \"count\": ").append(reasonCounts[k]).append("\n");
        json.append("    }");
    }

    json.append("\n  ]\n");
    json.append("}");

    writeWholeFile("report.json", json.toString());

    printTitle("Report Summary");
    System.out.print("Total Orders: " + totalOrders + "\n");
    System.out.print("Completed Orders: " + completedCount + "\n");
    System.out.print("Cancelled Orders: " + cancelledCount + "\n");
    System.out.print("Total Revenue: BDT " + revenueSum + "\n");
    System.out.print("Top 3 Cancellation Reasons:\n");

    for (int k = 0; k < top; k++) {
        System.out.print((k + 1) + ". " + reasons[k] + " – " + reasonCounts[k] + "\n");
    }

    System.out.print(MINT + "(Full report saved to report.json)\n" + RESET);
}

    /** Helper: normalize input to full Order ID format (e.g., add 'O' prefix if missing) */
private String normalizeOrderId(String input) {
    if (input == null) return "";

    String s = input.trim();

    // Remove optional O prefix if user types it
    if (!s.equals("") && (s.charAt(0) == 'O' || s.charAt(0) == 'o')) {
        s = s.substring(1).trim();
    }

    // If numeric, pad to 5 digits (01001 style)
    if (isNumeric(s)) {
        while (s.length() < 5) {
            s = "0" + s;
        }
    }

    return s;
}

    /** Helper: check if a string is numeric */
    private boolean isNumeric(String s) {
        if (s == null) return false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c < '0' || c > '9') return false;
        }
        return s.length() > 0;
    }

    /** Helper: get current date as YYYY-MM-DD */
  private String currentDateString() {
    int[] parts = getCurrentDateParts();
    return parts[0] + "-" + twoDigits(parts[1]) + "-" + twoDigits(parts[2]);
}

    /** Helper: convert a YYYY-MM-DD date string to an approximate day count for comparison */
    private int dateToDayCount(String dateStr) {
        if (dateStr == null || dateStr.length() == 0) return 0;
        String[] parts = dateStr.split("-");
        if (parts.length < 3) return 0;
        int y = DataPersistence.toInt(parts[0]);
        int m = DataPersistence.toInt(parts[1]);
        int d = DataPersistence.toInt(parts[2]);
        // approximate: year*360 + month*30 + day
        return y * 360 + m * 30 + d;
    }

    /** 
 * Accept a new order from the admin by manually inputting order details.
 * This will generate a new Order ID, collect product selections, and process the order.
 */
private String normalizePaymentMode(String input) {
    if (input == null) return "";

    input = input.trim().toLowerCase();

    if (input.equals("cod")) return "COD";
    if (input.equals("mockcard")) return "MockCard";

    return "";
}
private void acceptNewOrder(BufferedReader console) throws Exception {
    // 1. Display product catalog first
    printTitle("Product Catalog");

    System.out.printf(
    LAVENDER + "%-8s %-18s %-15s %-28s %-14s %-10s%n" + RESET,
    "ProdID", "Category", "Brand", "Name", "Price", "Stock"
);

    System.out.println(
        SOFTGRAY + "---------------------------------------------------------------------------------------" + RESET
    );

    for (int i = 0; i < dp.productCount; i++) {
        Product prod = dp.products[i];
        if (prod == null) continue;

        String category = (prod.category == null) ? "" : prod.category;
        String brand = (prod.brand == null) ? "" : prod.brand;
        String name = (prod.name == null) ? "" : prod.name;

        String stockColor;
        if (prod.stock <= 0) {
            stockColor = ROSE;
        } else if (prod.stock <= 5) {
            stockColor = ANSI_Yellow;
        } else {
            stockColor = MINT;
        }

      System.out.printf(
    "%-8s %-18s %-15s %-28s %-14s %s%-10d%s%n",
    prod.productId,
    trimTo(category, 18),
    trimTo(brand, 15),
    trimTo(name, 28),
    formatMoney(prod.price),
    stockColor,
    prod.stock,
    RESET
);
    }

    printLine();

    // 2. Ask number of products until valid
    int itemCount;
    while (true) {
        System.out.print(SOFTGRAY + "How many different products in this order? (1-10): " + RESET);
        String countStr = console.readLine();
        if (countStr == null) countStr = "";
        countStr = countStr.trim();

        itemCount = DataPersistence.toInt(countStr);

        if (itemCount >= 1 && itemCount <= 10) {
            break;
        }

        System.out.print(ROSE + "Invalid number. Please enter a value from 1 to 10.\n" + RESET);
    }

    // 3. Temporarily store items first
    Item[] tempItems = new Item[10];

    for (int i = 1; i <= itemCount; i++) {
        Product product;

        // Re-ask Product ID until valid
        while (true) {
            System.out.print(SOFTGRAY + "Enter Product ID for item " + i + ": " + RESET);
            String pid = console.readLine();
            if (pid == null) pid = "";
            pid = pid.trim();

            if (pid.equals("")) {
                System.out.print(ROSE + "Product ID cannot be empty. Try again.\n" + RESET);
                continue;
            }

            product = dp.findProductById(pid);
            if (product == null) {
                System.out.print(ROSE + "Product " + pid + " not found. Try again.\n" + RESET);
                continue;
            }

            // Optional: prevent duplicate product in same order
            boolean duplicate = false;
            for (int j = 0; j < i - 1; j++) {
                if (tempItems[j] != null &&
                    tempItems[j].productId.equalsIgnoreCase(product.productId)) {
                    duplicate = true;
                    break;
                }
            }

            if (duplicate) {
                System.out.print(ROSE + "This product is already added in the order. Choose a different product.\n" + RESET);
                continue;
            }

            break;
        }

        int qty;

        // Re-ask quantity until valid
        while (true) {
            System.out.print(SOFTGRAY + "Enter quantity for " + product.name + ": " + RESET);
            String qtyStr = console.readLine();
            if (qtyStr == null) qtyStr = "";
            qtyStr = qtyStr.trim();

            qty = DataPersistence.toInt(qtyStr);

            if (qty > 0) {
                break;
            }

            System.out.print(ROSE + "Invalid quantity. Please enter a positive number.\n" + RESET);
        }

        tempItems[i - 1] = new Item(product.productId, qty);
    }

    // 4. Re-ask shipping address until valid
    String address;
    while (true) {
        System.out.print(SOFTGRAY + "Enter shipping address: " + RESET);
        address = console.readLine();
        if (address == null) address = "";
        address = capitalizeWords(address.trim());

        if (!address.equals("")) {
            break;
        }

        System.out.print(ROSE + "Address cannot be empty. Try again.\n" + RESET);
    }

    // 5. Re-ask payment mode until valid
    String paymentMode;
    while (true) {
        System.out.print(SOFTGRAY + "Enter payment mode (COD or MockCard): " + RESET);
        paymentMode = console.readLine();
        paymentMode = normalizePaymentMode(paymentMode);

        if (!paymentMode.equals("")) {
            break;
        }

        System.out.print(ROSE + "Invalid payment mode. Use COD or MockCard only.\n" + RESET);
    }

    // 6. NOW create order and generate ID only after all inputs are valid
    Order newOrder = new Order();
    newOrder.orderId = dp.generateOrderId();
    newOrder.date = currentDateString();
    newOrder.status = "PENDING";
    newOrder.address = address;
    newOrder.paymentMode = paymentMode;

    for (int i = 0; i < itemCount; i++) {
        if (tempItems[i] != null) {
            if (!newOrder.addItem(tempItems[i])) {
                System.out.print(ROSE + "Failed to add item " + tempItems[i].productId + ". Order cancelled.\n" + RESET);
                return;
            }
        }
    }

    System.out.print(LAVENDER + "New Order ID: " + newOrder.orderId + "\n" + RESET);

    // 7. Show order summary before saving
    printTitle("Order Summary");
    for (int i = 0; i < newOrder.itemCount; i++) {
        Item it = newOrder.items[i];
        if (it == null) continue;

        Product p = dp.findProductById(it.productId);
        String itemName = (p != null ? p.name : it.productId);

        System.out.print(LAVENDER + "- " + RESET + itemName + " x" + it.quantity + "\n");
    }

    System.out.print(SOFTGRAY + "Address: " + RESET + newOrder.address + "\n");
    System.out.print(SOFTGRAY + "Payment: " + RESET + newOrder.paymentMode + "\n");
    System.out.print(SOFTGRAY + "Initial Status: " + RESET + ANSI_Yellow + newOrder.status + RESET + "\n");
    printLine();

    // 8. Save as PENDING
    dp.orders[dp.orderCount++] = newOrder;
    dp.saveOrders();

    // 9. Log creation
    log.write(newOrder.orderId, "Order created via admin interface (PENDING)");

    // 10. Final message
    System.out.print(MINT + "New order created successfully.\n" + RESET);
    System.out.print(SOFTGRAY + "Order ID: " + RESET + newOrder.orderId + "\n");
    System.out.print(SOFTGRAY + "Status: " + RESET + ANSI_Yellow + newOrder.status + RESET + "\n");
    System.out.print(ANSI_Yellow + "This order will remain PENDING until processed from 'Update Order Status'.\n" + RESET);
}
private void systemHealthCheck() {
    System.out.print(PINK + BOLD + "System Health Check\n" + RESET);
    printLine();

    System.out.print(MINT + "Orders loaded: " + RESET + dp.orderCount + "\n");
    System.out.print(MINT + "Products loaded: " + RESET + dp.productCount + "\n");
    System.out.print(MINT + "Admins loaded: " + RESET + dp.adminCount + "\n");

    int low = countLowStock(5);
    if (low > 0) {
        System.out.print(ANSI_Yellow + "Low stock products: " + low + RESET + "\n");
    } else {
        System.out.print(MINT + "No low stock products" + RESET + "\n");
    }

    printLine();
}
private void autoCancelStaleOrders(int days) throws Exception {
    int todayCount = dateToDayCount(currentDateString());
    int cancelled = 0;

    for (int i = 0; i < dp.orderCount; i++) {
        Order o = dp.orders[i];
        if (o == null) continue;
        if (!"PENDING".equals(o.status)) continue;
        if (o.date == null || o.date.trim().equals("")) continue;

        int orderDayCount = dateToDayCount(o.date.trim());
        if (orderDayCount == 0) continue;

        int diff = todayCount - orderDayCount;

        if (diff >= days) {
            o.status = "CANCELLED";
            o.cancelReason = "Auto-cancelled after " + diff + " day(s) in PENDING status";
            cancelled++;

            System.out.print(
                ROSE + "Cancelled Order: " + o.orderId +
                " (Pending for " + diff + " days)\n" + RESET
            );

            log.write("SYSTEM", "Auto-cancelled order " + o.orderId +
                    " after " + diff + " day(s) in PENDING status");
        }
    }

    if (cancelled > 0) {
        dp.saveAll();
        System.out.print(MINT + "Auto-cancelled " + cancelled +
                " stale PENDING orders.\n" + RESET);
    } else {
        System.out.print(ROSE + "No stale PENDING orders found.\n" + RESET);
    }
}
private void showRecentlyAutoCancelledOrders() throws Exception {
    printTitle("Recently Auto-Cancelled Orders");

    String json = readWholeFile("logs.json");
    if (json.equals("")) {
        System.out.print(ROSE + "No auto-cancelled orders found in logs.\n" + RESET);
        printLine();
        return;
    }

    String[] objects = splitTopLevelObjects(json);
    boolean found = false;

    for (int i = 0; i < objects.length; i++) {
        String message = extractJsonString(objects[i], "message");

        if (message.contains("Auto-cancelled order")) {
            System.out.print(ROSE + message + "\n" + RESET);
            found = true;
        }
    }

    if (!found) {
        System.out.print(ROSE + "No auto-cancelled orders found in logs.\n" + RESET);
    }

    printLine();
}
private void showOrdersForStatusUpdate() {
    System.out.println(PINK + BOLD + "\nOrders List (Choose an Order ID)" + RESET);
    printLine();

    if (dp.orderCount == 0) {
        System.out.println(ROSE+ "No orders found." + RESET);
        printLine();
        return;
    }

    // Table header
    System.out.printf(LAVENDER + "%-10s %-12s %-18s %-10s %-10s" + RESET + "%n",
            "OrderID", "Date", "Status", "Payment", "Total");
    System.out.println(SOFTGRAY + "--------------------------------------------------------------" + RESET);

    // Rows
    for (int i = 0; i < dp.orderCount; i++) {
        Order o = dp.orders[i];
        if (o == null) continue;

        String st = o.status == null ? "" : o.status.trim();

        // Color status
        String stColor = SOFTGRAY;
        if (st.equals("PENDING")) stColor = ANSI_MUTED_PEACH;            // warning
        else if (st.equals("OUT_FOR_DELIVERY")) stColor = ANSI_SOFT_CORAL;        // 🚚 on the way (attention)
        else if (st.equals("DELIVERED")) stColor = MINT; // success
        else if (st.equals("CANCELLED")) stColor = ROSE;     // warning
        else stColor = SOFTGRAY;

        int total = o.totalAmount;

        System.out.printf("%-10s %-12s %s%-18s%s %-10s %-10d%n",
                o.orderId,
                o.date,
                stColor, st, RESET,
                (o.paymentMode == null ? "" : o.paymentMode),
                total
        );
    }

    printLine();
}
private int computeOrderTotal(Order order) {

    if (order == null) return 0;

    int total = 0;

    for (int i = 0; i < order.itemCount; i++) {

        Item it = order.items[i];
        if (it == null) continue;

        int price = 0;

        // simulation order
        if (order.isSimulationOrder) {

            price = order.simulationItemPrice;

        } else {

            Product p = dp.findProductById(it.productId);

            if (p != null) {
                price = p.price;
            }
        }

        total += price * it.quantity;
    }

    return total;
}

private void showOrdersPreview() {
    System.out.println(PINK + BOLD + "\nOrders List (Preview)" + RESET);
    printLine();

    if (dp.orderCount == 0) {
        System.out.println(ROSE + "No orders available." + RESET);
        printLine();
        return;
    }

    // Header
    System.out.printf(LAVENDER + "%-10s %-12s %-18s %-10s %-10s" + RESET + "%n",
            "OrderID", "Date", "Status", "Payment", "Total");
    System.out.println(SOFTGRAY + "--------------------------------------------------------------" + RESET);

    // Rows
    for (int i = 0; i < dp.orderCount; i++) {
        Order o = dp.orders[i];
        if (o == null) continue;

        String st = (o.status == null) ? "" : o.status.trim();

        // Color by status
        String stColor = SOFTGRAY;
        if (st.equals("PENDING")) stColor = MINT;
        else if (st.equals("PACKED")) stColor = MINT;
        else if (st.equals("SHIPPED")) stColor = MINT;
        else if (st.equals("OUT_FOR_DELIVERY")) stColor = ANSI_SOFT_CORAL;
        else if (st.equals("DELIVERED")) stColor = MINT;
        else if (st.equals("CANCELLED")) stColor = ROSE;
        else stColor = SOFTGRAY;

        // ✅ FIX TOTAL: if stored total is 0, compute from items
        int total = o.totalAmount;
        if (total <= 0) {
            total = computeOrderTotal(o);
        }

        System.out.printf("%-10s %-12s %s%-18s%s %-10s %-10d%n",
                o.orderId,
                o.date,
                stColor, st, RESET,
                (o.paymentMode == null ? "" : o.paymentMode),
                total
        );
    }

    printLine();
}
private void showProductsPreview() {
    System.out.println(PINK + BOLD + "\nProducts List (Preview)" + RESET);
    printLine();

    if (dp.productCount == 0) {
        System.out.println(ROSE + "No products available." + RESET);
        printLine();
        return;
    }

    // Header
    System.out.printf(
        LAVENDER + "%-8s %-16s %-14s %-30s %-14s %-8s" + RESET + "%n",
        "ProdID", "Category", "Brand", "Name", "Price", "Stock"
    );

    System.out.println(
        SOFTGRAY +
        "------------------------------------------------------------------------------------------------" +
        RESET
    );

    // Rows
    for (int i = 0; i < dp.productCount; i++) {
        Product p = dp.products[i];
        if (p == null) continue;

        String stockColor = p.stock <= 5 ? ROSE : MINT;

        System.out.printf(
            "%-8s %-16s %-14s %-30s %-14s %s%-8d%s%n",
            p.productId,
            p.category,
            p.brand,
            p.name,
            formatMoney(p.price),
            stockColor,
            p.stock,
            RESET
        );
    }

    printLine();
}

private void printProductSummary() {
    int totalProducts = dp.productCount;
    int inStock = 0;
    int lowStock = 0;
    int outOfStock = 0;

    for (int i = 0; i < dp.productCount; i++) {
        Product p = dp.products[i];
        if (p == null) continue;

        if (p.stock <= 0) outOfStock++;
        else if (p.stock <= 5) lowStock++;
        else inStock++;
    }
    printTitle("Product Summary");
    System.out.println(SOFTGRAY + "Total Products: " + RESET + MINT + totalProducts + RESET);
    System.out.println(SOFTGRAY + "In Stock: " + RESET + MINT + inStock + RESET);
    System.out.println(SOFTGRAY + "Low Stock (<=5): " + RESET + ANSI_Yellow + lowStock + RESET);
    System.out.println(SOFTGRAY + "Out of Stock: " + RESET + ROSE + outOfStock + RESET);
    printLine();
}
private void showRestockPreview() {
    System.out.println(PINK + BOLD + "\nProducts Needing Restock" + RESET);
    printLine();

    System.out.printf(LAVENDER + "%-8s %-22s %-12s %-10s %-8s" + RESET + "%n",
            "ID", "Name", "Brand", "Category", "Stock");
    System.out.println(SOFTGRAY + "------------------------------------------------------------" + RESET);

    int lowCount = 0;
    int threshold = 5; // ✅ change this if you want (ex: 10)

    for (int i = 0; i < dp.productCount; i++) {
        Product p = dp.products[i];
        if (p == null) continue;

        if (p.stock <= threshold) {
            lowCount++;

            String stockColor = (p.stock == 0) ? ROSE : ANSI_Yellow; // 0 = red, low = peach

            System.out.printf("%-8s %-22s %-12s %-10s %s%-8d%s%n",
                    p.productId,
                    trimTo(p.name, 22),
                    trimTo(p.brand, 12),
                    trimTo(p.category, 10),
                    stockColor, p.stock, RESET
            );
        }
    }

    if (lowCount == 0) {
        System.out.println(MINT + "No products are low in stock right now." + RESET);
    }

    printLine();
}

// small helper so long names don't break your table
private String trimTo(String s, int max) {
    if (s == null) return "";
    s = s.trim();
    if (s.length() <= max) return s;
    return s.substring(0, max - 3) + "...";
}
private void showReorderPreview() {
    System.out.println(PINK + BOLD + "\nPrevious Orders (For Reorder)" + RESET);
    printLine();

    if (dp.orderCount == 0) {
        System.out.println(ROSE + "No orders found." + RESET);
        printLine();
        return;
    }

    System.out.printf(
        LAVENDER + "%-10s %-12s %-18s %-10s %-10s %-40s" + RESET + "%n",
        "OrderID", "Date", "Status", "Payment", "Total", "Items (Qty)"
    );

    System.out.println(
        SOFTGRAY + "--------------------------------------------------------------------------------------------------------------" + RESET
    );

    for (int i = 0; i < dp.orderCount; i++) {
        Order o = dp.orders[i];
        if (o == null) continue;

        // Skip cancelled orders
        if (o.status != null && o.status.equalsIgnoreCase("CANCELLED")) continue;

        String st = (o.status == null) ? "" : o.status.trim().toUpperCase();
        String stColor = SOFTGRAY;

        if (st.equals("PENDING")) stColor = MINT;
        else if (st.equals("PACKED")) stColor = MINT;
        else if (st.equals("SHIPPED")) stColor = MINT;
        else if (st.equals("OUT_FOR_DELIVERY")) stColor = MINT;
        else if (st.equals("DELIVERED")) stColor = MINT;

        String itemsSummary = buildOrderItemsSummary(o);

        // Optional truncation so the table does not break badly
        if (itemsSummary.length() > 40) {
            itemsSummary = itemsSummary.substring(0, 37) + "...";
        }

        System.out.printf(
            "%-10s %-12s %s%-18s%s %-10s %-10d %-40s%n",
            o.orderId,
            o.date,
            stColor, st, RESET,
            (o.paymentMode == null ? "" : o.paymentMode),
            o.totalAmount,
            itemsSummary
        );
    }

    printLine();
}private String buildOrderItemsSummary(Order order) {

    if (order == null || order.itemCount == 0) {
        return "No items";
    }

    String summary = "";

    for (int i = 0; i < order.itemCount; i++) {

        Item it = order.items[i];
        if (it == null) continue;

        String itemName;

        // If simulation order, use simulation item name
        if (order.isSimulationOrder) {

            if (order.simulationItemName == null || order.simulationItemName.trim().equals("")) {
                itemName = "Simulation Item";
            } else {
                itemName = order.simulationItemName.trim();
            }

        } else {

            // fallback if product not found
            itemName = it.productId;

            // search product name
            for (int j = 0; j < dp.productCount; j++) {

                Product p = dp.products[j];

                if (p != null && p.productId.equalsIgnoreCase(it.productId)) {

                    if (p.name != null && !p.name.trim().equals("")) {
                        itemName = p.name.trim();
                    }

                    break;
                }
            }
        }

        if (!summary.equals("")) {
            summary += ", ";
        }

        summary += itemName + " x" + it.quantity;
    }

    return summary;
}
private void showTimelinePreview() {
    System.out.print(PINK + "\nOrders Available for Timeline\n" + RESET);
    printLine();

    if (dp.orderCount == 0) {
        System.out.print(ROSE + "No orders found.\n" + RESET);
        printLine();
        return;
    }

    System.out.printf(LAVENDER + "%-8s %-12s %-18s\n" + RESET,
            "OrderID", "Date", "Status");
    System.out.print(SOFTGRAY + "----------------------------------------\n" + RESET);

    for (int i = 0; i < dp.orderCount; i++) {
        Order o = dp.orders[i];
        if (o == null) continue;

        System.out.printf("%-8s %-12s %-18s\n",
                o.orderId,
                o.date,
                o.status
        );
    }

    printLine();
}
private void deleteAllOrderHistory(BufferedReader console) throws Exception {
    Admin currentAdmin = dp.admins[dp.currentAdminIndex];

    if (currentAdmin == null || currentAdmin.role != Role.ADMIN) {
        System.out.print(ROSE + "Access denied. Admin only.\n" + RESET);
        return;
    }

    if (dp.orderCount == 0) {
        System.out.print(SOFTGRAY + "No orders to delete.\n" + RESET);
        return;
    }

    System.out.print(ROSE + "WARNING: This will delete ALL order history.\n" + RESET);
    System.out.print(SOFTGRAY + "Type DELETE to confirm: " + RESET);
    String conf1 = console.readLine();
    if (conf1 == null) conf1 = "";
    conf1 = conf1.trim();

    if (!conf1.equalsIgnoreCase("DELETE")) {
        System.out.print(ROSE + "Cancelled.\n" + RESET);
        return;
    }

    System.out.print(SOFTGRAY + "Are you REALLY sure? Type YES: " + RESET);
    String conf2 = console.readLine();
    if (conf2 == null) conf2 = "";
    conf2 = conf2.trim();

    if (!conf2.equalsIgnoreCase("YES")) {
        System.out.print(ROSE + "Cancelled.\n" + RESET);
        return;
    }

    int deletedCount = dp.orderCount;

    String archiveFileName = "orders_archive.json";
    String archivePath = dp.path(archiveFileName);

    String existingSessions = readWholeFile(archiveFileName);
    if (existingSessions.equals("")) existingSessions = "[]";
    String[] oldSessions = splitTopLevelObjects(existingSessions);

    String deletedAt = dp.currentDateTimeString();

    StringBuilder archive = new StringBuilder();
    archive.append("[\n");

    int written = 0;
    for (int i = 0; i < oldSessions.length; i++) {
        if (oldSessions[i] == null || oldSessions[i].trim().equals("")) continue;

        if (written > 0) archive.append(",\n");
        archive.append(oldSessions[i]);
        written++;
    }

    if (written > 0) archive.append(",\n");

    archive.append("  {\n");
    archive.append("    \"deletedBy\": ").append(q(currentAdmin.username)).append(",\n");
    archive.append("    \"deletedAt\": ").append(q(deletedAt)).append(",\n");
    archive.append("    \"orders\": ").append(ordersToJson(dp.orders, dp.orderCount, "    ")).append("\n");
    archive.append("  }\n");
    archive.append("]");

    writeWholeFile(archiveFileName, archive.toString());

    for (int i = 0; i < dp.orderCount; i++) {
        dp.orders[i] = null;
    }
    dp.orderCount = 0;

    dp.saveOrders();

    int[] receiptResult = deleteAllReceiptFiles();
    int receiptDeleted = receiptResult[0];
    int receiptFailed = receiptResult[1];

    long sizeBytes = 0;
    try {
        java.io.File f = new java.io.File(archivePath);
        if (f.exists()) sizeBytes = f.length();
    } catch (Exception e) {}

    long sizeKB = (sizeBytes + 1023) / 1024;

    log.write(
        "ADMIN",
        "Deleted ALL order history. Orders=" + deletedCount +
        ", Receipts=" + receiptDeleted +
        ", Backup=" + archiveFileName
    );
    dp.appendLoginAudit("DELETE_ORDERS", currentAdmin.username);

    String lastAuditLine = readLastAuditSummary();

    System.out.print(MINT + "All order history deleted successfully.\n" + RESET);
    System.out.print(SOFTGRAY + "Deleted Orders: " + RESET + MINT + deletedCount + RESET + "\n");
    System.out.print(SOFTGRAY + "Receipts Deleted: " + RESET + LAVENDER + receiptDeleted + RESET + "\n");

    if (receiptFailed > 0) {
        System.out.print(ROSE + "Receipts Failed: " + receiptFailed + RESET + "\n");
    }

    System.out.print(SOFTGRAY + "Backup File: " + RESET + LAVENDER + archiveFileName + RESET + "\n");
    System.out.print(SOFTGRAY + "Archive Size: " + RESET + LAVENDER + sizeKB + " KB" + RESET + "\n");

    if (lastAuditLine != null && !lastAuditLine.equals("")) {
        System.out.print(SOFTGRAY + "Last Audit: " + RESET + lastAuditLine + "\n");
    }
}

private int[] deleteAllReceiptFiles() {
    int deleted = 0;
    int failed = 0;

    try {
        java.io.File ordersFile = new java.io.File(dp.path("orders.json"));
        java.io.File dir = ordersFile.getParentFile();

        if (dir == null) dir = new java.io.File(".");

        java.io.File[] files = dir.listFiles();
        if (files == null) return new int[]{0, 0};

        for (int i = 0; i < files.length; i++) {
            java.io.File f = files[i];
            if (f == null) continue;

            String name = f.getName();
            if (name == null) continue;

            if (name.startsWith("receipt_") && name.endsWith(".json")) {
                boolean ok = false;
                try {
                    ok = f.delete();
                } catch (Exception ex) {
                    ok = false;
                }

                if (ok) deleted++;
                else failed++;
            }
        }
    } catch (Exception e) {
        // ignore
    }

    return new int[]{deleted, failed};
}
private void restoreOrdersFromArchive(BufferedReader console) throws Exception {
    Admin currentAdmin = dp.admins[dp.currentAdminIndex];
    if (currentAdmin == null || currentAdmin.role != Role.ADMIN) {
        System.out.print(ROSE + "Access denied. Admin only.\n" + RESET);
        return;
    }

    java.io.File archiveFile = new java.io.File(dp.path("orders_archive.json"));
    if (!archiveFile.exists()) {
        System.out.print(ROSE + "Archive file not found: orders_archive.json\n" + RESET);
        return;
    }

    previewArchiveSessions();

    System.out.print(SOFTGRAY +
        "Restore Options:\n" + RESET +
        "1) Restore latest delete only\n" +
        "2) Restore by date (YYYY-MM-DD)\n" +
        "3) Restore ALL archive orders\n" +
        "Choose (1-3): "
    );

    String opt = console.readLine();
    if (opt == null) opt = "";
    opt = opt.trim();

    if (!opt.equals("1") && !opt.equals("2") && !opt.equals("3")) {
        System.out.print(ROSE + "Invalid option.\n" + RESET);
        return;
    }

    String dateFilter = "";
    if (opt.equals("2")) {
        System.out.print(SOFTGRAY + "Enter date (YYYY-MM-DD): " + RESET);
        dateFilter = console.readLine();
        if (dateFilter == null) dateFilter = "";
        dateFilter = dateFilter.trim();

        if (dateFilter.length() != 10) {
            System.out.print(ROSE + "Invalid date format.\n" + RESET);
            return;
        }
    }

    System.out.print(ROSE + "This will modify orders.json.\n" + RESET);
    System.out.print(SOFTGRAY + "Type RESTORE to confirm: " + RESET);
    String conf = console.readLine();
    if (conf == null) conf = "";
    conf = conf.trim();

    if (!conf.equalsIgnoreCase("RESTORE")) {
        System.out.print(ROSE + "Restore cancelled.\n" + RESET);
        return;
    }

    backupOrdersBeforeRestore();

    int restoredCount = 0;
    if (opt.equals("1")) restoredCount = restoreLatestSessionOnly();
    else if (opt.equals("2")) restoredCount = restoreByDate(dateFilter);
    else restoredCount = restoreAllArchiveOrders();

    dp.saveOrders();
    dp.loadAll();

    log.write("ADMIN", "RESTORE from archive. Count=" + restoredCount);
    dp.appendLoginAudit("RESTORE_ORDERS", currentAdmin.username);

    System.out.print(MINT + "Restore complete.\n" + RESET);
    System.out.print(SOFTGRAY + "Orders Restored: " + RESET + MINT + restoredCount + RESET + "\n");
    System.out.print(SOFTGRAY + "You can undo using option 27.\n" + RESET);
}
private void previewArchiveSessions() {
    System.out.println(PINK + BOLD + "\nArchive Preview (Delete Sessions)" + RESET);
    printLine();

    String json = readWholeFile("orders_archive.json");
    if (json.equals("")) {
        System.out.println(ROSE + "No archive sessions found." + RESET);
        printLine();
        return;
    }

    String[] sessions = splitTopLevelObjects(json);

    if (sessions.length == 0) {
        System.out.println(ROSE + "No archive sessions found." + RESET);
        printLine();
        return;
    }

    for (int i = 0; i < sessions.length; i++) {
        String session = sessions[i];

        String deletedBy = extractJsonString(session, "deletedBy");
        String deletedAt = extractJsonString(session, "deletedAt");
        String ordersJson = extractJsonArray(session, "orders");
        int count = splitTopLevelObjects(ordersJson).length;

        System.out.println(
            LAVENDER + "Session " + (i + 1) + RESET +
            SOFTGRAY + " | By: " + RESET + deletedBy +
            SOFTGRAY + " | At: " + RESET + deletedAt +
            SOFTGRAY + " | Orders: " + RESET + count
        );
    }

    printLine();
}
private void backupOrdersBeforeRestore() {
    try {
        String currentOrders = readWholeFile("orders.json");
        if (currentOrders.equals("")) currentOrders = "[]";
        writeWholeFile("orders_restore_backup.json", currentOrders);
    } catch (Exception e) {
        // ignore
    }
}
private int restoreLatestSessionOnly() {
    try {
        String json = readWholeFile("orders_archive.json");
        if (json.equals("")) return 0;

        String[] sessions = splitTopLevelObjects(json);
        if (sessions.length == 0) return 0;

        String latestSession = sessions[sessions.length - 1];
        String ordersJson = extractJsonArray(latestSession, "orders");

        return appendOrdersFromJsonArray(ordersJson);
    } catch (Exception e) {
        return 0;
    }
}
private int restoreByDate(String dateFilter) {
    int restored = 0;

    try {
        String json = readWholeFile("orders_archive.json");
        if (json.equals("")) return 0;

        String[] sessions = splitTopLevelObjects(json);

        for (int i = 0; i < sessions.length; i++) {
            String deletedAt = extractJsonString(sessions[i], "deletedAt");

            if (deletedAt.startsWith(dateFilter)) {
                String ordersJson = extractJsonArray(sessions[i], "orders");
                restored += appendOrdersFromJsonArray(ordersJson);
            }
        }
    } catch (Exception e) {
        return restored;
    }

    return restored;
}
private int restoreAllArchiveOrders() {
    int restored = 0;

    try {
        String json = readWholeFile("orders_archive.json");
        if (json.equals("")) return 0;

        String[] sessions = splitTopLevelObjects(json);

        for (int i = 0; i < sessions.length; i++) {
            String ordersJson = extractJsonArray(sessions[i], "orders");
            restored += appendOrdersFromJsonArray(ordersJson);
        }
    } catch (Exception e) {
        return restored;
    }

    return restored;
}
private void undoLastRestore(BufferedReader console) throws Exception {
    Admin currentAdmin = dp.admins[dp.currentAdminIndex];
    if (currentAdmin == null || currentAdmin.role != Role.ADMIN) {
        System.out.print(ROSE + "Access denied. Admin only.\n" + RESET);
        return;
    }

    java.io.File backupFile = new java.io.File(dp.path("orders_restore_backup.json"));
    if (!backupFile.exists()) {
        System.out.print(ROSE + "No restore backup found. Cannot undo.\n" + RESET);
        return;
    }

    System.out.print(ROSE + "Undo will revert orders.json to previous state.\n" + RESET);
    System.out.print(SOFTGRAY + "Type UNDO to confirm: " + RESET);
    String conf = console.readLine();
    if (conf == null) conf = "";
    conf = conf.trim();

    if (!conf.equalsIgnoreCase("UNDO")) {
        System.out.print(ROSE + "Undo cancelled.\n" + RESET);
        return;
    }

    String backupJson = readWholeFile("orders_restore_backup.json");
    if (backupJson.equals("")) backupJson = "[]";

    replaceOrdersFromJsonArray(backupJson);
    dp.saveOrders();
    dp.loadAll();

    log.write("ADMIN", "UNDO restore (orders.json reverted).");
    dp.appendLoginAudit("UNDO_RESTORE", currentAdmin.username);

    System.out.print(MINT + "Undo successful. orders.json restored to previous state.\n" + RESET);
}
   }


