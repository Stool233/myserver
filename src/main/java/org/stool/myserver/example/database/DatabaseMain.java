package org.stool.myserver.example.database;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.stool.myserver.core.EntryPoint;
import org.stool.myserver.core.http.HttpMethod;
import org.stool.myserver.core.http.HttpServer;
import org.stool.myserver.route.RouteHandler;

public class DatabaseMain {

    private static SqlSessionFactory sqlSessionFactory = SqlSessionFactoryConfiguration.sqlSessionFactory();

    public static void main(String[] args) {
        EntryPoint entryPoint = EntryPoint.entryPoint();
        HttpServer httpServer = entryPoint.createHttpServer();

        RouteHandler routeHandler = RouteHandler.create(entryPoint);

        routeHandler.route(HttpMethod.GET, "/blog").handler(routingContext -> {
            String id = routingContext.request().getParam("id");
            routingContext.context().executeBlocking(future -> {
                SqlSession session = sqlSessionFactory.openSession();
                try {
                    BlogMapper mapper = session.getMapper(BlogMapper.class);
                    Blog blog = mapper.selectBlog(Integer.valueOf(id));
                    future.tryComplete(blog);
                } catch (Exception e) {
                    future.tryFail(e);
                } finally {
                    session.close();
                }
            }, ar -> {
                if (ar.succeeded()) {
                    Blog blog = (Blog) ar.result();
                    routingContext.response().end(blog.getContent());
                } else {
                    routingContext.response().end(ar.cause().getMessage());
                }
            });
        });


        httpServer.requestHandler(routeHandler).listen(8080);
    }
}
