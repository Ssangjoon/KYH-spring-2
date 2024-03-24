package hello.login.web.interceptor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

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
