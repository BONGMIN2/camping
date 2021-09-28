package camping.external;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Date;

@FeignClient(name="campsite", url="${api.url.campsite}")
public interface CampsiteService {
    @RequestMapping(method= RequestMethod.GET, path="/chkAndModifySite")
    public boolean modifySite(@RequestParam("siteId") Long siteId,
    @RequestParam("siteNum") int siteCount);
}

