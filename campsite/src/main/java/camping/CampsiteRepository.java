package camping;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(collectionResourceRel="campsites", path="campsites")
public interface CampsiteRepository extends PagingAndSortingRepository<Campsite, Long>{

    Campsite findBysiteId(Long SiteId);
}
