package camping;

import javax.persistence.*;
import org.springframework.beans.BeanUtils;
import java.util.List;
import java.util.Date;

@Entity
@Table(name="Payment_table")
public class Payment {

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Long id;
    private Long bookingId;
    private String status;

    @PostPersist
    public void onPostPersist(){
        PaymentApproved paymentApproved = new PaymentApproved();
        BeanUtils.copyProperties(this, paymentApproved);
        if(this.getStatus().equals("PaymentApproved"))
            paymentApproved.publishAfterCommit();

        PaymentCancelled paymentCancelled = new PaymentCancelled();
        BeanUtils.copyProperties(this, paymentCancelled);
        if(this.getStatus().equals("PaymentCancelled"))
            paymentCancelled.publishAfterCommit();

    }

    @PostUpdate
    public void onPostUpdate(){
        PaymentApproved paymentApproved = new PaymentApproved();
        BeanUtils.copyProperties(this, paymentApproved);
        if(this.getStatus().equals("PaymentApproved"))
            paymentApproved.publishAfterCommit();

        PaymentCancelled paymentCancelled = new PaymentCancelled();
        BeanUtils.copyProperties(this, paymentCancelled);
        if(this.getStatus().equals("PaymentCancelled"))
            paymentCancelled.publishAfterCommit();       
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    public Long getBookingId() {
        return bookingId;
    }

    public void setBookingId(Long bookingId) {
        this.bookingId = bookingId;
    }
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }




}