# Start from the official Jenkins image
FROM jenkins/jenkins:lts

# Switch to root user to install Docker and AWS CLI
USER root

# Install required packages for Docker and AWS CLI
RUN apt-get update && \
    apt-get install -y \
    apt-transport-https \
    ca-certificates \
    curl \
    gnupg-agent \
    software-properties-common lsb-release

# Add Docker’s official GPG key and set up the repository for Debian
RUN curl -fsSL https://download.docker.com/linux/debian/gpg | apt-key add - && \
    add-apt-repository \
    "deb [arch=amd64] https://download.docker.com/linux/debian $(lsb_release -cs) stable"

# Install Docker (use docker.io if docker-ce is unavailable)
RUN apt-get update && \
    apt-get install -y docker.io && \
    usermod -aG docker jenkins

# Install AWS CLI v2
RUN apt-get install -y unzip && \
    curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip" && \
    unzip awscliv2.zip && \
    ./aws/install --update && \
    rm -rf awscliv2.zip aws

# Switch back to Jenkins user
USER jenkins

# Define plugins to be installed via jenkins-plugin-cli
COPY plugins.txt /usr/share/jenkins/ref/plugins.txt

# Install Jenkins plugins
RUN jenkins-plugin-cli --plugin-file /usr/share/jenkins/ref/plugins.txt

# Expose Jenkins port
EXPOSE 8080
EXPOSE 50000
