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
    public boolean isSimulationOrder = false;
    public String simulationItemName = "";
    public int simulationItemPrice = 0;

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

        /** Prepare a text record of this order for saving to file */
public String toRecord() {
    // Build items list string as "ProductIDxQty,ProductIDxQty,..."
    StringBuilder itemsPart = new StringBuilder();
    for (int i = 0; i < itemCount; i++) {
        Item it = items[i];
        if (it == null) continue;

        itemsPart.append(it.productId).append("x").append(it.quantity);

        if (i < itemCount - 1) {
            itemsPart.append(",");
        }
    }

    String orderIdPart = (orderId == null ? "" : orderId);
    String datePart = (date == null ? "" : date);
    String addressPart = (address == null ? "" : address);
    String paymentPart = (paymentMode == null ? "" : paymentMode);
    String statusPart = (status == null ? "" : status);
    String reasonPart = (cancelReason == null ? "" : cancelReason);
    String trackingPart = (trackingId == null ? "" : trackingId);
    String simNamePart = (simulationItemName == null ? "" : simulationItemName);

    // Format:
    // OrderID|Date|Address|PaymentMode|Status|Total|ItemList|CancelReason|TrackingId|IsSimulationOrder|SimulationItemName|SimulationItemPrice
    return orderIdPart + "|" +
           datePart + "|" +
           addressPart + "|" +
           paymentPart + "|" +
           statusPart + "|" +
           totalAmount + "|" +
           itemsPart.toString() + "|" +
           reasonPart + "|" +
           trackingPart + "|" +
           isSimulationOrder + "|" +
           simNamePart + "|" +
           simulationItemPrice;
}
}