- 컨트롤러의 중요한 역할중 하나는 HTTP 요청이 정상인지 검증하는 것이다
- 컨트롤러 내에서 if else 문으로 일일히 처리 하게 된다면 중복 처리가 많아지고, 타입오류 처리가 안된다.
- 이러한 오류는 스프링 MVC에서 컨트롤러에 진입하기도 전에 예외가 발생하기 때문에, 컨트롤러가 호출되지도 않고, 400 예외가 발생 하면서 오류 페이지를 띄워준다
- 고객이 입력한 값도 어딘가에 별도로 관리가 되어야 한다

## BindingResult

- 스프링이 제공하는 검증 오류를 보관하는 객체이다. 검증 오류가 발생하면 여기에 보관하면 된다.
- BindingResult 가 있으면 @ModelAttribute 에 데이터 바인딩 시 오류가 발생해도 컨트롤러가 호출된다!
    - BindingResult 가 없으면 400 오류가 발생하면서 컨트롤러가 호출되지 않고, 오류 페이지로 이동한다

### 기본사용

```java
// 필드 오류
if(!StringUtils.hasText(item.getItemName())){
    // 필드에 오류가 있으면 FieldError 객체를 생성해서 bindingResult 에 담아두면 된다.
    bindingResult.addError(new FieldError("item", "itemName", "상품 이름은 필수 입니다."));
}
// 글로벌 오류
if(item.getPrice() != null && item.getQuantity() != null){
    int resultPrice = item.getPrice() * item.getQuantity();
    if(resultPrice < 10000){
        // 특정 필드를 넘어서는 오류가 있으면 ObjectError 객체를 생성해서 bindingResult 에 담아두면 된다.
        bindingResult.addError(new ObjectError("item", "가격 * 수량의 합은 10,000원 이상이어야 합니다. 현재 값 = " + resultPrice));
    }
}
```

### 사용자 입력 값 유지

```java
  if(item.getPrice() == null || item.getPrice() < 1000 || item.getPrice() > 100000){
      bindingResult.addError(new FieldError("item", "price", item.getPrice(), false, null, null,"가격은 1,000 ~ 1,000,000 까지 허용합니다."));
  }
  if(item.getPrice() != null && item.getQuantity() != null){
    int resultPrice = item.getPrice() * item.getQuantity();
    if(resultPrice < 10000){
        bindingResult.addError(new ObjectError("item", null, null, "가격 * 수량의 합은 10,000원 이상이어야 합니다. 현재 값 = " + resultPrice));
    }
}
```

### 오류 메시지사용

`spring.messages.basename=messages,errors`

```java
if(item.getPrice() == null || item.getPrice() < 1000 || item.getPrice() > 100000){
    bindingResult.addError(new FieldError("item", "price", item.getPrice(), false, new String[]{"range.item.price"}, new Object[]{1000,10000},null));
}
if(item.getPrice() != null && item.getQuantity() != null){
    int resultPrice = item.getPrice() * item.getQuantity();
    if(resultPrice < 10000){
        bindingResult.addError(new ObjectError("item", new String[]{"totalPriceMin"}, new Object[]{10000,resultPrice}, null));
    }
}
```

### 오류메시지 사용2

```java
// FieldError , ObjectError 는 다루기 너무 번거롭다.
// 컨트롤러에서 BindingResult 는 검증해야 할 객체인 target 바로 다음에 온다.
// 따라서 BindingResult 는 이미 본인이 검증해야 할 객체인 target 을 알고 있다.
log.info("objectName={}", bindingResult.getObjectName());
log.info("target={}", bindingResult.getTarget());

// 출력 결과
// objectName=item //@ModelAttribute name
// target=Item(id=null, itemName=상품, price=100, quantity=1234)
```

BindingResult 가 제공하는 rejectValue() , reject() 를 사용하면 FieldError , ObjectError 를 직
접 생성하지 않고, 깔끔하게 검증 오류를 다룰 수 있다.

```java
if (item.getPrice() == null || item.getPrice() < 1000 || item.getPrice() > 1000000) {
    bindingResult.rejectValue("price", "range", new Object[]{1000, 1000000},null);
}

if (item.getPrice() != null && item.getQuantity() != null) {
    int resultPrice = item.getPrice() * item.getQuantity();
    if (resultPrice < 10000) {
        bindingResult.reject("totalPriceMin", new Object[]{10000,resultPrice}, null);
    }
}
```

???????

```java
void rejectValue(@Nullable String field, String errorCode, @Nullable Object[] errorArgs
, @Nullable String defaultMessage);

field : 오류 필드명
errorCode : 오류 코드(이 오류 코드는 메시지에 등록된 코드가 아니다.messageResolver를,위한 오류 코드이다.)
errorArgs : 오류 메시지에서 {0} 을 치환하기 위한 값
defaultMessage : 오류 메시지를 찾을 수 없을 때 사용하는 기본 메시지
```

FieldError() 를 직접 다룰 때는 오류 코드를 range.item.price 와 같이 모두 입력했다. 그런데
rejectValue() 를 사용하고 부터는 오류 코드를 range 로 간단하게 입력했다. 그래도 오류 메시지를 잘 찾아서 출력한다. 무언가 규칙이 있는 것 처럼 보인다. 

이 부분을 이해하려면 MessageCodesResolver 를 이해해야 한다. 왜 이런식으로 오류 코드를 구성하는지 바로 다음에 자세히 알아보자.

### 오류 메시지 사용3

```java
객체 오류의 경우 다음 순서로 2가지 생성
1.: code + "." + object name
2.: code

예) 오류 코드: required, object name: item
1.: required.item
2.: required

필드 오류의 경우 다음 순서로 4가지 메시지 코드 생성
1.: code + "." + object name + "." + field
2.: code + "." + field
3.: code + "." + field type
4.: code

예) 오류 코드: typeMismatch, object name "user", field "age", field type: int
1. "typeMismatch.user.age"
2. "typeMismatch.age"
3. "typeMismatch.int"
4. "typeMismatch"

```

핵심은 구체적인 것에서! 덜 구체적인 것으로!
MessageCodesResolver 는 required.item.itemName 처럼 구체적인 것을 먼저 만들어주고, required 처럼 덜 구체적인 것을 가장 나중에 만든다.

이렇게 하면 앞서 말한 것 처럼 메시지와 관련된 공통 전략을 편리하게 도입할 수 있다

…….

이러저러해서 분리도 가능하지만 굳이 다 적지 않으련다. 

## Bean Validation

검증 기능을 지금처럼 매번 코드로 작성하는 것은 상당히 번거롭다. 특히 특정 필드에 대한 검증 로직은 대부분 빈 값인지 아닌지, 특정 크기를 넘는지 아닌지와 같이 매우 일반적인 로직이다.

```java
public class Item {
 private Long id;
 @NotBlank
 private String itemName;
 @NotNull
 @Range(min = 1000, max = 1000000)
 private Integer price;
 @NotNull
 @Max(9999)
 private Integer quantity;
 //...
}
```

이런 검증 로직을 모든 프로젝트에 적용할 수 있게 공통화하고, 표준화 한 것이 바로 Bean Validation 이다.

**검증 순서** 

1. @ModelAttribute 각각의 필드에 타입 변환 시도
    1. 성공하면 다음으로 
    2. 실패하면 typeMismatch 로 FieldError 추가
2. Validator 적용

**바인딩에 성공한 필드만 Bean Validation 적용**

BeanValidator는 바인딩에 실패한 필드는 BeanValidation을 적용하지 않는다.
생각해보면 타입 변환에 성공해서 바인딩에 성공한 필드여야 BeanValidation 적용이 의미 있다.
(일단 모델 객체에 바인딩 받는 값이 정상으로 들어와야 검증도 의미가 있다.)

@ModelAttribute → 각각의 필드 타입 변환시도 → 변환에 성공한 필드만 BeanValidation 적용

**예)**

itemName 에 문자 "A" 입력 → 타입 변환 성공 → itemName 필드에 BeanValidation 적용
price 에 문자 "A" 입력 → "A"를 숫자 타입 변환 시도 실패 → typeMismatch FieldError 추가 → price 필드는 BeanValidation 적용 X

## Bean Validation 에러코드

Bean Validation이 기본으로 제공하는 오류 메시지를 좀 더 자세히 변경하고 싶으면 어떻게 하면 될까?

NotBlank 라는 오류 코드를 기반으로 MessageCodesResolver 를 통해 다양한 메시지 코드가 순서대로 생성된다

**@NotBlank**

- NotBlank.item.itemName
- NotBlank.itemName
- NotBlank.java.lang.String
- NotBlank

**@Range**

- Range.item.price
- Range.price
- Range.java.lang.Integer
- Range

이제 메시지를 등록해보자

```java
# Bean Validation 추가
NotBlank={0} 공백X 
Range={0}, {2} ~ {1} 허용
Max={0}, 최대 {1}
```

{0} 은 필드명이고, {1} , {2} ...은 각 애노테이션 마다 다르다

**BeanValidation 메시지 찾는 순서**

1. 생성된 메시지 코드 순서대로 messageSource 에서 메시지 찾기
2. 애노테이션의 message 속성 사용 @NotBlank(message = "공백! {0}")
3. 라이브러리가 제공하는 기본 값 사용 공백일 수 없습니다

**애노테이션의 message 사용 예**

```java
@NotBlank(message = "공백은 입력할 수 없습니다.")
private String itemName;
```

## Bean Validation - 오브젝트 오류

Bean Validation에서 특정 필드( FieldError )가 아닌 해당 오브젝트 관련 오류( ObjectError )는 어떻게 처리할 수 있을까?

@ScriptAssert 을 억지로 사용하는 것 보다는 다음과 같이 오브젝트 오류 관련 부분만 직접 자바 코드로 작성하는 것을 권장한다

```java
if (item.getPrice() != null && item.getQuantity() != null) {
    int resultPrice = item.getPrice() * item.getQuantity();
    if (resultPrice < 10000) {
        bindingResult.reject("totalPriceMin", new Object[]{10000,resultPrice}, null);
    }
}
```

## 전송 객체 분리

도메인 객체가 아닌 컨트롤러까지 전달할 별도의 객체를 만들어서 전달한다. 

```java
@Data
public class ItemSaveForm {
    @NotBlank
    private String itemName;
    @NotNull
    @Range(min = 1000, max = 1000000)
    private Integer price;
    @NotNull
    @Max(value = 9999)
    private Integer quantity;

}
```

```java
@Data
public class ItemUpdateForm {
    @NotNull
    private Long id;
    @NotBlank
    private String itemName;
    @NotNull
    @Range(min = 1000, max = 1000000)
    private Integer price;
    private Integer quantity;
}
```

```java
@PostMapping("/add")
public String addItem(@Validated @ModelAttribute("item") ItemSaveForm form, 
BindingResult bindingResult, RedirectAttributes redirectAttributes) {
 //...
}
```

Item 대신에 ItemSaveform 을 전달 받는다. 그리고 @Validated 로 검증도 수행하고, BindingResult 로 검증 결과도 받는다.

## BeanValidation - HTTP 메시지 컨버터

@Valid , @Validated 는 HttpMessageConverter ( @RequestBody )에도 적용할 수 있다.

```java
@Slf4j
@RestController
@RequestMapping("/validation/api/items")
public class ValidationItemApiController {
    @PostMapping("/add")
    public Object addItem(@RequestBody @Validated ItemSaveForm form, BindingResult bindingResult){
        log.info("api 컨트롤러 호출");
        // HttpMessageConverter 는 @ModelAttribute 와 다르게 각각의 필드 단위로 적용되는 것이 아니라, 전체 객체 단위로 적용된다.
        // @ModelAttribute 는 필드 단위로 정교하게 바인딩이 적용된다. 특정 필드가 바인딩 되지 않아도
        // 나머지 필드는 정상 바인딩 되고, Validator를 사용한 검증도 적용할 수 있다.
        // @RequestBody 는 HttpMessageConverter 단계에서 JSON 데이터를 객체로 변경하지 못하면 이후 단계 자체가 진행되지 않고
        // 예외가 발생한다. 컨트롤러도 호출되지 않고, Validator도 적용할 수 없다.

        // => HttpMessageConverter 단계에서 실패하면 예외가 발생한다.
        if(bindingResult.hasErrors()){
            log.info("검증 오류 발생 errors ={}", bindingResult);
            return bindingResult.getAllErrors();
        }
        log.info("성공 로직 실행");
        return form;
    }
}
```

**API의 경우 3가지 경우를 나누어 생각해야 한다.**

- 성공 요청: 성공
- 실패 요청: JSON을 객체로 생성하는 것 자체가 실패함
- 검증 오류 요청: JSON을 객체로 생성하는 것은 성공했고, 검증에서 실패함

**실패요청**

```java
POST http://localhost:8080/validation/api/items/add
{"itemName":"hello", "price":"A", "quantity": 10}
```

price 값에 숫자가 아닌 문자를 전달해서 실패하게 만들어보자 

**실패 요청 결과**

```java
{
 "timestamp": "2021-04-20T00:00:00.000+00:00",
 "status": 400,
 "error": "Bad Request",
 "message": "",
 "path": "/validation/api/items/add"
}
```

**실패 요청 로그**

```java
.w.s.m.s.DefaultHandlerExceptionResolver : Resolved 
[org.springframework.http.converter.HttpMessageNotReadableException: JSON parse 
error: Cannot deserialize value of type `java.lang.Integer` from String "A": not 
a valid Integer value; nested exception is 
com.fasterxml.jackson.databind.exc.InvalidFormatException: Cannot deserialize 
value of type `java.lang.Integer` from String "A": not a valid Integer value
 at [Source: (PushbackInputStream); line: 1, column: 30] (through reference 
chain: hello.itemservice.domain.item.Item["price"])]
```

HttpMessageConverter 에서 요청 JSON을 ItemSaveForm 객체로 생성하는데 실패한다.

이 경우는 ItemSaveForm 객체를 만들지 못하기 때문에 컨트롤러 자체가 호출되지 않고 그 전에 예외가 발생한다. 물론 Validator도 실행되지 않는다.

**검증 오류 요청**

```java
POST http://localhost:8080/validation/api/items/add
{"itemName":"hello", "price":1000, "quantity": 10000}
```

수량( quantity )이 10000 이면 BeanValidation @Max(9999) 에서 걸린다.

**검증 오류 결과**

```java

 {
	 "codes": [
		 "Max.itemSaveForm.quantity",
		 "Max.quantity",
		 "Max.java.lang.Integer",
		 "Max"
	 ],
	 "arguments": [
		 {
			 "codes": [
				 "itemSaveForm.quantity",
				 "quantity"
			 ],
			 "arguments": null,
				 "defaultMessage": "quantity",
				 "code": "quantity"
		 },
		 9999
	 ],
	 "defaultMessage": "9999 이하여야 합니다",
	 "objectName": "itemSaveForm",
	 "field": "quantity",
	 "rejectedValue": 10000,
	 "bindingFailure": false,
	 "code": "Max"
	 }
]
```

return bindingResult.getAllErrors(); 는 ObjectError 와 FieldError 를 반환한다. 

스프링이 이 객체를 JSON으로 변환해서 클라이언트에 전달했다. 여기서는 예시로 보여주기 위해서 검증 오류 객체들을 그대로 반환했다. 

실제 개발할 때는 이 객체들을 그대로 사용하지 말고, 필요한 데이터만 뽑아서 별도의 API 스펙을 정의하고 그에 맞는 객체를 만들어서 반환해야 한다
