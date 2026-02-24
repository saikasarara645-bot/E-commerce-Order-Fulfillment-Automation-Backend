import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Comparator;

import javax.management.relation.Role;



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
    System.out.println(SOFTGRAY + "────────────────────────────────────────" + RESET);
    }

    private void printTitle(String text) {
    printLine();
    System.out.println(PINK + BOLD + text + RESET);
    printLine();
    }
     private void printDashboardBox(Admin admin) {
    String top    = "╔══════════════════════════════════════╗";
    String mid    = "║                                      ║";
    String bottom = "╚══════════════════════════════════════╝";

    System.out.println(SOFTGRAY + top + RESET);

    // Centered Title line
    String title = "ADMIN DASHBOARD";
    System.out.println(SOFTGRAY + "║" + RESET
            + PINK + BOLD + centerText(title, 38) + RESET
            + SOFTGRAY + "║" + RESET);

    System.out.println(SOFTGRAY + mid + RESET);

    // Info line: Logged in as
    String info = "Logged in as: " + admin.username + " (" + admin.role + ")";
    System.out.println(SOFTGRAY + "║" + RESET
            + LAVENDER + centerText(info, 38) + RESET
            + SOFTGRAY + "║" + RESET);

    System.out.println(SOFTGRAY + bottom + RESET);
}
private String centerText(String text, int width) {
    if (text == null) text = "";
    if (text.length() >= width) return text.substring(0, width);

    int left = (width - text.length()) / 2;
    int right = width - text.length() - left;

    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < left; i++) sb.append(' ');
    sb.append(text);
    for (int i = 0; i < right; i++) sb.append(' ');
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
private int countLowStock(int threshold) {
    int c = 0;
    for (int i = 0; i < dp.productCount; i++) {
        Product p = dp.products[i];
        if (p != null && p.stock <= threshold) c++;
    }
    return c;
}

    /** Admin Dashboard menu loop handling all features */
 public void adminDashboard(BufferedReader console) throws Exception {

    // Always refresh current admin (in case index changes later)
    Admin currentAdmin = dp.admins[dp.currentAdminIndex];
      printDashboardBox(currentAdmin);
      printLine();
    while (true) {

        // refresh current admin each loop (safe)
        currentAdmin = dp.admins[dp.currentAdminIndex];
        printRoleSummary(currentAdmin);
        printLine();
        // ===== MENU HEADER =====
        System.out.print("\n" + LAVENDER + BOLD + "____________________Menu:____________________" + RESET + "\n");

        // ===== ORDER MANAGEMENT =====
        System.out.print(PINK + BOLD + "ORDER MANAGEMENT" + RESET + "\n");
        System.out.print(LAVENDER + " 1." + RESET + " " + MINT + "Accept New Order" + RESET + "\n");
        System.out.print(LAVENDER + " 2." + RESET + " " + MINT + "Update Order Status" + RESET + "\n");
        System.out.print(LAVENDER + " 3." + RESET + " " + MINT + "View Order Logs" + RESET + "\n");
        System.out.print(LAVENDER + " 4." + RESET + " " + MINT + "Search/Filter Orders" + RESET + "\n");
        System.out.print(LAVENDER + " 5." + RESET + " " + MINT + "Generate Receipt" + RESET + "\n");

        // ===== PRODUCT & STOCK =====
        System.out.print("\n" + PINK + BOLD + "PRODUCT & STOCK" + RESET + "\n");
        System.out.print(LAVENDER + " 6." + RESET + " " + MINT + "Advanced Product Filter" + RESET + "\n");

        // Admin/Manager
        if (currentAdmin.role == Role.ADMIN || currentAdmin.role == Role.MANAGER) {
            System.out.print(LAVENDER + " 7." + RESET + " " + MINT + "Manage Products (Add/Edit/Delete)" + RESET + "\n");
            System.out.print(LAVENDER + " 8." + RESET + " " + MINT + "Low Stock Alerts" + RESET + "\n");
            System.out.print(LAVENDER + " 9." + RESET + " " + MINT + "Restock Product" + RESET + "\n");
            System.out.print(LAVENDER + "10." + RESET + " " + MINT + "Export Stock Report" + RESET + "\n");
        } else {
            // show restricted in peach (professional)
            System.out.print(LAVENDER + " 7." + RESET + " " + ROSE + "Manage Products (Admin/Manager only)" + RESET + "\n");
            System.out.print(LAVENDER + " 8." + RESET + " " + ROSE + "Low Stock Alerts (Admin/Manager only)" + RESET + "\n");
            System.out.print(LAVENDER + " 9." + RESET + " " + ROSE + "Restock Product (Admin/Manager only)" + RESET + "\n");
            System.out.print(LAVENDER + "10." + RESET + " " + ROSE + "Export Stock Report (Admin/Manager only)" + RESET + "\n");
        }

        // ===== OPERATIONS =====
        System.out.print("\n" + PINK + BOLD + "OPERATIONS" + RESET + "\n");
        System.out.print(LAVENDER + "11." + RESET + " " + MINT + "Reorder Previous Order" + RESET + "\n");
        System.out.print(LAVENDER + "12." + RESET + " " + MINT + "Retry Failed Order" + RESET + "\n");
        System.out.print(LAVENDER + "13." + RESET + " " + MINT + "Simulation Mode" + RESET + "\n");
        System.out.print(LAVENDER + "14." + RESET + " " + MINT + "Load Test Data" + RESET + "\n");
        System.out.print(LAVENDER + "15." + RESET + " " + MINT + "System Health Check" + RESET + "\n");
        System.out.print(LAVENDER + "16." + RESET + " " + MINT + "Show Order Timeline" + RESET + "\n");
        System.out.print(LAVENDER + "17." + RESET + " " + MINT + "Auto Cancel Stale Orders" + RESET + "\n");
      
        // ===== ADMIN ONLY =====
        System.out.print("\n" + PINK + BOLD + "SYSTEM (ADMIN ONLY)" + RESET + "\n");
        if (currentAdmin.role == Role.ADMIN) {
            System.out.print(LAVENDER + "18." + RESET + " " + MINT + "Bulk Import Orders" + RESET + "\n");
            System.out.print(LAVENDER + "19." + RESET + " " + MINT + "Archive Delivered Orders" + RESET + "\n");
            System.out.print(LAVENDER + "20." + RESET + " " + MINT + "Clear Logs" + RESET + "\n");
            System.out.print(LAVENDER + "21." + RESET + " " + MINT + "Add New Admin" + RESET + "\n");
            System.out.print(LAVENDER + "22." + RESET + " " + MINT + "Change Admin Password" + RESET + "\n");
            System.out.print(LAVENDER + "23." + RESET + " " + MINT + "Generate Report" + RESET + "\n");
            System.out.print(LAVENDER+"24."+RESET+" " + MINT+ "Delete ALL Order History" + RESET + "\n");
            System.out.print(LAVENDER+"25."+RESET+" " + MINT + "Restore Order History (Archive)" + RESET + "\n");
            System.out.print(LAVENDER+"26."+RESET +" "+ MINT + "Undo Last Restore" + RESET + "\n");


        } else {
            System.out.print(LAVENDER + "18." + RESET + " " + ROSE + "Bulk Import Orders (Admin only)" + RESET + "\n");
            System.out.print(LAVENDER + "19." + RESET + " " + ROSE + "Archive Delivered Orders (Admin only)" + RESET + "\n");
            System.out.print(LAVENDER + "20." + RESET + " " + ROSE + "Clear Logs (Admin only)" + RESET + "\n");
            System.out.print(LAVENDER + "21." + RESET + " " + ROSE + "Add New Admin (Admin only)" + RESET + "\n");
            System.out.print(LAVENDER + "22." + RESET + " " + ROSE + "Change Admin Password (Admin only)" + RESET + "\n");
            System.out.print(LAVENDER + "23." + RESET + " " + ROSE + "Generate Report (Admin only)" + RESET + "\n");
            System.out.print(LAVENDER+"24."+RESET +" "+ ROSE + "Delete ALL Order History(Admin only)" + RESET + "\n");
            System.out.print(LAVENDER+"25."+RESET + " "+ROSE + "Restore Order History(Admin Only)" + RESET + "\n");
            System.out.print(LAVENDER+"26."+RESET + " "+ROSE + "Undo Last Restore(Admin Only)" + RESET + "\n");


        }
       
        // ===== EXIT =====
        System.out.print("\n" + LAVENDER + " 0." + RESET + " " + ROSE + "Exit" + RESET + "\n");
        printLine();
        System.out.print(PINK + BOLD + "Please select an option → " + RESET);

        String choice = console.readLine();
        if (choice == null) choice = "";
        choice = choice.trim();
        System.out.print("\n");
        if (!choice.equals("")) {
        System.out.println(MINT + "You selected option: " + choice + RESET);
        printLine();   // optional but looks professional
        }
        switch (choice) {
            case "1": acceptNewOrder(console); break;
            case "2": handleStatusUpdate(console); break;

            case "3":
                System.out.println(PINK + BOLD + "==== Available Orders (Sorted by Date) ====" + RESET);
                printLine();
                Order[] sortedOrders = Arrays.copyOf(dp.orders, dp.orderCount);
                Arrays.sort(sortedOrders, Comparator.comparing(o -> o.date));

                for (Order order : sortedOrders) {
                    if (order != null) {
                        System.out.println(SOFTGRAY + order.orderId + RESET + SOFTGRAY + " | Date: " + RESET + MINT + order.date + RESET + SOFTGRAY + " | Status: " + RESET + LAVENDER + order.status + RESET);
                    }
                }

                System.out.print(LAVENDER + "Enter Order ID to view logs: " + RESET);
                String logId = console.readLine();
                if (logId != null && !logId.trim().equals("")) {
                    logId = normalizeOrderId(logId.trim());
                    log.viewLogsByOrder(logId);
                }
                break;

            case "4": handleOrderSearch(console); break;
            case "5": generateReceipt(console); break;
            case "6": handleAdvancedFilter(console); break;

            case "7":
                if (currentAdmin.role == Role.ADMIN || currentAdmin.role == Role.MANAGER) {
                    handleProductManagement(console);
                } else {
                    System.out.println(ROSE + "Restricted: Admin/Manager only." + RESET);
                }
                break;

            case "8":
                if (currentAdmin.role == Role.ADMIN || currentAdmin.role == Role.MANAGER) {
                    showLowStockAlerts();
                } else {
                    System.out.println(ROSE + "Restricted: Admin/Manager only." + RESET);
                }
                break;

            case "9":
                if (currentAdmin.role == Role.ADMIN || currentAdmin.role == Role.MANAGER) {
                    handleRestock(console);
                } else {
                    System.out.println(ROSE + "Restricted: Admin/Manager only." + RESET);
                }
                break;

            case "10":
                if (currentAdmin.role == Role.ADMIN || currentAdmin.role == Role.MANAGER) {
                    exportStockReport();
                } else {
                    System.out.println(ROSE + "Restricted: Admin/Manager only." + RESET);
                }
                break;
            case "11": handleReorder(console); break;
            case "12": retryCancelledOrder(console); break;
            case "13": runSimulation(console); break;
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
            case "15": systemHealthCheck(); break;
            case "16": showOrderTimeline(console); break;
            case "17": autoCancelStaleOrders(2); break;
            case "18":
                if (currentAdmin.role == Role.ADMIN) {
                    importOrdersFromFile(console);
                } else {
                    System.out.println(ROSE + "Restricted: Admin only." + RESET);
                }
                break;
            
            case "19":
                if (currentAdmin.role == Role.ADMIN) {
                    archiveDeliveredOrders(console);
                } else {
                    System.out.println(ROSE + "Restricted: Admin only." + RESET);
                }
                break;

            case "20":
                if (currentAdmin.role == Role.ADMIN) {
                    clearLogs(console);
                } else {
                    System.out.println(ROSE + "Restricted: Admin only." + RESET);
                }
                break;
            case "21":
                if (currentAdmin.role == Role.ADMIN) {
                    addNewAdmin(console);
                } else {
                    System.out.println(ROSE + "Restricted: Admin only." + RESET);
                }
                break; 
            
            case "22":
                if (currentAdmin.role == Role.ADMIN) {
                    changeAdminPassword(console);
                } else {
                    System.out.println(ROSE + "Restricted: Admin only." + RESET);
                }
                break;
    
            case "23":
                if (currentAdmin.role == Role.ADMIN) {
                    generateReport();
                } else {
                    System.out.println(ROSE + "Restricted: Admin only." + RESET);
                }
                break;
            
            case "24":
                  if (currentAdmin.role == Role.ADMIN) {
                    deleteAllOrderHistory(console);
                } else {
                    System.out.println(ROSE + "Restricted: Admin only." + RESET);
                }
                break;case "25":
                  if (currentAdmin.role == Role.ADMIN) {
                    restoreOrdersFromArchive(console);
                } else {
                    System.out.println(ROSE + "Restricted: Admin only." + RESET);
                }
                break;

            case "26":
                  if (currentAdmin.role == Role.ADMIN) {
                      undoLastRestore(console);
                } else {
                    System.out.println(ROSE + "Restricted: Admin only." + RESET);
                }
                break;    

  
            case "0":
                System.out.print(LAVENDER + "Exiting Admin Dashboard..." + RESET + "\n");
                System.out.print(LAVENDER+ "Thank you for using E-commerce Order Fulfillment Automation System" + RESET + "\n");
                return;

            default:
                System.out.print(ROSE + "Invalid option. Please try again." + RESET + "\n");
                break;
        }

        printLine();
    }
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

            if (!statusFilterUC.equals("") && (o.status == null || !o.status.toUpperCase().equals(statusFilterUC))) {
                continue;
            }
            if (!paymentFilterUC.equals("") && (o.paymentMode == null || !o.paymentMode.toUpperCase().equals(paymentFilterUC))) {
                continue;
            }
            if (!dateFilter.equals("") && (o.date == null || !o.date.equals(dateFilter))) {
                continue;
            }

            results[count++] = o;
        }

        if (count == 0) {
            System.out.print(ROSE + "No orders found matching the given criteria." + RESET + "\n");
        } else {
            String statusCrit = statusFilter.equals("") ? "Any" : statusFilter;
            String payCrit = paymentFilter.equals("") ? "Any" : paymentFilter;
            String dateCrit = dateFilter.equals("") ? "Any" : dateFilter;

            System.out.print(SOFTGRAY + "Orders matching filters - Status: " + RESET + statusCrit
                    + SOFTGRAY + ", Payment: " + RESET + payCrit
                    + SOFTGRAY + ", Date: " + RESET + dateCrit + ":\n" + RESET);

            for (int i = 0; i < count; i++) {
                Order o = results[i];

                // ✅ Status color (added PACKED + OUT_FOR_DELIVERY)
                String statusStr = o.status;
                if ("DELIVERED".equals(statusStr)) statusStr = LAVENDER + statusStr + RESET;
                else if ("CANCELLED".equals(statusStr)) statusStr = ROSE + statusStr + RESET;
                else if ("PENDING".equals(statusStr)) statusStr = MINT + statusStr + RESET;
                else if ("SHIPPED".equals(statusStr)) statusStr = MINT + statusStr + RESET;
                else if ("PACKED".equals(statusStr)) statusStr = MINT + statusStr + RESET;
                else if ("OUT_FOR_DELIVERY".equals(statusStr)) statusStr = MINT+ statusStr + RESET;

                int total = safeOrderTotal(o); // ✅ FIX total 0 issue

                System.out.print("- " + o.orderId + " | Date: " + o.date
                        + " | Payment: " + o.paymentMode
                        + " | Status: " + statusStr
                        + " | Total: BDT " + total);

                if ("CANCELLED".equals(o.status) && o.cancelReason != null && !o.cancelReason.equals("")) {
                    System.out.print(ROSE + " | CancelReason: " + o.cancelReason + RESET);
                }
                System.out.print("\n");
            }

            printLine();
            System.out.print(SOFTGRAY + "Enter Order ID to view details (or press Enter to skip): " + RESET);
            String selId = console.readLine();
            if (selId == null) selId = "";
            selId = selId.trim();

            // ✅ STRICT: must type exact ID like 01001 (no normalizeOrderId)
            if (!selId.equals("")) {
                Order target = null;
                for (int i = 0; i < dp.orderCount; i++) {
                    Order o = dp.orders[i];
                    if (o != null && o.orderId.equalsIgnoreCase(selId)) {
                        target = o;
                        break;
                    }
                }
                if (target != null) viewOrderDetails(target);
                else System.out.print(ROSE + "Order " + selId + " not found in results.\n" + RESET);
            }
        }
        return;
    }

    // ===========================
    // STANDARD SEARCH MODE
    // ===========================

    String q = query.trim();
    String idTry=normalizeOrderId(q);

    // ✅ STRICT ID RULE:
    // Remove this old behavior:
    // if (!q.startsWith("O") && isNumeric(q)) q = "O" + q;
    // Now user must type EXACT orderId (01001), not 1001.

    // Try exact Order ID match
    Order found = null;
    for (int i = 0; i < dp.orderCount; i++) {
        Order o = dp.orders[i];
        if (o != null && o.orderId != null && o.orderId.equalsIgnoreCase(idTry)) {
            found = o;
            break;
        }
    }

    if (found != null) {
        viewOrderDetails(found);
        return;
    }

    // Otherwise treat input as status query
    String statusQuery = q;
    Order[] results = new Order[dp.orderCount];
    int count = 0;

    for (int i = 0; i < dp.orderCount; i++) {
        Order o = dp.orders[i];
        if (o == null || o.status == null) continue;

        if (o.status.toUpperCase().contains(statusQuery)) {
            results[count++] = o;
        }
    }

    if (count == 0) {
        System.out.print(ROSE + "No orders found matching \"" + query + "\".\n" + RESET);
    } else {
        System.out.print("Orders with status containing \"" + query + "\":\n");

        for (int i = 0; i < count; i++) {
            Order o = results[i];

            String statusStr = o.status;
            if ("DELIVERED".equals(statusStr)) statusStr = LAVENDER + statusStr + RESET;
            else if ("CANCELLED".equals(statusStr)) statusStr = ROSE + statusStr + RESET;
            else if ("PENDING".equals(statusStr)) statusStr = MINT + statusStr + RESET;
            else if ("SHIPPED".equals(statusStr)) statusStr = MINT + statusStr + RESET;
            else if ("PACKED".equals(statusStr)) statusStr = MINT + statusStr + RESET;
            else if ("OUT_FOR_DELIVERY".equals(statusStr)) statusStr = MINT + statusStr + RESET;

            int total = safeOrderTotal(o); // ✅ FIX total 0 issue

            System.out.print(SOFTGRAY + "- " + o.orderId + " | Status: " + statusStr + " | Total: BDT " + total + RESET);

            if (o.cancelReason != null && !o.cancelReason.equals("")) {
                System.out.print(ROSE + " | CancelReason: " + o.cancelReason + RESET);
            }
            System.out.print("\n");
        }

        System.out.print(SOFTGRAY + "Enter Order ID to view details (or press Enter to skip): " + RESET);
        String selId = console.readLine();
        if (selId == null) selId = "";
        selId = selId.trim();

        // ✅ STRICT ID (no normalizeOrderId)
        if (!selId.equals("")) {
            Order target = null;
            for (int i = 0; i < dp.orderCount; i++) {
                Order o = dp.orders[i];
                if (o != null && o.orderId.equalsIgnoreCase(selId)) {
                    target = o;
                    break;
                }
            }
            if (target != null) viewOrderDetails(target);
            else System.out.print(ROSE + "Order " + selId + " not found in results.\n" + RESET);
        }
    }
}
  /** Feature 6: Manually progress an order status through the workflow (PENDING -> PACKED -> SHIPPED -> OUT_FOR_DELIVERY -> DELIVERED) */
    private void handleStatusUpdate(BufferedReader console) throws Exception {
    showOrdersForStatusUpdate();
    System.out.print(SOFTGRAY+"Enter Order ID to update status: "+RESET);
    String id = console.readLine();
    if (id == null) id = "";
    id = id.trim();
    if (id.equals("")) {
        System.out.print(ROSE+"Order ID cannot be empty.\n"+RESET);
        return;
    }
    id = normalizeOrderId(id);
    // Find the order by ID
    Order order = null;
    for (int i = 0; i < dp.orderCount; i++) {
        Order o = dp.orders[i];
        if (o != null && o.orderId.equalsIgnoreCase(id)) {
            order = o;
            break;
        }
    }
    if (order == null) {
        System.out.print(ROSE+"Order " + id + " not found.\n"+RESET);
        return;
    }
    String currentStatus = order.status;
    // If order already delivered or cancelled, no further updates allowed
    if (currentStatus.equals("DELIVERED") || currentStatus.equals("CANCELLED")) {
        System.out.print(ROSE+"Order " + id + " is " + currentStatus + "; status cannot be changed.\n"+RESET);
        return;
    }
    // If order is PENDING, attempt to process it (inventory check & payment)
    if (currentStatus.equals("PENDING")) {
        boolean processed = processPendingOrder(order, console);
        if (!processed) {
            // If processing failed, order status is now CANCELLED (reason set in processPendingOrder)
            System.out.print(ROSE+"Order processing failed. Status updated to CANCELLED ("+ order.cancelReason + ").\n"+RESET);
            dp.saveOrders();
            return;
        }
        // If processing succeeded, the order status is now PACKED
        currentStatus = order.status;
    }
    // Determine the next status in the workflow sequence
    String nextStatus = null;
    if (currentStatus.equals("PACKED")) {
        nextStatus = "SHIPPED";
    } else if (currentStatus.equals("SHIPPED")) {
        nextStatus = "OUT_FOR_DELIVERY";
    } else if (currentStatus.equals("OUT_FOR_DELIVERY")) {
        nextStatus = "DELIVERED";
    }
    if (nextStatus == null) {
        System.out.print("No further status transition available for " + currentStatus + ".\n");
        return;
    }
    // Update order status to the next stage
    order.status = nextStatus;
    if (nextStatus.equals("SHIPPED")) {
        // Assign a tracking ID once the order is shipped
        order.trackingId = "TRK" + order.orderId.substring(1);  // e.g., O1005 -> TRK1005
    }
    // Persist the updated orders list to file
    dp.saveOrders();
    log.write(order.orderId, "Status changed to " + nextStatus);
    System.out.print(MINT+"Order " + order.orderId + " status updated to " + nextStatus + ".\n"+RESET);
}
  /** Feature 5 & 8: Reorder a previous order (copy its items into a new order and process it) */
    private void handleReorder(BufferedReader console) throws Exception {
        showReorderPreview();
        System.out.print(SOFTGRAY+"Enter Order ID to reorder: "+RESET);
        String oldId = console.readLine();
        if (oldId == null) oldId = "";
        oldId = oldId.trim();
        if (oldId.equals("")) {
            System.out.print(ROSE+"Order ID cannot be empty.\n"+RESET);
            return;
        }
        oldId = normalizeOrderId(oldId);
        // Find the original order
        Order original = null;
        for (int i = 0; i < dp.orderCount; i++) {
            if (dp.orders[i] != null && dp.orders[i].orderId.equalsIgnoreCase(oldId)) {
                original = dp.orders[i];
                break;
            }
        }
        if (original == null) {
            System.out.print(ROSE+"Order " + oldId + " not found.\n"+RESET);
            return;
        }
        // Create a new order with the same items (and same address/payment as original, if available)
        Order newOrder = new Order();
        newOrder.orderId = dp.generateOrderId();
        newOrder.date = currentDateString();
        newOrder.address = original.address;
        newOrder.paymentMode = original.paymentMode.equals("") ? "COD" : original.paymentMode;
        // Copy each item from original
        for (int j = 0; j < original.itemCount; j++) {
            Item it = original.items[j];
            if (it == null) continue;
            newOrder.addItem(new Item(it.productId, it.quantity));
        }
        // Process the new order through inventory & payment
        boolean success = processPendingOrder(newOrder, console);
        // Add the new order to system records
        dp.orders[dp.orderCount++] = newOrder;
        if (!success) {
            System.out.print(ROSE+"Reorder created as " + newOrder.orderId + " but failed (" + newOrder.cancelReason + ").\n"+RESET);
        } else {
            System.out.print(MINT+"Reorder successful! New Order ID: " + newOrder.orderId + " (Status: " + newOrder.status + ").\n"+RESET);
            log.write(newOrder.orderId, "Reordered from " + oldId);
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
    System.out.print(SOFTGRAY+"Simulation scenarios:\n"+RESET);
    System.out.print(SOFTGRAY+"1. Successful order\n"+RESET);
    System.out.print(SOFTGRAY+"2. Payment failure scenario\n"+RESET);
    System.out.print(SOFTGRAY+"3. Inventory shortage scenario\n"+RESET);
    System.out.print(SOFTGRAY+"4. Random order scenario\n"+RESET);
    System.out.print(SOFTGRAY+"Choose scenario (1-4): "+RESET);
    String opt = console.readLine();
    if (opt == null) opt = "";
    opt = opt.trim();
    if (!opt.matches("[1-4]")) {
        System.out.print(ROSE+"Invalid scenario selection.\n"+RESET);
        return;
    }
    // Create a simulated order
    Order simOrder = new Order();
    simOrder.orderId = dp.generateOrderId();
    simOrder.date = currentDateString();
    simOrder.status = "PENDING";  // Default status

    // Build order based on scenario choice
    if (opt.equals("2")) {
        // Scenario 2: Payment failure – ensure total triggers a decline (simulate by prompting N)
        Product p = dp.products[0];
        if (p == null) {
            System.out.print(ROSE+"No products available for simulation.\n"+RESET);
            return;
        }
        simOrder.addItem(new Item(p.productId, 1));
        simOrder.paymentMode = "MockCard";
        simOrder.status = "CANCELLED"; // Simulate failure
        simOrder.cancelReason = "Payment Failure (MockCard)";
    } else if (opt.equals("3")) {
        // Scenario 3: Inventory shortage – order more than available stock of a product
        Product p = null;
        for (int i = 0; i < dp.productCount; i++) {
            if (dp.products[i] != null && dp.products[i].stock > 0 && dp.products[i].stock < 10) {
                p = dp.products[i];
                break;
            }
        }
        if (p == null) {
            p = dp.products[0];
        }
        int largeQty = (p.stock == 0 ? 5 : p.stock + 5);
        simOrder.addItem(new Item(p.productId, largeQty));
        simOrder.paymentMode = "COD";
        // Mark the order as cancelled due to inventory shortage
        simOrder.status = "CANCELLED"; // Simulate cancellation
        simOrder.cancelReason = "Inventory Shortage";
    } else {
        // Scenario 1 or 4: Successful or Random order – pick 1-2 random items within stock
        if (dp.productCount == 0) {
            System.out.print(ROSE+"No products available to simulate order.\n"+RESET);
            return;
        }
        Product p1 = dp.products[0];
        simOrder.addItem(new Item(p1.productId, 1));
        if (opt.equals("4") && dp.productCount > 1) {
            Product p2 = dp.products[1];
            simOrder.addItem(new Item(p2.productId, 1));
        }
        simOrder.paymentMode = "COD";
        // Successful order – set the status as "DELIVERED"
        simOrder.status = "DELIVERED"; // Mark as delivered for successful order
    }
    simOrder.address = "SimulatedAddress";

    // Process the simulated order
     processPendingOrder(simOrder, console);


    // Add to system records (orders.txt)
    dp.orders[dp.orderCount++] = simOrder;
    // Log to orders.txt
    dp.saveOrders();
    System.out.print(MINT+"Simulation Order " + simOrder.orderId + " created (Status: " + simOrder.status + ").\n"+RESET);
    // Log to log.txt
    log.write(simOrder.orderId, "Simulation order with status: " + simOrder.status);
}
   /** Feature 8: Retry processing a failed (cancelled) order by creating a fresh attempt */
    private void retryCancelledOrder(BufferedReader console) throws Exception {
            // ✅ Show cancelled orders first
        printTitle("Cancelled Orders:");
         boolean found = false;

        for (int i = 0; i < dp.orderCount; i++) {
             Order o = dp.orders[i];
        if (o != null && o.status.equals("CANCELLED")) {
            System.out.print("- " + o.orderId +
                             " | Reason: " + o.cancelReason + "\n");
            found = true;
        }
    }

    if (!found) {
        System.out.print(ROSE+"No cancelled orders to retry.\n"+RESET);
        return;
    }
        System.out.print(SOFTGRAY+"Enter Cancelled Order ID to retry: "+RESET);
        String cid = console.readLine();
        if (cid == null) cid = "";
        cid = cid.trim();
        if (cid.equals("")) {
            System.out.print(ROSE+"Order ID cannot be empty.\n"+RESET);
            return;
        }
        cid = normalizeOrderId(cid);
        // Find the cancelled order
        Order original = null;
        for (int i = 0; i < dp.orderCount; i++) {
            Order o = dp.orders[i];
            if (o != null && o.orderId.equalsIgnoreCase(cid) && o.status.equals("CANCELLED")) {
                original = o;
                break;
            }
        }
        if (original == null) {
            System.out.print(ROSE+"Order " + cid + " not found in cancelled list.\n"+RESET);
            return;
        }
        // Use handleReorder logic to attempt the order again (with same items)
        Order retryOrder = new Order();
        retryOrder.orderId = dp.generateOrderId();
        retryOrder.date = currentDateString();
        retryOrder.address = original.address;
        retryOrder.paymentMode = original.paymentMode.equals("") ? "COD" : original.paymentMode;
        for (int j = 0; j < original.itemCount; j++) {
            Item it = original.items[j];
            if (it == null) continue;
            retryOrder.addItem(new Item(it.productId, it.quantity));
        }
        boolean success = processPendingOrder(retryOrder, console);
        dp.orders[dp.orderCount++] = retryOrder;
        if (success) {
            System.out.print(MINT+"Order " + retryOrder.orderId + " reprocessed successfully (Status: " + retryOrder.status + ").\n"+RESET);
            log.write(retryOrder.orderId, "Retry successful for " + cid);
        } else {
            System.out.print(ROSE+"Retry order failed (" + retryOrder.cancelReason + "). New Order ID: " + retryOrder.orderId + "\n"+RESET);
        }
    }
 /** Feature 12: Archive delivered orders older than N days (moves them to archive_orders.txt and removes from active list) */
    private void archiveDeliveredOrders(BufferedReader console) throws Exception {
        System.out.print(SOFTGRAY+"Archive delivered orders older than how many days? "+RESET);
        String daysStr = console.readLine();
        if (daysStr == null) daysStr = "";
        daysStr = daysStr.trim();
        int N = DataPersistence.toInt(daysStr);
        if (N <= 0) {
            System.out.print(ROSE+"Invalid number of days.\n"+RESET);
            return;
        }
        String todayStr = currentDateString();
        // Convert date to a simple numeric day count (approximate)
        int todayCount = dateToDayCount(todayStr);
        FileWriter fw = new FileWriter(dp.path("archive_orders.txt"), true);
        int archivedCount = 0;
        // Use a new array to store remaining orders after archiving
        Order[] remaining = new Order[dp.orders.length];
        int remCount = 0;
        for (int i = 0; i < dp.orderCount; i++) {
            Order o = dp.orders[i];
            if (o == null) continue;
            if (o.status.equals("DELIVERED")) {
                // Calculate age in days
                int orderDayCount = dateToDayCount(o.date);
                int age = todayCount - orderDayCount;
                if (age > N) {
                    // Write order record to archive file
                    fw.write(o.toRecord() + "\n");
                    archivedCount++;
                    // Skip adding it to remaining active orders (effectively removing it)
                    log.write(o.orderId, "Archived after delivery (age " + age + " days)");
                    continue;
                }
            }
            // Keep order in the remaining list if not archived
            remaining[remCount++] = o;
        }
        fw.close();
        // Replace the active orders list with the remaining orders
        dp.orders = remaining;
        dp.orderCount = remCount;
        System.out.print(MINT+"Archived " + archivedCount + " delivered orders (older than " + N + " days).\n"+RESET);
    }  
}
