git clone https://github.com/solvingj/training.git
cd training/docker_environment
docker-compose --env-file .cicd.env -f docker-compose-cicd.yml -f docker-compose.yml up -d 
docker exec -it conan-training-cicd bash 
# can re-run above command from new shell if disconnected
