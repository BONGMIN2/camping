package camping;

import camping.config.kafka.KafkaProcessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service
public class PolicyHandler{
    @Autowired PaymentRepository paymentRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverBooked_ApprovePayment(@Payload Booked booked){

        if(booked.validate()) {
           System.out.println("\n\n##### listener ApprovePayment : " + booked.toJson() + "\n\n");
           
           Payment payment = new Payment(); 
           payment.setBookingId(booked.getId());
           payment.setStatus("PaymentApproved");
           paymentRepository.save(payment);

        }

    }
    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverCanceled_CanclePayment(@Payload BookCancelled bookCancelled){

        if(bookCancelled.validate()) {

           System.out.println("\n\n##### listener CanclePayment : " + bookCancelled.toJson() + "\n\n");
           Payment payment = paymentRepository.findByBookingId(bookCancelled.getId());
           payment.setStatus("PaymentCancelled");
           paymentRepository.save(payment);

        }
    }

    @StreamListener(KafkaProcessor.INPUT)
    public void whatever(@Payload String eventString){}


}