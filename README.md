# Cloud Lv2 개인 ["도서대여시스템(3팀)" 별개로 심플 시스템 구현]
![image](https://user-images.githubusercontent.com/88808412/134763175-59df144a-e12a-41d3-b031-bfd04ad79881.png)
   

# Table of contents

- [캠핑장 예약 시스템](#---)
  - [서비스 시나리오](#서비스-시나리오)
  - [체크포인트](#체크포인트)
  - [분석/설계](#분석설계)
  - [구현:](#구현)
    - [DDD 의 적용](#ddd-의-적용)
    - [Gateway 적용](# Gateway 적용)
    - [폴리글랏 퍼시스턴스](# 폴리글랏 퍼시스턴스)
    - [동기식 호출과 Fallback 처리](# 동기식 호출과 Fallback 처리)
    - [CQRS 적용](# CQRS 적용)
    - [SAGA 패턴 적용](# SAGA 패턴)
  - [운영](#운영)
    - [Deploy](# Deploy/Pipeline)
    - [Config Map](# Config Map)
    - [Self-healing(Liveness Probe)](# Self-healing(Liveness Probe))


# 서비스 시나리오

기능적 요구사항

1. 관리자는 캠핑장 사이트 정보(개수,사이트유형)를 등록할 수 있다.
2. 고객이 캠핑사이트를 예약한다.
3. 캠핑사이트가 예약되면 예약된 사이트개수 만큼 예약 가능한 사이트수가 줄어든다.
4. 예약이 되면 결제 정보가 승인된다.
5. 고객은 예약한 사이트를 취소할 수 있다. 
7. 예약이 취소되면 결제가 취소된다.
8. 고객은 모든 진행 내역을 마이페이지에서 조회할 수 있다.

 

비기능적 요구사항

1. 트랜잭션
   1) 예약 가능한 사이트 개수가 부족하면 예약이 되지 않는다. --> Sync 호출
   2) 예약이 취소되면 결제가 취소되고 예약 가능한 사이트 개수가 증가한다.(SAGA패턴) 
2. 장애격리
   1) 결제가 완료되지 않더라도 예약 기능은 365일 24시간 받을 수 있어야 한다 Async (event-driven), Eventual Consistency
   2) 예약이 몰려 사이트관리 시스템의 부하가 과중되면 사용자를 잠시동안 받지 않고 예약 진행을 잠시후에 하도록 유도한다 Circuit breaker, fallback
3. 성능
   1) 고객은 예약내역을 view(MyPage)를 통해 예약내역을 조회할 수 있다. CQRS




# 분석/설계


## AS-IS 조직 (Horizontally-Aligned)
![asis](https://user-images.githubusercontent.com/88808412/134913924-99926d2a-5596-40b5-99e5-cd497000f656.png)

## TO-BE 조직 (Vertically-Aligned)
![tobe](https://user-images.githubusercontent.com/88808412/134914079-f20d9d50-7ecf-4dad-b06c-66255f555ac6.png)

## Event Storming 결과
* MSAEz 로 모델링한 이벤트스토밍 결과:  https://labs.msaez.io/#/storming/XZacNdh26uV4zk3mhOUlKKWkLGT2/253af82d374cbe40c9c8c82b244e1194


### 이벤트 도출
![event도출](https://user-images.githubusercontent.com/88808412/134922257-4099b719-4b19-49f0-bf33-00ecafc18450.png)

### 부적격 이벤트 탈락
![부적격event](https://user-images.githubusercontent.com/88808412/134922378-4e9f6c30-e555-48e4-98ce-f13b6fe5bbe4.png)


### 액터, 커맨드 부착하여 읽기 좋게
![action](https://user-images.githubusercontent.com/88808412/134922486-ea1b6dd1-6358-462e-8ffd-f6eaf919ddde.png)

### 어그리게잇으로 묶기
![aggregate](https://user-images.githubusercontent.com/88808412/134922599-e967a830-44b4-4f8a-9765-5496645c90e8.png)

### 바운디드 컨텍스트로 묶기
![boundedct](https://user-images.githubusercontent.com/88808412/134922679-7cc382a4-503d-44a9-8b7f-95d7bda46019.png)

### 폴리시 부착과 컨텍스트 매핑 (점선은 Pub/Sub, 실선은 Req/Resp)
![policy_ct](https://user-images.githubusercontent.com/88808412/134922758-33975b05-01d5-4f0d-a0c7-4ec55a166852.png)

### 완성된 1차 모형
![complete](https://user-images.githubusercontent.com/88808412/134763506-462e0a92-db25-4626-b96b-19675beb1fb8.png)

### 1차 완성본에 대한 기능적/비기능적 요구사항을 커버하는지 검증
![검증1](https://user-images.githubusercontent.com/88808412/134923667-e163aa33-fde1-4e79-bf55-f1713c32d7ae.png)
![검증2](https://user-images.githubusercontent.com/88808412/134923738-3afc0388-3e44-48b9-ae71-3e123937c348.png)


# 구현

분석/설계 단계에서 도출된 헥사고날 아키텍처에 따라,구현한 각 서비스를 로컬에서 실행하는 방법은 아래와 같다 (각자의 포트넘버는 8081 ~ 8084, 8088 이다)

```shell
cd booking
mvn spring-boot:run

cd campsite
mvn spring-boot:run 

cd payment
mvn spring-boot:run 

cd gateway 
mvn spring-boot:run

cd view
mvn spring-boot:run 
```
## DDD(Domain-Driven-Design)의 적용
msaez Event-Storming을 통해 구현한 Aggregate 단위로 Entity 를 정의 하였으며,
Entity Pattern 과 Repository Pattern을 적용하기 위해 Spring Data REST 의 RestRepository 를 적용하였다.

Campsite 서비스의 campsite.java

```java

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
```

 Payment 서비스의 PolicyHandler.java
 booking 완료시 Payment 이력을 처리한다.
 
```java
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

```

campsite 서비스의 CampsiteRepository.java


```java
package camping;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(collectionResourceRel="campsites", path="campsites")
public interface CampsiteRepository extends PagingAndSortingRepository<Campsite, Long>{

    Campsite findBysiteId(Long SiteId);
}

```

## 적용 후 REST API 의 테스트
각 서비스들의 Rest API 호출을 통하여 테스트를 수행하였음

```shell
캠핑사이트 1번 50개 등록
http POST http://localhost:8088/campsites siteId=1 siteType=square bookableSite=50     
```
![캠프사이트등록](https://user-images.githubusercontent.com/88808412/134927991-668cabaf-4c95-44c4-bd93-7d16b6fbc170.png)

```shell
캠핑사이트 1번 1개 예약
http POST http://localhost:8088/bookings siteId=1 siteNum=1
```
![예약](https://user-images.githubusercontent.com/88808412/134928092-2bb9b8f8-5f38-4b35-8842-e95d4c27974c.png)

```shell
캠핑사이트 1번 100개 예약 실패(사이트 잔여수량 부족)  
http POST http://localhost:8088/bookings siteId=1 siteNum=100    
```
![초과예약](https://user-images.githubusercontent.com/88808412/134928203-a6d3940c-d72e-4414-8781-77c28c35f3e6.png)
                             


## Gateway 적용
Gateay 구성를 통하여 각 서비스들의 진입점을 설정하여 라우팅 설정하였다.(8081~8084, 8088)
```yaml
server:
  port: 8088

---

spring:
  profiles: default
  cloud:
    gateway:
      routes:
        - id: campsite
          uri: http://localhost:8081
          predicates:
            - Path=/campsites/**
        - id: booking
          uri: http://localhost:8082
          predicates:
            - Path=/bookings/**  
        - id: payment
          uri: http://localhost:8083
          predicates:
            - Path=/payments/** 
        - id: view
          uri: http://localhost:8084
          predicates:
            - Path= /myPages/**
      globalcors:
        corsConfigurations:
          '[/**]':
            allowedOrigins:
              - "*"
            allowedMethods:
              - "*"
            allowedHeaders:
              - "*"
            allowCredentials: true


---

spring:
  profiles: docker
  cloud:
    gateway:
      routes:
        - id: campsite
          uri: http://campsite:8080
          predicates:
            - Path=/campsites/** 
        - id: booking
          uri: http://booking:8080
          predicates:
            - Path=/bookings/** 
        - id: payment
          uri: http://payment:8080
          predicates:
            - Path=/payments/** 
        - id: view
          uri: http://view:8080
          predicates:
            - Path= /myPages/**
      globalcors:
        corsConfigurations:
          '[/**]':
            allowedOrigins:
              - "*"
            allowedMethods:
              - "*"
            allowedHeaders:
              - "*"
            allowCredentials: true

server:
  port: 8080


```

## 폴리글랏 퍼시스턴스
campsite, payment, view 마이크로서비스는 H2 DB로 구성하였고, booking 마이크로서비스는 HSQL DB로 구성하여 이종 DB사용하면서 정상 동작함을 확인 
![H2DB](https://user-images.githubusercontent.com/88808412/134929063-9034db8d-dbee-4690-b886-0c6e3a5ea385.png)
![HSQLDB](https://user-images.githubusercontent.com/88808412/134929017-11882529-9dfe-4ef6-a939-921c268a87f5.png)

## 동기식 호출과 Fallback 처리
예약수량보다 캠프사이트 잔여개수가 적으면 예약을 받지 않도록 FeignClient를 이용한 Req/Rsep 동기호출 하였음

Booking 서비스 내 CampsiteService.Java
```java
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
```

booking 서비스 내 Req/Resp
```java
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
```

campsite 서비스 내 booking 서비스 FeignClient 요청 대상
```java
 @RestController
 public class CampsiteController {
    @Autowired
    CampsiteRepository campsiteRepository;
    
    @RequestMapping(value = "/chkAndModifySite",
                    method = RequestMethod.GET,
                    produces = "application/json;charset=UTF-8")
                    
    public boolean modifySite(HttpServletRequest request, HttpServletResponse response) throws Exception 
          System.out.println("##### /campsite/modifySeat called #####");
    
          boolean status = false;
          Long siteId = Long.valueOf(request.getParameter("siteId"));
          int siteNum = Integer.parseInt(request.getParameter("siteNum"));   
          
          Campsite campsite = campsiteRepository.findBysiteId(siteId);
           
          if(campsite.getBookableSite() >= siteNum) {
                       status = true;
                       campsite.setBookableSite(campsite.getBookableSite()-siteNum);
                       campsiteRepository.save(campsite);
          }
          return status;
    }
 }
```
- 동작확인 : 예약 요청 사이트수가 잔여 사이트수보다 많으면 예약 불가함
![예약불가](https://user-images.githubusercontent.com/88808412/135109246-4e477c3f-143c-4678-b14f-7ca31b7aa469.png)


## CQRS 적용
mypage(View)는 Materialized View로 구현하여, 타 마이크로서비스의 데이터 원본에 Join SQL 등 구현 없이도 내 서비스의 화면 구성과 잦은 조회가 가능하게 구현 하였음.

- 캠프사이트 예약 Transaction 발생 후 myPage 조회 결과
![예약뷰](https://user-images.githubusercontent.com/88808412/135107602-c21e8cc7-b8dc-456b-bb02-229c9a676cce.png)
- 캠프사이트 예약취소 후 myPage 조회 결과
![예약취소뷰](https://user-images.githubusercontent.com/88808412/135108070-46140080-d81f-47be-bf7e-6e0963f7e4a0.png)

## SAGA 패턴
캠핑장 예약시스템은 마이크로서비스에서 이벤트가 Publish 되면 후속 서비스가 Trigger 되는 SAGA 패턴으로 디자인 되었다.
- Booking이 생성 완료되면 Payment 서비스를 trigger하여 Payment 상태를 업데이트한다.
- Booking
![image](https://user-images.githubusercontent.com/88808412/135114693-da901a3c-bdfd-456c-b959-1700864dbc28.png)
- Payment
![image](https://user-images.githubusercontent.com/88808412/135114772-5caacdee-6808-409d-9f08-6309f836410e.png)


# 운영


## Deploy/Pipeline
- yml 파일을 이용한 Docker Image Push/Deploy/서비스생성
```yaml
cd booking
az acr build --registry user2020 --image user2020.azurecr.io/booking:v1 .

cd kubernetes
kubectl apply -f deployment.yml
kubectl apply -f service.yaml

cd campsite
az acr build --registry user2020 --image user2020.azurecr.io/campsite:v1 .

cd kubernetes
kubectl apply -f deployment.yml
kubectl apply -f service.yaml

cd gateway
az acr build --registry user2020 --image user2020.azurecr.io/gateway:v1 .

cd kubernetes
kubectl apply -f deployment.yml
kubectl apply -f service.yaml

cd payment
az acr build --registry user2020 --image user2020.azurecr.io/payment:v1 .

cd kubernetes
kubectl apply -f deployment.yml
kubectl apply -f service.yaml

cd view
az acr build --registry user2020 --image user2020.azurecr.io/view:v1 .

cd kubernetes
kubectl apply -f deployment.yml
kubectl apply -f service.yaml
```
## Config Map
- 변경 가능성이 있는 항목은 ConfigMap을 사용하여 설정 변경 할 수 있도록 구성
-- booking 서비스에서 바라보는 campsite 서비스 url을 ConfigMap 사용하여 구현 함
![image](https://user-images.githubusercontent.com/88808412/135198241-def7cd7d-11bf-4d79-a75c-3bc3495b315b.png)
![image](https://user-images.githubusercontent.com/88808412/135198314-f92e148f-856a-456d-aae9-c095baaf73f8.png)

## Self-healing(Liveness Probe)
- Liveness Probe  설정 및 restart 시도 점검을 위해 잘못된 포트번호로 설정 변경 후 restart 점검 결과
![image](https://user-images.githubusercontent.com/88808412/135200760-df9d4fbc-d8ce-490f-bddb-5f16a6b91ccd.png)


