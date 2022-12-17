package lilytts.yaml;

public class AzureSpeechConnection {
    private String serviceRegion;
    private String subscriptionKey;
    
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
