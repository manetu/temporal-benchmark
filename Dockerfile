#
# Copyright Manetu Inc. All Rights Reserved.
#
#-------------------------------------------------------------------------------
# Build
#-------------------------------------------------------------------------------
FROM manetu/unified-builder:v3.0 as builder

COPY Makefile project.clj /src/
COPY src /src/src

RUN cd /src && make clean bin

#-------------------------------------------------------------------------------
# Runtime
#-------------------------------------------------------------------------------
FROM manetu/unified-builder:v3.0-jre as runtime

COPY --from=builder /src/target/temporal-benchmark /usr/local/bin

ENTRYPOINT ["java", "-jar", "/usr/local/bin/temporal-benchmark"]
