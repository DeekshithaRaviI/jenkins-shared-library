// vars/dockerPipeline.groovy

def call(Map config) {

    /* =========================
       Validate required inputs
       ========================= */
    if (!config.gitUrl) {
        error "❌ gitUrl is required"
    }
    if (!config.imageName) {
        error "❌ imageName is required"
    }

    def gitUrl               = config.gitUrl
    def branch               = config.branch ?: 'main'
    def imageName            = config.imageName
    def imageTag             = config.imageTag ?: 'latest'
    def containerPort        = config.containerPort ?: '8080'
    def dockerHubCredentials = config.dockerHubCredentials ?: 'dockerhub-credentials'
    def dockerfilePath       = config.dockerfilePath ?: 'Dockerfile'
    def appDirectory         = config.appDirectory ?: '.'

    def dockerImage   = "${imageName}:${imageTag}"
    def containerName = imageName.replaceAll('/', '-') + "-container"

    pipeline {
        agent any

        stages {

            stage('Clone Repository') {
                steps {
                    echo "🔄 Cloning repository: ${gitUrl}"
                    git branch: branch, url: gitUrl
                }
            }

            stage('Build Docker Image') {
                steps {
                    echo "🐋 Building Docker image: ${dockerImage}"
                    sh """
                        docker build \
                          -t ${dockerImage} \
                          -f ${dockerfilePath} \
                          ${appDirectory}
                    """
                }
            }

            stage('Run Docker Container') {
                steps {
                    echo "🚀 Running Docker container: ${containerName}"
                    sh """
                        docker rm -f ${containerName} || true
                        docker run -d \
                          --name ${containerName} \
                          -p ${containerPort}:${containerPort} \
                          ${dockerImage}
                        sleep 5
                        docker ps | grep ${containerName}
                    """
                }
            }

            stage('Test Container') {
                steps {
                    echo "✅ Testing container"
                    sh """
                        docker logs ${containerName}
                    """
                }
            }

            stage('Push to Docker Hub') {
                steps {
                    echo "📤 Pushing image to Docker Hub"
                    withCredentials([usernamePassword(
                        credentialsId: dockerHubCredentials,
                        usernameVariable: 'DOCKER_USER',
                        passwordVariable: 'DOCKER_PASS'
                    )]) {
                        sh """
                            echo \$DOCKER_PASS | docker login -u \$DOCKER_USER --password-stdin
                            docker push ${dockerImage}
                            docker logout
                        """
                    }
                }
            }

            stage('Cleanup Local Container') {
                steps {
                    echo "🧹 Cleaning up container"
                    sh """
                        docker rm -f ${containerName} || true
                    """
                }
            }
        }

        post {
            success {
                echo "✅ Pipeline completed successfully"
                echo "🐋 Image pushed: ${dockerImage}"
            }
            failure {
                echo "❌ Pipeline failed"
            }
            always {
                echo "🔚 Pipeline execution finished"
            }
        }
    }
}
