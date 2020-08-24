package com.sumkor.servlet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Cookie：在浏览器中保存用户的信息
 * 使用：由服务器创建，发送到浏览器保存，之后随着请求发回到服务器
 * <p>
 * Session：在服务器中保存用户信息
 * 使用：在服务器端创建Session，来保存当前访问服务器的用户的信息
 * <p>
 * ----------------------------
 * <p>
 * Cookie从何而来？
 * 1、浏览器向服务器发送请求
 * 2、服务器接收请求，生成一个cookie对象保存"标识"数据
 * 3、然后把cookie对象放在响应头，一并发回浏览器
 * 4、浏览器取出cookie对象的数据保存在浏览器的缓存中
 * 5、再次访问时请求头携带cookie数据发送到服务器
 * 6、服务器根据cookie的数据作出相应处理
 * <p>
 * 使用注意：
 * 1.cookie第一次是由servlet发送到浏览器中，第一次不能获取cookie
 * 2.不同的浏览器存放的cookie不是同一个
 * 3.如果设置了cookie的maxAge，则cookie会保存在浏览器所在电脑的硬盘上，如果没设置该属性，则保存在浏览器的内存中
 *
 * @author Sumkor
 * @since 2020/8/24
 */
@WebServlet(name = "mySessionServlet01", urlPatterns = {"/mySessionServlet01"}, loadOnStartup = 1)
public class MySessionServlet01 extends HttpServlet {

    private static final Logger logger = LogManager.getLogger();

    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        logger.info("处理Get()请求...");

        resp.setContentType("text/html");
        resp.setCharacterEncoding("UTF-8");

        HttpSession session = req.getSession();// 获取session（如果已经创建了session，则返回当前session）或创建session：
        session.setAttribute("key", "mySessionValue");

//        Cookie cookie = new Cookie("JSESSIONID", session.getId());
//        cookie.setMaxAge(3600);
//        resp.addCookie(cookie);// 向客户端设置Cookie
//        logger.info("设置cookie...");

        PrintWriter out = resp.getWriter();
        out.println("The session has been generated!");
        out.flush();
        out.close();
    }
}
