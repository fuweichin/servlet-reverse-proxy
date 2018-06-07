package net.bldgos.commons.web.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.StringTokenizer;

import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ReverseProxyServlet extends HttpServlet {
	private static final long serialVersionUID = -3999715834646315331L;

	private String proxyPass;
	private List<String> noBodyRequestMethods=new ArrayList<>();
	private List<String> overrideRequestHeaderNames=new ArrayList<>();
	private List<String> overrideResponseHeaderNames=new ArrayList<>();

	@Override
	public void init() throws ServletException {
		this.proxyPass=this.getInitParameter("proxyPass");
		for(StringTokenizer st=new StringTokenizer("OPTIONS,HEAD,GET", ",");st.hasMoreTokens();) {
			noBodyRequestMethods.add(st.nextToken().toUpperCase());
		}
		for(StringTokenizer st=new StringTokenizer(this.getInitParameter("overrideRequestHeaderNames"));st.hasMoreTokens();) {
			overrideRequestHeaderNames.add(st.nextToken().toLowerCase());
		}
		for(StringTokenizer st=new StringTokenizer(this.getInitParameter("overrideResponseHeaderNames"), ",");st.hasMoreTokens();) {
			overrideResponseHeaderNames.add(st.nextToken().toLowerCase());
		}
	}
	public static String getBOMHost(URL url) {
		String protocol=url.getProtocol();
		int port=url.getPort();
		if(port<0||
				port==80&&protocol.equals("http")||
				port==443&&protocol.equals("https")||
				port==22&&protocol.equals("ftp")) {
			return url.getHost();
		}else {
			return url.getHost()+":"+url.getPort();
		}
	}
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String pathname=request.getPathInfo();
		if(proxyPass==null) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}
		String relativePath=pathname==null?"":pathname.substring(1);
		if(request.getQueryString()!=null) {
			relativePath+="?"+request.getQueryString();
		}
		String contextURL=proxyPass.replace("${scheme}", request.getScheme());;
		URL baseURL=new URL(contextURL);
		URL url=new URL(baseURL,relativePath);
		HttpURLConnection conn=(HttpURLConnection)url.openConnection();
		//proxy request headers
		conn.setRequestMethod(request.getMethod());
		conn.setRequestProperty("Host",getBOMHost(url));
		//conn.setRequestProperty("Connection","Keep-Alive");
		for(Enumeration<String> e=request.getHeaderNames();e.hasMoreElements();) {
			String name=e.nextElement().toLowerCase();
			if(!overrideRequestHeaderNames.contains(name)) {
				String value=request.getHeader(name);
				conn.setRequestProperty(name, value);
			}
		}
		//connect
		conn.connect();
		//proxy request body
		if(!noBodyRequestMethods.contains(request.getMethod())) {
			try(ServletInputStream sis=request.getInputStream(); OutputStream os=conn.getOutputStream()) {
				copyLarge(sis, os);
			}
		}
		//proxy response headers
		response.setStatus(conn.getResponseCode());
		for(String name:conn.getHeaderFields().keySet()) {
			if(name==null)
				continue;
			if(!overrideResponseHeaderNames.contains(name.toLowerCase())) {
				String value=conn.getHeaderField(name);
				response.setHeader(name, value);
			}
		}
		//proxy response body
		try (InputStream is=conn.getInputStream(); ServletOutputStream sos=response.getOutputStream()){
			copyLarge(is, sos);
		}
	}
	static long copyLarge(InputStream is,OutputStream os) throws IOException {
		long count=0;
		byte[] b=new byte[4096];
		for(int len=0;(len=is.read(b,0,b.length))!=-1;) {
			os.write(b, 0, len);
			count+=len;
		}
		return count;
	}
}
