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

        stage('Build Maven') {
            steps {
                sh 'chmod +x mvnw'
                sh './mvnw clean package -DskipTests'
            }
        }

        stage('Stop Existing Containers') {
            steps {
                sh 'docker-compose down'
            }
        }

        stage('Build Docker Image') {
            steps {
                sh 'docker-compose build --no-cache'
            }
        }

        stage('Deploy') {
            steps {
                withCredentials([
                    string(credentialsId: 'finnhub-api-key', variable: 'FINNHUB_API_KEY'),
                    string(credentialsId: 'twelvedata-api-key', variable: 'TWELVE_DATA_API_KEY')
                ]) {
                    sh 'docker-compose up -d'
                }
            }
        }

        stage('Verify') {
            steps {
                sh 'docker ps'
            }
        }
    }
}