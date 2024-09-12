pipeline {
    agent any

    environment {
        AWS_DEFAULT_REGION = 'eu-central-1'
        AWS_SECRET_ACCESS_KEY = credentials('aws_secret_key')
        AWS_ACCESS_KEY_ID = credentials('aws_key_id')
        ACCOUNT_ID = '698059809282'
        ECR_REPO = "${ACCOUNT_ID}.dkr.ecr.${AWS_DEFAULT_REGION}.amazonaws.com/my-ecr-repo"
        ECS_CLUSTER = 'my-ecs-cluster'
        ECS_SERVICE = 'my-ecs-service'
        DOCKER_IMAGE = "${ECR_REPO}:latest"
    }

    stages {
        stage('Checkout Code from SCM') {
            steps {
                checkout scm  // Automatically checks out code from the repository defined in Jenkins job configuration
            }
        }

        stage('Build Service') {
            steps {
                script {
                    sh './gradlew clean build'
                }
            }
        }

        stage('Test Service') {
            steps {
                script {
                    sh './gradlew test'
                }
            }
        }

        stage('Terraform Init and Plan') {
            steps {
                script {
                    dir('infra/tf') {
                        sh '''
                        terraform init
                        terraform plan -out=tfplan
                        '''
                    }
                }
            }
        }

        stage('Review Plan') {
            steps {
                input message: 'Please review the Terraform plan. Do you want to apply the changes?', ok: 'Apply Changes'
            }
        }

        stage('Terraform Apply') {
            steps {
                script {
                    dir('infra/tf') {
                        sh '''
                        terraform apply -auto-approve tfplan
                        '''
                    }
                }
            }
        }

        stage('Login to AWS ECR') {
            steps {
                sh '''
                docker logout ${ECR_REPO} || true
                aws ecr get-login-password --region ${AWS_DEFAULT_REGION} | docker login --username AWS --password-stdin ${ECR_REPO}
                '''
            }
        }

        stage('Build and Push Multi-Platform Docker Image') {
            steps {
                sh '''
                docker buildx create --use || true
                docker buildx inspect --bootstrap || true
                docker buildx build --platform linux/amd64,linux/arm64 -t ${DOCKER_IMAGE} --push .
                '''
            }
        }

        stage('Force ECS Service Deployment') {
            steps {
                timeout(time: 15, unit: "MINUTES") {
                     input message: 'Do you want to approve the deployment?', ok: 'Yes'
                }

                echo "Initiating deployment"
                sh '''
                aws ecs update-service --cluster ${ECS_CLUSTER} --service ${ECS_SERVICE} --force-new-deployment
                '''
            }
        }
    }

    post {
        always {
            echo 'Cleaning up Docker login...'
            sh 'docker logout ${ECR_REPO} || true'
        }
        success {
            echo 'Deployment successful!'
        }
        failure {
            echo 'Deployment failed. Please check the logs.'
        }
    }
}
