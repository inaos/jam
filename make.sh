rm -rf build
mkdir build
cd build
cmake ../c
make
cd ..
rm -f inaos-example-lib/src/main/resources/linux-amd64/libjnisample.so
mkdir -p inaos-example-lib/src/main/resources/linux-amd64
cp build/libjnisample.so inaos-example-lib/src/main/resources/linux-amd64/libjnisample.so
mvn clean package -DskipTests
