package camping;

import camping.config.kafka.KafkaProcessor;

import java.util.Optional;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;


@Service
public class PolicyHandler{
    @Autowired BookingRepository bookingRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverPaymentApproved_UpdateStatus(@Payload PaymentApproved paymentApproved){

        if(!paymentApproved.validate()) return;

        System.out.println("\n\n##### listener UpdateStatus : " + paymentApproved.toJson() + "\n\n");

        Optional<Booking> optionalBooking = bookingRepository.findById(paymentApproved.getBookingId()); 
        Booking booking = optionalBooking.get();
        booking.setStatus(paymentApproved.getStatus());
        bookingRepository.save(booking);

    }


    @StreamListener(KafkaProcessor.INPUT)
    public void whatever(@Payload String eventString){}


}