package camping;

import javax.persistence.*;
import org.springframework.beans.BeanUtils;
import java.util.List;
import java.util.Date;

@Entity
@Table(name="Campsite_table")
public class Campsite {

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Long id;
    private Long siteId;
    private String siteType;
    private Long bookableSite;

    @PostPersist
    public void onPostPersist(){
        SiteRegistered siteRegistered = new SiteRegistered();
        BeanUtils.copyProperties(this, siteRegistered);
        siteRegistered.publishAfterCommit();

    }
    @PostUpdate
    public void onPostUpdate(){
        SiteModified siteModified = new SiteModified();
        BeanUtils.copyProperties(this, siteModified);
        siteModified.publishAfterCommit();

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