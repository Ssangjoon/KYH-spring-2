package hello.itemservice.web.validation;

import hello.itemservice.domain.item.Item;
import hello.itemservice.domain.item.ItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Controller
@RequestMapping("/validation/v2/items")
@RequiredArgsConstructor
public class ValidationItemControllerV2 {

    private final ItemRepository itemRepository;
    private final ItemValidator itemValidator;
    @InitBinder
    public void init(WebDataBinder dataBinder){
        // 이렇게 WebDataBinder 에 검증기를 추가하면 해당 컨트롤러에서는 검증기를 자동으로 적용할 수 있다.
        // @InitBinder 해당 컨트롤러에만 영향을 준다. 글로벌 설정은 별도로 해야한다.
        dataBinder.addValidators(itemValidator);
    }

    @GetMapping
    public String items(Model model) {
        List<Item> items = itemRepository.findAll();
        model.addAttribute("items", items);
        return "validation/v2/items";
    }

    @GetMapping("/{itemId}")
    public String item(@PathVariable long itemId, Model model) {
        Item item = itemRepository.findById(itemId);
        model.addAttribute("item", item);
        return "validation/v2/item";
    }

    @GetMapping("/add")
    public String addForm(Model model) {
        model.addAttribute("item", new Item());
        return "validation/v2/addForm";
    }

//    @PostMapping("/add")
    public String addItemV1(@ModelAttribute Item item, BindingResult bindingResult, RedirectAttributes redirectAttributes, Model model) {
        /* BindingResult
        - 스프링이 제공하는 검증 오류를 보관하는 객체이다. 검증 오류가 발생하면 여기에 보관하면 된다.
        - BindingResult 가 있으면 @ModelAttribute 에 데이터 바인딩 시 오류가 발생해도 컨트롤러가 호출된다!
        - BindingResult 가 없으면 400 오류가 발생하면서 컨트롤러가 호출되지 않고, 오류 페이지로 이동한다.
        - BindingResult 가 있으면 오류 정보( FieldError )를 BindingResult 에 담아서 컨트롤러를 정상 호출한다.
        */
        /*BindingResult 는 검증할 대상 바로 다음에 와야한다. 순서가 중요하다.*/
        // 검증 로직
        if(!StringUtils.hasText(item.getItemName())){
            // 필드에 오류가 있으면 FieldError 객체를 생성해서 bindingResult 에 담아두면 된다.
            bindingResult.addError(new FieldError("item", "itemName", "상품 이름은 필수 입니다."));
        }
        if(item.getPrice() == null || item.getPrice() < 1000 || item.getPrice() > 100000){
            bindingResult.addError(new FieldError("item", "price", "가격은 1,000 ~ 1,000,000 까지 허용합니다."));
        }
        if (item.getQuantity() == null || item.getQuantity() > 9999) {
            bindingResult.addError(new FieldError("item", "quantity", "수량은 최대 9,999 까지 허용합니다."));
        }
        // 특정 필드가 아닌 복합 룰 검증
        if(item.getPrice() != null && item.getQuantity() != null){
            int resultPrice = item.getPrice() * item.getQuantity();
            if(resultPrice < 10000){
                // 특정 필드를 넘어서는 오류가 있으면 ObjectError 객체를 생성해서 bindingResult 에 담아두면 된다.
                bindingResult.addError(new ObjectError("item", "가격 * 수량의 합은 10,000원 이상이어야 합니다. 현재 값 = " + resultPrice));
            }
        }
        //검증에 실패하면 다시 입력 폼으로
        if (bindingResult.hasErrors()) {
            log.info("errors = {}", bindingResult);
            return "validation/v2/addForm";
        }

        Item savedItem = itemRepository.save(item);
        redirectAttributes.addAttribute("itemId", savedItem.getId());
        redirectAttributes.addAttribute("status", true);
        return "redirect:/validation/v2/items/{itemId}";
    }

//    @PostMapping("/add")
    public String addItemV2(@ModelAttribute Item item, BindingResult bindingResult, RedirectAttributes redirectAttributes, Model model) {

        // 검증 로직
        if(!StringUtils.hasText(item.getItemName())){
            // 두 가지 생성자를 제공한다
            /*objectName : 오류가 발생한 객체 이름
            field : 오류 필드
            rejectedValue : 사용자가 입력한 값(거절된 값)
            bindingFailure : 타입 오류 같은 바인딩 실패인지, 검증 실패인지 구분 값
            codes : 메시지 코드
            arguments : 메시지에서 사용하는 인자
            defaultMessage : 기본 오류 메시지*/
            bindingResult.addError(new FieldError("item", "itemName", item.getItemName(), false, null, null,"상품 이름은 필수 입니다."));
        }
        if(item.getPrice() == null || item.getPrice() < 1000 || item.getPrice() > 100000){
            bindingResult.addError(new FieldError("item", "price", item.getPrice(), false, null, null,"가격은 1,000 ~ 1,000,000 까지 허용합니다."));
        }
        if (item.getQuantity() == null || item.getQuantity() > 9999) {
            bindingResult.addError(new FieldError("item", "quantity", item.getQuantity(), false, null, null,"수량은 최대 9,999 까지 허용합니다."));
        }
        // 특정 필드가 아닌 복합 룰 검증
        if(item.getPrice() != null && item.getQuantity() != null){
            int resultPrice = item.getPrice() * item.getQuantity();
            if(resultPrice < 10000){
                bindingResult.addError(new ObjectError("item", null, null, "가격 * 수량의 합은 10,000원 이상이어야 합니다. 현재 값 = " + resultPrice));
            }
        }
        //검증에 실패하면 다시 입력 폼으로
        if (bindingResult.hasErrors()) {
            log.info("errors = {}", bindingResult);
            return "validation/v2/addForm";
        }

        Item savedItem = itemRepository.save(item);
        redirectAttributes.addAttribute("itemId", savedItem.getId());
        redirectAttributes.addAttribute("status", true);
        return "redirect:/validation/v2/items/{itemId}";
    }
//    @PostMapping("/add")
    public String addItemV3(@ModelAttribute Item item, BindingResult bindingResult, RedirectAttributes redirectAttributes, Model model) {

        // 검증 로직
        if(!StringUtils.hasText(item.getItemName())){
            bindingResult.addError(new FieldError("item", "itemName", item.getItemName(), false, new String[]{"required.item.itemName"}, null,null));
        }
        if(item.getPrice() == null || item.getPrice() < 1000 || item.getPrice() > 100000){
            bindingResult.addError(new FieldError("item", "price", item.getPrice(), false, new String[]{"range.item.price"}, new Object[]{1000,10000},null));
        }
        if (item.getQuantity() == null || item.getQuantity() > 9999) {
            bindingResult.addError(new FieldError("item", "quantity", item.getQuantity(), false, new String[]{"max.item.quantity"}, new Object[]{9999},null));
        }
        // 특정 필드가 아닌 복합 룰 검증
        if(item.getPrice() != null && item.getQuantity() != null){
            int resultPrice = item.getPrice() * item.getQuantity();
            if(resultPrice < 10000){
                bindingResult.addError(new ObjectError("item", new String[]{"totalPriceMin"}, new Object[]{10000,resultPrice}, null));
            }
        }
        //검증에 실패하면 다시 입력 폼으로
        if (bindingResult.hasErrors()) {
            log.info("errors = {}", bindingResult);
            return "validation/v2/addForm";
        }

        Item savedItem = itemRepository.save(item);
        redirectAttributes.addAttribute("itemId", savedItem.getId());
        redirectAttributes.addAttribute("status", true);
        return "redirect:/validation/v2/items/{itemId}";
    }
//    @PostMapping("/add")
    public String addItemV4(@ModelAttribute Item item, BindingResult bindingResult, RedirectAttributes redirectAttributes, Model model) {
        // 컨트롤러에서 BindingResult 는 검증해야 할 객체인 target 바로 다음에 온다. 따라서 BindingResult 는 이미 본인이 검증해야 할 객체인 target 을 알고 있다.
        log.info("objectName={}", bindingResult.getObjectName());
        log.info("target={}", bindingResult.getTarget());

        /*BindingResult 가 제공하는 rejectValue() , reject() 를 사용하면
        FieldError , ObjectError 를 직접 생성하지 않고, 깔끔하게 검증 오류를 다룰 수 있다.*/
        /*
        field : 오류 필드명
        errorCode : 오류 코드(이 오류 코드는 메시지에 등록된 코드가 아니다.messageResolver를,위한 오류 코드이다.)
        errorArgs : 오류 메시지에서 {0} 을 치환하기 위한 값
        defaultMessage : 오류 메시지를 찾을 수 없을 때 사용하는 기본 메시지
        */

        if (!StringUtils.hasText(item.getItemName())) {
            bindingResult.rejectValue("itemName", "required");
//            1. rejectValue() 호출
//            2. MessageCodesResolver 를 사용해서 검증 오류 코드로 메시지 코드들을 생성
//            3. new FieldError() 를 생성하면서 메시지 코드들을 보관
//            4. th:erros 에서 메시지 코드들로 메시지를 순서대로 메시지에서 찾고, 노출
        }
        if (item.getPrice() == null || item.getPrice() < 1000 || item.getPrice() > 1000000) {
            bindingResult.rejectValue("price", "range", new Object[]{1000, 1000000},null);
        }
        if (item.getQuantity() == null || item.getQuantity() > 10000) {
            bindingResult.rejectValue("quantity", "max", new Object[]{9999}, null);
        }
        //특정 필드 예외가 아닌 전체 예외
        if (item.getPrice() != null && item.getQuantity() != null) {
            int resultPrice = item.getPrice() * item.getQuantity();
            if (resultPrice < 10000) {
                bindingResult.reject("totalPriceMin", new Object[]{10000,resultPrice}, null);
            }
        }
        /*rejectValue() 를 사용하고 부터는 오류 코드를 range 로 간단하게 입력했다. 그래도 오류 메시지를 잘 찾아서 출
        력한다. 무언가 규칙이 있는 것 처럼 보인다. 이 부분을 이해하려면 MessageCodesResolver 를 이해해야 한다.
        => MessageCodesResolverTest 참고해보자
        */

        /*핵심은 구체적인 것에서! 덜 구체적인 것으로!
          MessageCodesResolver 는 required.item.itemName 처럼 구체적인 것을 먼저 만들어주고, required 처럼
          덜 구체적인 것을 가장 나중에 만든다. 이렇게 하면 앞서 말한 것 처럼 메시지와 관련된 공통 전략을 편리하게 도입할 수 있다.

          왜 이렇게 복잡하게 사용하는가?
          모든 오류 코드에 대해서 메시지를 각각 다 정의하면 개발자 입장에서 관리하기 너무 힘들다.
          크게 중요하지 않은 메시지는 범용성 있는 requried 같은 메시지로 끝내고, 정말 중요한 메시지는 꼭 필요할 때 구체적으로
          적어서 사용하는 방식이 더 효과적이다.
          => errors.properties 를 참고해보자.
          */
        if (bindingResult.hasErrors()) {
            log.info("errors={}", bindingResult);
            return "validation/v2/addForm";
        }
        //성공 로직
        Item savedItem = itemRepository.save(item);
        redirectAttributes.addAttribute("itemId", savedItem.getId());
        redirectAttributes.addAttribute("status", true);
        return "redirect:/validation/v2/items/{itemId}";
    }
//    @PostMapping("/add")
    public String addItemV5(@ModelAttribute Item item, BindingResult bindingResult, RedirectAttributes redirectAttributes, Model model) {

        itemValidator.validate(item, bindingResult);

        if (bindingResult.hasErrors()) {
            log.info("errors={}", bindingResult);
            return "validation/v2/addForm";
        }

        //성공 로직
        Item savedItem = itemRepository.save(item);
        redirectAttributes.addAttribute("itemId", savedItem.getId());
        redirectAttributes.addAttribute("status", true);
        return "redirect:/validation/v2/items/{itemId}";
    }
    @PostMapping("/add")
    public String addItemV6(@Validated @ModelAttribute Item item, BindingResult bindingResult, RedirectAttributes redirectAttributes, Model model) {
        /*@Validated 는 검증기를 실행하라는 애노테이션이다.
        이 애노테이션이 붙으면 앞서 WebDataBinder 에 등록한 검증기를 찾아서 실행한다. 그런데 여러 검증기를 등록한다
        면 그 중에 어떤 검증기가 실행되어야 할지 구분이 필요하다. 이때 supports() 가 사용된다.
        여기서는 supports(Item.class) 호출되고, 결과가 true 이므로 ItemValidator 의 validate()가 호출된다.*/
        if (bindingResult.hasErrors()) {
            log.info("errors={}", bindingResult);
            return "validation/v2/addForm";
        }

        //성공 로직
        Item savedItem = itemRepository.save(item);
        redirectAttributes.addAttribute("itemId", savedItem.getId());
        redirectAttributes.addAttribute("status", true);
        return "redirect:/validation/v2/items/{itemId}";
    }

    @GetMapping("/{itemId}/edit")
    public String editForm(@PathVariable Long itemId, Model model) {
        Item item = itemRepository.findById(itemId);
        model.addAttribute("item", item);
        return "validation/v2/editForm";
    }

    @PostMapping("/{itemId}/edit")
    public String edit(@PathVariable Long itemId, @ModelAttribute Item item) {
        itemRepository.update(itemId, item);
        return "redirect:/validation/v2/items/{itemId}";
    }

}

