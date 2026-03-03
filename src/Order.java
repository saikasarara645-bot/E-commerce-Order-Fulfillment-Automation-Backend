public class Order {
    public String orderId;
    public String date;           // e.g., "2026-02-01"
    public String address;
    public String paymentMode;
    public String status;
    public Item[] items = new Item[10];  // max 10 items per order for simplicity
    public int itemCount = 0;
    public int totalAmount;
    public String cancelReason;
    public String trackingId;