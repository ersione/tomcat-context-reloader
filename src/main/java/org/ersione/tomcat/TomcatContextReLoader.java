package org.ersione.tomcat;

import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.Host;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.ersione.security.OTPUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Tomcat의 server.xml 에 Host 또는 Context Element안에 다음을 추가해서 사용한다.
 * <pre>
 * &#60;Valve className="org.ersione.tomcat.TomcatContextReLoader" key="otp_key"/>
 * </pre>
 * Host 에 추가시 http://host:port/reloadContext?context=/test
 * <br>
 * Context 에 추가시 http://host:port/test/reloadContext 호출 시 Context 가 Reload 됩니다.
 */
public class TomcatContextReLoader extends ValveBase {

    private static final Log LOGGER = LogFactory.getLog(TomcatContextReLoader.class);

    private final String RELOAD_CONTEXT_URI = "/reloadContext";

    public TomcatContextReLoader() {
        LOGGER.info("Ersione TomcatContextReLoader Initialized.");
    }

    private OTPUtil otpUtil;

    private String key;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
        otpUtil = OTPUtil.getInstance(key);
    }

    @Override
    public void invoke(Request request, Response response) throws IOException, ServletException {
        Container container = this.getContainer();
        String requestUri = request.getRequestURI();
        String keyParam = request.getParameter("key");

        if (requestUri.endsWith(RELOAD_CONTEXT_URI) && keyParam != null && !keyParam.isEmpty()) {
            if (container instanceof Host) {
                String contextParam = request.getParameter("context");
                if (contextParam != null) {
                    Context context = (Context) container.findChild(contextParam);
                    if (context != null) {
                        if (otpUtil.verify(keyParam)) {
                            this.reloadContext(response, context);
                            return;
                        } else {
                            this.invalidKey(response, context);
                            return;
                        }
                    }
                }
            } else if (container instanceof Context && requestUri.startsWith(request.getContextPath() + RELOAD_CONTEXT_URI)) {
                Context context = (Context) container;
                if (otpUtil.verify(keyParam)) {
                    this.reloadContext(response, context);
                    return;
                } else {
                    this.invalidKey(response, context);
                    return;
                }
            }
        }
        this.getNext().invoke(request, response);
    }

    private void reloadContext(Response response, Context context) throws IOException {
        context.reload();
        HttpServletResponse httpServletResponse = response.getResponse();
        httpServletResponse.setContentType("text/plain;charset=utf-8");
        PrintWriter writer = httpServletResponse.getWriter();
        writer.write("[Ersione TomcatContextReLoader] Context Reloaded!!");
        writer.close();
    }

    private void invalidKey(Response response, Context context) throws IOException {
        HttpServletResponse httpServletResponse = response.getResponse();
        httpServletResponse.setContentType("text/plain;charset=utf-8");
        PrintWriter writer = httpServletResponse.getWriter();
        writer.write("[Ersione TomcatContextReLoader] Invalid Key.");
        writer.close();
    }
}
