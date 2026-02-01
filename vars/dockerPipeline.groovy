def call(Map config) {

    pipeline {
        agent any

        stages {

            stage('Build Docker Image') {
                steps {
                    sh """
                      docker build -t ${config.imageName}:${config.imageTag} .
                    """
                }
            }

            stage('Run Docker Container') {
                steps {
                    sh """
                      docker run -d -p ${config.containerPort}:${config.containerPort} \
                      --name demo_container ${config.imageName}:${config.imageTag} || true
                    """
                }
            }

            stage('Push Image to Docker Hub') {
                steps {
                    withCredentials([
                        usernamePassword(
                            credentialsId: config.dockerHubCreds,
                            usernameVariable: 'DOCKER_USER',
                            passwordVariable: 'DOCKER_PASS'
                        )
                    ]) {
                        sh """
                          echo $DOCKER_PASS | docker login -u $DOCKER_USER --password-stdin
                          docker tag ${config.imageName}:${config.imageTag} $DOCKER_USER/${config.imageName}:${config.imageTag}
                          docker push $DOCKER_USER/${config.imageName}:${config.imageTag}
                        """
                    }
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
