run:
    gradle runServer

format:
    ktlint --format "src/**/*.kt"

build:
    gradle shadowJar
