package com.boxdispatch.Security;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.util.UUID;

@Component
@Order(1)
@Slf4j
public class RequestLoggingFilter implements Filter {

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        String requestId = UUID.randomUUID().toString().substring(0, 8);
        long startTime = System.currentTimeMillis();

        request.setAttribute("requestId", requestId);

        log.info("[{}] --> {} {} | IP: {}",
                requestId,
                request.getMethod(),
                request.getRequestURI(),
                request.getRemoteAddr());

        chain.doFilter(req, res);

        long duration = System.currentTimeMillis() - startTime;
        log.info("[{}] <-- {} {} | Status: {} | {}ms",
                requestId,
                request.getMethod(),
                request.getRequestURI(),
                response.getStatus(),
                duration);
    }
}
