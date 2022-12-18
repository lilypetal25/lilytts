package lilytts.yaml;

public class AzureSpeechConnection {
    private String displayName;
    private String serviceRegion;
    private String subscriptionKey;

    public String getDisplayName() {
        return displayName;
    }
    
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
    
    public String getServiceRegion() {
        return serviceRegion;
    }

    public void setServiceRegion(String serviceRegion) {
        this.serviceRegion = serviceRegion;
    }
    
    public String getSubscriptionKey() {
        return subscriptionKey;
    }
    
    public void setSubscriptionKey(String subscriptionKey) {
        this.subscriptionKey = subscriptionKey;
    }
}
