package com.blog.config;

import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class UploadHeaderFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (response instanceof HttpServletResponse) {
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            String path = ((javax.servlet.http.HttpServletRequest) request).getRequestURI();
            if (path.startsWith("/uploads/")) {
                httpResponse.setHeader("Cross-Origin-Resource-Policy", "cross-origin");
            }
        }
        chain.doFilter(request, response);
    }
}
