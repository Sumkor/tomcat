# 1. Tomcat底层架构

## 1.1 Tomcat是一个Servlet容器

```code
class Tomcat {
    Connector connector;// 处理请求
    List<Servlet> servlets;// Servlet容器
}
```

## 1.2 Tomcat中如何部署应用

部署war包：  
org.apache.catalina.startup.HostConfig#deployApps
org.apache.catalina.startup.HostConfig#deployWARs

## 1.3 Tomcat中的4个容器：

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
    List<Servlet> servlet;// Servlet类的实例，单个或多个
}

Pipeline {
    List<Valve> valves;// 阀门
}
```

org.apache.catalina.core.StandardWrapper中，存储单个Servlet类型的实例、实例池：  
org.apache.catalina.core.StandardWrapper#instance  
org.apache.catalina.core.StandardWrapper#instancePool  

一个请求Request进来，经过 Engine->Host->Context->Wrapper->Servlet 路径，依次通过了各个管道的Valve之后，
最终调动Servlet.doGet方法。

想知道Tomcat如何构建Servlet实例并调用它的doGet方法，应该从Wrapper的最后一个Valve入手。

StandardWrapper构造函数中，创建了StandardWrapperValve。  
StandardWrapperValve中，根据请求去执行servlet实例的方法:      
org.apache.catalina.core.StandardWrapperValve#invoke

> Servlet和Filter的执行顺序  
> https://blog.csdn.net/weixin_43343423/article/details/91194399  
> org.apache.catalina.core.ApplicationFilterChain#internalDoFilter

## 1.4 架构图

![tomcat架构](./tomcat架构.png)

# 2. Tomcat如何处理请求

## 2.1 HttpServletRequest的实现

org.apache.catalina.connector.RequestFacade 使用门面模式，实现Servlet规范，暴露给外部使用
org.apache.catalina.connector.Request 内部真正的实现
  

## 2.2 处理请求

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
 
### 2.2.1 启动HTTP服务，绑定端口 

org.apache.catalina.startup.Bootstrap#load  
org.apache.catalina.startup.Catalina#load  
org.apache.catalina.util.LifecycleBase#init   
org.apache.catalina.core.StandardServer#initInternal  
org.apache.catalina.connector.Connector#initInternal  
org.apache.coyote.AbstractProtocol#init  
org.apache.tomcat.util.net.AbstractEndpoint#init  
org.apache.tomcat.util.net.NioEndpoint#bind   

绑定8080端口  
org.apache.tomcat.util.net.NioEndpoint#initServerSocket   
```java
ServerSocketChannel serverSock = ServerSocketChannel.open();
SocketProperties socketProperties.setProperties(serverSock.socket());
InetSocketAddress addr = new InetSocketAddress(getAddress(), getPortWithOffset());
serverSock.socket().bind(addr,getAcceptCount());
```


### 2.2.2 启动HTTP服务，监听请求

org.apache.catalina.startup.Bootstrap#start
org.apache.catalina.startup.Catalina#start  
org.apache.catalina.core.StandardServer#startInternal  
org.apache.coyote.AbstractProtocol#start  
org.apache.tomcat.util.net.NioEndpoint#startInternal  

其中，启动两个线程    
org.apache.tomcat.util.net.NioEndpoint#Poller.run  
org.apache.tomcat.util.net.Acceptor#run  

#### A. 监听Socket

侦听对此套接字的连接并接受它，该方法将阻塞，直到建立连接  
org.apache.tomcat.util.net.Acceptor#run  
org.apache.tomcat.util.net.NioEndpoint#serverSocketAccept  
```java
SocketChannel socket = serverSock.accept();
```

#### B. 轮询Channel  

org.apache.tomcat.util.net.NioEndpoint#Poller.run  
```java
int keyCount = selector.select(selectorTimeout);
Iterator<SelectionKey> iterator = keyCount > 0 ? selector.selectedKeys().iterator() : null;
```

### 2.2.3 接收请求

建立连接  
org.apache.tomcat.util.net.Acceptor#run  
```java
SocketChannel socket = serverSock.accept();
```

org.apache.tomcat.util.net.NioEndpoint#setSocketOptions  
org.apache.tomcat.util.net.NioEndpoint#Poller.register  

处理Socket读写   
org.apache.tomcat.util.net.NioEndpoint#Poller.run  
```java
Iterator<SelectionKey> iterator = keyCount > 0 ? selector.selectedKeys().iterator() : null;
SelectionKey sk = iterator.next();
NioSocketWrapper socketWrapper = (NioSocketWrapper) sk.attachment();
processKey(sk, socketWrapper);
```
org.apache.tomcat.util.net.NioEndpoint#Poller.processKey  
org.apache.tomcat.util.net.AbstractEndpoint#processSocket  

在线程池中，从Socket中读取数据  
org.apache.tomcat.util.net.NioEndpoint#SocketProcessor.doRun  
org.apache.coyote.AbstractProtocol#ConnectionHandler.process  
org.apache.coyote.AbstractProcessorLight#process  

读取HTTP请求行、请求头  
org.apache.coyote.http11.Http11Processor#service  

将请求传递给Servlet容器：Engine->Host->Context->Wrapper
org.apache.coyote.http11.Http11Processor#service  
org.apache.catalina.connector.CoyoteAdapter.service

到达Wrapper容器的最后一个valve  
org.apache.catalina.core.StandardWrapperValve.invoke

### 2.2.4 生成Servlet

项目启动的时候，生成servlet实例，后续请求都不会生成实例
org.apache.catalina.startup.HostConfig#deployApps()  
org.apache.catalina.startup.HostConfig#deployDirectories  
org.apache.catalina.startup.HostConfig#DeployDirectory.run  
org.apache.catalina.core.StandardHost#addChild  
org.apache.catalina.core.StandardContext#startInternal  
org.apache.catalina.core.StandardWrapper#load  
org.apache.catalina.core.StandardWrapper#loadServlet  

### 2.2.5 Tomcat如何维护请求地址与Servlet类的映射关系？

如何根据请求地址，找到对应的Wrapper？  

在web.xml文件中配置：  
```xml
  <servlet>
    <servlet-name>ServletDemo</servlet-name>
    <servlet-class>servlet.ServletDemo</servlet-class>
  </servlet>
  <servlet-mapping>
    <servlet-name>ServletDemo</servlet-name>
    <url-pattern>/servlet/ServletDemo</url-pattern>
  </servlet-mapping>
```

为请求选择Wrapper，即Servlet类型  
org.apache.catalina.core.StandardContextValve#invoke  
```java
Wrapper wrapper = request.getWrapper();
```

具体是什么时候在Request对象中设置Wrapper的呢？  

在处理请求的时候，将请求交给容器之前，首先为请求设置容器对象：  
org.apache.coyote.http11.Http11Processor#service  
org.apache.catalina.connector.CoyoteAdapter#service  
org.apache.catalina.connector.CoyoteAdapter#postParseRequest  
为请求设置Context容器：  
org.apache.catalina.mapper.Mapper#internalMap  
为请求设置Wrapper容器（通过请求路径匹配）：  
org.apache.catalina.mapper.Mapper#internalMapWrapper   
org.apache.catalina.mapper.Mapper#internalMapExactWrapper  



