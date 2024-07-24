# Spring WebFlux webserver for RAG application
This project works with https://github.com/sebampuero/langchain_rag_python_example and https://github.com/sebampuero/ragbot_chat_react

## Prerequisites
Latest version of Docker installed

## Installation
1. Build the Docker image.
2. Build the Docker image provided in the https://github.com/sebampuero/langchain_rag_python_example repository.
3. Use the following docker-compose.yml:

```yaml
services:
  springwebflux:
    container_name: springwebflux
    image: my-app:latest
    restart: unless-stopped
    ports:
      - 0.0.0.0:8080:8080
    environment:
      - API_URL=http://lcpython:8000/prompt

  lcpython:
    container_name: lcpython
    image: lcpython:latest
    restart: unless-stopped
    environment:
      - OPENAI_API_KEY=<>
      - CHAT_MODEL=<>
      - LOCATION=<>
      - PROJECT=<>
      - AWS_ACCESS_KEY_ID=<>
      - AWS_SECRET_ACCESS_KEY=<>
      - AWS_DEFAULT_REGION=<>
```

4. Start the docker containers.
5. Build the react webapp and serve the static files using a webserver (NGINX, Apache)
6. Enjoy