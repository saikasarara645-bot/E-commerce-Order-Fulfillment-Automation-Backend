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
            System.out.print(LAVENDER+"26."+RESET + " "+ROSE + "Undo Last Restore" + RESET + "\n");


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
