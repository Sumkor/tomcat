## Tomcat是一个Servlet容器

```code
class Tomcat {
    Connector connector;// 处理请求
    List<Servlet> servlets;// Servlet容器
}
```

## HttpServletRequest的实现

org.apache.catalina.connector.RequestFacade 使用门面模式  


## Tomcat中的4个容器：

org.apache.catalina.Container 的四个实现接口：
 - org.apache.catalina.Wrapper 代表一个Servlet类的类型.
 - org.apache.catalina.Context 应用，一个应用中有多个Servlet.
 - org.apache.catalina.Host 虚拟主机，其中包含多个Context.
 - org.apache.catalina.Engine 引擎，由多个Host组成.

可以在conf\server.xml中定义

```code

Engine {
    List<Host> hosts;// 主机的集合
}

Host {
    List<Context> contexts;// 应用的集合
}

Context {
    List<Wrapper> wrappers;// Servlet类的类型
}

Wrapper {
    List<Servlet> servlet;// Servlet类的实例
}
```





