package com.sumkor.filter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * 在3.0之后新增@WebFilter注解，当使用注解配置多个Filter时，用户无法控制其执行顺序，此时Filter过滤的顺序是按照Filter的类名来控制的，按自然排序的规则。
 *
 * @author Sumkor
 * @since 2020/7/24
 */
@WebFilter(filterName = "myFilter02", urlPatterns = "/myServlet", dispatcherTypes = {DispatcherType.REQUEST, DispatcherType.FORWARD})
public class MyFilter02 implements Filter {

    private static final Logger logger = LogManager.getLogger();

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
        logger.info("before filter 02 ----- {}", httpServletRequest.getRequestURI());
        filterChain.doFilter(servletRequest, servletResponse);// 调用Servlet.service()
        logger.info("after filter 02 ----- {}", httpServletRequest.getRequestURI());
    }

    @Override
    public void destroy() {
    }
}
