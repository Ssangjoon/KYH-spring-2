package hello.exception.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.BAD_REQUEST, reason = "error.bad")
public class BadReqeustException extends RuntimeException{
}
    /*스프링 부트가 기본으로 제공하는 ExceptionResolver 는 다음과 같다.
        HandlerExceptionResolverComposite 에 다음 순서로 등록
        1. ExceptionHandlerExceptionResolver
        2. ResponseStatusExceptionResolver
        3. DefaultHandlerExceptionResolver 우선 순위가 가장 낮다.*/