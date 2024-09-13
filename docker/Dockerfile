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

# Add Docker’s official GPG key and set up the Docker CE repository for Debian
RUN curl -fsSL https://download.docker.com/linux/debian/gpg | apt-key add - && \
    add-apt-repository \
    "deb [arch=$(dpkg --print-architecture)] https://download.docker.com/linux/debian $(lsb_release -cs) stable"

# Install Docker CE (Community Edition)
RUN apt-get update && \
    apt-get install -y docker-ce docker-ce-cli containerd.io && \
    usermod -aG docker jenkins

RUN docker buildx version

ENV DOCKER_CLI_EXPERIMENTAL=enabled

# Install AWS CLI v2 for ARM64
RUN apt-get install -y unzip && \
    curl "https://awscli.amazonaws.com/awscli-exe-linux-aarch64.zip" -o "awscliv2.zip" && \
    unzip awscliv2.zip && \
    ./aws/install --update && \
    rm -rf awscliv2.zip aws

# Install Terraform
RUN curl -o terraform.zip https://releases.hashicorp.com/terraform/1.5.0/terraform_1.5.0_linux_amd64.zip && \
    unzip terraform.zip && \
    mv terraform /usr/local/bin/ && \
    terraform -version && \
    rm terraform.zip

# Switch back to Jenkins user
USER jenkins

# Define plugins to be installed via jenkins-plugin-cli
COPY plugins.txt /usr/share/jenkins/ref/plugins.txt

# Install Jenkins plugins
RUN jenkins-plugin-cli --plugin-file /usr/share/jenkins/ref/plugins.txt

# Expose Jenkins port
EXPOSE 8080
EXPOSE 50000
