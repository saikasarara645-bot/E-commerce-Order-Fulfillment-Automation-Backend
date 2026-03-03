
import java.io.BufferedReader;
/*PaymentService.java – Handles payment simulation (COD vs. MockCard)
*/
public class PaymentService {
 private final Log log;
 public PaymentService(Log log) {
 this.log = log;
 }
 

 /** Process payment for an order. Returns true if payment succeeds.
*/
 public boolean processPayment(Order order, BufferedReader console)
throws Exception {
 String mode = safe(order.paymentMode);
 if (mode.equalsIgnoreCase("COD")) {
 // Cash on Delivery always "approved" (no upfront failure)
 log.write(order.orderId, "PAYMENT OK (Cash on Delivery)");
 return true;

 }

if (mode.equalsIgnoreCase("MockCard")) {
 // Simulate card payment by asking admin to approve (Y) or decline (N)
 System.out.print("MockCard Payment: Total BDT " + order.totalAmount + "\n");
 System.out.print("Approve? (Y/N): ");
 String ans = safe(console.readLine());

 if (startsWithYes(ans)) {
 log.write(order.orderId, "PAYMENT OK (MockCard approved)");
 return true;
 } else {
 log.write(order.orderId, "PAYMENT FAIL (MockCard declined)");
 return false;
 }
 }

 // If payment mode is unknown, treat as failure
 log.write(order.orderId, "PAYMENT FAIL (Unknown payment mode: " + mode + ")");
 return false;
 }