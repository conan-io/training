git clone https://github.com/solvingj/training.git
cd training/docker_environment
docker-compose --env-file .ci.env -f docker-compose-ci.yml -f docker-compose.yml up -d 
docker exec -it conan-training-ci bash 
# can re-run above command from new shell if disconnected
