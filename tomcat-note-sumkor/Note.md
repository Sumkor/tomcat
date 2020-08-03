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
Servlet并不是单例，只是容器让它只实例化一次，表现出来的是单例的效果而已。  
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
```java
/**
 * The fully qualified servlet class name for this servlet.
 */
protected String servletClass = null;
/**
 * The (single) possibly uninitialized instance of this servlet.
 */
protected volatile Servlet instance = null;
/**
 * Stack containing the STM instances.
 */
protected Stack<Servlet> instancePool = null;
/**
 * Does this servlet implement the SingleThreadModel interface?
 */
protected volatile boolean singleThreadModel = false;
/**
 * Maximum number of STM instances.
 */
protected int maxInstances = 20;
```


一个请求Request进来，经过 Engine->Host->Context->Wrapper->Servlet 路径，依次通过了各个管道的Valve之后，
最终调动Servlet.doGet方法。

想知道Tomcat如何构建Servlet实例并调用它的doGet方法，应该从Wrapper的最后一个Valve入手。

StandardWrapper构造函数中，创建了StandardWrapperValve：  
```java
public StandardWrapper() {

    super();
    swValve = new StandardWrapperValve();
    pipeline.setBasic(swValve);
    broadcaster = new NotificationBroadcasterSupport();

}
```  

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

 - 重要代码 

读取HTTP请求行、请求头  
org.apache.coyote.http11.Http11Processor#service  

其中将请求传递给Servlet容器：Engine->Host->Context->Wrapper  
org.apache.catalina.connector.CoyoteAdapter.service

 - 重要代码

到达Wrapper容器的最后一个valve，执行servlet实例方法    
org.apache.catalina.core.StandardWrapperValve.invoke


> Servlet和Filter的执行顺序  
> https://blog.csdn.net/weixin_43343423/article/details/91194399  
> org.apache.catalina.core.ApplicationFilterChain#internalDoFilter

### 2.2.4 生成Servlet

项目启动的时候，生成servlet实例，后续请求都不会生成实例  
org.apache.catalina.startup.HostConfig#deployApps()  
org.apache.catalina.startup.HostConfig#deployDirectories  
org.apache.catalina.startup.HostConfig#DeployDirectory.run  
org.apache.catalina.core.StandardHost#addChild  
org.apache.catalina.core.StandardContext#startInternal  
org.apache.catalina.core.StandardWrapper#load  
org.apache.catalina.core.StandardWrapper#loadServlet  
```java
Servlet servlet = (Servlet) instanceManager.newInstance(servletClass);
```

### 2.2.5 Tomcat如何维护请求地址与Servlet类的映射关系？

如何根据请求地址，找到对应的Wrapper？  

在web.xml文件中配置Servlet与请求地址的映射关系：  
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

#### A.请求在容器中的传递

为请求设置容器，并依次执行各个容器  
org.apache.catalina.connector.CoyoteAdapter.service
```java
boolean postParseSuccess = false;
// Parse and set Catalina and configuration specific request parameters
// 前置处理请求，为请求设置对应的host、context、wrapper容器
postParseSuccess = postParseRequest(req, request, res, response);
// Calling the container 
// 取得Engine容器，依次执行它的valve
connector.getService().getContainer().getPipeline().getFirst().invoke(request, response);
```

org.apache.catalina.core.StandardEngineValve#invoke  
```java
// Select the Host to be used for this Request
Host host = request.getHost();
// Ask this Host to process this request
host.getPipeline().getFirst().invoke(request, response);
```

org.apache.catalina.core.StandardHostValve#invoke  
```java
// Select the Context to be used for this Request
Context context = request.getContext();
context.getPipeline().getFirst().invoke(request, response);
```

最终将请求传递给Wrapper  
org.apache.catalina.core.StandardContextValve#invoke  
```java
// Select the Wrapper to be used for this Request 
Wrapper wrapper = request.getWrapper();
// 从Wrapper的pipeline中取出第一个valve，将请求传递过去
wrapper.getPipeline().getFirst().invoke(request, response);
```

#### B.具体是什么时候在Request对象中设置容器的呢？  

在处理请求的时候，将请求交给容器之前，首先为请求设置容器对象：  
org.apache.coyote.http11.Http11Processor#service  
org.apache.catalina.connector.CoyoteAdapter#service  
org.apache.catalina.connector.CoyoteAdapter#postParseRequest  
为请求设置Host、Context容器：  
org.apache.catalina.mapper.Mapper#internalMap  
为请求设置Wrapper容器（通过请求路径匹配）：  
org.apache.catalina.mapper.Mapper#internalMapWrapper   
org.apache.catalina.mapper.Mapper#internalMapExactWrapper  

# Tomcat自定义类加载器

类加载过程：
1. 加载class文件到JVM（染色体）
2. 校验
3. 准备（Java内存模型）（申请肚子）
4. 解析（接口、方法、字段）
5. 初始化（静态变量、静态方法块）
6. 使用（new对象）
7. 销毁

## 类加载器

为每个应用生成不同的WebappClassLoader实例 

Tomcat平台 -----> commonClassLoader   
应用A:com.sumkor.Test -----> WebappClassLoader  
应用B:com.sumkor.Test -----> WebappClassLoader  
 

《深入理解Java虚拟机》  
9.2.1　Tomcat：正统的类加载器架构  
所以Tomcat 6之后也顺理成章地把/common、/server和/shared这3个目录默认合并到一起变成1个/lib目录，这个目录里的类库
相当于以前/common目录中类库的作用  

java.net.URLClassLoader作用在于，可以通过URL资源地址，去加载指定路径下的类文件。  


初始化Tomcat自身的类加载器：  
org.apache.catalina.startup.Bootstrap.initClassLoaders  
```java
ClassLoader commonLoader = null;
ClassLoader catalinaLoader = null;
ClassLoader sharedLoader = null;

private void initClassLoaders() {
    try {
        commonLoader = createClassLoader("common", null);
        if (commonLoader == null) {
            // no config file, default to this loader - we might be in a 'single' env.
            commonLoader = this.getClass().getClassLoader();
        }
        catalinaLoader = createClassLoader("server", commonLoader);
        sharedLoader = createClassLoader("shared", commonLoader);
    } catch (Throwable t) {
        handleThrowable(t);
        log.error("Class loader creation threw exception", t);
        System.exit(1);
    }
}
```

自定义webapp类加载器：  
org.apache.catalina.loader.WebappClassLoader  
具体实现：  
org.apache.catalina.loader.WebappClassLoaderBase#loadClass  

## 热部署

主体：Host  
配置：server.xml的Host标签配置autoDeploy="true"  
触发条件：在webapp目录下放入新的web应用，或者在webapp目录下移除web项目，或者修改了Context的属性？      
结果，触发Host重新部署或取消部署Context  

https://www.cnblogs.com/Marydon20170307/p/7141784.html

## 热加载

主体：Context   
配置：server.xml的Context标签的reloadable为true
触发条件：WEB-INF目录下类文件的修改时间有变动，或者jar文件增加、修改、删除    
结果：触发Context重新加载类  

注意，增加类文件不会立即触发重新加载，因为类加载是按需加载？   

热加载实现：  
org.apache.catalina.loader.WebappLoader#backgroundProcess  
org.apache.catalina.loader.WebappClassLoaderBase#modified   

热加载，需要想办法将旧的class对象，从jvm中卸载掉。  
把用到旧class对象的线程停掉，触发jvm执行垃圾回收。但是很难被回收，结果会导致jvm中的对象越来越多。  

 

# embed





