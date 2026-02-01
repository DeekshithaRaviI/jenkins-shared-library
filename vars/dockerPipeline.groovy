def call(Map config) {
    pipeline {
        agent any
        
        stages {
            stage('Clone Repository') {
                steps {
                    git branch: 'main', 
                        url: config.gitRepo
                }
            }
            
            stage('Build Docker Image') {
                steps {
                    script {
                        sh """
                            docker build -t ${config.imageName}:${config.imageTag} .
                        """
                    }
                }
            }
            
            stage('Run Docker Container') {
                steps {
                    script {
                        sh """
                            docker rm -f demo_container || true
                            docker run -d -p ${config.containerPort}:${config.containerPort} \
                                --name demo_container ${config.imageName}:${config.imageTag}
                        """
                    }
                }
            }
            
            stage('Push Image to Docker Hub') {
                steps {
                    script {
                        withCredentials([
                            usernamePassword(
                                credentialsId: config.dockerHubCreds,
                                usernameVariable: 'DOCKER_USER',
                                passwordVariable: 'DOCKER_PASS'
                            )
                        ]) {
                            sh """
                                echo "\$DOCKER_PASS" | docker login -u "\$DOCKER_USER" --password-stdin
                                docker tag ${config.imageName}:${config.imageTag} \$DOCKER_USER/${config.imageName}:${config.imageTag}
                                docker push \$DOCKER_USER/${config.imageName}:${config.imageTag}
                                docker logout
                            """
                        }
                    }
                }
            }
        }
        
        post {
            always {
                sh "docker rm -f demo_container || true"
            }
            success {
                echo "✅ Pipeline completed successfully!"
            }
            failure {
                echo "❌ Pipeline failed. Check logs above."
            }
        }
    }
}
