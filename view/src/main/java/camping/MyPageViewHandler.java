package camping;

import camping.config.kafka.KafkaProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Service
public class MyPageViewHandler {


    @Autowired
    private MyPageRepository myPageRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void whenBooked_then_CREATE_1 (@Payload Booked booked) {
        try {

            if (!booked.validate()) return;

            // view 객체 생성
            MyPage myPage = new MyPage();
            // view 객체에 이벤트의 Value 를 set 함
            myPage.setBookingId(booked.getId());
            myPage.setSiteId(booked.getSiteId());
            myPage.setSiteNum(booked.getSiteNum());
            myPage.setStatus(booked.getStatus());
            // view 레파지 토리에 save
            myPageRepository.save(myPage);

        }catch (Exception e){
            e.printStackTrace();
        }
    }


    @StreamListener(KafkaProcessor.INPUT)
    public void whenPaymentApproved_then_UPDATE_1(@Payload PaymentApproved paymentApproved) {
        try {
            if (!paymentApproved.validate()) return;
                // view 객체 조회

                    List<MyPage> myPageList = myPageRepository.findByBookingId(paymentApproved.getBookingId());
                    for(MyPage myPage : myPageList){
                    // view 객체에 이벤트의 eventDirectValue 를 set 함
                    myPage.setPaymentId(paymentApproved.getId());
                    myPage.setStatus(paymentApproved.getStatus());
                // view 레파지 토리에 save
                myPageRepository.save(myPage);
                }

        }catch (Exception e){
            e.printStackTrace();
        }
    }
    @StreamListener(KafkaProcessor.INPUT)
    public void whenPaymentCancelled_then_UPDATE_2(@Payload PaymentCancelled paymentCancelled) {
        try {
            if (!paymentCancelled.validate()) return;
                // view 객체 조회

                    List<MyPage> myPageList = myPageRepository.findByBookingId(paymentCancelled.getBookingId());
                    for(MyPage myPage : myPageList){
                    // view 객체에 이벤트의 eventDirectValue 를 set 함
                    myPage.setStatus(paymentCancelled.getStatus());
                // view 레파지 토리에 save
                myPageRepository.save(myPage);
                }

        }catch (Exception e){
            e.printStackTrace();
        }
    }

}

