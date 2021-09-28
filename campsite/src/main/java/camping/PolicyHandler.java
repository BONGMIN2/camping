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
    @Autowired CampsiteRepository campsiteRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverBookCancelled_ChangeSite(@Payload BookCancelled bookCancelled){

        if(!bookCancelled.validate()) return;

     //   System.out.println("\n\n##### listener ChangeSite : " + bookCancelled.toJson() + "\n\n");

        Long siteId = Long.valueOf(bookCancelled.getSiteId());
        Campsite campsite = campsiteRepository.findBysiteId(siteId);
        campsite.setBookableSite(campsite.getBookableSite()+bookCancelled.getSiteNum().intValue());
        campsiteRepository.save(campsite);

;


    }


    @StreamListener(KafkaProcessor.INPUT)
    public void whatever(@Payload String eventString){}


}