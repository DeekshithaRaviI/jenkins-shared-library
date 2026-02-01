def call(Map config) {

    pipeline {
        agent any

        environment {
            IMAGE_NAME = config.imageName
            IMAGE_TAG  = config.imageTag
            DOCKERHUB  = credentials(config.dockerHubCreds)
        }

        stages {

            stage('Clone Repository') {
                steps {
                    git config.gitRepo
                }
            }

            stage('Build Docker Image') {
                steps {
                    sh """
                      docker build -t ${IMAGE_NAME}:${IMAGE_TAG} .
                    """
                }
            }

            stage('Run Docker Container') {
                steps {
                    sh """
                      docker run -d -p ${config.containerPort}:${config.containerPort} \
                      --name demo_container ${IMAGE_NAME}:${IMAGE_TAG} || true
                    """
                }
            }

            stage('Push Image to Docker Hub') {
                steps {
                    sh """
                      echo ${DOCKERHUB_PSW} | docker login -u ${DOCKERHUB_USR} --password-stdin
                      docker tag ${IMAGE_NAME}:${IMAGE_TAG} ${DOCKERHUB_USR}/${IMAGE_NAME}:${IMAGE_TAG}
                      docker push ${DOCKERHUB_USR}/${IMAGE_NAME}:${IMAGE_TAG}
                    """
                }
            }
        }

        post {
            always {
                sh "docker rm -f demo_container || true"
            }
        }
    }
}
