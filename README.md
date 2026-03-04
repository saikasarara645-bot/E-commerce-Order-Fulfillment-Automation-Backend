# E-commerce-Order-Fulfillment-Automation-Backend

#Project Overview

The E-commerce Order Fulfillment Automation Backend is a Java-based command-line interface (CLI) application designed to simulate an end-to-end order fulfillment workflow. This system manages the order lifecycle, including order intake, payment processing, inventory management, order status tracking, and reporting.

The backend system is designed to be used by admins who can perform various tasks like accepting orders, updating order statuses, managing products, handling payments, generating invoices, and more.

#Features

#1. Order Management

-**Accept New Order**: Admins can create new orders and assign products to them.

-**Update Order Status**: Admins can manually update the status of an order (e.g., from "PENDING" to "PACKED").

-**View Order Logs**: Admins can view logs associated with orders.

-**Search/Filter Orders**: Admins can search orders by ID or filter by status.

-**Generate Receipt**: Admins can generate a receipt for a delivered order.

#2. Product & Stock Management

-**Advanced Product Filter**: Admins can filter products based on categories and brands.

-**Manage Products**: Admins can add, edit, or delete products.

-**Low Stock Alerts**: The system alerts when stock is running low for a product.

-**Restock Product**: Admins can increase stock quantities for products.

-**Export Stock Report**: Admins can generate a stock report showing current inventory.

#3. Admin Operations

-**Bulk Import Orders**: Admins can bulk import orders from external files.

-**Archive Delivered Orders**: Admins can archive orders that have been delivered.

-**Clear Logs**: Admins can clear logs to maintain a clean audit trail.

-**Add New Admin**: Only admins can add new admin accounts.

-**Change Admin Password**: Admins can change their password securely.

#4. System Features (Admin Only)

-**Simulation Mode**: Admins can simulate different order processing scenarios.

-**Load Test Data**: Admins can load test data into the system for scenarios.

-**Generate Report**: Admins can generate detailed reports about orders, revenue, and more.

-**Undo Last Restore**: Admins can undo the last data restoration.

#5. Order Processing Logic

-**Order Validation**: Checks that orders are valid (correct data, items, and quantities).

-**Inventory Verification & Reservation**: Verifies product availability and reserves stock.

-**Invoice Generation**: Generates an invoice after payment is confirmed.

-**Payment Simulation**: Supports Cash on Delivery (COD) and MockCard payments.

-**Shipping Simulation**: Tracks orders with generated tracking IDs and simulates shipping delays.

#Technologies

**Programming Language**: Java (JDK 8 or above)

**Libraries Used**: Only 5 core Java functions as specified in project requirements:

System.out.print() for all outputs

BufferedReader.readLine() for user input and file reading

FileWriter.write() for file writing

String.split() for parsing text data

MessageDigest.digest() for password hashing

##System Architecture

#Main Components
------------------------------------------------
-**Admin.java**: Handles authentication and user roles.

-**Order.java**: Represents the structure of an order, including items, payment mode, status, and other details.

-**Product.java**: Defines product attributes such as product ID, category, brand, and stock.

-**PaymentService.java**: Simulates payment processing for orders.

-**Log.java**: Handles logging and viewing order timelines.

-**DataPersistence.java**: Manages loading and saving of data to text files (orders, products, and admins).

-**Workflow.java**: Manages the core order processing logic and the Admin Dashboard interface.

-**Main.java**: The entry point of the application, initializing data and starting the CLI.

-**Item.java**: Represents an item in an order, with product ID and quantity.

-**Role.java**: Defines user roles (ADMIN, MANAGER, SUPPORT) used for permission management.

#File Structure

#/src

 **Main.java**              - Entry point for the CLI
 **Admin.java**             - Admin logic for authentication and role management
 **Order.java**             - Order details and item management
 **Product.java**           - Product details and inventory management
 **PaymentService.java**    - Payment simulation logic
 **Log.java**               - Logging system
 **DataPersistence.java**   - Data loading and saving logic
 **Workflow.java**          - Workflow orchestration and admin dashboard
 **Item.java**              - Item details for orders (product ID, quantity)
 **Role.java**              - Enum defining roles (ADMIN, MANAGER, SUPPORT)
  
#/data

  **admins.txt**            - Stores admin credentials (username|password hash|role)
  **products.txt**          - Stores product details (productId|category|brand|name|price|stock)
  **orders.txt**            - Stores orders                                                                                   (orderId|date|address|paymentMode|status|items|totalAmount)

  **logs.txt**              - Stores logs for system actions
  **orders_import.json**    - Orders for bulk import (JSON format)
  **orders_import.txt**     - Orders for bulk import (TXT format)
  **stock_report.txt**      - Product stock report
  **report.txt**            - Revenue and cancellation reports
  


#How to Run the Project

#Clone the repository:

git clone <repository-url>

#Compile the Java files:

javac *.java

#Run the Main class:

java Main

#Login:

After running the application, log in using the admin credentials. The default credentials (if no admin is loaded) are:

Username: saika

Password: saika123

#Running Simulation Scenarios

The system includes simulation modes to test different scenarios, including:

**Successful order processing**

**Payment failure scenarios**

**Inventory shortage scenarios**

**Random order scenarios**

To run a simulation, choose option 13 from the Admin Dashboard and select the scenario you'd like to simulate.

#Future Improvements

*Full JSON Support*: Switch from text files to JSON for better data management.

*Reporting*: Implement more detailed reporting on orders, sales, and revenue.

*Advanced Admin Features*: Add features like password recovery and permission-based actions for                            managers.
