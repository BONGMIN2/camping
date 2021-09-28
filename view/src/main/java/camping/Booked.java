package camping;

public class Booked extends AbstractEvent {

    private Long id;
    private Long siteId;
    private Long SiteNum;
    private String status;

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
    public Long getSiteNum() {
        return SiteNum;
    }

    public void setSiteNum(Long SiteNum) {
        this.SiteNum = SiteNum;
    }
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}