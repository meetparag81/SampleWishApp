# Use Tomcat 9 with Java 17
FROM tomcat:9.0-jdk17

# ── System deps + Google Chrome ──────────────────────────────────────────────
RUN apt-get update && apt-get install -y \
    wget gnupg unzip curl \
 && wget -q -O - https://dl-ssl.google.com/linux/linux_signing_key.pub | apt-key add - \
 && echo "deb [arch=amd64] http://dl.google.com/linux/chrome/deb/ stable main" \
      > /etc/apt/sources.list.d/google-chrome.list \
 && apt-get update && apt-get install -y google-chrome-stable \
 && rm -rf /var/lib/apt/lists/*

# ── Chrome for Testing ChromeDriver URL (Chrome 115+) ────────────────────────
# Match ChromeDriver version to the installed Chrome MAJOR version
RUN CHROME_MAJOR=$(google-chrome --version | awk '{print $3}' | cut -d. -f1) && \
    CHROME_FULL=$(curl -s "https://googlechromelabs.github.io/chrome-for-testing/LATEST_RELEASE_${CHROME_MAJOR}") && \
    wget -O /tmp/chromedriver.zip \
      "https://storage.googleapis.com/chrome-for-testing-public/${CHROME_FULL}/linux64/chromedriver-linux64.zip" && \
    unzip /tmp/chromedriver.zip -d /tmp/chromedriver_extracted && \
    mv /tmp/chromedriver_extracted/chromedriver-linux64/chromedriver /usr/local/bin/chromedriver && \
    chmod +x /usr/local/bin/chromedriver && \
    rm -rf /tmp/chromedriver.zip /tmp/chromedriver_extracted

# ── Copy web resources and Java sources ──────────────────────────────────────
# Your JSPs, WEB-INF, and all jars (including Apache POI) live under src/main/webapp
COPY src/main/webapp /opt/app/WebContent
COPY src             /opt/app/src

# ── Compile Java source with all jars in WEB-INF/lib on classpath ────────────
RUN SERVLET_JAR=/usr/local/tomcat/lib/servlet-api.jar && \
    LIB_DIR=/opt/app/WebContent/WEB-INF/lib && \
    CP=$(find "$LIB_DIR" -name "*.jar" | tr '\n' ':')${SERVLET_JAR} && \
    mkdir -p /opt/app/WebContent/WEB-INF/classes && \
    find /opt/app/src -name "*.java" | xargs javac -cp "$CP" \
         -d /opt/app/WebContent/WEB-INF/classes

# ── Assemble WAR and deploy to Tomcat ────────────────────────────────────────
RUN cd /opt/app && \
    jar -cf /usr/local/tomcat/webapps/SampleWishApp.war -C WebContent . && \
    mkdir -p /usr/local/tomcat/webapps/SampleWishApp/outputs

EXPOSE 8080
CMD ["/usr/local/tomcat/bin/catalina.sh", "run"]
