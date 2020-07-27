# Tomcat底层架构

## Tomcat是一个Servlet容器

```code
class Tomcat {
    Connector connector;// 处理请求
    List<Servlet> servlets;// Servlet容器
}
```

## Tomcat中如何部署应用

部署war包：  
org.apache.catalina.startup.HostConfig#deployApps
org.apache.catalina.startup.HostConfig#deployWARs

## Tomcat中的4个容器：

org.apache.catalina.Container 的四个实现接口：
 - org.apache.catalina.Engine 引擎，由多个Host组成.
 - org.apache.catalina.Host 虚拟主机，其中包含多个Context.
 - org.apache.catalina.Context 应用，一个应用中有多个Servlet.
 - org.apache.catalina.Wrapper 代表一个Servlet类的类型.

> Servlet到底是单例还是多例？  
https://www.cnblogs.com/softidea/p/7245899.html   
Servlet并不是单例，只是容器让它只实例化一次，变现出来的是单例的效果而已。  
在web.xml中声明了几次，即使同一个Servlet，如果声明多次，也会生成多个实例（实测如此）。   
是否实现SingleThreadModel(已经声明为废弃，官方不建议使用)，如果实现则最多会创建20个实例。
> - 单例：所有访问Servlet的请求共用同一个Servlet实例
> - 多例：每一访问Servlet的请求单独有一个Servlet实例
  


对于每个容器，有个公共的组件管道org.apache.catalina.Pipeline  
管道之下还有阀门org.apache.catalina.Valve    

容器和阀门均可在conf\server.xml中定义

```code

Engine {
    Pipeline pipeline;// 管道
    List<Host> hosts;// 主机的集合
}

Host {
    Pipeline pipeline;// 管道
    List<Context> contexts;// 应用的集合
}

Context {
    Pipeline pipeline;// 管道
    List<Wrapper> wrappers;// Servlet类的类型
}

Wrapper {
    Pipeline pipeline;// 管道
    List<Servlet> servlet;// Servlet类的实例
}

Pipeline {
    List<Valve> valves;// 阀门
}
```

一个请求Request进来，经过 Engine->Host->Context->Wrapper->Servlet 路径，依次通过了各个管道的Valve之后，
最终调动Servlet.doGet方法

想知道Tomcat如何构建Servlet实例并调用它的doGet方法，应该从Wrapper的最后一个Valve入手。

org.apache.catalina.core.StandardWrapper 构造函数中，创建了  
org.apache.catalina.core.StandardWrapperValve#invoke

> Servlet和Filter的执行顺序
> https://blog.csdn.net/weixin_43343423/article/details/91194399
> org.apache.catalina.core.ApplicationFilterChain#internalDoFilter

## 架构图

![tomcat架构](./tomcat架构.png)

# Tomcat如何处理请求

## HttpServletRequest的实现

org.apache.catalina.connector.RequestFacade 使用门面模式，实现Servlet规范，暴露给外部使用
org.apache.catalina.connector.Request 内部真正的实现
  

## 请求

浏览器：
 1. 构造数据，根据HTTP协议
 2. 建立TCP连接（Socket）
 3. 发送数据
 
Tomcat：
 1. 接收数据（从Socket中取数据）
 
IO模型：NIO、BIO(tomcat9不再支持)
 
Connector从Socket中取数据，再根据HTTP协议构造Request对象 

创建协议  
org.apache.catalina.connector.Connector#Connector(java.lang.String)  
org.apache.coyote.ProtocolHandler#create  

代表HTTP1.1协议，使用NIO模型  
org.apache.coyote.http11.Http11NioProtocol 
 
接收Socket  
org.apache.tomcat.util.net.Acceptor.run
org.apache.tomcat.util.net.NioEndpoint#serverSocketAccept  

从Socket中读取数据  
org.apache.tomcat.util.net.NioEndpoint#SocketProcessor
org.apache.coyote.AbstractProtocol#ConnectionHandler.process
org.apache.coyote.AbstractProcessorLight#process



