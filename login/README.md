도메인 = 화면, UI, 기술 인프라 등등의 영역은 제외한 시스템이 구현해야 하는 핵심 비즈니스 업무 영역을 말함 

⇒ 

향후 web을 다른 기술로 바꾸어도 도메인은 그대로 유지할 수 있어야 한다.
이렇게 하려면 web은 domain을 알고있지만 domain은 web을 모르도록 설계해야 한다. 이것을 web은 domain을 의존하지만, domain은 web을 의존하지 않는다고 표현한다. 예를 들어 web 패키지를 모두 삭제해도 domain에는 전혀 영향이 없도록 의존관계를 설계하는 것이 중요하다. 반대로 이야기하면 domain은 web을 참조하면 안된다.

## 쿠키

- 영속 쿠키 : 만료 날짜를 입력하면 해당 날짜까지 유지
- 세션 쿠키 : 만료 날짜를 생략하면 브라우저 종료시 까지만 유지

```java
Cookie idCookie = new Cookie("memberId", String.valueOf(loginMember.getId())); 
response.addCookie(idCookie);
```

## 쿠키와 보안 문제

- 쿠키 값은 임의로 변경할 수 있다.
    - 클라이언트가 쿠키를 강제로 변경하면 다른 사용자가 된다.
- 쿠키에 보관된 정보는 훔쳐갈 수 있다.

### 대안

- 쿠키에 중요한 값을 노출하지 않고, 사용자 별로 예측 불가능한 임의의 토큰(랜덤 값)을 노출하고, 서버에서 토큰과 사용자 id를 매핑해서 인식한다. 그리고 서버에서 토큰을 관리한다.
- 토큰은 해커가 임의의 값을 넣어도 찾을 수 없도록 예상 불가능 해야 한다.
- 해커가 토큰을 털어가도 시간이 지나면 사용할 수 없도록 서버에서 해당 토큰의 만료시간을 짧게(예: 30분) 유지
한다. 또는 해킹이 의심되는 경우 서버에서 해당 토큰을 강제로 제거하면 된다.

## 세션

### 클라이언트와 서버는 결국 쿠키로 연결되어야 한다.

- 서버는 클라이언트에 MySessionId라는 이름으로 세션ID만 쿠키에 담아서 전달한다.
- 클라이언트는 쿠키 저장소에 mySessionId 쿠키를 보관한다.

**중요**

- 여기서 중요한 포인트는 회원과 관련된 정보는 전혀 클라이언트에 전달하지 않는다는 것이다.
- 오직 추정 불가능한 세션ID만 쿠키를 통해 클라이언트에 전달한다.

```java
package hello.login.web.session;

import org.springframework.stereotype.Component;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class SessionManager {
    public static final String SESSION_COOKIE_NAME = "mySessionId";
    private Map<String, Object> sessionStore = new ConcurrentHashMap<>();

    /**
     * 세션 생성
     */
    public void createSession(Object value, HttpServletResponse response) {
        //세션 id를 생성하고, 값을 세션에 저장
        String sessionId = UUID.randomUUID().toString();
        sessionStore.put(sessionId, value);
        //쿠키 생성
        Cookie mySessionCookie = new Cookie(SESSION_COOKIE_NAME, sessionId);
        response.addCookie(mySessionCookie);
    }
    /**
     * 세션 조회
     */
    public Object getSession(HttpServletRequest request){
        Cookie sessionCookie = findCookie(request, SESSION_COOKIE_NAME);
        if(sessionCookie == null){
            return null;
        }
        return sessionStore.get(sessionCookie.getValue());
    }

    /**
     * 세션 만료
     */
    public void expire(HttpServletRequest request){
        Cookie sessionCookie = findCookie(request, SESSION_COOKIE_NAME);
        if(sessionCookie != null){
            sessionStore.remove(sessionCookie.getValue());
        }
    }
    public Cookie findCookie(HttpServletRequest request, String cookieName){
        Cookie[] cookies = request.getCookies();
        if(cookies == null){
            return null;
        }
        return Arrays.stream(cookies)
                .filter(cookie -> cookie.getName().equals(cookieName))
                .findAny()
                .orElse(null);
    }
}

```

## 서블릿 HTTP 세션1

세션이라는 개념은 대부분의 웹 애플리케이션에 필요한 것이다. 어쩌면 웹이 등장하면서 부터 나온 문제이다.
서블릿은 세션을 위해 HttpSession 이라는 기능을 제공하는데, 지금까지 나온 문제들을 해결해준다.
우리가 직접 구현한 세션의 개념이 이미 구현되어 있고, 더 잘 구현되어 있다.

### HttpSession 소개

서블릿이 제공하는 HttpSession 도 결국 우리가 직접 만든 SessionManager 와 같은 방식으로 동작한다.
서블릿을 통해 HttpSession 을 생성하면 다음과 같은 쿠키를 생성한다. 쿠키 이름이 JSESSIONID 이고, 값은 추정 불가능한 랜덤 값이다.

`Cookie: JSESSIONID=5B78E23B513F50164D6FDD8C97B0AD05` 

### HttpSession 사용

```java
public String loginV3(@Valid @ModelAttribute LoginForm form, BindingResult result, HttpServletRequest request){
        if(result.hasErrors()){
            return "login/loginForm";
        }
        Member loginMember = loginService.login(form.getLoginId(),form.getPassword());

        log.info("login? {}", loginMember);
        if(loginMember == null){
            result.reject("loginFail", "아이디 또는 비밀번호가 맞지 않습니다.");
            return "login/loginForm";
        }

        // 세션이 있으면 기존 세션 반환, 없으면 신규 세션 반환
        // request.getSession(false) => 없으면 신규 세션을 생성하지 않고 null 반환한다.
        HttpSession session = request.getSession();
        // 세션에 로그인 회원 정보 보관
        session.setAttribute(SessionConst.LOGIN_MEMBER, loginMember);

        return "redirect:/";
    }
```

```java
@PostMapping("/logout")
public String logoutV3(HttpServletRequest request) {
    HttpSession session = request.getSession(false);
    if(session != null){
        session.invalidate();
    }

    return "redirect:/";
}
```

## 서블릿 HTTP 세션2

### @SessionAttrubute

스프링은 세션을 더 편리하게 사용할 수 있도록 @SessionAttribute 을 지원한다.
이미 로그인 된 사용자를 찾을 때는 다음과 같이 사용하면 된다. 참고로 이 기능은 세션을 생성하지 않는다.
`@SessionAttribute(name = "loginMember", required = false) Member loginMember`

```java
package hello.login.web;

import hello.login.domain.member.Member;
import hello.login.domain.member.MemberRepository;
import hello.login.web.argumentresolver.Login;
import hello.login.web.session.SessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.SessionAttribute;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

@Slf4j
@Controller
@RequiredArgsConstructor
public class HomeController {

    private final MemberRepository memberRepository;
    private final SessionManager sessionManager;

//    @GetMapping("/")
    public String home() {
        return "home";
    }
//    @GetMapping("/")
    public String homeLogin(@CookieValue(name = "memberId", required = false) Long memberId, Model model) {
        if(memberId == null){
            return "home";
        }
        Member loginMember = memberRepository.findById(memberId);
        if(loginMember == null){
            return "home";
        }
        model.addAttribute("member", loginMember);
        return "loginHome";
    }

//    @GetMapping("/")
    public String homeLoginV2(HttpServletRequest request, Model model) {
        // 세션 관리자에 저장된 회원 정보 조회
        Member member = (Member)sessionManager.getSession(request);

        if(member == null){
            return "home";
        }
        model.addAttribute("member", member);
        return "loginHome";
    }

//    @GetMapping("/")
    public String homeLoginV3(HttpServletRequest request, Model model) {

        HttpSession session = request.getSession(false);
        if (session == null){
            return "home";
        }
        Member loginMember = (Member)session.getAttribute(SessionConst.LOGIN_MEMBER);

        // 세션에 회원 데이터가 없으면 home
        if(loginMember == null){
            return "home";
        }

        // 세션이 유지되면 로그인으로 이동
        model.addAttribute("member", loginMember);
        return "loginHome";
    }

//    @GetMapping("/")
    public String homeLoginV3Spring(
            @SessionAttribute(name = SessionConst.LOGIN_MEMBER, required = false) Member loginMember, Model model) {

        // 세션에 회원 데이터가 없으면 home
        if(loginMember == null){
            return "home";
        }

        // 세션이 유지되면 로그인으로 이동
        model.addAttribute("member", loginMember);
        return "loginHome";
    }
}
```

세션을 찾고, 세션에 들어있는 데이터를 찾는 번거로운 과정을 스프링이 한번에 편리하게 처리해주는 것을 확인할 수 있다.

## TrackingModes

URL 전달 방식을 끄고 항상 쿠키를 통해서만 세션을 유지하고 싶으면 다음 옵션을 넣어주면 된다. 이렇게 하면 URL에 jsessionid 가 노출되지 않는다.

`server.servlet.session.tracking-modes=cookie`

만약 URL에 jsessionid가 꼭 필요하다면 application.properties에 다음 옵션을 추가

`spring.mvc.pathmatch.matching-strategy=ant_path_matcher`

## 세션 정보와 타임아웃 설정

### 세션 정보 확인

```java
package hello.login.web.session;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Date;

@Slf4j
@RestController
public class SessionInfoController {
    @GetMapping("/session-info")
    public String sessionInfo(HttpServletRequest request){

        HttpSession session = request.getSession(false);
        if(session == null){
            return "세션이 없습니다.";
        }

        // 세션 데이터 출력
        session.getAttributeNames().asIterator()
                .forEachRemaining(name -> log.info("session name={}, value={}",
                        name, session.getAttribute(name)));

        log.info("sessionId={}", session.getId()); // JSESSIONID 의 값
        log.info("maxInactiveInterval={}", session.getMaxInactiveInterval()); // 세션의 유효 시간, 예) 1800초, (30분)
        log.info("creationTime={}", new Date(session.getCreationTime())); // 세션 생성일시
        // 세션과 연결된 사용자가 최근에 서버에 접근한 시간, 클라이언트에서 서버로 sessionId ( JSESSIONID )를 요청한 경우에 갱신된다.
        log.info("lastAccessedTime={}", new Date(session.getLastAccessedTime()));
        // 새로 생성된 세션인지, 아니면 이미 과거에 만들어졌고, 클라이언트에서 서버로 sessionId ( JSESSIONID )를 요청해서 조회된 세션인지 여부
        log.info("isNew={}", session.isNew());

        return "세션 출력";

    }
}

```

![Untitled](https://prod-files-secure.s3.us-west-2.amazonaws.com/cf9135fb-4c49-4728-ad1e-1602d2797b14/91c99698-f544-4f44-a407-9dd47b756065/Untitled.png)

### 세션 타임아웃 설정

세션은 사용자가 로그아웃을 직접 호출해서 session.invalidate() 가 호출 되는 경우에 삭제된다. 그런데 대부분의 사용자는 로그아웃을 선택하지 않고, 그냥 웹 브라우저를 종료한다. 

문제는 HTTP가 비 연결성(ConnectionLess)이므로 서버 입장에서는 해당 사용자가 웹 브라우저를 종료한 것인지 아닌지를 인식할 수 없다. 따라서 서버에서 세션데이터를 언제 삭제해야 하는지 판단하기가 어렵다.

**스프링 부트 글로벌 설정**

`server.servlet.session.timeout=60`

**특정 세션 단위로 설정**

`session.setMaxInactiveInterval(1800); //1800초`

### 세션 타임아웃 발생

세션의 타임아웃 시간은 해당 세션과 관련된 JSESSIONID 를 전달하는 HTTP 요청이 있으면 현재 시간으로 다시 초기화 된다. 이렇게 초기화 되면 세션 타임아웃으로 설정한 시간동안 세션을 추가로 사용할 수 있다.

### 정리

서블릿의 HttpSession 이 제공하는 타임아웃 기능 덕분에 세션을 안전하고 편리하게 사용할 수 있다. 실무에서 주의할 점은 세션에는 최소한의 데이터만 보관해야 한다는 점이다. 보관한 데이터 용량 * 사용자 수로 세션의 메모리 사용량
이 급격하게 늘어나서 장애로 이어질 수 있다. 추가로 세션의 시간을 너무 길게 가져가면 메모리 사용이 계속 누적 될 수있으므로 적당한 시간을 선택하는 것이 필요하다. 기본이 30분이라는 것을 기준으로 고민하면 된다.

# Login - filter, interceptor
공통 관심사는 AOP로도 해결할 수 있지만, 웹과 관련딘 공통 관심사는 서블릿 필터 또는 스프링 인터셉터를 사용하는 것이 좋다. 

## 서블릿 필터 소개

필터는 서블릿이 지원하는 수문장이다. 

**필터흐름**

HTTP 요청 -> WAS -> 필터 -> 서블릿 -> 컨트롤러

- 필터를 적용하면 필터가 호출 된 다음에 서블릿이 호출된다. 그래서 모든 고객의 요청 로그를 남기는 요구사항이 있다면 필터를 사용하면 된다.
- 참고로 필터는 특정 URL 패턴에 적용할 수 있다. /* 이라고 하면 모든 요청에 필터가 적용된다.
참고로 스프링을 사용하는 경우 여기서 말하는 서블릿은 스프링의 디스패처 서블릿으로 생각하면 된다.

**필터제한**

HTTP 요청 -> WAS -> 필터 -> 서블릿 -> 컨트롤러 //로그인 사용자
HTTP 요청 -> WAS -> 필터(적절하지 않은 요청이라 판단, 서블릿 호출X) //비 로그인 사용자

- 필터에서 적절하지 않은 요청이라고 판단하면 거기에서 끝을 낼 수도 있다. 그래서 로그인 여부를 체크하기에 딱 좋다.

**필터체인**

HTTP 요청 -> WAS -> 필터1 -> 필터2 -> 필터3 -> 서블릿 -> 컨트롤러

**필터 인터페이스**

```java

package javax.servlet;

import java.io.IOException;

public interface Filter {

    public default void init(FilterConfig filterConfig) throws ServletException {}

     public void doFilter(ServletRequest request, ServletResponse response,
            FilterChain chain) throws IOException, ServletException;

    public default void destroy() {}
}

```

필터 인터페이스를 구현하고 등록하면 서블릿 컨테이너가 필터를 싱글톤 객체로 생성하고, 관리한다.

- init(): 필터 초기화 메서드, 서블릿 컨테이너가 생성될 때 호출된다.
- doFilter(): 고객의 요청이 올 때 마다 해당 메서드가 호출된다. 필터의 로직을 구현하면 된다.
- destroy(): 필터 종료 메서드, 서블릿 컨테이너가 종료될 때 호출된다.

## 서블릿 필터 등록

필터를 등록하는 방법은 여러가지가 있지만, 스프링 부트를 사용한다면 FilterRegistrationBean 을 사용해서 등록하면 된다.

```java
@Configuration
public class WebConfig implements WebMvcConfigurer {
    // @ServletComponentScan @WebFilter(filterName = "logFilter", urlPatterns = "/*")로
    // 필터 등록이 가능하지만 필터 순서 조절이 안된다. 따라서 FilterRegistrationBean 을 사용하자.
    // 실무에서 HTTP 요청시 같은 요청의 로그에 모두 같은 식별자를 자동으로 남기는 방법은 logback mdc로 검색 해보자.
    @Bean
    public FilterRegistrationBean logFilter(){
        FilterRegistrationBean<Filter> filterFilterRegistrationBean = new FilterRegistrationBean<>();
        filterFilterRegistrationBean.setFilter(new LogFilter());
        filterFilterRegistrationBean.setOrder(1);
        filterFilterRegistrationBean.addUrlPatterns("/*");

        return  filterFilterRegistrationBean;
    }
}
```

### 로그인 체크 필터

```java
package hello.login.web.filter;

import hello.login.web.SessionConst;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.PatternMatchUtils;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

@Slf4j
public class LoginCheckFilter implements Filter {

    private static final String[] whiteList = {"/", "/member/add", "/login", "/logout", "/css/*"};

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String requestURI = httpRequest.getRequestURI();
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        try{
            log.info("인증 체크 필터 시작 {}", requestURI);
            if(isLoginCheckPath(requestURI)){
                log.info("인증 체크 로직 실행 {}", requestURI);
                HttpSession session = httpRequest.getSession(false);
                if(session == null || session.getAttribute(SessionConst.LOGIN_MEMBER) == null){

                    log.info("미인증 사용자 요청 {}", requestURI);
                    // 로그인으로 redirect
                    httpResponse.sendRedirect("/login?redirectURL=" + requestURI);
                    return;
                }
            }
            chain.doFilter(request,response);
        } catch (Exception e){
            throw e; // 예외 로깅 가능하지만, 톰캣까지 예외를 보내주어야 함
        } finally {
            log.info("인증 체크 필터 종료 {}", requestURI);
        }
    }

    /**
     * 화이트 리스트의 경우 인증 체크 x
     */
    private boolean isLoginCheckPath(String reqeustURI){
        return !PatternMatchUtils.simpleMatch(whiteList, reqeustURI);
    }

}

```

```java
public String loginV4(@Valid @ModelAttribute LoginForm form, BindingResult result,
                      @RequestParam(defaultValue = "/") String redirectURL,
                      HttpServletRequest request){
    if(result.hasErrors()){
        return "login/loginForm";
    }
    Member loginMember = loginService.login(form.getLoginId(),form.getPassword());

    log.info("login? {}", loginMember);
    if(loginMember == null){
        result.reject("loginFail", "아이디 또는 비밀번호가 맞지 않습니다.");
        return "login/loginForm";
    }

    // 세션이 있으면 기존 세션 반환, 없으면 신규 세션 반환
    // request.getSession(false) => 없으면 신규 세션을 생성하지 않고 null 반환한다.
    HttpSession session = request.getSession();
    // 세션에 로그인 회원 정보 보관
    session.setAttribute(SessionConst.LOGIN_MEMBER, loginMember);

    return "redirect:" + redirectURL;
}
```

## 스프링 인터셉터 - 소개

스프링 인터셉터도 서블릿 필터와 같이 웹과 관련된 공통 관심 사항을 효과적으로 해결할 수 있는 기술이다.
서블릿 필터가 서블릿이 제공하는 기술이라면, 스프링 인터셉터는 스프링 MVC가 제공하는 기술이다. 둘다 웹과 관련된 공통 관심 사항을 처리하지만, 적용되는 순서와 범위, 그리고 사용방법이 다르다.

**스프링 인터셉터 흐름**

HTTP 요청 -> WAS -> 필터 -> 서블릿 -> 스프링 인터셉터 -> 컨트롤러

- 스프링 인터셉터는 디스패처 서블릿과 컨트롤러 사이에서 컨트롤러 호출 직전에 호출 된다.
- 스프링 인터셉터는 스프링 MVC가 제공하는 기능이기 때문에 결국 디스패처 서블릿 이후에 등장하게 된다. 
스프링 MVC의 시작점이 디스패처 서블릿이라고 생각해보면 이해가 될 것이다.
- 스프링 인터셉터에도 URL 패턴을 적용할 수 있는데, 서블릿 URL 패턴과는 다르고, 매우 정밀하게 설정할 수 있다.

**스프링 인터셉터 제한**

```
HTTP 요청 -> WAS -> 필터 -> 서블릿 -> 스프링 인터셉터 -> 컨트롤러 //로그인 사용자
HTTP 요청 -> WAS -> 필터 -> 서블릿 -> 스프링 인터셉터(적절하지 않은 요청이라 판단, 컨트롤러 호출
X) // 비 로그인 사용자
```

**스프링 인터셉터 체인**

HTTP 요청 -> WAS -> 필터 -> 서블릿 -> 인터셉터1 -> 인터셉터2 -> 컨트롤러

지금까지 내용을 보면 서블릿 필터와 호출 되는 순서만 다르고, 제공하는 기능은 비슷해 보인다. 앞으로 설명하겠지만, 스프링 인터셉터는 서블릿 필터보다 편리하고, 더 정교하고 다양한 기능을 지원한다.

**스프링 인터셉터 인터페이스** 

스프링의 인터셉터를 사용하려면 HandlerInterceptor 인터페이스를 구현하면 된다.

```java
public interface HandlerInterceptor {

	default boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {

		return true;
	}

	default void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
			@Nullable ModelAndView modelAndView) throws Exception {
	}

	default void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
			@Nullable Exception ex) throws Exception {
	}

}

```

블릿 필터의 경우 단순하게 doFilter() 하나만 제공된다. 인터셉터는 컨트롤러 호출 전( preHandle ), 호출 후( postHandle ), 요청 완료 이후( afterCompletion )와 같이 단계적으로 잘 세분화 되어 있다.
서블릿 필터의 경우 단순히 request , response 만 제공했지만, 인터셉터는 어떤 컨트롤러( handler )가 호출되는지 호출 정보도 받을 수 있다. 그리고 어떤 modelAndView 가 반환되는지 응답 정보도 받을 수 있다.

![Untitled](https://prod-files-secure.s3.us-west-2.amazonaws.com/cf9135fb-4c49-4728-ad1e-1602d2797b14/d4ef360f-14f0-4c23-a882-6ca1e7575955/Untitled.png)

**정상흐름**

- preHandle : 컨트롤러 호출 전에 호출된다. (더 정확히는 핸들러 어댑터 호출 전에 호출된다.)
    - preHandle 의 응답값이 true 이면 다음으로 진행하고, false 이면 더는 진행하지 않는다. false 인 경우 나머지 인터셉터는 물론이고, 핸들러 어댑터도 호출되지 않는다. 그림에서 1번에서 끝이 나버린다.
- postHandle : 컨트롤러 호출 후에 호출된다. (더 정확히는 핸들러 어댑터 호출 후에 호출된다.)
- afterCompletion : 뷰가 렌더링 된 이후에 호출된다.

![Untitled](https://prod-files-secure.s3.us-west-2.amazonaws.com/cf9135fb-4c49-4728-ad1e-1602d2797b14/1fb2bef5-78c1-4f26-a89f-5fa0a4ff52c1/Untitled.png)

**예외 발생시**

- preHandle : 컨트롤러 호출 전에 호출된다.
- postHandle : 컨트롤러에서 예외가 발생하면 postHandle 은 호출되지 않는다.
- afterCompletion : afterCompletion 은 항상 호출된다. 이 경우 예외( ex )를 파라미터로 받아서 어떤 예외가 발생했는지 로그로 출력할 수 있다.

## 스프링 인터셉터 - 요청 로그

```java
package hello.login.web.interceptor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.UUID;

@Slf4j
public class LogInterceptor implements HandlerInterceptor {

    public static final String LOG_ID = "logId";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        String requestURI = request.getRequestURI();
        String uuid = UUID.randomUUID().toString();

        // 서블릿 필터의 경우 지역변수로 해결이 가능하지만, 스프링 인터셉터는 호출 시점이 완전히 분리되어 있다.
        // 따라서 preHandle 에서 지정한 값을 postHandle , afterCompletion 에서 함께 사용하려면 어딘가에 담아두어야 한다.
        // LogInterceptor 도 싱글톤 처럼 사용되기 때문에 맴버변수를 사용하면 위험하다. 따라서 request 에 담아두었다.
        request.setAttribute(LOG_ID, uuid);

        // @RequestMapping: HandlerMethod
        // 핸들러 정보는 어떤 핸들러 매핑을 사용하는가에 따라 달라진다.
        // 스프링을 사용하면 일반적으로 @Controller, @RequestMapping 을 활용한 핸들러 매핑을 사용하는데,
        // 이 경우 핸들러 정보로 HandlerMethod 가 넘어온다.
        // 정적 리소스 : ResourceHttpRequestHandler
        // @Controller 가 아니라 /resources/static 와 같은 정적 리소스가 호출 되는 경우
        // ResourceHttpRequestHandler 가 핸들러 정보로 넘어오기 때문에 타입에 따라서 처리가 필요하다.
        if(handler instanceof HandlerMethod){
            HandlerMethod hm = (HandlerMethod) handler; // 호출할 컨트롤러 메서드의 모든 정보가 포함되어 있다.
        }
        log.info("REQUEST [{}][{}][{}]", uuid, requestURI, handler);
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        log.info("postHandle [{}]", modelAndView);
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 종료 로그를 postHandle 이 아니라 afterCompletion 에서 실행한 이유는,
        // 예외가 발생한 경우 postHandle가 호출되지 않기 때문이다.
        // afterCompletion 은 예외가 발생해도 호출 되는 것을 보장한다.
        String requestURI = request.getRequestURI();
        String uuid = (String) request.getAttribute(LOG_ID);
        log.info("REQUEST [{}][{}][{}]", uuid, requestURI, handler);
        if(ex != null){
            log.error("afterCompletion error!!", ex);
        }
    }
}

```

```java
@Configuration
public class WebConfig implements WebMvcConfigurer { 
  @Override
	public void addInterceptors(InterceptorRegistry registry) { 
	        registry.addInterceptor(new LogInterceptor()) 
	                .order(1)
	                .addPathPatterns("/**")
	                .excludePathPatterns("/css/**", "/*.ico", "/error"); 
  }
//...
}
```

**스프링의 URL 경로**
스프링이 제공하는 URL 경로는 서블릿 기술이 제공하는 URL 경로와 완전히 다르다. 더욱 자세하고, 세밀하게 설정할 수 있다.

https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/web/util/pattern/PathPattern.html

## 스프링 인터셉터 - 인증 체크

```java
package hello.login.web.interceptor;

import hello.login.web.SessionConst;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@Slf4j
public class LoginCheckInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        String requestURI = request.getRequestURI();
        log.info("인증 체크 인터셉터 실행 {}", requestURI);

        HttpSession session = request.getSession();

        if(session == null || session.getAttribute(SessionConst.LOGIN_MEMBER) == null){
            log.info("미인증 사용자 요청");
            response.sendRedirect("/login?redirectURL=" + requestURI);
            return false;
        }

        return true;
    }
}

```

인증이라는 것은 컨트롤러 호출 전에만 호출되면 된다. 따라서 preHandle 만 구현하면 된다.

```java
@Configuration
public class WebConfig implements WebMvcConfigurer { 
    @Override
		public void addInterceptors(InterceptorRegistry registry) { 
		        registry.addInterceptor(new LogInterceptor()) 
		                .order(1)
		                .addPathPatterns("/**")                .excludePathPatterns("/css/**", "/*.ico", "/error"); 
		        registry.addInterceptor(new LoginCheckInterceptor())
		                .order(2)
		                .addPathPatterns("/**")
		                .excludePathPatterns(
		"/", "/members/add", "/login", "/logout", 
		"/css/**", "/*.ico", "/error"
    ); 
  }
//...
}
```

## ArgumentResolve 활용

```java
@GetMapping("/")
public String homeLoginV3ArgumentResolver(@Login Member loginMember, Model model) {

    // 세션에 회원 데이터가 없으면 home
    if(loginMember == null){
        return "home";
    }

    // 세션이 유지되면 로그인으로 이동
    model.addAttribute("member", loginMember);
    return "loginHome";
}
```

@Login 애노테이션이 있으면 직접 만든 ArgumentResolver 가 동작해서 자동으로 세션에 있는 로그인 회원을 찾아주고, 만약 세션에 없다면 null 을 반환하도록 개발해보자.

```java
@Target(ElementType.PARAMETER) // 파라미터에서만 사용
@Retention(RetentionPolicy.RUNTIME) // 리플렉션 등을 활용할 수 있도록 런타임까지 애노테이션 정보가 남아있음
public @interface Login {
}
```

```java
@Slf4j
public class LoginMemberArgumentResolver implements HandlerMethodArgumentResolver {
    @Override
    public boolean supportsParameter(MethodParameter parameter) {

        log.info("supportsParameter 실행");

        boolean hasLoginAnnotation = parameter.hasParameterAnnotation(Login.class);
        boolean hasMemberType = Member.class.isAssignableFrom(parameter.getParameterType());

        return hasLoginAnnotation && hasMemberType;
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer, NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {

        log.info("resolverArgument 실행");

        HttpServletRequest request = (HttpServletRequest) webRequest.getNativeRequest();
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }

        return session.getAttribute(SessionConst.LOGIN_MEMBER);
    }
}
```

```java
@Configuration
public class WebConfig implements WebMvcConfigurer { 
    @Override
		public void addArgumentResolvers(List<HandlerMethodArgumentResolver> 
		resolvers) {
        resolvers.add(new LoginMemberArgumentResolver()); 
    }
//...
}
```
