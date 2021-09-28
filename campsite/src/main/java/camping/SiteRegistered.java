package camping;

public class SiteRegistered extends AbstractEvent {

    private Long id;
    private Long siteId;
    private String siteType;
    private Long bookableSite;

    public SiteRegistered(){
        super();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    public Long getSiteId() {
        return siteId;
    }

    public void setSiteId(Long siteId) {
        this.siteId = siteId;
    }
    public String getSiteType() {
        return siteType;
    }

    public void setSiteType(String siteType) {
        this.siteType = siteType;
    }
    public Long getBookableSite() {
        return bookableSite;
    }

    public void setBookableSite(Long bookableSite) {
        this.bookableSite = bookableSite;
    }
}