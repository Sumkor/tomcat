<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<body>
<h2>Hello World!</h2>


<a href="myServlet">点击Get()请求</a><br>
<form action="myServlet" method="Post">
    <input type="submit" value="表单Post()请求">
</form>

<a href="myAsyncServlet">异步Get()请求</a><br>

<a href="mySessionServlet01">mySessionServlet01</a><br>
<a href="mySessionServlet02">mySessionServlet02</a><br>

</body>
</html>
