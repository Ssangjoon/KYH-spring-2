package com.inflearn.spring.younghanspringmastery.web.member;

import com.inflearn.spring.younghanspringmastery.domain.member.Member;
import com.inflearn.spring.younghanspringmastery.domain.member.MemberRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequiredArgsConstructor
@RequestMapping("/members")
public class MemberController {
    private final MemberRepository memberRepository;
    @GetMapping("/add")
    public String addForm(Member member){
        return "members/addMemberForm";
    }
    @PostMapping("/add")
    public String save(@Valid Member member, BindingResult result){
        if(result.hasErrors()){
            return "members/addMemberForm";
        }
        memberRepository.save(member);
        return "redirect:/";
    }
}
