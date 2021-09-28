package camping;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

 @RestController
 public class CampsiteController {
    @Autowired
    CampsiteRepository campsiteRepository;
    
    @RequestMapping(value = "/chkAndModifySite",
                    method = RequestMethod.GET,
                    produces = "application/json;charset=UTF-8")
                    
    public boolean modifySite(HttpServletRequest request, HttpServletResponse response) throws Exception {
          System.out.println("##### /campsite/modifySeat called #####");
    
          boolean status = false;
          Long siteId = Long.valueOf(request.getParameter("siteId"));
          int siteNum = Integer.parseInt(request.getParameter("siteNum"));   
          
          Campsite campsite = campsiteRepository.findBysiteId(siteId);
    
         //siege - circuit break
            try {
                 Thread.currentThread().sleep((long) (100 + Math.random() * 220));
          } catch (InterruptedException e) {
                e.printStackTrace();
          } 
          // circuit break end
          
          if(campsite.getBookableSite() >= siteNum) {
                       status = true;
                       campsite.setBookableSite(campsite.getBookableSite()-siteNum);
                       campsiteRepository.save(campsite);
          }
          return status;
    }
 }