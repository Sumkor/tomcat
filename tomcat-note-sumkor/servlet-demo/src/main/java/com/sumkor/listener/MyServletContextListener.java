package com.sumkor.listener;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

/**
 * @author Sumkor
 * @since 2020/7/24
 */
@WebListener
public class MyServletContextListener implements ServletContextListener {

    private static final Logger logger = LogManager.getLogger();

    @Override
    public void contextInitialized(final ServletContextEvent sce) {
        logger.info("init servlet context");
    }

    @Override
    public void contextDestroyed(final ServletContextEvent sce) {
        logger.info("destroy servlet container");
    }
}
