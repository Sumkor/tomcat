package com.sumkor.servlet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.servlet.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;

/**
 * @author Sumkor
 * @since 2020/8/8
 */
@WebServlet(name = "myAsyncServlet", urlPatterns = {"/myAsyncServlet"}, asyncSupported = true, loadOnStartup = 1)
public class MyAsyncServlet extends HttpServlet {

    private static final Logger logger = LogManager.getLogger();

    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        logger.info("处理异步Get()请求...");
        resp.setContentType("text/html;charset=UTF-8");
        PrintWriter out = resp.getWriter();
        out.println("进入Servlet的时间：" + new Date());
        out.println("<br/>");
        out.flush();

        AsyncContext asyncContext = req.startAsync();// 异步上下文，存储当前的请求响应对象
        asyncContext.setTimeout(1000000);// 异步执行任务，超时配置
        asyncContext.start(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(5000);
                    ServletResponse resp = asyncContext.getResponse();
                    resp.setContentType("text/html;charset=UTF-8");
                    PrintWriter out = resp.getWriter();
                    out.println("执行业务代码：" + new Date());
                    out.println("<br/>");
                    out.flush();
                    asyncContext.complete();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        // 监听异步任务执行结果
        asyncContext.addListener(new AsyncListener() {
            @Override
            public void onComplete(AsyncEvent asyncEvent) throws IOException {
                logger.info("异步请求执行完成");
            }

            @Override
            public void onTimeout(AsyncEvent asyncEvent) throws IOException {
                logger.info("异步请求执行超时", asyncEvent.getThrowable());
            }

            @Override
            public void onError(AsyncEvent asyncEvent) throws IOException {
                logger.info("异步请求执行失败", asyncEvent.getThrowable());
            }

            @Override
            public void onStartAsync(AsyncEvent asyncEvent) throws IOException {
                logger.info("异步请求执行开始");
            }
        });

        out.println("离开Servlet的时间：" + new Date());
        out.println("<br/>");
        out.flush();
    }
}
