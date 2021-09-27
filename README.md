# Cloud Lv2 개인 [캠핑장 예약 시스템]
![image](https://user-images.githubusercontent.com/88808412/134763175-59df144a-e12a-41d3-b031-bfd04ad79881.png)
   

# Table of contents

- [캠핑장 예약 시스템](#---)
  - [서비스 시나리오](#서비스-시나리오)
  - [체크포인트](#체크포인트)
  - [분석/설계](#분석설계)
  - [구현:](#구현)
    - [DDD 의 적용](#ddd-의-적용)
    - [폴리글랏 퍼시스턴스](#폴리글랏-퍼시스턴스)
    - [폴리글랏 프로그래밍](#폴리글랏-프로그래밍)
    - [동기식 호출 과 Fallback 처리](#동기식-호출-과-Fallback-처리)
    - [비동기식 호출 과 Eventual Consistency](#비동기식-호출-과-Eventual-Consistency)
  - [운영](#운영)
    - [CI/CD 설정](#cicd설정)
    - [동기식 호출 / 서킷 브레이킹 / 장애격리](#동기식-호출-서킷-브레이킹-장애격리)
    - [오토스케일 아웃](#오토스케일-아웃)
    - [무정지 재배포](#무정지-재배포)


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


## 헥사고날 아키텍처 다이어그램 도출
    
TBD



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

Bookrental 서비스의 rental.java

```java
![image](https://user-images.githubusercontent.com/88808412/134926774-cebfaa83-370a-4f6b-a9a6-6b9e9443b85a.png)



 Payment 서비스의 PolicyHandler.java
 rental 완료시 Payment 이력을 처리한다.
```java
package book.rental.system;

import book.rental.system.config.kafka.KafkaProcessor;

import java.util.Optional;

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
    public void wheneverBookRented_PayPoint(@Payload BookRented bookRented){

        if(!bookRented.validate()) return;

        System.out.println("\n\n##### listener PayPoint : " + bookRented.toJson() + "\n\n");

        if("RENT".equals(bookRented.getRentStatus())){

            Payment payment =new Payment();

            payment.setBookId(bookRented.getBookid());
            payment.setCustomerId(bookRented.getCustomerId());
            payment.setPrice(bookRented.getPrice());
            payment.setRentalId(bookRented.getRentalId());
            paymentRepository.save(payment);
        }else{
            System.out.println("\n\n##### listener PayPoint Process Failed : Status -->" +bookRented.getRentStatus() + "\n\n");
        }
    }


    @StreamListener(KafkaProcessor.INPUT)
    public void whatever(@Payload String eventString){}


}

```

 BookRental 서비스의 RentalRepository.java


```java
package book.rental.system;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(collectionResourceRel="rentals", path="rentals")
public interface RentalRepository extends PagingAndSortingRepository<Rental, Long>{


}
```

## 적용 후 REST API 의 테스트
각 서비스들의 Rest API 호출을 통하여 테스트를 수행하였음

```shell
책 대여 처리
http post localhost:8081/rent bookId=1 price=1000 startDate=20210913 returnDate=20211013 customerId=1234 customerPhoneNo=01012345678 rentStatus=RENT

책 대여를 위한 예치금 적립
TBD

책 등록 
TBD
```

## Gateway 적용
GateWay 구성를 통하여 각 서비스들의 진입점을 설정하여 라우팅 설정하였다.
```yaml
server:
  port: 8088

---

spring:
  profiles: default
  cloud:
    gateway:
      routes:
        - id: Rental
          uri: http://localhost:8081
          predicates:
            - Path=/rentals/** 
        - id: Book
          uri: http://localhost:8082
          predicates:
            - Path=/books/** 
        - id: Payment
          uri: http://localhost:8083
          predicates:
            - Path=/payments/** 
        - id: Alert
          uri: http://localhost:8084
          predicates:
            - Path=/alerts/** 
        - id: View
          uri: http://localhost:8085
          predicates:
            - Path= /mypages/**
        - id: Point
          uri: http://localhost:8086
          predicates:
            - Path=/points/** 
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
        - id: Rental
          uri: http://Rental:8080
          predicates:
            - Path=/rentals/** 
        - id: Book
          uri: http://Book:8080
          predicates:
            - Path=/books/** 
        - id: Payment
          uri: http://Payment:8080
          predicates:
            - Path=/payments/** 
        - id: Alert
          uri: http://Alert:8080
          predicates:
            - Path=/alerts/** 
        - id: View
          uri: http://View:8080
          predicates:
            - Path= /mypages/**
        - id: Point
          uri: http://Point:8080
          predicates:
            - Path=/points/** 
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
## CQRS 적용
TBD

## 폴리글랏 퍼시스턴스
TBD

## 동기식 호출과 Fallback 처리
TBD

## 비동기식 호출 / 시간적 디커플링 / 장애격리 / 최종 (Eventual) 일관성 테스트
TBD

# 운영
TBD

