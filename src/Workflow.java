import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Comparator;



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
     /** Wrapper for Admin authentication */
    public boolean adminLogin(BufferedReader console) throws Exception {
        return Admin.authenticate(dp, console);
    }
private void printLine() {
    System.out.println(SOFTGRAY + "╠════════════════════════════════════════════════════════════╣" + RESET);
}

private void printTitle(String text) {
    String border = "╔════════════════════════════════════════════════════════════╗";
    String bottom = "╚════════════════════════════════════════════════════════════╝";

    System.out.println(SOFTGRAY + border + RESET);
    System.out.println(SOFTGRAY + "║" + RESET
            + PINK + BOLD + centerText("✦ " + text + " ✦", 60) + RESET
            + SOFTGRAY + "║" + RESET);
    System.out.println(SOFTGRAY + bottom + RESET);
}

private void printDashboardBox(Admin admin) {

    String username = (admin != null && admin.username != null)
            ? admin.username
            : "Unknown";

    String role = (admin != null && admin.role != null)
            ? admin.role.name()
            : "Unknown";

    String top    = "╔════════════════════════════════════════════════════════════╗";
    String mid    = "║                                                            ║";
    String sep    = "╠════════════════════════════════════════════════════════════╣";
    String bottom = "╚════════════════════════════════════════════════════════════╝";

    System.out.println(SOFTGRAY + top + RESET);

    System.out.println(SOFTGRAY + "║" + RESET
            + PINK + BOLD + centerText(" ADMIN DASHBOARD ", 60) + RESET
            + SOFTGRAY + "║" + RESET);

    System.out.println(SOFTGRAY + "║" + RESET
            + LAVENDER + centerText("E-Commerce Order Fulfillment Automation System", 60) + RESET
            + SOFTGRAY + "║" + RESET);

    System.out.println(SOFTGRAY + sep + RESET);

    System.out.println(SOFTGRAY + "║" + RESET
            + MINT + centerText("Logged in as: " + username + " (" + role + ")", 60) + RESET
            + SOFTGRAY + "║" + RESET);

    System.out.println(SOFTGRAY + mid + RESET);

    System.out.println(SOFTGRAY + bottom + RESET);
}
private String centerText(String text, int width) {

    if (text == null) text = "";
    text = text.trim();

    int len = text.length();

    if (len >= width) {
        return text.substring(0, width);
    }

    int left = (width - len) / 2;
    int right = width - len - left;

    String result = "";

    for (int i = 0; i < left; i++) {
        result += " ";
    }

    result += text;

    for (int i = 0; i < right; i++) {
        result += " ";
    }

    return result;
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
private int countLowStock(int threshold) {
    int c = 0;
    for (int i = 0; i < dp.productCount; i++) {
        Product p = dp.products[i];
        if (p != null && p.stock <= threshold) c++;
    }
    return c;
}
private void printFooter(String message) {
    System.out.println();
    System.out.println(SOFTGRAY + "────────────────────────────────────────────────────────────" + RESET);
    System.out.println(MINT + centerText(message, 60) + RESET);
}
private void printSection(String text) {
    System.out.println();
    System.out.println(PINK + BOLD + ">> " + text + RESET);
    System.out.println(SOFTGRAY + "────────────────────────────────────────────────────────────" + RESET);
}
private void printMenuOption(int num, String text, boolean enabled) {
    String label = "[" + num + "] ";

    if (enabled) {
        System.out.println(LAVENDER + label + RESET + MINT + text + RESET);
    } else {
        System.out.println(LAVENDER + label + RESET + ROSE + text + " (Restricted)" + RESET);
    }
}

    /** Admin Dashboard menu loop handling all features */
 public void adminDashboard(BufferedReader console) throws Exception {
    while (true) {
        // Refresh current admin every loop
        Admin currentAdmin = dp.admins[dp.currentAdminIndex];

        // Safe role flags
        boolean isAdmin = currentAdmin != null && currentAdmin.role == Role.ADMIN;
        boolean isManager = currentAdmin != null && currentAdmin.role == Role.MANAGER;
        boolean canManageStock = isAdmin || isManager;

        // ===== DASHBOARD HEADER =====
        printDashboardBox(currentAdmin);
        printRoleSummary(currentAdmin);
        printLine();

        // ===== MENU HEADER =====
        System.out.println();
        System.out.println(LAVENDER + BOLD + "==================== MAIN MENU ====================" + RESET);

        // ===== ORDER MANAGEMENT =====
        printSection("ORDER MANAGEMENT");
        printMenuOption(1, "Accept New Order", true);
        printMenuOption(2, "Update Order Status", true);
        printMenuOption(3, "View Order Logs", true);
        printMenuOption(4, "Search / Filter Orders", true);
        printMenuOption(5, "Generate Receipt", true);

        // ===== PRODUCT & STOCK =====
        printSection("PRODUCT & STOCK");
        printMenuOption(6, "Advanced Product Filter", true);
        printMenuOption(7, "Manage Products (Add/Edit/Delete)", canManageStock);
        printMenuOption(8, "Low Stock Alerts", canManageStock);
        printMenuOption(9, "Restock Product", canManageStock);
        printMenuOption(10, "Export Stock Report", canManageStock);

        // ===== OPERATIONS =====
        printSection("OPERATIONS");
        printMenuOption(11, "Reorder Previous Order", true);
        printMenuOption(12, "Retry Failed Order", true);
        printMenuOption(13, "Simulation Mode", true);
        printMenuOption(14, "Load Test Data", true);
        printMenuOption(15, "System Health Check", true);
        printMenuOption(16, "Show Order Timeline", true);
        printMenuOption(17, "Auto Cancel Stale Orders", true);

        // ===== ADMIN ONLY =====
        printSection("SYSTEM (ADMIN ONLY)");
        printMenuOption(18, "Bulk Import Orders", isAdmin);
        printMenuOption(19, "Archive Delivered Orders", isAdmin);
        printMenuOption(20, "Clear Logs", isAdmin);
        printMenuOption(21, "Add New Admin", isAdmin);
        printMenuOption(22, "Change Admin Password", isAdmin);
        printMenuOption(23, "Generate Report", isAdmin);
        printMenuOption(24, "Delete ALL Order History", isAdmin);
        printMenuOption(25, "Restore Order History (Archive)", isAdmin);
        printMenuOption(26, "Undo Last Restore", isAdmin);

        System.out.println();
        System.out.println(LAVENDER + "[0] " + RESET + MINT + "Exit" + RESET);

        printFooter("System Ready • Awaiting Command");
        System.out.print(SOFTGRAY + "Please select an option: " + RESET);

        String choice = console.readLine();
        if (choice == null) choice = "";
        choice = choice.trim();

        System.out.println();
        if (!choice.equals("")) {
            System.out.println(MINT + "You selected option: " + choice + RESET);
            printLine();
        }

        switch (choice) {
            case "1":
                acceptNewOrder(console);
                break;

            case "2":
                handleStatusUpdate(console);
                break;

            case "3":
                printTitle("Available Orders (Sorted by Date)");

                Order[] sortedOrders = Arrays.copyOf(dp.orders, dp.orderCount);
                Arrays.sort(sortedOrders, Comparator.comparing(
                        o -> (o == null || o.date == null) ? "" : o.date
                ));

                for (Order order : sortedOrders) {
                    if (order != null) {
                        String status = (order.status == null) ? "(Unknown)" : order.status;
                        String date = (order.date == null) ? "(No Date)" : order.date;

                        System.out.println(
                                SOFTGRAY + order.orderId + RESET +
                                SOFTGRAY + " | Date: " + RESET + MINT + date + RESET +
                                SOFTGRAY + " | Status: " + RESET + LAVENDER + status + RESET
                        );
                    }
                }

                System.out.print(LAVENDER + "Enter Order ID to view logs: " + RESET);
                String logId = console.readLine();
                if (logId != null && !logId.trim().equals("")) {
                    Order target = findOrderById(logId.trim());
                    if (target != null) {
                        log.viewLogsByOrder(target.orderId);
                    } else {
                        System.out.println(ROSE + "Order not found." + RESET);
                    }
                }
                break;

            case "4":
                handleOrderSearch(console);
                break;

            case "5":
                generateReceipt(console);
                break;

            case "6":
                handleAdvancedFilter(console);
                break;

            case "7":
                if (canManageStock) {
                    handleProductManagement(console);
                } else {
                    System.out.println(ROSE + "Restricted: Admin/Manager only." + RESET);
                }
                break;

            case "8":
                if (canManageStock) {
                    showLowStockAlerts();
                } else {
                    System.out.println(ROSE + "Restricted: Admin/Manager only." + RESET);
                }
                break;

            case "9":
                if (canManageStock) {
                    handleRestock(console);
                } else {
                    System.out.println(ROSE + "Restricted: Admin/Manager only." + RESET);
                }
                break;

            case "10":
                if (canManageStock) {
                    exportStockReport();
                } else {
                    System.out.println(ROSE + "Restricted: Admin/Manager only." + RESET);
                }
                break;

            case "11":
                handleReorder(console);
                break;

            case "12":
                retryCancelledOrder(console);
                break;

            case "13":
                runSimulation(console);
                break;

            case "14":
                System.out.print(LAVENDER + "Enter test data filename (e.g. testdata.txt): " + RESET);
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

            case "15":
                systemHealthCheck();
                break;

            case "16":
                showOrderTimeline(console);
                break;

            case "17":
                autoCancelStaleOrders(2);
                break;

            case "18":
                if (isAdmin) {
                    importOrdersFromFile(console);
                } else {
                    System.out.println(ROSE + "Restricted: Admin only." + RESET);
                }
                break;

            case "19":
                if (isAdmin) {
                    archiveDeliveredOrders(console);
                } else {
                    System.out.println(ROSE + "Restricted: Admin only." + RESET);
                }
                break;

            case "20":
                if (isAdmin) {
                    clearLogs(console);
                } else {
                    System.out.println(ROSE + "Restricted: Admin only." + RESET);
                }
                break;

            case "21":
                if (isAdmin) {
                    addNewAdmin(console);
                } else {
                    System.out.println(ROSE + "Restricted: Admin only." + RESET);
                }
                break;

            case "22":
                if (isAdmin) {
                    changeAdminPassword(console);
                } else {
                    System.out.println(ROSE + "Restricted: Admin only." + RESET);
                }
                break;

            case "23":
                if (isAdmin) {
                    generateReport();
                } else {
                    System.out.println(ROSE + "Restricted: Admin only." + RESET);
                }
                break;

            case "24":
                if (isAdmin) {
                    deleteAllOrderHistory(console);
                } else {
                    System.out.println(ROSE + "Restricted: Admin only." + RESET);
                }
                break;

            case "25":
                if (isAdmin) {
                    restoreOrdersFromArchive(console);
                } else {
                    System.out.println(ROSE + "Restricted: Admin only." + RESET);
                }
                break;

            case "26":
                if (isAdmin) {
                    undoLastRestore(console);
                } else {
                    System.out.println(ROSE + "Restricted: Admin only." + RESET);
                }
                break;

            case "0":
                System.out.print(LAVENDER + "Exiting Admin Dashboard..." + RESET + "\n");
                System.out.print(LAVENDER + "Thank you for using E-commerce Order Fulfillment Automation System\n" + RESET);
                return;

            default:
                System.out.print(ROSE + "Invalid option. Please try again.\n" + RESET);
                break;
        }

        printLine();
    }
}private void addNewAdmin(BufferedReader console) throws Exception {
    Admin currentAdmin = dp.admins[dp.currentAdminIndex];

    if (currentAdmin == null || !currentAdmin.hasPermission(Role.ADMIN)) {
        System.out.print(ROSE + "Permission denied. Only admins can add new admins.\n" + RESET);
        return;
    }

    printTitle("Add New Admin");

    System.out.print(SOFTGRAY + "Enter new admin username: " + RESET);
    String username = console.readLine();
    if (username == null) username = "";
    username = username.trim();

    if (username.equals("")) {
        System.out.print(ROSE + "Username cannot be empty.\n" + RESET);
        return;
    }

    // Check duplicate username
    for (int i = 0; i < dp.adminCount; i++) {
        Admin a = dp.admins[i];
        if (a != null && a.username != null && a.username.equalsIgnoreCase(username)) {
            System.out.print(ROSE + "Username already exists. Choose another username.\n" + RESET);
            return;
        }
    }

    System.out.print(SOFTGRAY + "Enter new admin password: " + RESET);
    String password = console.readLine();
    if (password == null) password = "";
    password = password.trim();

    if (password.equals("")) {
        System.out.print(ROSE + "Password cannot be empty.\n" + RESET);
        return;
    }

    System.out.print(SOFTGRAY + "Enter role (ADMIN, MANAGER, SUPPORT): " + RESET);
    String roleStr = console.readLine();
    if (roleStr == null) roleStr = "";
    roleStr = roleStr.trim().toUpperCase();

    Role role;
    try {
        role = Role.valueOf(roleStr);
    } catch (IllegalArgumentException e) {
        System.out.print(ROSE + "Invalid role. Allowed roles: ADMIN, MANAGER, SUPPORT.\n" + RESET);
        return;
    }

    String hashedPassword = Admin.hashPassword(password);
    Admin newAdmin = new Admin(username, hashedPassword, role);

    dp.addAdmin(newAdmin);
    dp.saveAll();

    printLine();
    System.out.println(MINT + "New admin added successfully." + RESET);
    System.out.println(SOFTGRAY + "Username: " + username + RESET);
    System.out.println(SOFTGRAY + "Role: " + role.name() + RESET);
}

private String formatOrderStatus(String status) {
    if (status == null) return "(UNKNOWN)";

    String s = status.trim().toUpperCase();

    if ("DELIVERED".equals(s)) return LAVENDER + s + RESET;
    if ("CANCELLED".equals(s)) return ROSE + s + RESET;
    if ("PENDING".equals(s)) return MINT + s + RESET;
    if ("PACKED".equals(s)) return MINT + s + RESET;
    if ("SHIPPED".equals(s)) return MINT + s + RESET;
    if ("OUT_FOR_DELIVERY".equals(s)) return MINT + s + RESET;

    return s;
}
private void printOrderSummaryLine(Order o, boolean includeDateAndPayment) {
    if (o == null) return;

    String orderId = (o.orderId == null || o.orderId.trim().equals("")) ? "(Unknown)" : o.orderId;
    String date = (o.date == null || o.date.trim().equals("")) ? "(N/A)" : o.date;
    String payment = (o.paymentMode == null || o.paymentMode.trim().equals("")) ? "(N/A)" : o.paymentMode;
    String rawStatus = (o.status == null) ? "" : o.status.trim().toUpperCase();
    String statusStr = formatOrderStatus(rawStatus);
    int total = safeOrderTotal(o);

    if (includeDateAndPayment) {
        System.out.print("- " + orderId
                + " | Date: " + date
                + " | Payment: " + payment
                + " | Status: " + statusStr
                + " | Total: BDT " + total);
    } else {
        System.out.print("- " + orderId
                + " | Status: " + statusStr
                + " | Total: BDT " + total);
    }

    if ("CANCELLED".equals(rawStatus) &&
            o.cancelReason != null &&
            !o.cancelReason.trim().equals("")) {
        System.out.print(ROSE + " | CancelReason: " + o.cancelReason.trim() + RESET);
    }

    System.out.print("\n");
}
private void promptAndShowOrderDetails(BufferedReader console) throws Exception {
    System.out.print(SOFTGRAY + "Enter Order ID to view details (or press Enter to skip): " + RESET);
    String selId = console.readLine();
    if (selId == null) selId = "";
    selId = selId.trim();

    if (!selId.equals("")) {
        Order target = findOrderById(selId);

        if (target != null) {
            printLine();
            viewOrderDetails(target);
        } else {
            System.out.print(ROSE + "Order " + normalizeOrderId(selId) + " not found.\n" + RESET);
        }
    }
}private void handleOrderSearch(BufferedReader console) throws Exception {
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
        statusFilter = statusFilter.trim().toUpperCase();

        System.out.print(SOFTGRAY + "Enter Payment Mode to filter (or press Enter for any): " + RESET);
        String paymentFilter = console.readLine();
        if (paymentFilter == null) paymentFilter = "";
        paymentFilter = paymentFilter.trim().toUpperCase();

        System.out.print(SOFTGRAY + "Enter Date to filter (YYYY-MM-DD, or press Enter for any): " + RESET);
        String dateFilter = console.readLine();
        if (dateFilter == null) dateFilter = "";
        dateFilter = dateFilter.trim();

        Order[] results = new Order[dp.orderCount];
        int count = 0;

        printLine();

        for (int i = 0; i < dp.orderCount; i++) {
            Order o = dp.orders[i];
            if (o == null) continue;

            String status = (o.status == null) ? "" : o.status.trim().toUpperCase();
            String payment = (o.paymentMode == null) ? "" : o.paymentMode.trim().toUpperCase();
            String date = (o.date == null) ? "" : o.date.trim();

            if (!statusFilter.equals("") && !status.equals(statusFilter)) continue;
            if (!paymentFilter.equals("") && !payment.equals(paymentFilter)) continue;
            if (!dateFilter.equals("") && !date.equals(dateFilter)) continue;

            results[count++] = o;
        }

        if (count == 0) {
            System.out.print(ROSE + "No orders found matching the given criteria.\n" + RESET);
            return;
        }

        String statusCrit = statusFilter.equals("") ? "Any" : statusFilter;
        String payCrit = paymentFilter.equals("") ? "Any" : paymentFilter;
        String dateCrit = dateFilter.equals("") ? "Any" : dateFilter;

        System.out.print(SOFTGRAY + "Orders matching filters - Status: " + RESET + statusCrit
                + SOFTGRAY + ", Payment: " + RESET + payCrit
                + SOFTGRAY + ", Date: " + RESET + dateCrit + ":\n" + RESET);

        for (int i = 0; i < count; i++) {
            printOrderSummaryLine(results[i], true);
        }

        printLine();
        promptAndShowOrderDetails(console);
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

    String statusQuery = query.toUpperCase();
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
        return;
    }

    System.out.print(SOFTGRAY + "Orders with status containing \"" + query + "\":\n" + RESET);

    for (int i = 0; i < count; i++) {
        printOrderSummaryLine(results[i], false);
    }

    printLine();
    promptAndShowOrderDetails(console);
}
private Order findOrderById(String inputId) {
    if (inputId == null) return null;

    inputId = normalizeOrderId(inputId.trim());
    if (inputId.equals("")) return null;

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
  /** Feature 6: Manually progress an order status through the workflow (PENDING -> PACKED -> SHIPPED -> OUT_FOR_DELIVERY -> DELIVERED) */
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

    if (currentStatus.equals("DELIVERED") || currentStatus.equals("CANCELLED")) {
        System.out.print(ROSE + "Order " + order.orderId + " is " + currentStatus + "; status cannot be changed.\n" + RESET);
        return;
    }

    if (currentStatus.equals("PENDING")) {
        boolean processed = processPendingOrder(order, console);

        if (!processed) {
            System.out.print(ROSE + "Order processing failed. Status updated to CANCELLED (" 
                    + order.cancelReason + ").\n" + RESET);
            dp.saveOrders();
            return;
        }

        currentStatus = (order.status == null) ? "" : order.status.trim().toUpperCase();
    }

    String nextStatus = null;
    if (currentStatus.equals("PACKED")) {
        nextStatus = "SHIPPED";
    } else if (currentStatus.equals("SHIPPED")) {
        nextStatus = "OUT_FOR_DELIVERY";
    } else if (currentStatus.equals("OUT_FOR_DELIVERY")) {
        nextStatus = "DELIVERED";
    }

    if (nextStatus == null) {
        System.out.print(ROSE + "No further status transition available for " + currentStatus + ".\n" + RESET);
        return;
    }

    order.status = nextStatus;

    if (nextStatus.equals("SHIPPED")) {
        String normalizedId = normalizeOrderId(order.orderId);
        order.trackingId = "TRK" + normalizedId;
    }

    dp.saveOrders();
    log.write(order.orderId, "Status changed to " + nextStatus);

    System.out.print(MINT + "Order " + order.orderId + " status updated to " + nextStatus + ".\n" + RESET);
}
  /** Feature 5 & 8: Reorder a previous order (copy its items into a new order and process it) */
private void handleReorder(BufferedReader console) throws Exception {
    showReorderPreview();

    System.out.print(SOFTGRAY + "Enter Order ID to reorder: " + RESET);
    String oldId = console.readLine();
    if (oldId == null) oldId = "";
    oldId = oldId.trim();

    if (oldId.equals("")) {
        System.out.print(ROSE + "Order ID cannot be empty.\n" + RESET);
        return;
    }

    Order original = findOrderById(oldId);

    if (original == null) {
        System.out.print(ROSE + "Order " + normalizeOrderId(oldId) + " not found.\n" + RESET);
        return;
    }

    Order newOrder = new Order();
    newOrder.orderId = dp.generateOrderId();
    newOrder.date = currentDateString();
    newOrder.address = original.address;
    newOrder.paymentMode = (original.paymentMode == null || original.paymentMode.trim().equals(""))
            ? "COD"
            : original.paymentMode;

    for (int j = 0; j < original.itemCount; j++) {
        Item it = original.items[j];
        if (it == null) continue;
        newOrder.addItem(new Item(it.productId, it.quantity));
    }

    boolean success = processPendingOrder(newOrder, console);

    dp.orders[dp.orderCount++] = newOrder;

    if (!success) {
        System.out.print(ROSE + "Reorder created as " + newOrder.orderId +
                " but failed (" + newOrder.cancelReason + ").\n" + RESET);
    } else {
        System.out.print(MINT + "Reorder successful! New Order ID: " +
                newOrder.orderId + " (Status: " + newOrder.status + ").\n" + RESET);
        log.write(newOrder.orderId, "Reordered from " + normalizeOrderId(oldId));
    }
}
        /** Feature 6 (continued): View or filter products by brand or category */
    private void handleAdvancedFilter(BufferedReader console) throws Exception {
        showProductsPreview2();
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
        System.out.print(ANSI_Yellow+"Low Stock Items (stock < 5):\n"+RESET);
        for (int i = 0; i < dp.productCount; i++) {
            Product p = dp.products[i];
            if (p == null) continue;
            if (p.stock < 5) {
                anyLow = true;
                // Highlight low stock product in yellow
                System.out.print(ANSI_Yellow+ p.productId + " | " + p.name + " | Stock: " + p.stock + RESET + "\n");
            }
        }
        if (!anyLow) {
            System.out.print(ROSE+"None (all products have sufficient stock).\n"+RESET);
        }
    }
    
      /** Feature 18: Export current stock levels of all products to stock_report.txt */
    private void exportStockReport() throws Exception {
        FileWriter fw = new FileWriter(dp.path("stock_report.txt"), false);
        fw.write("ProductID | Name | Price | Stock\n");
        for (int i = 0; i < dp.productCount; i++) {
            Product p = dp.products[i];
            if (p == null) continue;
            fw.write(p.productId + " | " + p.name + " | " + p.price + " | " + p.stock + "\n");
        }
        fw.close();
        System.out.print(MINT+"Stock report generated in stock_report.txt\n"+RESET);
    }
    
    /** Feature 10: Bulk import orders from orders_import.txt */
   private void importOrdersFromFile(BufferedReader console) throws Exception{
        BufferedReader br = null;
        int importedCount = 0;
        try {
       br = new BufferedReader(new FileReader(dp.path("orders_import.json")));
        String line;
        while ((line = br.readLine()) != null) {
            line = line.trim();
            if (line.length() == 0) continue;

            // Extract fields manually
            String orderId = extract(line, "\"orderId\":\"", "\"");
            String date = extract(line, "\"date\":\"", "\"");
            String address = extract(line, "\"address\":\"", "\"");
            String paymentMode = extract(line, "\"paymentMode\":\"", "\"");
            String itemList = extract(line, "\"items\":\"", "\"");

            // Create Order object
            Order o = new Order();
            o.orderId = orderId;
            o.date = date;
            o.address = address;
            o.paymentMode = paymentMode;
            o.status = "PENDING";
            dp.parseItemsIntoOrder(o, itemList);

            // Calculate total
            int total = 0;
            for (int i = 0; i < o.itemCount; i++) {
                Product p = dp.findProductById(o.items[i].productId);
                if (p != null) {
                    total += p.price * o.items[i].quantity;
                }
            }
            o.totalAmount = total;

            dp.orders[dp.orderCount++] = o;
            System.out.print(MINT+"Imported: " + o.orderId + "\n"+RESET);
            }
        } catch (Exception e) {
            System.out.print(ROSE+"Error reading orders_import.txt\n"+RESET);
        } finally {
            if (br != null) br.close();
        }
        System.out.print(MINT+importedCount + " orders imported from orders_import.txt.\n"+RESET);
    }
    private String extract(String src, String prefix, String endToken) {
    int start = src.indexOf(prefix);
    if (start == -1) return "";
    start += prefix.length();
    int end = src.indexOf(endToken, start);
    if (end == -1) return src.substring(start);
    return src.substring(start, end);
}
 /** Feature 11: Simulation mode to generate and process orders in various scenarios */
 private void runSimulation(BufferedReader console) throws Exception {
    printTitle("Simulation Mode");

    System.out.print(SOFTGRAY + "Simulation scenarios:\n" + RESET);
    System.out.print(LAVENDER + "[1] " + RESET + "Successful order\n");
    System.out.print(LAVENDER + "[2] " + RESET + "Payment failure scenario\n");
    System.out.print(LAVENDER + "[3] " + RESET + "Inventory shortage scenario\n");
    System.out.print(LAVENDER + "[4] " + RESET + "Random order scenario\n");
    System.out.print(SOFTGRAY + "Choose scenario (1-4): " + RESET);

    String opt = console.readLine();
    if (opt == null) opt = "";
    opt = opt.trim();

    if (!opt.matches("[1-4]")) {
        System.out.print(ROSE + "Invalid scenario selection.\n" + RESET);
        return;
    }

    if (dp.productCount == 0) {
        System.out.print(ROSE + "No products available for simulation.\n" + RESET);
        return;
    }

    if (dp.orderCount >= dp.orders.length) {
        System.out.print(ROSE + "Cannot create simulation order: order storage is full.\n" + RESET);
        return;
    }

    Order simOrder = new Order();
    simOrder.orderId = dp.generateOrderId();
    simOrder.date = currentDateString();
    simOrder.status = "PENDING";
    simOrder.address = "Simulated Address";

    boolean shouldProcess = true;
    String scenarioName = "";

    if (opt.equals("1")) {
        scenarioName = "Successful Order";

        Product p = findFirstAvailableProduct();
        if (p == null) {
            System.out.print(ROSE + "No in-stock product available for successful simulation.\n" + RESET);
            return;
        }

        simOrder.addItem(new Item(p.productId, 1));
        simOrder.paymentMode = "COD";
    }

    else if (opt.equals("2")) {
        scenarioName = "Payment Failure";

        Product p = findFirstAvailableProduct();
        if (p == null) {
            System.out.print(ROSE + "No in-stock product available for payment failure simulation.\n" + RESET);
            return;
        }

        simOrder.addItem(new Item(p.productId, 1));
        simOrder.paymentMode = "MockCard";

        // Let processing happen so the payment flow decides result
    }

    else if (opt.equals("3")) {
        scenarioName = "Inventory Shortage";

        Product p = findLowOrAnyProduct();
        if (p == null) {
            System.out.print(ROSE + "No product available for inventory shortage simulation.\n" + RESET);
            return;
        }

        int shortageQty = (p.stock <= 0) ? 5 : p.stock + 5;
        simOrder.addItem(new Item(p.productId, shortageQty));
        simOrder.paymentMode = "COD";

        // Let processing fail naturally due to insufficient stock
    }

    else if (opt.equals("4")) {
        scenarioName = "Random Order";

        Product p1 = findFirstAvailableProduct();
        if (p1 == null) {
            System.out.print(ROSE + "No in-stock product available for random simulation.\n" + RESET);
            return;
        }

        simOrder.addItem(new Item(p1.productId, 1));

        Product p2 = findSecondDifferentAvailableProduct(p1.productId);
        if (p2 != null) {
            simOrder.addItem(new Item(p2.productId, 1));
        }

        simOrder.paymentMode = Math.random() < 0.5 ? "COD" : "MockCard";
    }

    if (shouldProcess) {
        processPendingOrder(simOrder, console);
    }

    dp.orders[dp.orderCount++] = simOrder;
    dp.saveOrders();

    System.out.print(MINT + "Simulation Order " + simOrder.orderId
            + " created successfully.\n" + RESET);
    System.out.print(SOFTGRAY + "Scenario: " + scenarioName + "\n" + RESET);
    System.out.print(SOFTGRAY + "Status: " + simOrder.status + "\n" + RESET);

    if (simOrder.cancelReason != null && !simOrder.cancelReason.trim().equals("")) {
        System.out.print(ROSE + "Reason: " + simOrder.cancelReason + "\n" + RESET);
    }

    log.write(simOrder.orderId, "Simulation order created [" + scenarioName + "] with status: " + simOrder.status);
}
private Product findFirstAvailableProduct() {
    for (int i = 0; i < dp.productCount; i++) {
        Product p = dp.products[i];
        if (p != null && p.stock > 0) {
            return p;
        }
    }
    return null;
}
private Product findLowOrAnyProduct() {
    for (int i = 0; i < dp.productCount; i++) {
        Product p = dp.products[i];
        if (p != null && p.stock > 0 && p.stock < 10) {
            return p;
        }
    }
    return findFirstAvailableProduct();
}
private Product findSecondDifferentAvailableProduct(String excludeProductId) {
    for (int i = 0; i < dp.productCount; i++) {
        Product p = dp.products[i];
        if (p != null && p.stock > 0 && !p.productId.equalsIgnoreCase(excludeProductId)) {
            return p;
        }
    }
    return null;
}
   /** Feature 8: Retry processing a failed (cancelled) order by creating a fresh attempt */
 private void retryCancelledOrder(BufferedReader console) throws Exception {
    printTitle("Cancelled Orders:");
    boolean found = false;

    for (int i = 0; i < dp.orderCount; i++) {
        Order o = dp.orders[i];
        if (o == null) continue;

        if ("CANCELLED".equalsIgnoreCase(o.status == null ? "" : o.status.trim())) {
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

    System.out.print(SOFTGRAY + "Enter Cancelled Order ID to retry: " + RESET);
    String cid = console.readLine();
    if (cid == null) cid = "";
    cid = cid.trim();

    if (cid.equals("")) {
        System.out.print(ROSE + "Order ID cannot be empty.\n" + RESET);
        return;
    }

    Order original = findOrderById(cid);

    if (original == null || !"CANCELLED".equalsIgnoreCase(original.status == null ? "" : original.status.trim())) {
        System.out.print(ROSE + "Order " + normalizeOrderId(cid) + " not found in cancelled list.\n" + RESET);
        return;
    }

    Order retryOrder = new Order();
    retryOrder.orderId = dp.generateOrderId();
    retryOrder.date = currentDateString();
    retryOrder.address = original.address;
    retryOrder.paymentMode = (original.paymentMode == null || original.paymentMode.trim().equals(""))
            ? "COD"
            : original.paymentMode.trim();

    for (int j = 0; j < original.itemCount; j++) {
        Item it = original.items[j];
        if (it == null) continue;
        retryOrder.addItem(new Item(it.productId, it.quantity));
    }

    boolean success = processPendingOrder(retryOrder, console);
    dp.orders[dp.orderCount++] = retryOrder;

    if (success) {
        System.out.print(MINT + "Order " + retryOrder.orderId +
                " reprocessed successfully (Status: " + retryOrder.status + ").\n" + RESET);
        log.write(retryOrder.orderId, "Retry successful for " + normalizeOrderId(cid));
    } else {
        System.out.print(ROSE + "Retry order failed (" + retryOrder.cancelReason +
                "). New Order ID: " + retryOrder.orderId + "\n" + RESET);
    }
}
 /** Feature 12: Archive delivered orders older than N days (moves them to archive_orders.txt and removes from active list) */
  private void archiveDeliveredOrders(BufferedReader console) throws Exception {
    printTitle("Archive Delivered Orders");

    System.out.print(SOFTGRAY + "Archive delivered orders older than how many days? " + RESET);
    String daysStr = console.readLine();
    if (daysStr == null) daysStr = "";
    daysStr = daysStr.trim();

    int days = DataPersistence.toInt(daysStr);
    if (days <= 0) {
        System.out.print(ROSE + "Invalid number of days.\n" + RESET);
        return;
    }

    if (dp.orderCount == 0) {
        System.out.print(ROSE + "No orders available to archive.\n" + RESET);
        return;
    }

    String todayStr = currentDateString();
    int todayCount = dateToDayCount(todayStr);

    Order[] remaining = new Order[dp.orders.length];
    int remainingCount = 0;
    int archivedCount = 0;

    try (FileWriter fw = new FileWriter(dp.path("archive_orders.txt"), true)) {
        for (int i = 0; i < dp.orderCount; i++) {
            Order o = dp.orders[i];
            if (o == null) continue;

            String status = (o.status == null) ? "" : o.status.trim().toUpperCase();
            String date = (o.date == null) ? "" : o.date.trim();

            boolean shouldArchive = false;
            int age = 0;

            if ("DELIVERED".equals(status) && !date.equals("")) {
                int orderDayCount = dateToDayCount(date);
                age = todayCount - orderDayCount;

                if (age > days) {
                    shouldArchive = true;
                }
            }

            if (shouldArchive) {
                fw.write(o.toRecord() + "\n");
                archivedCount++;
                log.write(o.orderId, "Archived after delivery (age " + age + " days)");
            } else {
                remaining[remainingCount++] = o;
            }
        }
    }

    dp.orders = remaining;
    dp.orderCount = remainingCount;
    dp.saveOrders();

    if (archivedCount == 0) {
        System.out.print(ROSE + "No delivered orders older than " + days + " days were found.\n" + RESET);
    } else {
        System.out.print(MINT + "Archived " + archivedCount
                + " delivered order(s) older than " + days + " days.\n" + RESET);
    }
} 
     /** Feature 20: Change password for the currently logged-in admin account */
  private void changeAdminPassword(BufferedReader console) throws Exception {
    printTitle("Change Admin Password");

    Admin admin = dp.admins[dp.currentAdminIndex];
    if (admin == null) {
        System.out.print(ROSE + "No logged-in admin found.\n" + RESET);
        return;
    }

    System.out.print(SOFTGRAY + "Enter current password: " + RESET);
    String currentPass = console.readLine();
    if (currentPass == null) currentPass = "";
    currentPass = currentPass.trim();

    if (currentPass.equals("")) {
        System.out.print(ROSE + "Current password cannot be empty.\n" + RESET);
        return;
    }

    String currentHash = Admin.hashPassword(currentPass);
    if (admin.passHash == null || !admin.passHash.equals(currentHash)) {
        System.out.print(ROSE + "Current password is incorrect.\n" + RESET);
        return;
    }

    System.out.print(SOFTGRAY + "Enter new password: " + RESET);
    String newPass1 = console.readLine();
    if (newPass1 == null) newPass1 = "";
    newPass1 = newPass1.trim();

    if (newPass1.equals("")) {
        System.out.print(ROSE + "New password cannot be empty.\n" + RESET);
        return;
    }

    if (newPass1.length() < 4) {
        System.out.print(ROSE + "New password must be at least 4 characters long.\n" + RESET);
        return;
    }

    System.out.print(SOFTGRAY + "Confirm new password: " + RESET);
    String newPass2 = console.readLine();
    if (newPass2 == null) newPass2 = "";
    newPass2 = newPass2.trim();

    if (!newPass1.equals(newPass2)) {
        System.out.print(ROSE + "Password confirmation does not match.\n" + RESET);
        return;
    }

    String newHash = Admin.hashPassword(newPass1);

    if (admin.passHash.equals(newHash)) {
        System.out.print(ROSE + "New password must be different from the current password.\n" + RESET);
        return;
    }

    admin.passHash = newHash;
    dp.saveAll();

    String adminName = (admin.username == null || admin.username.trim().equals(""))
            ? "ADMIN"
            : admin.username.trim();

    log.write(adminName, "Password changed");
    System.out.print(MINT + "Admin password changed successfully.\n" + RESET);
}
    /** Feature 14: Clear all logs (logs.txt) after confirmation */
private void clearLogs(BufferedReader console) throws Exception {
    printTitle("Clear Logs");

    Admin currentAdmin = dp.admins[dp.currentAdminIndex];
    if (currentAdmin == null || currentAdmin.role != Role.ADMIN) {
        System.out.print(ROSE + "Permission denied. Only admins can clear logs.\n" + RESET);
        return;
    }

    String adminName = (currentAdmin.username == null || currentAdmin.username.trim().equals(""))
            ? "Admin"
            : currentAdmin.username.trim();

System.out.print(ANSI_Yellow + "Type CLEAR to confirm log deletion: " + RESET);
String confirm = console.readLine();
if (confirm == null) confirm = "";
confirm = confirm.trim();

if (!confirm.equalsIgnoreCase("CLEAR")) {
    System.out.print(ROSE + "Log clearance cancelled.\n" + RESET);
    return;
}

    try (FileWriter fw = new FileWriter(dp.path("logs.txt"), false)) {
        fw.write("");
    }

    System.out.print(MINT + "All logs cleared successfully by " + adminName + ".\n" + RESET);
}
    /** Feature 16: Generate a receipt text file for a delivered order */
/** Feature 16: Generate a receipt text file for a delivered order */
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

    String filename = "receipt_" + order.orderId + ".txt";
    FileWriter fw = new FileWriter(dp.path(filename), false);

    fw.write("Receipt for Order " + order.orderId + "\n");

    String addr = (order.address == null || order.address.trim().equals(""))
            ? "(Not Provided)"
            : order.address.trim();
    fw.write("Address: " + addr + "\n");

    fw.write("Status: " + order.status + "\n");

    if (order.trackingId != null && !order.trackingId.trim().equals("")) {
        fw.write("Tracking ID: " + order.trackingId.trim() + "\n");
    } else {
        fw.write("Tracking ID: (Not assigned)\n");
    }

    fw.write("Items:\n");
    for (int j = 0; j < order.itemCount; j++) {
        Item it = order.items[j];
        if (it == null) continue;

        Product p = dp.findProductById(it.productId);
        String itemName = (p != null) ? p.name : it.productId;
        int priceEach = (p != null) ? p.price : 0;

        fw.write("- " + itemName + " (x" + it.quantity + " @ BDT " + priceEach + " each)\n");
    }

    fw.write("--------------------------------------\n");
    fw.write("Total Paid: BDT " + order.totalAmount + "\n");
    fw.write("Thank you for your purchase!\n");

    fw.close();

    System.out.print(MINT + "Receipt generated: " + filename + "\n" + RESET);
}
  /** Feature 14: Increase stock of an existing product (restock) */
  private void handleRestock(BufferedReader console) throws Exception {
    printTitle("Restock Product");

    Admin currentAdmin = dp.admins[dp.currentAdminIndex];
    boolean allowed = currentAdmin != null &&
            (currentAdmin.role == Role.ADMIN || currentAdmin.role == Role.MANAGER);

    if (!allowed) {
        System.out.print(ROSE + "Permission denied. Only Admin or Manager can restock products.\n" + RESET);
        return;
    }

    showRestockPreview();

    System.out.print(SOFTGRAY + "Enter Product ID to restock: " + RESET);
    String pid = console.readLine();
    if (pid == null) pid = "";
    pid = pid.trim();

    if (pid.equals("")) {
        System.out.print(ROSE + "Product ID cannot be empty.\n" + RESET);
        return;
    }

    Product product = dp.findProductById(pid);
    if (product == null) {
        System.out.print(ROSE + "Product " + pid + " not found.\n" + RESET);
        return;
    }

    System.out.print(SOFTGRAY + "Enter quantity to add: " + RESET);
    String qtyStr = console.readLine();
    if (qtyStr == null) qtyStr = "";
    qtyStr = qtyStr.trim();

    int addQty = DataPersistence.toInt(qtyStr);
    if (addQty <= 0) {
        System.out.print(ROSE + "Invalid quantity. Please enter a positive number.\n" + RESET);
        return;
    }

    int oldStock = product.stock;
    product.stock += addQty;

    dp.saveProducts();

    String actor = (currentAdmin.username == null || currentAdmin.username.trim().equals(""))
            ? "ADMIN"
            : currentAdmin.username.trim();

    log.write(actor, "Restocked " + product.productId + " (+" + addQty + "), stock: " + oldStock + " -> " + product.stock);

    System.out.print(MINT + "Product restocked successfully.\n" + RESET);
    System.out.print(SOFTGRAY + "Product ID: " + RESET + product.productId + "\n");
    System.out.print(SOFTGRAY + "Product Name: " + RESET + product.name + "\n");
    System.out.print(SOFTGRAY + "Previous Stock: " + RESET + oldStock + "\n");
    System.out.print(SOFTGRAY + "Added Quantity: " + RESET + addQty + "\n");
    System.out.print(SOFTGRAY + "New Stock: " + RESET + product.stock + "\n");
}
    
   private String padRight(String s, int width) {
    if (s == null) s = "";

    if (width <= 0) return "";

    if (s.length() > width) {
        return (width == 1) ? "…" : s.substring(0, width - 1) + "…";
    }

    return String.format("%-" + width + "s", s);
}
private String formatMoney(int n) {
    return String.format("BDT %,d", n);
}
private void printProductTable(Product[] list, int count, String title) {
    printTitle(title);

    if (list == null || count <= 0) {
        System.out.println(ROSE + "No products found." + RESET);
        printLine();
        return;
    }

    String headerColor = LAVENDER + BOLD;
    String divider = "────────────────────────────────────────────────────────────────────────────────────────────────────";

    System.out.println(
            headerColor
            + padRight("#", 5)
            + padRight("ID", 10)
            + padRight("Name", 26)
            + padRight("Brand", 16)
            + padRight("Category", 18)
            + padRight("Price", 14)
            + padRight("Stock", 10)
            + RESET
    );

    System.out.println(SOFTGRAY + divider + RESET);

    boolean hasRows = false;

    for (int i = 0; i < count; i++) {
        Product p = list[i];
        if (p == null) continue;

        hasRows = true;

        String productId = (p.productId == null || p.productId.trim().equals("")) ? "(N/A)" : p.productId.trim();
        String name = (p.name == null || p.name.trim().equals("")) ? "(Unnamed)" : p.name.trim();
        String brand = (p.brand == null || p.brand.trim().equals("")) ? "(N/A)" : p.brand.trim();
        String category = (p.category == null || p.category.trim().equals("")) ? "(N/A)" : p.category.trim();

        String stockText = String.valueOf(p.stock);
        String stockColor;

        if (p.stock <= 0) {
            stockColor = ROSE;
            stockText = "OUT";
        } else if (p.stock <= 5) {
            stockColor = ANSI_Yellow;
        } else {
            stockColor = MINT;
        }

        System.out.println(
                SOFTGRAY + padRight(String.valueOf(i + 1), 5) + RESET
                + SOFTGRAY + padRight(productId, 10) + RESET
                + padRight(name, 26)
                + padRight(brand, 16)
                + padRight(category, 18)
                + padRight(formatMoney(p.price), 14)
                + stockColor + padRight(stockText, 10) + RESET
        );
    }

    if (!hasRows) {
        System.out.println(ROSE + "No products found." + RESET);
    }

    printLine();
}
  /** Feature 23: Manage products (Add, Edit, Delete products) */
/** Feature 23: Manage products (Add, Edit, Delete products) */
private void handleProductManagement(BufferedReader console) throws Exception {
    printTitle("Product Management");

    Admin currentAdmin = dp.admins[dp.currentAdminIndex];
    boolean allowed = currentAdmin != null &&
            (currentAdmin.role == Role.ADMIN || currentAdmin.role == Role.MANAGER);

    if (!allowed) {
        System.out.print(ROSE + "Permission denied. Only Admin or Manager can manage products.\n" + RESET);
        return;
    }

    String actor = (currentAdmin.username == null || currentAdmin.username.trim().equals(""))
            ? "ADMIN"
            : currentAdmin.username.trim();

    System.out.print(SOFTGRAY + "Choose action - [A]dd, [E]dit, [D]elete: " + RESET);
    String action = console.readLine();
    if (action == null) action = "";
    action = action.trim().toUpperCase();

    // ===========================
    // ADD PRODUCT
    // ===========================
    if (action.equals("A")) {
        if (dp.productCount >= dp.products.length) {
            System.out.print(ROSE + "Product list is full, cannot add more products.\n" + RESET);
            return;
        }

        System.out.print(SOFTGRAY + "Enter Category (Smartphone/Laptop/Home Appliance/Accessories/Power Bank): " + RESET);
        String category = console.readLine();
        if (category == null) category = "";
        category = category.trim();

        if (category.equals("")) {
            System.out.print(ROSE + "Category cannot be empty.\n" + RESET);
            return;
        }

        String newId = dp.generateProductIdByCategory(category);
        while (dp.findProductById(newId) != null) {
            newId = dp.generateProductIdByCategory(category);
        }

        System.out.print(MINT + "Auto Generated Product ID: " + newId + "\n" + RESET);

        System.out.print(SOFTGRAY + "Enter Brand: " + RESET);
        String brand = console.readLine();
        if (brand == null) brand = "";
        brand = brand.trim();

        System.out.print(SOFTGRAY + "Enter Product Name: " + RESET);
        String name = console.readLine();
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

        if (brand.equals("") || name.equals("")) {
            System.out.print(ROSE + "Brand and Product Name cannot be empty.\n" + RESET);
            return;
        }

        int price = DataPersistence.toInt(priceStr);
        int stock = DataPersistence.toInt(stockStr);

        if (price <= 0) {
            System.out.print(ROSE + "Invalid price. Price must be greater than 0.\n" + RESET);
            return;
        }

        if (stock < 0) {
            System.out.print(ROSE + "Invalid stock. Stock cannot be negative.\n" + RESET);
            return;
        }

        Product newProduct = new Product(newId, category, brand, name, price, stock);
        dp.products[dp.productCount++] = newProduct;
        dp.saveProducts();

        System.out.print(MINT + "Product " + newId + " added successfully.\n" + RESET);
        log.write(actor, "Added product " + newId + " [" + name + "]");
        return;
    }

    // ===========================
    // EDIT PRODUCT
    // ===========================
    if (action.equals("E")) {
        showProductsPreview2();

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

        System.out.print(SOFTGRAY + "Edit field - [N]ame, [P]rice, [S]tock, [B]rand, [C]ategory: " + RESET);
        String field = console.readLine();
        if (field == null) field = "";
        field = field.trim().toUpperCase();

        if (field.equals("N")) {
            System.out.print(SOFTGRAY + "Enter new Name: " + RESET);
            String newName = console.readLine();
            if (newName == null) newName = "";
            newName = newName.trim();

            if (newName.equals("")) {
                System.out.print(ROSE + "Name cannot be empty.\n" + RESET);
                return;
            }

            String oldName = product.name;
            product.name = newName;
            dp.saveProducts();

            System.out.print(MINT + "Product " + product.productId + " name updated.\n" + RESET);
            log.write(actor, "Edited product " + product.productId + " (Name: " + oldName + " -> " + newName + ")");
            return;
        }

        if (field.equals("P")) {
            System.out.print(SOFTGRAY + "Enter new Price: " + RESET);
            String newPriceStr = console.readLine();
            if (newPriceStr == null) newPriceStr = "";
            newPriceStr = newPriceStr.trim();

            int newPrice = DataPersistence.toInt(newPriceStr);
            if (newPrice <= 0) {
                System.out.print(ROSE + "Invalid price.\n" + RESET);
                return;
            }

            int oldPrice = product.price;
            product.price = newPrice;
            dp.saveProducts();

            System.out.print(MINT + "Product " + product.productId + " price updated.\n" + RESET);
            log.write(actor, "Edited product " + product.productId + " (Price: " + oldPrice + " -> " + newPrice + ")");
            return;
        }

        if (field.equals("S")) {
            System.out.print(SOFTGRAY + "Enter new Stock value: " + RESET);
            String newStockStr = console.readLine();
            if (newStockStr == null) newStockStr = "";
            newStockStr = newStockStr.trim();

            int newStock = DataPersistence.toInt(newStockStr);
            if (newStock < 0) {
                System.out.print(ROSE + "Invalid stock. Stock cannot be negative.\n" + RESET);
                return;
            }

            int oldStock = product.stock;
            product.stock = newStock;
            dp.saveProducts();

            System.out.print(MINT + "Product " + product.productId + " stock updated.\n" + RESET);
            log.write(actor, "Edited product " + product.productId + " (Stock: " + oldStock + " -> " + newStock + ")");
            return;
        }

        if (field.equals("B")) {
            System.out.print(SOFTGRAY + "Enter new Brand: " + RESET);
            String newBrand = console.readLine();
            if (newBrand == null) newBrand = "";
            newBrand = newBrand.trim();

            if (newBrand.equals("")) {
                System.out.print(ROSE + "Brand cannot be empty.\n" + RESET);
                return;
            }

            String oldBrand = product.brand;
            product.brand = newBrand;
            dp.saveProducts();

            System.out.print(MINT + "Product " + product.productId + " brand updated.\n" + RESET);
            log.write(actor, "Edited product " + product.productId + " (Brand: " + oldBrand + " -> " + newBrand + ")");
            return;
        }

        if (field.equals("C")) {
            System.out.print(SOFTGRAY + "Enter new Category: " + RESET);
            String newCategory = console.readLine();
            if (newCategory == null) newCategory = "";
            newCategory = newCategory.trim();

            if (newCategory.equals("")) {
                System.out.print(ROSE + "Category cannot be empty.\n" + RESET);
                return;
            }

            String oldCategory = product.category;
            product.category = newCategory;
            dp.saveProducts();

            System.out.print(MINT + "Product " + product.productId + " category updated.\n" + RESET);
            log.write(actor, "Edited product " + product.productId + " (Category: " + oldCategory + " -> " + newCategory + ")");
            return;
        }

        System.out.print(ROSE + "Invalid field selection.\n" + RESET);
        return;
    }

    // ===========================
    // DELETE PRODUCT
    // ===========================
    if (action.equals("D")) {
        showProductsPreview2();

        System.out.print(SOFTGRAY + "Enter Product ID to delete: " + RESET);
        String delId = console.readLine();
        if (delId == null) delId = "";
        delId = delId.trim();

        if (delId.equals("")) {
            System.out.print(ROSE + "Product ID cannot be empty.\n" + RESET);
            return;
        }

        int idx = -1;
        Product target = null;

        for (int i = 0; i < dp.productCount; i++) {
            Product p = dp.products[i];
            if (p != null && p.productId != null && p.productId.equalsIgnoreCase(delId)) {
                idx = i;
                target = p;
                break;
            }
        }

        if (idx == -1 || target == null) {
            System.out.print(ROSE + "Product " + delId + " not found.\n" + RESET);
            return;
        }

        System.out.print(ANSI_Yellow + "Are you sure you want to delete " + target.productId + " (" + target.name + ")? (Y/N): " + RESET);
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

        System.out.print(MINT + "Product " + target.productId + " deleted successfully.\n" + RESET);
        log.write(actor, "Deleted product " + target.productId + " [" + target.name + "]");
        return;
    }

    System.out.print(ROSE + "Invalid action.\n" + RESET);
}
/** Process a PENDING order through inventory check, reservation, invoice generation, and payment simulation */
private boolean processPendingOrder(Order order, BufferedReader console) throws Exception {
    if (order == null) return false;

    String currentStatus = (order.status == null) ? "" : order.status.trim().toUpperCase();
    if (!"PENDING".equals(currentStatus)) return false;

    if (order.itemCount <= 0) {
        order.status = "CANCELLED";
        order.cancelReason = "No items in order";
        log.write(order.orderId, "Order cancelled - " + order.cancelReason);
        return false;
    }

    // ===========================
    // Step 1: Validate all items and stock first
    // ===========================
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

        if (it.quantity <= 0) {
            order.status = "CANCELLED";
            order.cancelReason = "Invalid quantity for " + it.productId;
            log.write(order.orderId, "Order cancelled - " + order.cancelReason);
            return false;
        }

        if (prod.stock < it.quantity) {
            order.status = "CANCELLED";
            order.cancelReason = "Inventory Shortage: " + it.productId;
            log.write(order.orderId, "Order cancelled - " + order.cancelReason);
            return false;
        }
    }

    // ===========================
    // Step 2: Reserve stock
    // ===========================
    for (int i = 0; i < order.itemCount; i++) {
        Item it = order.items[i];
        if (it == null) continue;

        Product prod = dp.findProductById(it.productId);
        if (prod != null) {
            prod.stock -= it.quantity;
        }
    }
    dp.saveProducts();
    log.write(order.orderId, "Inventory OK - stock reserved");

    // ===========================
    // Step 3: Calculate total
    // ===========================
    int total = 0;
    for (int i = 0; i < order.itemCount; i++) {
        Item it = order.items[i];
        if (it == null) continue;

        Product prod = dp.findProductById(it.productId);
        int price = (prod != null) ? prod.price : 0;
        total += price * it.quantity;
    }
    order.totalAmount = total;

    // ===========================
    // Step 4: Process payment
    // ===========================
    boolean paymentSuccess;
    String paymentMode = (order.paymentMode == null) ? "" : order.paymentMode.trim();

    if (console == null) {
        // Simulation mode
        if ("COD".equalsIgnoreCase(paymentMode)) {
            paymentSuccess = true;
            log.write(order.orderId, "PAYMENT OK (COD)");
        } else {
            paymentSuccess = false;
            log.write(order.orderId, "PAYMENT FAIL (Auto decline for simulation)");
        }
    } else {
        paymentSuccess = paymentService.processPayment(order, console);
    }

    // ===========================
    // Step 5: Rollback stock if payment fails
    // ===========================
    if (!paymentSuccess) {
        for (int i = 0; i < order.itemCount; i++) {
            Item it = order.items[i];
            if (it == null) continue;

            Product prod = dp.findProductById(it.productId);
            if (prod != null) {
                prod.stock += it.quantity;
            }
        }

        dp.saveProducts();
        order.status = "CANCELLED";
        order.cancelReason = "Payment Declined";
        log.write(order.orderId, "Order cancelled - " + order.cancelReason);
        return false;
    }

    // ===========================
    // Step 6: Mark as PACKED
    // ===========================
    order.status = "PACKED";
    log.write(order.orderId, "Status changed to PACKED");

    // ===========================
    // Step 7: Generate invoice
    // ===========================
    generateInvoice(order);

    return true;
}
private void generateInvoice(Order order) {
    if (order == null) return;

    try {
        String datePart = "UNKNOWN";
        if (order.date != null && order.date.length() >= 7) {
            datePart = order.date.substring(0, 7).replace("-", "");
        }

        String orderPart = normalizeOrderId(order.orderId);
        if (orderPart == null || orderPart.trim().equals("")) {
            orderPart = "0000";
        }

        String invoiceId = "INV-" + datePart + "-" + orderPart;

        try (FileWriter fw = new FileWriter(dp.path("invoices.txt"), true)) {
            fw.write(invoiceId + "|BDT " + order.totalAmount + "\n");
        }

        System.out.print(MINT + "Invoice generated: " + invoiceId + "\n" + RESET);
        log.write(order.orderId, "Invoice generated: " + invoiceId);

    } catch (Exception e) {
        log.write(order.orderId, "Invoice generation failed");
    }
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

    System.out.print(LAVENDER + "Address: " + address + "\n" + RESET);
    System.out.print(LAVENDER + "Payment Mode: " + paymentMode + "\n" + RESET);
    System.out.print(LAVENDER + "Total Amount: BDT " + order.totalAmount + "\n" + RESET);
    System.out.print("Items:\n");

    boolean hasItems = false;
    for (int i = 0; i < order.itemCount; i++) {
        Item it = order.items[i];
        if (it == null) continue;

        hasItems = true;
        Product p = dp.findProductById(it.productId);
        String itemName = (p != null && p.name != null && !p.name.trim().equals(""))
                ? p.name.trim()
                : it.productId;

        System.out.print(LAVENDER + "- " + itemName + " (x" + it.quantity + ")\n" + RESET);
    }

    if (!hasItems) {
        System.out.print(ROSE + "- No items found\n" + RESET);
    }
}
     /** Feature 17: Generate a report of revenue and cancellations, write to report.txt */
    private void generateReport() throws Exception {
        int totalOrders = dp.orderCount;
        int completedCount = 0;
        int cancelledCount = 0;
        int revenueSum = 0;
        // Count cancellation reasons
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
                // Increment count for this reason
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
        // Sort cancellation reasons by frequency (descending)
        for (int i = 0; i < reasonTypes - 1; i++) {
            int maxIndex = i;
            for (int j = i + 1; j < reasonTypes; j++) {
                if (reasonCounts[j] > reasonCounts[maxIndex]) {
                    maxIndex = j;
                }
            }
            // swap
            String tempReason = reasons[i];
            reasons[i] = reasons[maxIndex];
            reasons[maxIndex] = tempReason;
            int tempCount = reasonCounts[i];
            reasonCounts[i] = reasonCounts[maxIndex];
            reasonCounts[maxIndex] = tempCount;
        }
        // Build report content
        StringBuilder report = new StringBuilder();
        report.append("Total Orders: ").append(totalOrders).append("\n");
        report.append("Completed Orders: ").append(completedCount).append("\n");
        report.append("Cancelled Orders: ").append(cancelledCount).append("\n");
        report.append("Total Revenue: BDT ").append(revenueSum).append("\n");
        report.append("Top 3 Cancellation Reasons:\n");
        for (int k = 0; k < reasonTypes && k < 3; k++) {
            report.append((k + 1) + ". " + reasons[k] + " – " + reasonCounts[k] + "\n");
        }
        // Write report to file and display summary in console
        FileWriter fw = new FileWriter(dp.path("report.txt"), false);
        fw.write(report.toString());
        fw.close();
        printTitle("Report Summary");
        System.out.print(report.toString());
        System.out.print(MINT+"(Full report saved to report.txt)\n"+RESET);
    }


    /** Helper: normalize input to full Order ID format (e.g., add 'O' prefix if missing) */
private String normalizeOrderId(String input) {
    if (input == null) return "";
    String s = input.trim();

    // Remove optional 'O' prefix if user types it
    if (s.length() > 0 && (s.charAt(0) == 'O' || s.charAt(0) == 'o')) {
        s = s.substring(1).trim();
    }

    // If numeric, pad to 5 digits (01001 style)
    if (isNumeric(s)) {
        while (s.length() < 5) s = "0" + s;
        return s;
    }

    // Otherwise return as-is (maybe they typed status)
    return input.trim();
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
        // Use system date for realism
        LocalDate today = LocalDate.now();
        return today.toString();
    }

    /** Helper: convert a YYYY-MM-DD date string to an approximate day count for comparison */
  private int dateToDayCount(String dateStr) {
    if (dateStr == null || dateStr.length() != 10) return 0;

    // Format assumed: YYYY-MM-DD
    int year =
            (dateStr.charAt(0) - '0') * 1000 +
            (dateStr.charAt(1) - '0') * 100 +
            (dateStr.charAt(2) - '0') * 10 +
            (dateStr.charAt(3) - '0');

    int month =
            (dateStr.charAt(5) - '0') * 10 +
            (dateStr.charAt(6) - '0');

    int day =
            (dateStr.charAt(8) - '0') * 10 +
            (dateStr.charAt(9) - '0');

    return year * 360 + month * 30 + day;
}
  
private void acceptNewOrder(BufferedReader console) throws Exception {
    printTitle("Accept New Order");

    if (dp.orderCount >= dp.orders.length) {
        System.out.print(ROSE + "Order list is full. Cannot accept more orders.\n" + RESET);
        return;
    }

    if (dp.productCount == 0) {
        System.out.print(ROSE + "No products available. Cannot create order.\n" + RESET);
        return;
    }

    // 1. Create new order
    Order newOrder = new Order();
    newOrder.orderId = dp.generateOrderId();
    newOrder.date = currentDateString();
    newOrder.status = "PENDING";

    System.out.print(LAVENDER + "New Order ID: " + newOrder.orderId + "\n" + RESET);

    // 2. Show product catalog
    printTitle("Product Catalog");
    for (int i = 0; i < dp.productCount; i++) {
        Product prod = dp.products[i];
        if (prod == null) continue;

        String stockColor = (prod.stock <= 0) ? ROSE : (prod.stock <= 5 ? ANSI_Yellow : MINT);

        System.out.print(
                LAVENDER + prod.productId + RESET
                + " - " + MINT + prod.name + RESET
                + SOFTGRAY + " | Brand: " + RESET + prod.brand
                + SOFTGRAY + " | Price: " + RESET + formatMoney(prod.price)
                + SOFTGRAY + " | Stock: " + RESET + stockColor + prod.stock + RESET
                + "\n"
        );
    }
    printLine();

    // 3. Ask how many different products
    System.out.print(SOFTGRAY + "How many different products in this order? (1-10): " + RESET);
    String countStr = console.readLine();
    if (countStr == null) countStr = "";
    countStr = countStr.trim();

    int requestedItemCount = DataPersistence.toInt(countStr);
    if (requestedItemCount < 1 || requestedItemCount > 10) {
        System.out.print(ROSE + "Invalid number of products. Order cancelled.\n" + RESET);
        return;
    }

    // 4. Add items
    for (int i = 1; i <= requestedItemCount; i++) {
        System.out.print(SOFTGRAY + "Enter Product ID for item " + i + ": " + RESET);
        String pid = console.readLine();
        if (pid == null) pid = "";
        pid = pid.trim();

        if (pid.equals("")) {
            System.out.print(ROSE + "Product ID cannot be empty. Order cancelled.\n" + RESET);
            return;
        }

        Product product = dp.findProductById(pid);
        if (product == null) {
            System.out.print(ROSE + "Product " + pid + " not found. Order cancelled.\n" + RESET);
            return;
        }

        System.out.print(SOFTGRAY + "Enter quantity for " + product.name + ": " + RESET);
        String qtyStr = console.readLine();
        if (qtyStr == null) qtyStr = "";
        qtyStr = qtyStr.trim();

        int qty = DataPersistence.toInt(qtyStr);
        if (qty <= 0) {
            System.out.print(ROSE + "Invalid quantity. Order cancelled.\n" + RESET);
            return;
        }

        // Merge quantity if product already added
        boolean merged = false;
        for (int j = 0; j < newOrder.itemCount; j++) {
            Item existing = newOrder.items[j];
            if (existing != null && existing.productId.equalsIgnoreCase(product.productId)) {
                existing.quantity += qty;
                merged = true;
                break;
            }
        }

        if (!merged) {
            if (!newOrder.addItem(new Item(product.productId, qty))) {
                System.out.print(ROSE + "Failed to add item " + product.productId + ". Order cancelled.\n" + RESET);
                return;
            }
        }
    }

    if (newOrder.itemCount <= 0) {
        System.out.print(ROSE + "Order has no valid items. Order cancelled.\n" + RESET);
        return;
    }

    // 5. Shipping address
    System.out.print(SOFTGRAY + "Enter shipping address: " + RESET);
    String address = console.readLine();
    if (address == null) address = "";
    address = address.trim();

    if (address.equals("")) {
        System.out.print(ROSE + "Address cannot be empty. Order cancelled.\n" + RESET);
        return;
    }
    newOrder.address = address;

    // 6. Payment mode
    System.out.print(SOFTGRAY + "Enter payment mode (COD or MockCard): " + RESET);
    String paymentMode = console.readLine();
    if (paymentMode == null) paymentMode = "";
    paymentMode = paymentMode.trim();

    if (!paymentMode.equalsIgnoreCase("COD") && !paymentMode.equalsIgnoreCase("MockCard")) {
        System.out.print(ROSE + "Invalid payment mode. Use COD or MockCard. Order cancelled.\n" + RESET);
        return;
    }
    newOrder.paymentMode = paymentMode;
    printTitle("Order Summary");
    for (int i = 0; i < newOrder.itemCount; i++) {
    Item it = newOrder.items[i];
    if (it == null) continue;

    Product p = dp.findProductById(it.productId);
    String name = (p != null) ? p.name : it.productId;

    System.out.println("- " + name + " x" + it.quantity);
}
System.out.println("Address: " + newOrder.address);
System.out.println("Payment: " + newOrder.paymentMode);
printLine();

    // 7. Log creation
    log.write(newOrder.orderId, "Order created via admin interface (pending)");

    // 8. Process order
    boolean processed = processPendingOrder(newOrder, console);

    // 9. Store order
    dp.orders[dp.orderCount++] = newOrder;
    dp.saveOrders();

    // 10. Final result
    if (!processed) {
        System.out.print(ROSE + "Order processing failed.\n" + RESET);
        System.out.print(ROSE + "Order ID: " + newOrder.orderId + "\n" + RESET);
        System.out.print(ROSE + "Final Status: " + newOrder.status + "\n" + RESET);
        System.out.print(ROSE + "Reason: " + newOrder.cancelReason + "\n" + RESET);
    } else {
        System.out.print(MINT + "New order accepted and processed successfully!\n" + RESET);
        System.out.print(SOFTGRAY + "Order ID: " + RESET + newOrder.orderId + "\n");
        System.out.print(SOFTGRAY + "Status: " + RESET + newOrder.status + "\n");
        System.out.print(SOFTGRAY + "Total: " + RESET + formatMoney(newOrder.totalAmount) + "\n");
    }
}
private void systemHealthCheck() {
    printTitle("System Health Check");

    int delivered = 0;
    int cancelled = 0;
    int pending = 0;
    int packed = 0;
    int shipped = 0;
    int outForDelivery = 0;

    int inStock = 0;
    int lowStock = 0;
    int outOfStock = 0;

    // Analyze orders
    for (int i = 0; i < dp.orderCount; i++) {
        Order o = dp.orders[i];
        if (o == null || o.status == null) continue;

        String status = o.status.trim().toUpperCase();

        if ("DELIVERED".equals(status)) delivered++;
        else if ("CANCELLED".equals(status)) cancelled++;
        else if ("PENDING".equals(status)) pending++;
        else if ("PACKED".equals(status)) packed++;
        else if ("SHIPPED".equals(status)) shipped++;
        else if ("OUT_FOR_DELIVERY".equals(status)) outForDelivery++;
    }

    // Analyze products
    for (int i = 0; i < dp.productCount; i++) {
        Product p = dp.products[i];
        if (p == null) continue;

        if (p.stock <= 0) {
            outOfStock++;
        } else if (p.stock <= 5) {
            lowStock++;
        } else {
            inStock++;
        }
    }

    System.out.println(MINT + "Data Summary" + RESET);
    printLine();
    System.out.println(SOFTGRAY + "Orders loaded:   " + RESET + dp.orderCount);
    System.out.println(SOFTGRAY + "Products loaded: " + RESET + dp.productCount);
    System.out.println(SOFTGRAY + "Admins loaded:   " + RESET + dp.adminCount);

    System.out.println();
    System.out.println(MINT + "Order Status Summary" + RESET);
    printLine();
    System.out.println(SOFTGRAY + "Pending:          " + RESET + pending);
    System.out.println(SOFTGRAY + "Packed:           " + RESET + packed);
    System.out.println(SOFTGRAY + "Shipped:          " + RESET + shipped);
    System.out.println(SOFTGRAY + "Out for Delivery: " + RESET + outForDelivery);
    System.out.println(SOFTGRAY + "Delivered:        " + RESET + delivered);
    System.out.println(SOFTGRAY + "Cancelled:        " + RESET + cancelled);

    System.out.println();
    System.out.println(MINT + "Inventory Summary" + RESET);
    printLine();
    System.out.println(SOFTGRAY + "In-stock products:   " + RESET + inStock);

    if (lowStock > 0) {
        System.out.println(ANSI_Yellow + "Low-stock products:  " + lowStock + RESET);
    } else {
        System.out.println(MINT + "Low-stock products:  0" + RESET);
    }

    if (outOfStock > 0) {
        System.out.println(ROSE + "Out-of-stock items:  " + outOfStock + RESET);
    } else {
        System.out.println(MINT + "Out-of-stock items:  0" + RESET);
    }

    System.out.println();
    System.out.println(MINT + "System Status" + RESET);
    printLine();

    if (dp.orderCount == 0 && dp.productCount == 0) {
        System.out.println(ROSE + "Warning: No operational data loaded." + RESET);
    } else if (outOfStock > 0 || lowStock > 0) {
        System.out.println(ANSI_Yellow + "System is operational, but inventory needs attention." + RESET);
    } else {
        System.out.println(MINT + "System is operational and healthy." + RESET);
    }

    printLine();
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

    // Find the real order first
    Order order = findOrderById(id);

    if (order == null) {
        System.out.print(ROSE + "Order " + normalizeOrderId(id) + " not found.\n" + RESET);
        return;
    }

    String realOrderId = order.orderId;

    System.out.print(PINK + BOLD + "Timeline for " + realOrderId + "\n" + RESET);
    printLine();

    BufferedReader br = null;
    boolean found = false;

    try {
        br = new BufferedReader(new FileReader(dp.path("logs.txt")));
        String line;

        while ((line = br.readLine()) != null) {
            if (line != null && line.contains(realOrderId)) {
                found = true;
                System.out.print(SOFTGRAY + "- " + RESET + line + "\n");
            }
        }
    } catch (Exception e) {
        System.out.print(ROSE + "logs.txt not found.\n" + RESET);
    } finally {
        if (br != null) br.close();
    }

    if (!found) {
        System.out.print(ROSE + "No timeline entries found for " + realOrderId + ".\n" + RESET);
    }

    printLine();
}

private void autoCancelStaleOrders(int days) throws Exception {
    printTitle("Auto Cancel Stale Orders");

    if (days <= 0) {
        System.out.print(ROSE + "Invalid number of days. Must be greater than 0.\n" + RESET);
        return;
    }

    String todayStr = currentDateString();
    int todayCount = dateToDayCount(todayStr);

    int cancelledCount = 0;
    int invalidDateCount = 0;

    for (int i = 0; i < dp.orderCount; i++) {
        Order o = dp.orders[i];
        if (o == null) continue;

        String status = (o.status == null) ? "" : o.status.trim().toUpperCase();
        String dateStr = (o.date == null) ? "" : o.date.trim();

        if (!"PENDING".equals(status)) continue;

        if (dateStr.equals("")) {
            invalidDateCount++;
            continue;
        }

        int orderDay = dateToDayCount(dateStr);
        if (orderDay == 0) {
            invalidDateCount++;
            continue;
        }

        int diff = todayCount - orderDay;

        if (diff >= days) {
            o.status = "CANCELLED";
            o.cancelReason = "Auto-cancelled: stale pending order (" + diff + " days old)";
            cancelledCount++;

            log.write(o.orderId, "Auto-cancelled after " + diff + " days in PENDING status");
            System.out.print(ROSE + "Cancelled: " + o.orderId + " (age: " + diff + " days)\n" + RESET);
        }
    }

    if (cancelledCount > 0) {
        dp.saveOrders();
        System.out.print(MINT + "Auto-cancelled " + cancelledCount + " stale PENDING order(s).\n" + RESET);
    } else {
        System.out.print(ROSE + "No stale PENDING orders found.\n" + RESET);
    }

    if (invalidDateCount > 0) {
        System.out.print(ANSI_Yellow + "Skipped " + invalidDateCount + " order(s) due to invalid date format.\n" + RESET);
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
    if (order == null || order.items == null || order.itemCount <= 0) {
        return 0;
    }

    int total = 0;

    for (int i = 0; i < order.itemCount; i++) {
        Item item = order.items[i];
        if (item == null) continue;

        Product product = dp.findProductById(item.productId);
        if (product == null) continue;

        int price = product.price;
        int qty = item.quantity;

        if (qty > 0 && price > 0) {
            total += price * qty;
        }
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
private void showProductsPreview2() {
    System.out.println(PINK + BOLD + "\nProducts List (Preview)" + RESET);
    printLine();

    if (dp.productCount == 0) {
        System.out.println(ROSE + "No products available." + RESET);
        printLine();
        return;
    }

    // Header
    System.out.printf(
        LAVENDER + "%-8s %-16s %-14s %-30s %-10s %-8s" + RESET + "%n",
        "ProdID", "Category", "Brand", "Name", "Price", "Stock"
    );
    System.out.println(SOFTGRAY +
        "--------------------------------------------------------------------------"
        + RESET
    );

    // Rows
    for (int i = 0; i < dp.productCount; i++) {
        Product p = dp.products[i];
        if (p == null) continue;

        String stockColor = p.stock <= 5 ? ROSE : MINT;

        System.out.printf(
            "%-8s %-16s %-14s %-30s %-10d %s%-8d%s%n",
            p.productId,
            p.category,
            p.brand,
            p.name,
            p.price,
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

private int safeOrderTotal(Order order) {
    if (order == null) return 0;

    // If total was already calculated, reuse it
    if (order.totalAmount > 0) {
        return order.totalAmount;
    }

    int total = 0;

    if (order.items == null || order.itemCount <= 0) {
        return 0;
    }

    for (int i = 0; i < order.itemCount; i++) {
        Item item = order.items[i];
        if (item == null) continue;

        Product product = dp.findProductById(item.productId);
        if (product == null) continue;

        int price = product.price;
        int qty = item.quantity;

        if (price > 0 && qty > 0) {
            total += price * qty;
        }
    }

    return total;
}

private void showRestockPreview() {
    printTitle("Products Needing Restock");

    int threshold = 5;
    int lowCount = 0;

    System.out.printf(
            LAVENDER + "%-10s %-24s %-16s %-18s %-10s" + RESET + "%n",
            "ID", "Name", "Brand", "Category", "Stock"
    );
    System.out.println(
            SOFTGRAY + "--------------------------------------------------------------------------------" + RESET
    );

    for (int i = 0; i < dp.productCount; i++) {
        Product p = dp.products[i];
        if (p == null) continue;

        if (p.stock <= threshold) {
            lowCount++;

            String productId = (p.productId == null || p.productId.trim().equals("")) ? "(N/A)" : p.productId.trim();
            String name = (p.name == null || p.name.trim().equals("")) ? "(Unnamed)" : p.name.trim();
            String brand = (p.brand == null || p.brand.trim().equals("")) ? "(N/A)" : p.brand.trim();
            String category = (p.category == null || p.category.trim().equals("")) ? "(N/A)" : p.category.trim();

            String stockText;
            String stockColor;

            if (p.stock <= 0) {
                stockText = "OUT";
                stockColor = ROSE;
            } else {
                stockText = String.valueOf(p.stock);
                stockColor = ANSI_Yellow;
            }

            System.out.printf(
                    "%-10s %-24s %-16s %-18s %s%-10s%s%n",
                    productId,
                    trimTo(name, 24),
                    trimTo(brand, 16),
                    trimTo(category, 18),
                    stockColor, stockText, RESET
            );
        }
    }

    if (lowCount == 0) {
        System.out.println(MINT + "No products are low in stock right now." + RESET);
    } else {
        System.out.println();
        System.out.println(ANSI_Yellow + "Total products needing attention: " + lowCount + RESET);
    }

    printLine();
}

// small helper so long names don't break your table
private String trimTo(String s, int max) {
    if (s == null) s = "";
    if (max <= 0) return "";
    if (s.length() <= max) return s;
    return s.substring(0, max - 1) + "…";
}
private String buildOrderItemsSummary(Order order) {
    if (order == null || order.itemCount == 0) {
        return "No items";
    }

    String summary = "";

    for (int i = 0; i < order.itemCount; i++) {
        Item it = order.items[i];
        if (it == null) continue;

        String productName = it.productId; // fallback if product not found

        // Find product name from product list
        for (int j = 0; j < dp.productCount; j++) {
            Product p = dp.products[j];
            if (p != null && p.productId.equalsIgnoreCase(it.productId)) {
                productName = p.name;
                break;
            }
        }

        if (!summary.equals("")) {
            summary += ", ";
        }

        summary += productName + " x" + it.quantity;
    }

    return summary;
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

    // ✅ Admin-only protection
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

    // ✅ Count before delete
    int deletedCount = dp.orderCount;

    String archiveFileName = "orders_archive.txt";
    String archivePath = dp.path(archiveFileName);

    // ===============================
    // ✅ BACKUP ORDERS (SOFT DELETE)
    // ===============================
    FileWriter backup = null;
    try {
        backup = new FileWriter(archivePath, true);
        backup.write("\n=== ARCHIVE DELETE by " + currentAdmin.username + " ===\n");
        backup.write("Deleted at: " + dp.currentDateTimeString() + "\n");

        for (int i = 0; i < dp.orderCount; i++) {
            Order o = dp.orders[i];
            if (o == null) continue;

            StringBuilder itemList = new StringBuilder();
            for (int j = 0; j < o.itemCount; j++) {
                Item it = o.items[j];
                if (it == null) continue;
                itemList.append(it.productId).append("x").append(it.quantity);
                if (j < o.itemCount - 1) itemList.append(",");
            }

            backup.write(
                o.orderId + "|" + o.date + "|" + o.address + "|" +
                o.paymentMode + "|" + o.status + "|" +
                o.totalAmount + "|" + itemList.toString()
            );

            if (o.cancelReason != null && !o.cancelReason.equals("")) {
                backup.write("|" + o.cancelReason);
            }

            if (o.trackingId != null && !o.trackingId.equals("")) {
                backup.write("|" + o.trackingId);
            }

            backup.write("\n");
        }
    } finally {
        if (backup != null) backup.close();
    }

    // ===============================
    // ✅ CLEAR ORDERS FROM MEMORY
    // ===============================
    for (int i = 0; i < dp.orderCount; i++) {
        dp.orders[i] = null;
    }
    dp.orderCount = 0;

    // ✅ Clear orders.txt
    dp.saveOrders();

    // ===============================
    // ✅ DELETE RECEIPT FILES
    // ===============================
    int[] receiptResult = deleteAllReceiptFiles();
    int receiptDeleted = receiptResult[0];
    int receiptFailed = receiptResult[1];

    // ===============================
    // ✅ ARCHIVE SIZE
    // ===============================
    long sizeBytes = 0;
    try {
        java.io.File f = new java.io.File(archivePath);
        if (f.exists()) sizeBytes = f.length();
    } catch (Exception e) {}

    long sizeKB = (sizeBytes + 1023) / 1024;

    // ===============================
    // ✅ LOGGING & AUDIT
    // ===============================
    log.write(
        "ADMIN",
        "Deleted ALL order history. Orders=" + deletedCount +
        ", Receipts=" + receiptDeleted +
        ", Backup=" + archiveFileName
    );
    dp.appendLoginAudit("DELETE_ORDERS", currentAdmin.username);

    String lastAuditLine = readLastLine(dp.path("login_audit.txt"));

    // ===============================
    // ✅ FINAL OUTPUT
    // ===============================
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

private String readLastLine(String filePath) {
    BufferedReader br = null;
    try {
        br = new BufferedReader(new FileReader(filePath));
        String line;
        String last = "";
        while ((line = br.readLine()) != null) {
            line = line.trim();
            if (!line.equals("")) last = line;
        }
        return last;
    } catch (Exception e) {
        return "";
    } finally {
        try { if (br != null) br.close(); } catch (Exception ex) {}
    }
}

private int[] deleteAllReceiptFiles() {
    int deleted = 0;
    int failed = 0;

    try {
        // Find the folder where orders.txt exists (same folder will contain receipts usually)
        java.io.File ordersFile = new java.io.File(dp.path("orders.txt"));
        java.io.File dir = ordersFile.getParentFile();

        // If no parent folder, use current directory
        if (dir == null) dir = new java.io.File(".");

        java.io.File[] files = dir.listFiles();
        if (files == null) return new int[]{0, 0};

        for (int i = 0; i < files.length; i++) {
            java.io.File f = files[i];
            if (f == null) continue;

            String name = f.getName();
            if (name == null) continue;

            // receipts like: receipt_O1016.txt or receipt_01016.txt
            if (name.startsWith("receipt_") && name.endsWith(".txt")) {
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
        // ignore (no crash)
    }

    return new int[]{deleted, failed};
}

private void restoreOrdersFromArchive(BufferedReader console) throws Exception {
    Admin currentAdmin = dp.admins[dp.currentAdminIndex];
    if (currentAdmin == null || currentAdmin.role != Role.ADMIN) {
        System.out.print(ROSE + "Access denied. Admin only.\n" + RESET);
        return;
    }

    String archivePath = dp.path("orders_archive.txt");
    java.io.File archiveFile = new java.io.File(archivePath);

    if (!archiveFile.exists()) {
        System.out.print(ROSE + "Archive file not found: orders_archive.txt\n" + RESET);
        return;
    }

    // ✅ Preview first
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

    // ✅ Confirm restore
    System.out.print(ROSE + "This will modify orders.txt.\n" + RESET);
    System.out.print(SOFTGRAY + "Type RESTORE to confirm: " + RESET);
    String conf = console.readLine();
    if (conf == null) conf = "";
    conf = conf.trim();
    if (!conf.equalsIgnoreCase("RESTORE")) {
        System.out.print(ROSE + "Restore cancelled.\n" + RESET);
        return;
    }

    // ✅ Backup orders.txt so we can UNDO
    backupOrdersBeforeRestore();

    int restoredCount = 0;
    if (opt.equals("1")) restoredCount = restoreLatestSessionOnly();
    else if (opt.equals("2")) restoredCount = restoreByDate(dateFilter);
    else restoredCount = restoreAllArchiveOrders();

    // Reload into memory
    dp.loadAll();

    log.write("ADMIN", "RESTORE from archive. Count=" + restoredCount);
    dp.appendLoginAudit("RESTORE_ORDERS", currentAdmin.username);

    System.out.print(MINT + "Restore complete.\n" + RESET);
    System.out.print(SOFTGRAY + "Orders Restored: " + RESET + MINT + restoredCount + RESET + "\n");
    System.out.print(SOFTGRAY + "You can undo using option 26.\n" + RESET);
}

private void previewArchiveSessions() {
    System.out.println(PINK + BOLD + "\nArchive Preview (Delete Sessions)" + RESET);
    printLine();

    BufferedReader br = null;
    try {
        br = new BufferedReader(new FileReader(dp.path("orders_archive.txt")));
        String line;

        String currentUser = "";
        String currentDateTime = "";
        int currentCount = 0;

        int sessionNo = 0;

        while ((line = br.readLine()) != null) {
            line = line.trim();
            if (line.equals("")) continue;

            // session header
            if (line.startsWith("=== ARCHIVE DELETE by")) {
                // print previous session
                if (sessionNo > 0) {
                    System.out.println(LAVENDER + "Session " + sessionNo + RESET +
                            SOFTGRAY + " | By: " + RESET + currentUser +
                            SOFTGRAY + " | At: " + RESET + currentDateTime +
                            SOFTGRAY + " | Orders: " + RESET + currentCount);
                    currentCount = 0;
                }
                sessionNo++;
                currentUser = line.replace("=== ARCHIVE DELETE by", "").replace("===", "").trim();
                currentDateTime = "";
                continue;
            }

            if (line.startsWith("Deleted at:")) {
                currentDateTime = line.replace("Deleted at:", "").trim();
                continue;
            }

            // order lines
            if (line.contains("|")) {
                currentCount++;
            }
        }

        // last session
        if (sessionNo > 0) {
            System.out.println(LAVENDER + "Session " + sessionNo + RESET +
                    SOFTGRAY + " | By: " + RESET + currentUser +
                    SOFTGRAY + " | At: " + RESET + currentDateTime +
                    SOFTGRAY + " | Orders: " + RESET + currentCount);
        } else {
            System.out.println(ROSE + "No archive sessions found." + RESET);
        }

    } catch (Exception e) {
        System.out.println(ROSE + "Failed to preview archive." + RESET);
    } finally {
        try { if (br != null) br.close(); } catch (Exception ex) {}
    }

    printLine();
}

private void backupOrdersBeforeRestore() {
    BufferedReader br = null;
    FileWriter fw = null;

    try {
        br = new BufferedReader(new FileReader(dp.path("orders.txt")));
        fw = new FileWriter(dp.path("orders_restore_backup.txt"), false);

        String line;
        while ((line = br.readLine()) != null) {
            fw.write(line + "\n");
        }
    } catch (Exception e) {
        // if orders.txt doesn't exist yet, still create empty backup
        try {
            fw = new FileWriter(dp.path("orders_restore_backup.txt"), false);
        } catch (Exception ex) {}
    } finally {
        try { if (br != null) br.close(); } catch (Exception ex) {}
        try { if (fw != null) fw.close(); } catch (Exception ex) {}
    }
}

private int restoreLatestSessionOnly() {
    BufferedReader br = null;
    FileWriter fw = null;

    int restored = 0;

    try {
        br = new BufferedReader(new FileReader(dp.path("orders_archive.txt")));

        // First pass: find the last session start
        String line;
        int lastSessionLine = -1;
        int lineNo = 0;

        while ((line = br.readLine()) != null) {
            lineNo++;
            if (line.trim().startsWith("=== ARCHIVE DELETE by")) {
                lastSessionLine = lineNo;
            }
        }
        br.close();

        if (lastSessionLine == -1) return 0;

        // Second pass: read from last session and write only order lines
        br = new BufferedReader(new FileReader(dp.path("orders_archive.txt")));
        fw = new FileWriter(dp.path("orders.txt"), true); // append

        lineNo = 0;
        boolean inLatest = false;

        while ((line = br.readLine()) != null) {
            lineNo++;
            String t = line.trim();

            if (lineNo == lastSessionLine) inLatest = true;

            if (!inLatest) continue;

            if (t.equals("") || t.startsWith("===") || t.startsWith("Deleted at:")) continue;

            if (t.contains("|")) {
                fw.write(t + "\n");
                restored++;
            }
        }

    } catch (Exception e) {
        return restored;
    } finally {
        try { if (br != null) br.close(); } catch (Exception ex) {}
        try { if (fw != null) fw.close(); } catch (Exception ex) {}
    }

    return restored;
}
private int restoreByDate(String dateFilter) {
    BufferedReader br = null;
    FileWriter fw = null;

    int restored = 0;

    try {
        br = new BufferedReader(new FileReader(dp.path("orders_archive.txt")));
        fw = new FileWriter(dp.path("orders.txt"), true);

        String line;
        boolean sessionMatch = false;

        while ((line = br.readLine()) != null) {
            String t = line.trim();
            if (t.equals("")) continue;

            if (t.startsWith("=== ARCHIVE DELETE by")) {
                sessionMatch = false; // reset for new session
                continue;
            }

            if (t.startsWith("Deleted at:")) {
                String dt = t.replace("Deleted at:", "").trim();
                // match only date part
                if (dt.startsWith(dateFilter)) sessionMatch = true;
                continue;
            }

            if (!sessionMatch) continue;

            if (t.startsWith("===") || t.startsWith("Deleted at:")) continue;

            if (t.contains("|")) {
                fw.write(t + "\n");
                restored++;
            }
        }

    } catch (Exception e) {
        return restored;
    } finally {
        try { if (br != null) br.close(); } catch (Exception ex) {}
        try { if (fw != null) fw.close(); } catch (Exception ex) {}
    }

    return restored;
}

private int restoreAllArchiveOrders() {
    BufferedReader br = null;
    FileWriter fw = null;

    int restored = 0;

    try {
        br = new BufferedReader(new FileReader(dp.path("orders_archive.txt")));
        fw = new FileWriter(dp.path("orders.txt"), true);

        String line;
        while ((line = br.readLine()) != null) {
            String t = line.trim();
            if (t.equals("")) continue;

            if (t.startsWith("===") || t.startsWith("Deleted at:")) continue;

            if (t.contains("|")) {
                fw.write(t + "\n");
                restored++;
            }
        }

    } catch (Exception e) {
        return restored;
    } finally {
        try { if (br != null) br.close(); } catch (Exception ex) {}
        try { if (fw != null) fw.close(); } catch (Exception ex) {}
    }

    return restored;
}
private void undoLastRestore(BufferedReader console) throws Exception {
    Admin currentAdmin = dp.admins[dp.currentAdminIndex];
    if (currentAdmin == null || currentAdmin.role != Role.ADMIN) {
        System.out.print(ROSE + "Access denied. Admin only.\n" + RESET);
        return;
    }

    java.io.File backupFile = new java.io.File(dp.path("orders_restore_backup.txt"));
    if (!backupFile.exists()) {
        System.out.print(ROSE + "No restore backup found. Cannot undo.\n" + RESET);
        return;
    }

    System.out.print(ROSE + "Undo will revert orders.txt to previous state.\n" + RESET);
    System.out.print(SOFTGRAY + "Type UNDO to confirm: " + RESET);
    String conf = console.readLine();
    if (conf == null) conf = "";
    conf = conf.trim();

    if (!conf.equalsIgnoreCase("UNDO")) {
        System.out.print(ROSE + "Undo cancelled.\n" + RESET);
        return;
    }

    BufferedReader br = null;
    FileWriter fw = null;

    try {
        br = new BufferedReader(new FileReader(dp.path("orders_restore_backup.txt")));
        fw = new FileWriter(dp.path("orders.txt"), false); // overwrite

        String line;
        while ((line = br.readLine()) != null) {
            fw.write(line + "\n");
        }
    } finally {
        try { if (br != null) br.close(); } catch (Exception ex) {}
        try { if (fw != null) fw.close(); } catch (Exception ex) {}
    }

    dp.loadAll();

    log.write("ADMIN", "UNDO restore (orders.txt reverted).");
    dp.appendLoginAudit("UNDO_RESTORE", currentAdmin.username);

    System.out.print(MINT + "Undo successful. orders.txt restored to previous state.\n" + RESET);
}
   }



