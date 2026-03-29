
FROM tomcat:9.0-jdk17

RUN apt-get update && apt-get install -y \
    wget gnupg unzip curl ca-certificates \
 && curl -fsSL https://dl.google.com/linux/linux_signing_key.pub \
    | gpg --dearmor -o /usr/share/keyrings/google-chrome.gpg \
 && echo "deb [arch=amd64 signed-by=/usr/share/keyrings/google-chrome.gpg] \
    http://dl.google.com/linux/chrome/deb/ stable main" \
    > /etc/apt/sources.list.d/google-chrome.list \
 && apt-get update && apt-get install -y google-chrome-stable \
 && rm -rf /var/lib/apt/lists/*

RUN CHROME_MAJOR=$(google-chrome --version | awk '{print $3}' | cut -d. -f1) && \
    CHROME_FULL=$(curl -s "https://googlechromelabs.github.io/chrome-for-testing/LATEST_RELEASE_${CHROME_MAJOR}") && \
    wget -O /tmp/chromedriver.zip \
      "https://storage.googleapis.com/chrome-for-testing-public/${CHROME_FULL}/linux64/chromedriver-linux64.zip" && \
    unzip /tmp/chromedriver.zip -d /tmp/chromedriver_extracted && \
    mv /tmp/chromedriver_extracted/chromedriver-linux64/chromedriver /usr/local/bin/chromedriver && \
    chmod +x /usr/local/bin/chromedriver && \
    rm -rf /tmp/chromedriver.zip /tmp/chromedriver_extracted

WORKDIR /opt/app

# Copy web resources (JSP, web.xml, WEB-INF/lib with Selenium + POI jars)
COPY src/main/webapp/ /opt/app/

# Copy Java sources (com.samplewishapp.* under src/main/java)
COPY src/main/java/ /opt/app/src/

# Compile all servlets/pages with classpath including WEB-INF/lib and servlet-api
RUN SERVLET_JAR="/usr/local/tomcat/lib/servlet-api.jar" && \
    LIB_DIR="/opt/app/WEB-INF/lib" && \
    CP=$(find "$LIB_DIR" -name "*.jar" | paste -sd: -):"$SERVLET_JAR" && \
    mkdir -p /opt/app/WEB-INF/classes && \
    find /opt/app/src -name "*.java" -exec javac -cp "$CP" -d /opt/app/WEB-INF/classes {} + && \
    rm -rf /opt/app/src

# Deploy compiled classes + jars into Tomcat webapp
RUN mkdir -p /usr/local/tomcat/webapps/SampleWishApp/WEB-INF/lib && \
    mkdir -p /usr/local/tomcat/webapps/SampleWishApp/WEB-INF/classes && \
    mkdir -p /usr/local/tomcat/webapps/SampleWishApp/uploads && \
    mkdir -p /usr/local/tomcat/webapps/SampleWishApp/outputs && \
    cp -r /opt/app/WEB-INF/lib/*.jar /usr/local/tomcat/webapps/SampleWishApp/WEB-INF/lib/ && \
    cp -r /opt/app/WEB-INF/classes/* /usr/local/tomcat/webapps/SampleWishApp/WEB-INF/classes/ && \
    cp /opt/app/*.jsp /opt/app/*.html /usr/local/tomcat/webapps/SampleWishApp/

EXPOSE 8080
CMD ["/usr/local/tomcat/bin/catalina.sh", "run"]