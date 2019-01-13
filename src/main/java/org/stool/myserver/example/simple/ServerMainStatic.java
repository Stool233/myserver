package org.stool.myserver.example.simple;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stool.myserver.core.http.HttpServer;
import org.stool.myserver.core.net.Buffer;

import java.nio.charset.Charset;

public class ServerMainStatic {


    private static Logger log = LoggerFactory.getLogger(ServerMainStatic.class);

    public static void main(String[] args) {
        startServer();
    }

    private static void startServer() {

        HttpServer server = HttpServer.server();


        server.requestHandler(request -> {

            request.response().end("<!DOCTYPE html>\n" +
                    "<html class=\"theme theme-white\">\n" +
                    "<head>\n" +
                    "<meta charset=\"utf-8\">\n" +
                    "<title>China’s New Role in the World Stage</title>\n" +
                    "<link href=\"https://www.zybuluo.com/static/assets/template-theme-white.css\" rel=\"stylesheet\" media=\"screen\">\n" +
                    "<style type=\"text/css\">\n" +
                    "\n" +
                    "#wmd-preview h1  {\n" +
                    "    color: #0077bb; /* 将标题改为蓝色 */\n" +
                    "}</style>\n" +
                    "</head>\n" +
                    "<body class=\"theme theme-white\">\n" +
                    "<div id=\"wmd-preview\" class=\"wmd-preview wmd-preview-full-reader\"><div class=\"md-section-divider\"></div><div class=\"md-section-divider\"></div><h1 data-anchor-id=\"z83h\" id=\"chinas-new-role-in-the-world-stage\">China’s New Role in the World Stage</h1><p data-anchor-id=\"6y9u\"><code>未分类</code></p><hr><p data-anchor-id=\"xrxj\">More than thirty years ago China <strong>launched</strong> <strong>economic reforms</strong> that would transform the country and its place in the world. At a speed no one could have <strong>predicted</strong> even a fifteen years ago, China has <strong>generated</strong> <strong>rapid</strong> growth and <strong>lifted</strong> more than 500 million <strong>citizens</strong> out of <strong>rapid</strong>. The country has been pounding away at 10 per cent growth or more for almost thirty years now. Today China is the second biggest economy and it has become an export superpower. China’s exports to the United States increased 1,600 per cent in fifteen years. </p><p data-anchor-id=\"3xaj\">China’s role in the world stage is obviously evolving. Today, China's economy is featured by a more sophisticated structure with more technology, more innovation, more skilled people, more brands. Despite there is still only one global superpower, i.e. the United States., it is very difficult to think of any global problem that could be resolved satisfactorily without China’s involvement.</p><p data-anchor-id=\"an5d\">China is now the new engine of global economic growth, and the sound economic growth in China is conducive to the world at large.</p><p data-anchor-id=\"4bbs\">Chinese economy is doing very well, is now very much about focusing on innovation, on the entrepreneurship, and the green economy, more focused on quality growth.</p><p data-anchor-id=\"mere\">Chinese companies now are taking the world lead, they are innovative, they are the vanguards, they are not longer just copying the technology about 10 to 15 years ago, which was very natural at that time, But now they are at the front of the (world's advanced) technology. Such as Huawei. </p><p data-anchor-id=\"xo0q\">China is committed to being better integrated into the global economy, leading to a bigger cake for all, while ensuring that the distribution of that cake is rule-based. Although China is the second largest economy in the world, it is still far behind number one. But that distance is also potential. There is a long way to go, but China aspires to play a crucial role in global recovery and sustained prosperity.</p></div>\n" +
                    "</body>\n" +
                    "</html>");


        }).listen(8081);

        server.start();

    }


}
