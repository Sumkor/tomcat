package com.sumkor.valves;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.RequestFilterValve;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;

import javax.servlet.ServletException;
import java.io.IOException;

/**
 * 自定义阀门
 *
 * @author Sumkor
 * @since 2020/7/24
 */
public class MyValve extends RequestFilterValve {

    private static final Log log = LogFactory.getLog(MyValve.class);

    @Override
    public void invoke(Request request, Response response) throws IOException, ServletException {
        log.info("---------MyValve work---------");
        getNext().invoke(request, response);// 执行下一个阀门
    }

    @Override
    protected Log getLog() {
        return null;
    }
}
