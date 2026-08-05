pipeline {

    agent any

    environment {
        GIT_URL = 'https://github.com/Neueda-Learning/107-NaNOfYourBusiness-PortfolioManagementSystem.git'
        BRANCH = 'main'
    }

    stages {

        stage('Checkout Source') {
            steps {
                git branch: "${BRANCH}",
                    url: "${GIT_URL}"
            }
        }

        stage('Stop Existing Containers') {
            steps {
                sh 'docker-compose down || true'
            }
        }

        stage('Build Docker Image') {
            steps {
                sh 'docker-compose build --no-cache'
            }
        }

        stage('Deploy') {
            steps {
                sh 'docker-compose up -d'
            }
        }

        stage('Verify') {
            steps {
                sh 'docker ps'
            }
        }
    }
}