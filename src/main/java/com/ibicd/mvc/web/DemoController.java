package com.ibicd.mvc.web;

import com.ibicd.mvc.annotaion.CustController;
import com.ibicd.mvc.annotaion.CustRequestMapping;
import com.ibicd.mvc.annotaion.CustRequestParam;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@CustController
@CustRequestMapping(value = "/demo")
public class DemoController {

    @CustRequestMapping("/test1")
    public void test1(HttpServletResponse response,
                      @CustRequestParam("param") String param) {
        System.out.println(param);
        try {
            response.getWriter().write("doTest method success! param:" + param);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @CustRequestMapping("/test2")
    public void test2(HttpServletResponse response) {
        try {
            response.getWriter().println("doTest2 method success!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
