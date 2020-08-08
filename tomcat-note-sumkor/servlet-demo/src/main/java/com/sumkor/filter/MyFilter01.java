package com.sumkor.filter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * 在3.0之后新增@WebFilter注解，当使用注解配置多个Filter时，用户无法控制其执行顺序，此时Filter过滤的顺序是按照Filter的类名来控制的，按自然排序的规则。
 * <p>
 * 执行结果：
 * 27-Jul-2020 22:55:00.029 INFO [http-nio-8080-exec-4] com.sumkor.valves.MyValve.invoke ---------MyValve work---------
 * [2020-07-27 22:59:07.421] -- [INFO] -- [http-nio-8080-exec-4 >>>> MyFilter01.java:29 >>>> Method = doFilter] -- [Content = before filter 01 ----- /servlet-demo/myServlet]
 * [2020-07-27 22:59:07.421] -- [INFO] -- [http-nio-8080-exec-4 >>>> MyFilter02.java:29 >>>> Method = doFilter] -- [Content = before filter 02 ----- /servlet-demo/myServlet]
 * [2020-07-27 22:59:07.421] -- [INFO] -- [http-nio-8080-exec-4 >>>> MyServlet.java:25 >>>> Method = doGet] -- [Content = 处理Get()请求...]
 * [2020-07-27 22:59:07.423] -- [INFO] -- [http-nio-8080-exec-4 >>>> MyFilter02.java:31 >>>> Method = doFilter] -- [Content = after filter 02 ----- /servlet-demo/myServlet]
 * [2020-07-27 22:59:07.423] -- [INFO] -- [http-nio-8080-exec-4 >>>> MyFilter01.java:31 >>>> Method = doFilter] -- [Content = after filter 01 ----- /servlet-demo/myServlet]
 *
 * @author Sumkor
 * @since 2020/7/24
 */
@WebFilter(filterName = "myFilter01", urlPatterns = "/*", dispatcherTypes = {DispatcherType.REQUEST, DispatcherType.FORWARD})
public class MyFilter01 implements Filter {

    private static final Logger logger = LogManager.getLogger();

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
        logger.info("before filter 01 ----- {}", httpServletRequest.getRequestURI());
        filterChain.doFilter(servletRequest, servletResponse);// 调用ApplicationFilterChain.doFilter()，在里面不停的执行下一个Filter，最后执行Servlet.service()
        logger.info("after filter 01 ----- {}", httpServletRequest.getRequestURI());
    }

    @Override
    public void destroy() {
    }
}
