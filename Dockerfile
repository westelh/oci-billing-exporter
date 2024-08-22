FROM gcr.io/distroless/java17-debian12
LABEL maintainer=westelh
WORKDIR /app
COPY build/libs libs/
COPY build/resources resources/
COPY build/classes classes/
ENTRYPOINT ["java", "-cp", "/app/resources:/app/classes:/app/libs/*", "dev.westelh.obe.Main"]
EXPOSE 2112
