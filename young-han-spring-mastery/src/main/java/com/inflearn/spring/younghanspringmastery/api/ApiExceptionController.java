package com.inflearn.spring.younghanspringmastery.api;

import com.inflearn.spring.younghanspringmastery.web.exception.UserException;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController("/api")
public class ApiExceptionController {
    @GetMapping("/members/{id}")
    public MemberDto getMember(@PathVariable String id){
        if(id.equals("ex")){
            throw new RuntimeException("잘못된 사용자");
        }
        if(id.equals("bad")){
            throw new IllegalArgumentException("잘못된 입력값");
        }
        if(id.equals("user-ex")){
            throw new UserException("사용자 오류");
        }
        return new MemberDto(id,"hello " + id);
    }
    @Data
    @AllArgsConstructor
    static class MemberDto{
        private String memberId;
        private String name;
    }
}
