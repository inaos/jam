rm -rf build
mkdir build
cd build
cmake ../c
make
cd ..
rm -f inaos-example-lib/src/main/resources/libjnisample.so
cp build/libjnisample.so inaos-example-lib/src/main/resources/libjnisample.so
mvn clean package -DskipTests
