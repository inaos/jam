rm -rf build
mkdir build
cd build
cmake ../c
make
cd ..
rm -f jam-example-lib/src/main/resources/linux-amd64/libjnisample.so
mkdir -p jam-example-lib/src/main/resources/linux-amd64
cp build/libjnisample.so jam-example-lib/src/main/resources/linux-amd64/libjnisample.so
cp build/libexample3.so jam-example-lib/src/main/resources/linux-amd64/libexample3.so
mvn clean install -DskipTests
mvn surefire:test
