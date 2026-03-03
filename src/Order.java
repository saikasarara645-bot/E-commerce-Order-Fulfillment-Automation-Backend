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
    
      public Order() {
        this.status = "PENDING";
        this.cancelReason = "";
        this.trackingId = "";
        this.address = "";
        this.paymentMode = "";
    }

    /** Add an item to the order (returns false if capacity reached or invalid quantity) */
    public boolean addItem(Item it) {
        if (it == null || it.quantity <= 0 || itemCount >= items.length) {
            // Reject invalid item input (e.g., zero quantity or capacity exceeded)
            return false;
        }
        items[itemCount++] = it;
        return true;
    }