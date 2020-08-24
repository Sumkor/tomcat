package com.sumkor.servlet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * 如果在mySessionServlet01的同一个窗口来调用mySessionServlet02。这时客户端已经有了一个临时Cookie，就是JSESSIONID，
 * 因此，会将这个Cookie放到HTTP头的Cookie字段中发送给服务端。
 * 服务端在收到这个HTTP请求时就可以从Cookie中得到JSESSIONID的值，并从Session Map中找到这个Session对象，也就是getSession方法的返回值。
 * 因此，从技术层面上来说，所有拥有同一个Session ID的页面都应该属于同一个会话。
 * <p>
 * https://blog.csdn.net/findmyself_for_world/article/details/41846347
 *
 * @author Sumkor
 * @since 2020/8/24
 */
@WebServlet(name = "mySessionServlet02", urlPatterns = {"/mySessionServlet02"}, loadOnStartup = 1)
public class MySessionServlet02 extends HttpServlet {

    private static final Logger logger = LogManager.getLogger();

    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        logger.info("处理Get()请求...");

        resp.setContentType("text/html");
        resp.setCharacterEncoding("UTF-8");

        HttpSession session = req.getSession();

        PrintWriter out = resp.getWriter();
        out.println(session.getAttribute("key"));
        out.flush();
        out.close();
    }
}
