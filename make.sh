rm -rf build
mkdir build
cd build
cmake ../c
make
cd ..
rm -f inaos-example-lib/src/main/resources/linux-amd64/libjnisample.so
mkdir -p inaos-example-lib/src/main/resources/linux-amd64
cp build/libjnisample.so inaos-example-lib/src/main/resources/linux-amd64/libjnisample.so
cp build/libexample3.so inaos-example-lib/src/main/resources/linux-amd64/libexample3.so
mvn clean install -DskipTests
mvn surefire:test
