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

    def gitUrl              = config.gitUrl
    def branch              = config.branch ?: 'main'
    def imageName           = config.imageName
    def imageTag            = config.imageTag ?: 'latest'
    def containerPort       = config.containerPort ?: '8080'
    def dockerHubCredentials= config.dockerHubCredentials ?: 'dockerhub-credentials'
    def dockerfilePath      = config.dockerfilePath ?: 'Dockerfile'
    def appDirectory        = config.appDirectory ?: '.'

    /* =========================
       Safe derived values
       ========================= */
    def dockerImage   = "${imageName}:${imageTag}"
    def containerName = imageName.replaceAll('/', '-') + "-container"

    pipeline {
        agent any

        environment {
            DOCKER_IMAGE   = dockerImage
            CONTAINER_NAME = containerName
        }

        stages {

            /* =========================
               Clone Application Repo
               ========================= */
            stage('Clone Repository') {
                steps {
                    echo "🔄 Cloning repository: ${gitUrl}"
                    git branch: branch, url: gitUrl
                }
            }

            /* =========================
               Build Docker Image
               ========================= */
            stage('Build Docker Image') {
                steps {
                    echo "🐋 Building Docker image: ${DOCKER_IMAGE}"
                    sh """
                        docker build \
                        -t ${DOCKER_IMAGE} \
                        -f ${dockerfilePath} \
                        ${appDirectory}
                    """
                }
            }

            /* =========================
               Run Docker Container
               ========================= */
            stage('Run Docker Container') {
                steps {
                    echo "🚀 Running Docker container: ${CONTAINER_NAME}"
                    sh """
                        docker rm -f ${CONTAINER_NAME} || true
                        docker run -d \
                          --name ${CONTAINER_NAME} \
                          -p ${containerPort}:${containerPort} \
                          ${DOCKER_IMAGE}
                        sleep 5
                        docker ps | grep ${CONTAINER_NAME}
                    """
                }
            }

            /* =========================
               Test Container
               ========================= */
            stage('Test Container') {
                steps {
                    echo "✅ Verifying container logs"
                    sh """
                        docker logs ${CONTAINER_NAME}
                    """
                }
            }

            /* =========================
               Push Image to Docker Hub
               ========================= */
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
                            docker push ${DOCKER_IMAGE}
                            docker logout
                        """
                    }
                }
            }

            /* =========================
               Cleanup
               ========================= */
            stage('Cleanup Local Container') {
                steps {
                    echo "🧹 Cleaning up container"
                    sh """
                        docker rm -f ${CONTAINER_NAME} || true
                    """
                }
            }
        }

        post {
            success {
                echo "✅ Pipeline completed successfully"
                echo "🐋 Image pushed: ${DOCKER_IMAGE}"
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
