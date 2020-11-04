package com.ibicd.mvc.servlet;

import com.ibicd.mvc.annotaion.CustController;
import com.ibicd.mvc.annotaion.CustRequestMapping;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;
import java.util.Map.Entry;

/**
 * 自定义Servlet
 */
public class CustServlet extends HttpServlet {

    private Properties properties = new Properties();

    private List<String> classNames = new ArrayList<>();

    private Map<String, Object> ioc = new HashMap<>();

    private Map<String, Method> handlerMapping = new HashMap<>();

    private Map<String, Object> controllerMap = new HashMap<>();

    @Override
    public void init(ServletConfig config) {

        //获取配置文件的位置
        String configLocation = config.getInitParameter("contextConfigLocation");

        //1.加载配置文件
        loadConfig(configLocation);

        //2.初始化所有相关联的类,扫描用户设定的包下面所有的类
        scanClasses(properties.getProperty("scanPackage"));

        //3.拿到扫描到的类,通过反射机制,实例化,并且放到ioc容器中(k-v  beanName-bean) beanName默认是首字母小写
        doInstance();

        //4.初始化HandlerMapping(将url和method对应上)
        initHandlerMapping();
    }


    private void scanClasses(String packageName) {
        URL url = this.getClass().getClassLoader().getResource("/" + packageName.replaceAll("\\.", "/"));
        File dir = new File(url.getFile());
        for (File file : dir.listFiles()) {
            if (file.isDirectory()) {
                //递归读取包
                scanClasses(packageName + "." + file.getName());
            } else {
                String className = packageName + "." + file.getName().replace(".class", "");
                //将指定扫描的类添加到List 中
                classNames.add(className);
            }
        }
    }


    private void doInstance() {
        if (classNames.isEmpty()) {
            return;
        }

        for (String className : classNames) {
            try {
                Class<?> aClass = Class.forName(className);
                if (aClass.isAnnotationPresent(CustController.class)) {
                    String simpleName = aClass.getSimpleName();
                    String iocName = simpleName.substring(0, 1).toLowerCase().concat(simpleName.substring(1, simpleName.length()));
                    ioc.put(iocName, aClass.newInstance());
                }
            } catch (Exception e) {
                e.printStackTrace();
                continue;
            }
        }
    }

    private void initHandlerMapping() {
        if (ioc.isEmpty()) {
            return;
        }

        try {
            for (Entry<String, Object> entry : ioc.entrySet()) {
                Class<?> aClass = entry.getValue().getClass();
                if (!aClass.isAnnotationPresent(CustController.class)) {
                    continue;
                }

                String webUrl = "";
                if (aClass.isAnnotationPresent(CustRequestMapping.class)) {
                    webUrl = aClass.getAnnotation(CustRequestMapping.class).value();
                }

                for (Method method : aClass.getMethods()) {
                    if (!method.isAnnotationPresent(CustRequestMapping.class)) {
                        continue;
                    }
                    method.setAccessible(true);

                    String url = method.getAnnotation(CustRequestMapping.class).value();
                    url = (webUrl + "/" + url).replaceAll("/+", "/");
                    handlerMapping.put(url, method);
                    controllerMap.put(url, entry.getValue());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doDispatch(req, resp);
    }

    private void doDispatch(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (handlerMapping.isEmpty()) {
            return;
        }

        String url = req.getRequestURI();
        String contextPath = req.getContextPath();

        url = url.replace(contextPath, "").replaceAll("/+", "/");

        if (!this.handlerMapping.containsKey(url)) {
            resp.getWriter().write("404 NOT FOUND!");
            return;
        }

        Method method = this.handlerMapping.get(url);
        method.setAccessible(true);
        //获取方法的参数列表
        Class<?>[] parameterTypes = method.getParameterTypes();
        //获取请求参数
        Map<String, String[]> parameterMap = req.getParameterMap();

        Object[] paramValues = new Object[parameterTypes.length];

        for (int i = 0; i < parameterTypes.length; i++) {
            String simpleName = parameterTypes[i].getSimpleName();
            if ("HttpServletRequest".equals(simpleName)) {
                paramValues[i] = req;
                continue;
            }

            if ("HttpServletResponse".equals(simpleName)) {
                paramValues[i] = resp;
                continue;
            }

            //设置请求参数
            if ("String".equals(simpleName)) {
                for (Entry<String, String[]> param : parameterMap.entrySet()) {
                    String value = Arrays.toString(param.getValue()).replaceAll("\\[|\\]", "").replaceAll(",\\s", ",");
                    paramValues[i] = value;
                }
            }
        }


        try {
            method.invoke(this.controllerMap.get(url), paramValues);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    private void loadConfig(String location) {
        if (location.contains("classpath:")) {
            location = location.replaceAll("classpath:", "");
        }
        InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream(location);

        try {
            properties.load(resourceAsStream);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (null != resourceAsStream) {
                try {
                    resourceAsStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
