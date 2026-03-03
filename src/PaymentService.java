
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
