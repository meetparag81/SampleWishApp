FROM tomcat:9.0-jdk11

# Install Chrome + dependencies
RUN apt-get update && apt-get install -y \
    wget gnupg unzip \
 && wget -q -O - https://dl-ssl.google.com/linux/linux_signing_key.pub | apt-key add - \
 && echo "deb [arch=amd64] http://dl.google.com/linux/chrome/deb/ stable main" \
      > /etc/apt/sources.list.d/google-chrome.list \
 && apt-get update && apt-get install -y google-chrome-stable \
 && rm -rf /var/lib/apt/lists/*

# Install ChromeDriver
RUN CHROME_VERSION=$(google-chrome --version | awk '{print $3}' | cut -d. -f1) && \
    wget -O /tmp/chromedriver.zip \
      "https://chromedriver.storage.googleapis.com/${CHROME_VERSION}.0/chromedriver_linux64.zip" || \
      wget -O /tmp/chromedriver.zip \
      "https://chromedriver.storage.googleapis.com/114.0.5735.90/chromedriver_linux64.zip" && \
    unzip /tmp/chromedriver.zip -d /usr/local/bin && \
    chmod +x /usr/local/bin/chromedriver && \
    rm /tmp/chromedriver.zip

# Deploy your webapp
COPY SampleWishApp.war /usr/local/tomcat/webapps/SampleWishApp.war

# Optional: put tests + libs into image so Actions can run them
# Make sure these folders exist next to the Dockerfile
COPY src /opt/app/src
WORKDIR /opt/app

EXPOSE 8080

CMD ["/usr/local/tomcat/bin/catalina.sh", "run"]
# Now you can build and run this Docker image, and it will have Tomcat with your webapp,