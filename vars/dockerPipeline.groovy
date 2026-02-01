// vars/dockerPipeline.groovy

def call(Map config) {
    // Validate required parameters
    def gitUrl = config.gitUrl ?: error("gitUrl is required")
    def imageName = config.imageName ?: error("imageName is required")
    def imageTag = config.imageTag ?: 'latest'
    def containerPort = config.containerPort ?: '8080'
    def dockerHubCredentials = config.dockerHubCredentials ?: 'dockerhub-credentials'
    def dockerfilePath = config.dockerfilePath ?: 'Dockerfile'
    def appDirectory = config.appDirectory ?: '.'
    
    pipeline {
        agent any
        
        environment {
            DOCKER_IMAGE = "${imageName}:${imageTag}"
            CONTAINER_NAME = "${imageName}-container"
        }
        
        stages {
            stage('Clone Repository') {
                steps {
                    script {
                        echo "🔄 Cloning repository: ${gitUrl}"
                        git branch: config.branch ?: 'main', url: gitUrl
                    }
                }
            }
            
            stage('Build Docker Image') {
                steps {
                    script {
                        echo "🐋 Building Docker image: ${DOCKER_IMAGE}"
                        sh """
                            docker build -t ${DOCKER_IMAGE} -f ${dockerfilePath} ${appDirectory}
                        """
                    }
                }
            }
            
            stage('Run Docker Container') {
                steps {
                    script {
                        echo "🚀 Running Docker container: ${CONTAINER_NAME}"
                        // Stop and remove existing container if exists
                        sh """
                            docker rm -f ${CONTAINER_NAME} || true
                            docker run -d --name ${CONTAINER_NAME} -p ${containerPort}:${containerPort} ${DOCKER_IMAGE}
                            sleep 5
                            docker ps | grep ${CONTAINER_NAME}
                        """
                    }
                }
            }
            
            stage('Test Container') {
                steps {
                    script {
                        echo "✅ Testing container health"
                        sh """
                            docker logs ${CONTAINER_NAME}
                            echo "Container is running successfully!"
                        """
                    }
                }
            }
            
            stage('Push to Docker Hub') {
                steps {
                    script {
                        echo "📤 Pushing image to Docker Hub"
                        withCredentials([usernamePassword(
                            credentialsId: dockerHubCredentials,
                            usernameVariable: 'DOCKER_USER',
                            passwordVariable: 'DOCKER_PASS'
                        )]) {
                            sh """
                                echo \$DOCKER_PASS | docker login -u \$DOCKER_USER --password-stdin
                                docker push ${DOCKER_IMAGE}
                                docker logout
                            """
                        }
                    }
                }
            }
            
            stage('Cleanup Local Container') {
                steps {
                    script {
                        echo "🧹 Cleaning up local test container"
                        sh """
                            docker stop ${CONTAINER_NAME} || true
                            docker rm ${CONTAINER_NAME} || true
                        """
                    }
                }
            }
        }
        
        post {
            success {
                echo "✅ Pipeline completed successfully!"
                echo "🐋 Docker image pushed: ${DOCKER_IMAGE}"
            }
            failure {
                echo "❌ Pipeline failed!"
            }
            always {
                echo "🔚 Pipeline execution finished"
            }
        }
    }
}