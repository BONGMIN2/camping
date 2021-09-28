package camping;

import javax.persistence.*;
import org.springframework.beans.BeanUtils;
import java.util.List;
import java.util.Date;

@Entity
@Table(name="Booking_table")
public class Booking {

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    private Long siteId;
    private Long siteNum;
    private String status;

   // *************************************************************************
   // 동기호출 : FeignClient를 통한 구현
   // *************************************************************************
   // Booking 수행시, campsite 자리가 없는 상태에서  예약시도 할 경우, 새로운 예약을 못하도록 한다.
   // CampsiteService에서 좌석수 체크 (http://localhost:8081/campsites의 /chkAndModifySeat) 결과가 false 로 나올 경우
   // 강제 Exception을 발생시켜서 => 서비스 내림, Booking 이 POST되지 않도록 처리한다.
   // *************************************************************************

    @PostPersist
    public void onPostPersist() throws Exception{
        
        boolean rslt = BookingApplication.applicationContext.getBean(camping.external.CampsiteService.class)
        .modifySite(this.getSiteId(),this.getSiteNum().intValue());
      

        if(rslt){
            System.out.println("=========Booking Result : true==========");
            this.setStatus("Booking completed");
            Booked booked = new Booked();
            BeanUtils.copyProperties(this, booked);
            booked.publishAfterCommit();
        }else{
            System.out.println("=========Booking Result : false==========");
            throw new Exception("사이트 잔여개수 부족");
        }
   
   }
    @PreRemove
    public void onPreRemove(){
        BookCancelled bookCancelled = new BookCancelled();
        BeanUtils.copyProperties(this, bookCancelled);
        bookCancelled.publishAfterCommit();

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
    public Long getSiteNum() {
        return siteNum;
    }

    public void setSiteNum(Long siteNum) {
        this.siteNum = siteNum;
    }
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }




}