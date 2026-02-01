// Jenkins Shared Library - Docker Pipeline
// File: vars/dockerPipeline.groovy
// Purpose: Reusable CI/CD pipeline for Docker applications

def call(Map config) {
    pipeline {
        agent any
        
        environment {
            IMAGE_NAME = "${config.imageName}"
            IMAGE_TAG = "${config.imageTag}"
            CONTAINER_NAME = "demo_container"
        }
        
        stages {
            stage('Clone Repository') {
                steps {
                    script {
                        echo "📥 Cloning repository: ${config.gitRepo}"
                        git branch: 'main', url: config.gitRepo
                        echo "✅ Repository cloned successfully"
                    }
                }
            }
            
            stage('Build Docker Image') {
                steps {
                    script {
                        echo "🔨 Building Docker image: ${IMAGE_NAME}:${IMAGE_TAG}"
                        sh """
                            docker build -t ${IMAGE_NAME}:${IMAGE_TAG} .
                        """
                        echo "✅ Docker image built successfully"
                    }
                }
            }
            
            stage('Run Docker Container') {
                steps {
                    script {
                        echo "🚀 Running Docker container on port ${config.containerPort}"
                        // Remove existing container if exists
                        sh """
                            docker rm -f ${CONTAINER_NAME} 2>/dev/null || true
                            docker run -d -p ${config.containerPort}:${config.containerPort} \
                                --name ${CONTAINER_NAME} ${IMAGE_NAME}:${IMAGE_TAG}
                        """
                        echo "✅ Container started successfully"
                        
                        // Wait for container to be healthy
                        sleep(time: 5, unit: 'SECONDS')
                        
                        // Verify container is running
                        sh """
                            docker ps | grep ${CONTAINER_NAME}
                        """
                    }
                }
            }
            
            stage('Push Image to Docker Hub') {
                steps {
                    script {
                        echo "📤 Pushing image to Docker Hub"
                        withCredentials([
                            usernamePassword(
                                credentialsId: config.dockerHubCreds,
                                usernameVariable: 'DOCKER_USER',
                                passwordVariable: 'DOCKER_PASS'
                            )
                        ]) {
                            sh """
                                # Login to Docker Hub
                                echo "\$DOCKER_PASS" | docker login -u "\$DOCKER_USER" --password-stdin
                                
                                # Tag image with Docker Hub username
                                docker tag ${IMAGE_NAME}:${IMAGE_TAG} \$DOCKER_USER/${IMAGE_NAME}:${IMAGE_TAG}
                                
                                # Push to Docker Hub
                                docker push \$DOCKER_USER/${IMAGE_NAME}:${IMAGE_TAG}
                                
                                # Logout for security
                                docker logout
                            """
                        }
                        echo "✅ Image pushed to Docker Hub successfully"
                    }
                }
            }
        }
        
        post {
            always {
                script {
                    echo "🧹 Cleaning up..."
                    sh "docker rm -f ${CONTAINER_NAME} 2>/dev/null || true"
                }
            }
            success {
                echo """
                ========================================
                ✅ PIPELINE COMPLETED SUCCESSFULLY! ✅
                ========================================
                Image: ${IMAGE_NAME}:${IMAGE_TAG}
                Container: ${CONTAINER_NAME}
                Port: ${config.containerPort}
                ========================================
                """
            }
            failure {
                echo """
                ========================================
                ❌ PIPELINE FAILED! ❌
                ========================================
                Please check the logs above for errors.
                ========================================
                """
            }
        }
    }
}