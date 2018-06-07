# servlet-reverse-proxy
servlet-reverse-proxy is a Java servlet to play a role of reverse-proxy.

## Why servlet-reverse-proxy
Our front-end webapp uses canvas, we want to take screenshot from canvas, but some images on canvas come from another origin and calling `canvas.toDataURL()` will throw an error or gets a blank image.
After some googling we found what it is about, see [stackoverflow -  is-canvas-security-model-ignoring...](https://stackoverflow.com/questions/2985097/is-canvas-security-model-ignoring-access-control-allow-origin-headers#answer-2985136)
and [whatwg - security-with-canvas-elements](https://html.spec.whatwg.org/multipage/canvas.html#security-with-canvas-elements)

## Usage

### use standard-alone war
1. Edit [pom.xml](pom.xml), set `<packaging>war</packaging>`
2. Edit [web.xml](src/main/webapp/WEB-INF/web.xml), register servlet `ReverseProxyServlet`.
3. Package this project as "img_c.war" and deploy it.

### use embedded jar
1. Edit [pom.xml](pom.xml), set `<packaging>jar</packaging>`
2. Edit your own web.xml, register servlet `ReverseProxyServlet`. see [web.xml](src/main/webapp/WEB-INF/web.xml) in this project.
