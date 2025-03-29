package com.example.app;

import java.io.File;

import org.apache.catalina.Context;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.webresources.DirResourceSet;
import org.apache.catalina.webresources.StandardRoot;
import org.apache.catalina.servlets.DefaultServlet;
import org.h2.tools.Server;

public class EmbeddedTomcatServer {

  public static void main(String[] args) throws Exception {

    Server webServer = Server.createWebServer("-webPort", "8082", "-tcpAllowOthers").start();
    System.out.println("H2 Web Console started at: " + webServer.getURL());

    Tomcat tomcat = new Tomcat();
    tomcat.setPort(8081);
    tomcat.setBaseDir("temp");

    String webappDir = "backend/demo/src/main/webapp";
    Context ctx = tomcat.addWebapp("", new File(webappDir).getAbsolutePath());
    ((StandardContext) ctx).setMetadataComplete(false);

    StandardRoot resources = new StandardRoot(ctx);
    File additionWebInfClasses = new File("backend/demo/target/classes");
    resources.addPreResources(new DirResourceSet(resources, "/WEB-INF/classes",
        additionWebInfClasses.getAbsolutePath(), "/"));
    ctx.setResources(resources);
    tomcat.setAddDefaultWebXmlToWebapp(false);
    Tomcat.addDefaultMimeTypeMappings(ctx);

    Tomcat.addServlet(ctx, "Static", new DefaultServlet());
    ctx.addServletMappingDecoded("/static/*", "Static");

    tomcat.getConnector();
    tomcat.getConnector().setProperty("maxConnections", "10000");
    tomcat.getConnector().setProperty("acceptCount", "1000");
    tomcat.getConnector().setProperty("maxThreads", "50");
    tomcat.start();
    tomcat.getServer().await();
  }

}
