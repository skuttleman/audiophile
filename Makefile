
help:
	@echo "Help"
	@echo ""
	@echo "  build     - build an uberjar"
	@echo "  clean     - removes cached artifacts"
	@echo "  deploy    - deploy the uberjar (use build first)"
	@echo "  docker    - builds docker containers and pushes them to docker hub."
	@echo "  help      - display this information"
	@echo "  run       - run the application in dev mode"
	@echo "  run-jar   - compile artifact and run the jar"
	@echo "  run-multi - run the application split into multiple instances of microservices"
	@echo "  run-split - run the application split into microservices"
	@echo "  tests     - run tests"

run:
	bin/exec.sh run

run-jar:
	bin/exec.sh run jar

run-split:
	bin/exec.sh run split

run-multi:
	bin/exec.sh run multi

tests:
	bin/exec.sh test

build:
	bin/exec.sh build

clean:
	bin/exec.sh clean

deploy:
	bin/exec.sh deploy

install:
	bin/exec.sh install

docker:
	bin/exec.sh docker

wipe:
	bin/exec.sh wipe

k8s-logs:
	kubectl -n core logs --prefix -f -lgroup=application --all-containers=true --max-log-requests=22 | grep --color=never -v '/health'
