package com.inflearn.spring.younghanspringmastery;

import com.inflearn.spring.younghanspringmastery.domain.item.Item;
import com.inflearn.spring.younghanspringmastery.domain.item.ItemRepository;
import com.inflearn.spring.younghanspringmastery.domain.member.Member;
import com.inflearn.spring.younghanspringmastery.domain.member.MemberRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TestDataInit {

    private final ItemRepository itemRepository;
    private final MemberRepository memberRepository;

    /**
     * 테스트용 데이터 추가
     */
    @PostConstruct
    public void init() {
        itemRepository.save(new Item("itemA", 10000, 10));
        itemRepository.save(new Item("itemB", 20000, 20));

        Member member = new Member();
        member.setLoginId("test");
        member.setPassword("test!");
        member.setName("테스터");
        memberRepository.save(member);
    }

}